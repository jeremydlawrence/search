package org.example.search.controller;

import org.example.search.dto.SearchResult;
import org.example.search.model.Product;
import org.example.search.service.OpenSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchControllerTests {

    @Mock
    private OpenSearchService openSearchService;

    private SearchController searchController;

    @BeforeEach
    void setUp() {
        searchController = new SearchController(openSearchService);
        ReflectionTestUtils.setField(searchController, "productIndexName", "products");
    }

    @Test
    void checkOpenSearchHealth_shouldReturnHealthyStatus() {
        when(openSearchService.clusterHealth()).thenReturn("GREEN");

        String result = searchController.checkOpenSearchHealth();

        assertEquals("OpenSearch status: GREEN", result);
    }

    @Test
    void checkOpenSearchHealth_shouldThrowExceptionWhenServiceReturnsNull() {
        when(openSearchService.clusterHealth()).thenReturn(null);

        assertThrows(RuntimeException.class, () -> searchController.checkOpenSearchHealth());
    }

    @ParameterizedTest
    @CsvSource({
        "0, 10, test, null, null",
        "20, 5, jacket, null, null",
        "0, 10, shoes, Nike, null",
        "0, 10, shoes, null, athletic",
        "0, 10, shoes, Nike, athletic"
    })
    void search_shouldReturnResultsWithVariousParams(int from, int size, String query, String brand, String category) throws Exception {
        Product product = new Product();
        product.setId("1");
        product.setTitle("Test Product");
        product.setPrice(BigDecimal.valueOf(99.99));
        SearchResult searchResult = SearchResult.<Product>builder()
                .results(List.of(product))
                .total(1L)
                .start(from)
                .took(10L)
                .build();

        when(openSearchService.search(any(), eq(Product.class))).thenReturn(searchResult);
        String result = searchController.search(from, size, query, brand, category);
        assertNotNull(result);
    }
}
