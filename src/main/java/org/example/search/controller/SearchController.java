package org.example.search.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.example.search.dto.ProductField;
import org.example.search.dto.QueryType;
import org.example.search.dto.SearchFilter;
import org.example.search.model.Product;
import org.example.search.dto.SearchSpec;
import org.example.search.service.OpenSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class SearchController {
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final OpenSearchService openSearchService;

    @Value("${open-search.product.index-name}")
    private String productIndexName;

    @Autowired
    public SearchController(final OpenSearchService openSearchService) {
        this.openSearchService = openSearchService;
    }

    @GetMapping("/index-health")
    @Operation(summary = "Check OpenSearch cluster health", description = "Verifies cluster staus of OpenSearch")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OpenSearch is healthy"),
            @ApiResponse(responseCode = "503", description = "Cannot connect to OpenSearch")
    })
    public String checkOpenSearchHealth() {
        final String status = openSearchService.clusterHealth();
        if (status != null) {
            return String.format("OpenSearch status: %s", status);
        } else {
            throw new RuntimeException("Cannot connect to OpenSearch");
        }
    }

    @GetMapping("v1/product/search")
    @Operation(summary = "Search products")
    public String search(
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam String query,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String category) {
        try {
            final List<SearchFilter> filters = new ArrayList<>();
            filters.add(SearchFilter.builder()
                    .fieldName(ProductField.FTS.value())
                    .fieldValue(query)
                    .queryType(QueryType.MATCH)
                    .build());
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

            final SearchSpec.SearchSpecBuilder searchSpec = SearchSpec.builder()
                    .index(productIndexName)
                    .from(from)
                    .size(size)
                    .filters(filters);

            return objectMapper.writeValueAsString(openSearchService.search(searchSpec.build(), Product.class));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
