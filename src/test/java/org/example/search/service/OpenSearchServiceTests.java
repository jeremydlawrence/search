package org.example.search.service;

import org.apache.commons.lang3.tuple.Pair;
import org.example.search.dto.QueryOperator;
import org.example.search.dto.QueryType;
import org.example.search.dto.SearchFilter;
import org.example.search.dto.SearchResult;
import org.example.search.dto.SearchSpec;
import org.example.search.dto.SortSpec;
import org.example.search.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.HitsMetadata;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenSearchServiceTests {

    @Mock
    private OpenSearchClient openSearchClient;

    @Mock
    Hit<Product> hit;

    @Mock
    SearchResponse<Product> mockResponse;

    @Mock
    HitsMetadata<Product> hitsMetadata;

    private OpenSearchService openSearchService;

    @BeforeEach
    void setUp() {
        openSearchService = new OpenSearchService(openSearchClient);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 10, products",
        "20, 5, products",
        "100, 50, products"
    })
    void search_shouldReturnSearchResultWithPagination(int from, int size, String index) throws IOException {
        Product product = new Product();
        product.setId("1");
        product.setTitle("Test Product");
        product.setPrice(BigDecimal.valueOf(99.99));

        SearchFilter filter = SearchFilter.builder()
                .fieldName("title")
                .fieldValue("test")
                .queryType(QueryType.MUST_MATCH)
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index(index)
                .from(from)
                .size(size)
                .filters(List.of(filter))
                .build();

        when(hit.source()).thenReturn(product);
        when(hitsMetadata.hits()).thenReturn(List.of(hit));
        when(hitsMetadata.total()).thenReturn(mock(org.opensearch.client.opensearch.core.search.TotalHits.class));
        when(hitsMetadata.total().value()).thenReturn(1L);
        when(mockResponse.hits()).thenReturn(hitsMetadata);
        when(mockResponse.took()).thenReturn(10L);

        when(openSearchClient.search(any(SearchRequest.class), eq(Product.class))).thenReturn(mockResponse);

        SearchResult<Product> result = openSearchService.search(searchSpec, Product.class);

        assertNotNull(result);
        assertEquals(from, result.getStart());
        assertEquals(1L, result.getTotal());
        assertNotNull(result.getTook());
        assertEquals(1, result.getResults().size());
    }

    @Test
    void search_shouldApplyMatchFilter() throws IOException {
        SearchFilter filter = SearchFilter.builder()
                .fieldName("title")
                .fieldValue("jacket")
                .queryType(QueryType.MUST_MATCH)
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .from(0)
                .size(10)
                .filters(List.of(filter))
                .build();

        when(hitsMetadata.hits()).thenReturn(List.of());
        when(hitsMetadata.total()).thenReturn(mock(org.opensearch.client.opensearch.core.search.TotalHits.class));
        when(hitsMetadata.total().value()).thenReturn(0L);
        when(mockResponse.hits()).thenReturn(hitsMetadata);
        when(mockResponse.took()).thenReturn(5L);

        when(openSearchClient.search(any(SearchRequest.class), eq(Product.class))).thenReturn(mockResponse);

        SearchResult<Product> result = openSearchService.search(searchSpec, Product.class);

        assertNotNull(result);
        assertEquals(0, result.getResults().size());
        verify(openSearchClient).search(any(SearchRequest.class), eq(Product.class));
    }

    @Test
    void search_shouldApplyTermFilter() throws IOException {
        SearchFilter filter = SearchFilter.builder()
                .fieldName("brand")
                .fieldValue("Nike")
                .queryType(QueryType.FILTER_TERM)
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .from(0)
                .size(10)
                .filters(List.of(filter))
                .build();

        when(hitsMetadata.hits()).thenReturn(List.of());
        when(hitsMetadata.total()).thenReturn(mock(org.opensearch.client.opensearch.core.search.TotalHits.class));
        when(hitsMetadata.total().value()).thenReturn(0L);
        when(mockResponse.hits()).thenReturn(hitsMetadata);
        when(mockResponse.took()).thenReturn(5L);

        when(openSearchClient.search(any(SearchRequest.class), eq(Product.class))).thenReturn(mockResponse);

        SearchResult<Product> result = openSearchService.search(searchSpec, Product.class);

        assertNotNull(result);
        assertEquals(0, result.getResults().size());
    }

    @Test
    void search_shouldApplyMultipleFilters() throws IOException {
        SearchFilter matchFilter = SearchFilter.builder()
                .fieldName("title")
                .fieldValue("shoes")
                .queryType(QueryType.MUST_MATCH)
                .build();

        SearchFilter termFilter = SearchFilter.builder()
                .fieldName("brand")
                .fieldValue("Nike")
                .queryType(QueryType.FILTER_TERM)
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .from(0)
                .size(10)
                .filters(List.of(matchFilter, termFilter))
                .build();

        when(hitsMetadata.hits()).thenReturn(List.of());
        when(hitsMetadata.total()).thenReturn(mock(org.opensearch.client.opensearch.core.search.TotalHits.class));
        when(hitsMetadata.total().value()).thenReturn(0L);
        when(mockResponse.hits()).thenReturn(hitsMetadata);
        when(mockResponse.took()).thenReturn(5L);

        when(openSearchClient.search(any(SearchRequest.class), eq(Product.class))).thenReturn(mockResponse);

        SearchResult<Product> result = openSearchService.search(searchSpec, Product.class);

        assertNotNull(result);
        assertEquals(0, result.getResults().size());
    }

    @Test
    void search_shouldReturnEmptyResultsWhenNoHits() throws IOException {
        SearchFilter filter = SearchFilter.builder()
                .fieldName("title")
                .fieldValue("nonexistent")
                .queryType(QueryType.MUST_MATCH)
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .from(0)
                .size(10)
                .filters(List.of(filter))
                .build();

        when(hitsMetadata.hits()).thenReturn(List.of());
        when(hitsMetadata.total()).thenReturn(mock(org.opensearch.client.opensearch.core.search.TotalHits.class));
        when(hitsMetadata.total().value()).thenReturn(0L);
        when(mockResponse.hits()).thenReturn(hitsMetadata);
        when(mockResponse.took()).thenReturn(5L);

        when(openSearchClient.search(any(SearchRequest.class), eq(Product.class))).thenReturn(mockResponse);

        SearchResult<Product> result = openSearchService.search(searchSpec, Product.class);

        assertNotNull(result);
        assertEquals(0, result.getResults().size());
        assertEquals(0L, result.getTotal());
    }

    @Test
    void search_shouldThrowIOExceptionWhenSearchFails() throws IOException {
        SearchFilter filter = SearchFilter.builder()
                .fieldName("title")
                .fieldValue("test")
                .queryType(QueryType.MUST_MATCH)
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .from(0)
                .size(10)
                .filters(List.of(filter))
                .build();

        when(openSearchClient.search(any(SearchRequest.class), eq(Product.class))).thenThrow(new IOException("Search failed"));

        assertThrows(IOException.class, () -> openSearchService.search(searchSpec, Product.class));
    }

    @Test
    void getFieldValue_shouldReturnFieldValueForString() {
        FieldValue result = openSearchService.getFieldValue("test value");

        assertNotNull(result);
        assertEquals("test value", result.stringValue());
    }

    @Test
    void getFieldValue_shouldThrowExceptionForNonString() {
        assertThrows(IllegalArgumentException.class, () -> openSearchService.getFieldValue(123));
    }

    @Test
    void getVectorValue_shouldReturnListForList() {
        List<Float> input = List.of(1.0f, 2.0f, 3.0f);
        List<Float> result = openSearchService.getVectorValue(input);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(1.0f, result.get(0));
    }

    @Test
    void getVectorValue_shouldThrowExceptionForNonList() {
        assertThrows(IllegalArgumentException.class, () -> openSearchService.getVectorValue("not a list"));
    }

    @ParameterizedTest
    @CsvSource({
            "title, test query",
            "brand, Nike",
            "description, running shoes"
    })
    void getMatchQuery_shouldBuildMatchQuery(String fieldName, String fieldValue) {
        SearchFilter filter = SearchFilter.builder()
                .fieldName(fieldName)
                .fieldValue(fieldValue)
                .queryType(QueryType.MUST_MATCH)
                .queryOperator(QueryOperator.AND)
                .boost(1.0f)
                .build();

        Query result = openSearchService.getMatchQuery(filter);

        assertNotNull(result);
        assertTrue(result.isMatch());
    }

    @Test
    void getMatchQuery_shouldApplyBoost() {
        SearchFilter filter = SearchFilter.builder()
                .fieldName("title")
                .fieldValue("test")
                .queryType(QueryType.MUST_MATCH)
                .boost(3.0f)
                .build();

        Query result = openSearchService.getMatchQuery(filter);

        assertNotNull(result);
    }

    @Test
    void getTermQuery_shouldBuildTermQuery() {
        SearchFilter filter = SearchFilter.builder()
                .fieldName("brand")
                .fieldValue("Nike")
                .queryType(QueryType.FILTER_TERM)
                .build();

        Query result = openSearchService.getTermQuery(filter);

        assertNotNull(result);
        assertTrue(result.isTerm());
    }

    @Test
    void getKnnQuery_shouldBuildKnnQuery() {
        List<Float> vector = List.of(1.0f, 2.0f, 3.0f);
        SearchFilter filter = SearchFilter.builder()
                .fieldName("embedding")
                .fieldValue(vector)
                .queryType(QueryType.KNN_MATCH)
                .build();

        var result = openSearchService.getKnnQuery(filter);

        assertNotNull(result);
    }

    @Test
    void getSortOptions_shouldReturnEmptyListWhenNoSort() {
        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .from(0)
                .size(10)
                .build();

        List<SortOptions> result = openSearchService.getSortOptions(searchSpec);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getSortOptions_shouldReturnSortOptionsWhenSortProvided() {
        SortSpec sortSpec = SortSpec.builder()
                .sort(Pair.of("price", "desc"))
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .from(0)
                .size(10)
                .sort(sortSpec)
                .build();

        List<SortOptions> result = openSearchService.getSortOptions(searchSpec);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getSortOptions_shouldHandleMultipleSortFields() {
        SortSpec sortSpec = SortSpec.builder()
                .sort(Pair.of("price", "desc"))
                .sort(Pair.of("title", "asc"))
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .from(0)
                .size(10)
                .sort(sortSpec)
                .build();

        List<SortOptions> result = openSearchService.getSortOptions(searchSpec);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void buildFlatQuery_shouldBuildQueryWithMustMatch() {
        SearchFilter filter = SearchFilter.builder()
                .fieldName("title")
                .fieldValue("shoes")
                .queryType(QueryType.MUST_MATCH)
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .filters(List.of(filter))
                .build();

        Query result = openSearchService.buildFlatQuery(searchSpec);

        assertNotNull(result);
        assertTrue(result.isBool());
    }

    @Test
    void buildFlatQuery_shouldBuildQueryWithFilterTerm() {
        SearchFilter filter = SearchFilter.builder()
                .fieldName("brand")
                .fieldValue("Nike")
                .queryType(QueryType.FILTER_TERM)
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .filters(List.of(filter))
                .build();

        Query result = openSearchService.buildFlatQuery(searchSpec);

        assertNotNull(result);
        assertTrue(result.isBool());
    }

    @Test
    void buildFlatQuery_shouldBuildQueryWithShouldMatch() {
        SearchFilter filter = SearchFilter.builder()
                .fieldName("title")
                .fieldValue("shoes")
                .queryType(QueryType.SHOULD_MATCH)
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .filters(List.of(filter))
                .build();

        Query result = openSearchService.buildFlatQuery(searchSpec);

        assertNotNull(result);
        assertTrue(result.isBool());
    }

    @Test
    void buildFlatQuery_shouldBuildQueryWithMultipleFilters() {
        SearchFilter mustFilter = SearchFilter.builder()
                .fieldName("title")
                .fieldValue("shoes")
                .queryType(QueryType.MUST_MATCH)
                .build();

        SearchFilter termFilter = SearchFilter.builder()
                .fieldName("brand")
                .fieldValue("Nike")
                .queryType(QueryType.FILTER_TERM)
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .filters(List.of(mustFilter, termFilter))
                .build();

        Query result = openSearchService.buildFlatQuery(searchSpec);

        assertNotNull(result);
        assertTrue(result.isBool());
    }

    @Test
    void buildHybridQuery_shouldThrowExceptionWhenMissingMustMatch() {
        SearchFilter knnFilter = SearchFilter.builder()
                .fieldName("embedding")
                .fieldValue(List.of(1.0f, 2.0f))
                .queryType(QueryType.KNN_MATCH)
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .filters(List.of(knnFilter))
                .hybrid(true)
                .pipeline("hybrid_pipeline")
                .build();

        assertThrows(IllegalArgumentException.class, () -> openSearchService.buildHybridQuery(searchSpec));
    }

    @Test
    void buildHybridQuery_shouldThrowExceptionWhenMissingKnnMatch() {
        SearchFilter mustFilter = SearchFilter.builder()
                .fieldName("title")
                .fieldValue("shoes")
                .queryType(QueryType.MUST_MATCH)
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .filters(List.of(mustFilter))
                .hybrid(true)
                .pipeline("hybrid_pipeline")
                .build();

        assertThrows(IllegalArgumentException.class, () -> openSearchService.buildHybridQuery(searchSpec));
    }

    @Test
    void buildHybridQuery_shouldBuildQueryWithBothFilters() {
        SearchFilter mustFilter = SearchFilter.builder()
                .fieldName("title")
                .fieldValue("shoes")
                .queryType(QueryType.MUST_MATCH)
                .build();

        SearchFilter knnFilter = SearchFilter.builder()
                .fieldName("embedding")
                .fieldValue(List.of(1.0f, 2.0f, 3.0f))
                .queryType(QueryType.KNN_MATCH)
                .build();

        SearchSpec searchSpec = SearchSpec.builder()
                .index("products")
                .filters(List.of(mustFilter, knnFilter))
                .hybrid(true)
                .pipeline("hybrid_pipeline")
                .build();

        Query result = openSearchService.buildHybridQuery(searchSpec);

        assertNotNull(result);
    }
}
