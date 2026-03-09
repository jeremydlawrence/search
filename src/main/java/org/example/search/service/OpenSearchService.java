package org.example.search.service;

import org.apache.commons.lang3.tuple.Pair;
import org.example.search.dto.QueryOperator;
import org.example.search.dto.QueryType;
import org.example.search.dto.SearchFilter;
import org.example.search.dto.SearchResult;
import org.example.search.dto.SearchSpec;
import org.example.search.model.IndexableDocument;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.HybridQuery;
import org.opensearch.client.opensearch._types.query_dsl.KnnQuery;
import org.opensearch.client.opensearch._types.query_dsl.Operator;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class OpenSearchService implements SearchService {
    private static final Logger logger = LoggerFactory.getLogger(OpenSearchService.class);

    private final OpenSearchClient client;

    @Autowired
    public OpenSearchService(final OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public String clusterHealth() {
        try {
            final HealthStatus health = client.cluster().health().status();
            return health.toString();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public <T extends IndexableDocument> SearchResult<T> search(SearchSpec searchSpec, Class<T> clazz) throws IOException {
        // Validate SearchSpec
        if (searchSpec == null) {
            throw new IllegalArgumentException("searchSpec cannot be null");
        }
        if (searchSpec.isHybrid() && searchSpec.getPipeline() == null) {
            throw new IllegalArgumentException("searchSpec.pipeline cannot be null if performing a hybrid search");
        }

        // Build query
        final Query finalQuery = searchSpec.isHybrid()
                ? buildHybridQuery(searchSpec)
                : buildFlatQuery(searchSpec);

        // Build sorts
        final List<SortOptions> sorts = getSortOptions(searchSpec);

        // Build search request
        final SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                .index(searchSpec.getIndex())
                .from(searchSpec.getFrom())
                .size(searchSpec.getSize())
                .query(finalQuery)
                .sort(sorts);
        if (searchSpec.getPipeline() != null) {
            requestBuilder.pipeline(searchSpec.getPipeline());
        }
        if (searchSpec.getFields() != null && !searchSpec.getFields().isEmpty()) {
            requestBuilder.source(SourceConfig.of(s -> s.filter(
                    sf -> sf.includes(searchSpec.getFields()))));
        }
        final SearchRequest searchRequest = requestBuilder.build();

        // Make search call
        logger.info("Search query: {}", searchRequest.toJsonString());
        final long start = System.currentTimeMillis();
        final SearchResponse<T> result = client.search(searchRequest, clazz);
        final long roundTripTime = System.currentTimeMillis() - start;
        logger.info("Search took: {}ms", result.took());

        // Compile response object
        return SearchResult.<T>builder()
                .start(searchSpec.getFrom())
                .total(result.hits().total().value())
                .took(roundTripTime)
                .results(result.hits().hits().stream()
                        .map(Hit::source)
                        .toList())
                .build();
    }

    protected Query buildFlatQuery(final SearchSpec searchSpec) {
        final BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        searchSpec.getFilters().forEach(filter -> {
            // Collect scored queries
            if (filter.getQueryType().equals(QueryType.MUST_MATCH)) {
                boolQuery.must(getMatchQuery(filter));
            }
            if (filter.getQueryType().equals(QueryType.KNN_MATCH)) {
                boolQuery.must(getKnnQuery(filter).toQuery());
            }
            if (filter.getQueryType().equals(QueryType.SHOULD_MATCH)) {
                boolQuery.should(getMatchQuery(filter));
            }

            // Collect filter only queries
            else if (filter.getQueryType().equals(QueryType.FILTER_TERM)) {
                boolQuery.filter(getTermQuery(filter));
            }
            else if (filter.getQueryType().equals(QueryType.FILTER_MATCH)) {
                boolQuery.filter(getMatchQuery(filter));
            }
        });
        return Query.of(q -> q.bool(boolQuery.build()));
    }

    protected Query buildHybridQuery(final SearchSpec searchSpec) {
        // Extract two main filters
        final SearchFilter lexicalFilter = searchSpec.getFilters().stream()
                .filter(filter -> filter.getQueryType().equals(QueryType.MUST_MATCH))
                .findFirst()
                .orElse(null);
        final SearchFilter knnFilter = searchSpec.getFilters().stream()
                .filter(filter -> filter.getQueryType().equals(QueryType.KNN_MATCH))
                .findFirst()
                .orElse(null);

        // Require both filters for hybrid search
        if (knnFilter == null || lexicalFilter == null) {
            throw new IllegalArgumentException("Hybrid search requires both a MUST_MATCH and a KNN_MATCH SearchFilter");
        }

        // Build query
        final HybridQuery.Builder hybridQuery = new HybridQuery.Builder();
        final BoolQuery.Builder lexicalQuery = new BoolQuery.Builder();
        final KnnQuery.Builder knnQuery = getKnnQuery(knnFilter).toBuilder();

        lexicalQuery.must(getMatchQuery(lexicalFilter));

        searchSpec.getFilters().forEach(filter -> {
            // Collect boost queries
            if (filter.getQueryType().equals(QueryType.SHOULD_MATCH)) {
                lexicalQuery.should(getMatchQuery(filter));
            }

            // Collect filter only queries
            else if (filter.getQueryType().equals(QueryType.FILTER_TERM)) {
                final Query termQuery = getTermQuery(filter);
                lexicalQuery.filter(termQuery);
                knnQuery.filter(termQuery);
            }
        });

        return HybridQuery.of(h -> h
                .queries(Arrays.asList(lexicalQuery.build().toQuery(), knnQuery.build().toQuery())))
                .toQuery();
    }

    protected Query getMatchQuery(final SearchFilter filter) {
        final Operator operator = QueryOperator.AND.equals(filter.getQueryOperator())
                ? Operator.And
                : null;
        final Float boost = filter.getBoost() != null
                ? filter.getBoost()
                : 1.0f;
        return Query.of(qb -> qb.match(mq -> mq
                .field(filter.getFieldName())
                .query(getFieldValue(filter.getFieldValue()))
                .operator(operator)
                .boost(boost)));
    }

    protected KnnQuery getKnnQuery(final SearchFilter filter) {
        return KnnQuery.of(k -> k
                    .field(filter.getFieldName())
                    .vector(getVectorValue(filter.getFieldValue()))
                    .k(10));
    }

    protected Query getTermQuery(final SearchFilter filter) {
        return Query.of(f -> f.term(t -> t
                .field(filter.getFieldName())
                .value(getFieldValue(filter.getFieldValue()))));
    }

    protected List<SortOptions> getSortOptions(SearchSpec searchSpec) {
        final List<SortOptions> sorts = new ArrayList<>();
        if (searchSpec.getSort() != null) {
        final List<Pair<String, String>> specSorts = searchSpec.getSort().getSorts();
        if (specSorts != null && !specSorts.isEmpty()) {
            specSorts.forEach(specSort -> {
                final SortOrder sortOrder = SortOrder.Desc.jsonValue().equals(specSort.getRight())
                        ? SortOrder.Desc
                        : SortOrder.Asc;
                sorts.add(new SortOptions.Builder()
                        .field(f -> f
                                .field(specSort.getLeft())
                                .order(sortOrder))
                        .build());
            });
        }
        }
        return sorts;
    }

    protected FieldValue getFieldValue(Object value) {
        if (value instanceof String) {
            return FieldValue.of((String) value);
        }

        throw new IllegalArgumentException("Value type not supported: " + value.getClass());
    }

    protected List<Float> getVectorValue(Object value) {
        if (value instanceof List<?>) {
            return (List<Float>) value;
        }

        throw new IllegalArgumentException("Value type not supported: " + value.getClass());
    }
}
