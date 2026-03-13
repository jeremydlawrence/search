package org.example.search.util;

import org.apache.commons.lang3.tuple.Pair;
import org.example.search.dto.ProductField;
import org.example.search.dto.QueryOperator;
import org.example.search.dto.QueryType;
import org.example.search.dto.SearchFilter;
import org.example.search.dto.SearchSpec;
import org.example.search.dto.SortSpec;
import org.example.search.dto.SortType;
import org.example.search.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductSearchFormer {
    private static final Integer FROM_DEFAULT = 0;
    private static final Integer SIZE_DEFAULT = 10;

    private final String productIndexName;
    private final EmbeddingService embeddingService;

    @Autowired
    public ProductSearchFormer(
            @Value("${open-search.product.index-name}")
            final String productIndexName,
            final EmbeddingService embeddingService) {
        this.productIndexName = productIndexName;
        this.embeddingService = embeddingService;
    }

    public SearchSpec formSearch(
            final String query,
            final Integer from,
            final Integer size,
            final String brand,
            final String category,
            final SortType sortType,
            final List<String> fields) {
        final String normalizedQuery = normalizeQuery(query);
        final List<SearchFilter> filters = new ArrayList<>();
        final SortSpec.SortSpecBuilder sortSpec = SortSpec.builder();
        final SearchSpec.SearchSpecBuilder searchSpec = SearchSpec.builder()
                .index(productIndexName)
                .from(validateFrom(from))
                .size(validateSize(size))
                .fields(validateFields(fields));

        // Query string based recall
        final SearchFilter.SearchFilterBuilder lexicalFtsBuilder = SearchFilter.builder()
                .fieldName(ProductField.FTS.value())
                .fieldValue(normalizedQuery)
                .queryOperator(QueryOperator.AND);
        final SearchFilter.SearchFilterBuilder titleBoostBuilder = SearchFilter.builder()
                .fieldName(ProductField.TITLE.value())
                .fieldValue(normalizedQuery)
                .queryType(QueryType.SHOULD_MATCH)
                .boost(3.0f);
        switch (sortType) {
            case RELEVANCE, LEXICAL -> {
                filters.add(lexicalFtsBuilder
                        .queryType(QueryType.MUST_MATCH)
                        .build());
                filters.add(titleBoostBuilder.build());
                sortSpec.sort(Pair.of("_score", "desc"));
            }
            case SEMANTIC -> {
                final List<Float> queryVectors = embeddingService.getEmbeddings(List.of(normalizedQuery)).getFirst();
                filters.add(SearchFilter.builder()
                        .fieldName(ProductField.FTS_EMBEDDING.value())
                        .fieldValue(queryVectors)
                        .queryType(QueryType.KNN_MATCH)
                        .build());
                filters.add(titleBoostBuilder.build());
                sortSpec.sort(Pair.of("_score", "desc"));
            }
            case HYBRID -> {
                searchSpec.hybrid(true);
                searchSpec.pipeline("hybrid_search_pipeline");

                // lexical match
                filters.add(lexicalFtsBuilder
                        .queryType(QueryType.MUST_MATCH)
                        .build());
                filters.add(titleBoostBuilder.build());

                // vector match
                final List<Float> queryVectors = embeddingService.getEmbeddings(List.of(normalizedQuery)).getFirst();
                filters.add(SearchFilter.builder()
                        .fieldName(ProductField.FTS_EMBEDDING.value())
                        .fieldValue(queryVectors)
                        .queryType(QueryType.KNN_MATCH)
                        .build());
                sortSpec.sort(Pair.of("_score", "desc"));
            }
            case PRICE_ASCENDING, PRICE_DESCENDING -> {
                filters.add(lexicalFtsBuilder
                        .queryType(QueryType.FILTER_MATCH)
                        .build());
                final String order = SortType.PRICE_DESCENDING.equals(sortType) ? "desc" : "asc";
                sortSpec.sort(Pair.of(ProductField.PRICE.value(), order));
            }
        }

        // Field based filters
        if (brand != null) {
            filters.add(SearchFilter.builder()
                    .fieldName(ProductField.BRAND_KEYWORD.value())
                    .fieldValue(brand)
                    .queryType(QueryType.FILTER_TERM)
                    .build());
        }
        if (category != null) {
            filters.add(SearchFilter.builder()
                    .fieldName(ProductField.CATEGORY_KEYWORD.value())
                    .fieldValue(category)
                    .queryType(QueryType.FILTER_TERM)
                    .build());
        }

        return searchSpec
                .filters(filters)
                .sort(sortSpec.build())
                .build();
    }

    protected Integer validateFrom(final Integer from) {
        final boolean isValid = from != null && from >= FROM_DEFAULT;
        return isValid ? from : FROM_DEFAULT;
    }

    protected Integer validateSize(final Integer size) {
        final boolean isValid = size != null && size > 0;
        return isValid ? size : SIZE_DEFAULT;
    }

    protected List<String> validateFields(final List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            return List.of("id");
        }

        List<String> validFields = fields.stream()
                .filter(field -> {
                    for (ProductField pf : ProductField.values()) {
                        if (pf.value().equals(field)) {
                            return true;
                        }
                    }
                    return false;
                })
                .toList();

        return validFields.isEmpty() ? List.of("id") : validFields;
    }

    protected static String normalizeQuery(final String query) {
        if (query == null || query.isEmpty()) {
            return query;
        }

        return collapseWhitespace(query.trim().toLowerCase());
    }

    protected static String collapseWhitespace(String str) {
        if (str == null || str.isEmpty()) return str;

        StringBuilder sb = new StringBuilder(str.length());
        boolean lastWasWhitespace = false;

        // Trim manually or call str.trim() first if needed
        String trimmed = str.trim();

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isWhitespace(c)) {
                if (!lastWasWhitespace) {
                    sb.append(' ');
                    lastWasWhitespace = true;
                }
            } else {
                sb.append(c);
                lastWasWhitespace = false;
            }
        }
        return sb.toString();
    }
}
