package org.example.search.service;

import org.example.search.dto.QueryType;
import org.example.search.dto.SearchResult;
import org.example.search.dto.SearchSpec;
import org.example.search.model.IndexableDocument;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Operator;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

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
        final BoolQuery.Builder finalQuery = new BoolQuery.Builder();

        searchSpec.getFilters().forEach(filter -> {
            // Collect scored queries
            if (filter.getQueryType().equals(QueryType.MATCH)) {
                finalQuery.must(m -> m.match(
                        mq -> mq
                                .field(filter.getFieldName())
                                .query(getFieldValue(filter.getFieldValue()))
                                .operator(Operator.And)
                ));
            }

            // Collect filter only queries
            else if (filter.getQueryType().equals(QueryType.TERM)) {
                finalQuery.filter(f -> f.term(t -> t
                        .field(filter.getFieldName())
                        .value(getFieldValue(filter.getFieldValue()))
                ));
            }
        });

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(searchSpec.getIndex())
                .query(Query.of(q -> q.bool(finalQuery.build())))
                .from(searchSpec.getFrom())
                .size(searchSpec.getSize())
                .build();
        SearchResponse<T> result = client.search(searchRequest, clazz);

        logger.info("Search query: {}", searchRequest.toJsonString());

        return SearchResult.<T>builder()
                .start(searchSpec.getFrom())
                .total(result.hits().total().value())
                .took(result.took())
                .results(result.hits().hits().stream()
                        .map(Hit::source)
                        .toList())
                .build();
    }

    private FieldValue getFieldValue(Object fieldName) {
        if (fieldName instanceof String) {
            return FieldValue.of((String) fieldName);
        }

        throw new IllegalArgumentException("fieldName type not supported: " + fieldName.getClass());
    }
}
