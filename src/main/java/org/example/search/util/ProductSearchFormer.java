package org.example.search.util;

import org.example.search.dto.ProductField;
import org.example.search.dto.QueryOperator;
import org.example.search.dto.QueryType;
import org.example.search.dto.SearchFilter;
import org.example.search.dto.SearchSpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductSearchFormer {
    private static final Integer FROM_DEFAULT = 0;
    private static final Integer SIZE_DEFAULT = 10;

    private final String productIndexName;

    public ProductSearchFormer(
            @Value("${open-search.product.index-name}")
            final String productIndexName) {
        this.productIndexName = productIndexName;
    }

    public SearchSpec formSearch(
            final String query,
            final Integer from,
            final Integer size,
            final String brand,
            final String category) {
        final List<SearchFilter> filters = new ArrayList<>();
        final SearchSpec.SearchSpecBuilder searchSpec = SearchSpec.builder()
                .index(productIndexName)
                .from(validateFrom(from))
                .size(validateSize(size));

        // FTS query match
        filters.add(SearchFilter.builder()
                .fieldName(ProductField.FTS.value())
                .fieldValue(query)
                .queryType(QueryType.MUST_MATCH)
                .queryOperator(QueryOperator.AND)
                .build());
        filters.add(SearchFilter.builder()
                .fieldName(ProductField.TITLE.value())
                .fieldValue(query)
                .queryType(QueryType.SHOULD_MATCH)
                .boost(3.0f)
                .build());

        // Field based filters
        if (brand != null) {
            filters.add(SearchFilter.builder()
                    .fieldName(ProductField.BRAND.value())
                    .fieldValue(brand)
                    .queryType(QueryType.TERM)
                    .build());
        }
        if (category != null) {
            filters.add(SearchFilter.builder()
                    .fieldName(ProductField.CATEGORY.value())
                    .fieldValue(category)
                    .queryType(QueryType.TERM)
                    .build());
        }

        return searchSpec
                .filters(filters)
                .build();
    }

    private Integer validateFrom(final Integer from) {
        final boolean isValid = from != null && from >= FROM_DEFAULT;
        return isValid ? from : FROM_DEFAULT;
    }

    private Integer validateSize(final Integer size) {
        final boolean isValid = size != null && size > 0;
        return isValid ? size : SIZE_DEFAULT;
    }
}
