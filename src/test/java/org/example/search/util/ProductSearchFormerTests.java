package org.example.search.util;

import org.example.search.dto.SearchSpec;
import org.example.search.dto.SortType;
import org.example.search.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ProductSearchFormerTests {

    @Mock
    private EmbeddingService embeddingService;

    private ProductSearchFormer productSearchFormer;

    private static final String TEST_INDEX = "products";

    @BeforeEach
    void setUp() {
        productSearchFormer = new ProductSearchFormer(TEST_INDEX, embeddingService);
    }

    @ParameterizedTest
    @CsvSource({
        "null, 0",
        "-1, 0",
        "0, 0",
        "10, 10",
        "100, 100"
    })
    void validateFrom_shouldReturnCorrectValue(String input, int expected) {
        Integer value = "null".equals(input) ? null : Integer.parseInt(input);
        Integer result = productSearchFormer.validateFrom(value);
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @CsvSource({
        "null, 10",
        "0, 10",
        "-5, 10",
        "1, 1",
        "25, 25",
        "100, 100"
    })
    void validateSize_shouldReturnCorrectValue(String input, int expected) {
        Integer value = "null".equals(input) ? null : Integer.parseInt(input);
        Integer result = productSearchFormer.validateSize(value);
        assertEquals(expected, result);
    }

    @ParameterizedTest
    @CsvSource({
        "null",
        "''"
    })
    void validateFields_shouldReturnIdWhenNullOrEmpty(String input) {
        List<String> fields = "null".equals(input) ? null : List.of();
        List<String> result = productSearchFormer.validateFields(fields);
        assertEquals(List.of("id"), result);
    }

    @Test
    void validateFields_shouldReturnIdWhenOnlyInvalidFields() {
        List<String> fields = List.of("invalid", "also_invalid");
        List<String> result = productSearchFormer.validateFields(fields);
        assertEquals(List.of("id"), result);
    }

    @Test
    void validateFields_shouldFilterInvalidFields() {
        List<String> fields = List.of("id", "invalid", "title");
        List<String> result = productSearchFormer.validateFields(fields);
        assertEquals(2, result.size());
        assertTrue(result.contains("id"));
        assertTrue(result.contains("title"));
    }

    @Test
    void validateFields_shouldReturnAllValidFields() {
        List<String> fields = List.of("id", "title", "brand", "price");
        List<String> result = productSearchFormer.validateFields(fields);
        assertEquals(4, result.size());
    }

    @ParameterizedTest
    @CsvSource({
        "null, null, 0, 10",
        "20, 50, 20, 50",
        "5, 25, 5, 25"
    })
    void formSearch_shouldUsePaginationValues(String fromInput, String sizeInput, int expectedFrom, int expectedSize) throws Exception {
        Integer from = "null".equals(fromInput) ? null : Integer.parseInt(fromInput);
        Integer size = "null".equals(sizeInput) ? null : Integer.parseInt(sizeInput);
        
        SearchSpec result = productSearchFormer.formSearch(
                "test", from, size, null, null, SortType.RELEVANCE, null);

        assertEquals(expectedFrom, result.getFrom());
        assertEquals(expectedSize, result.getSize());
    }

    @ParameterizedTest
    @CsvSource({
        "Nike, null, 3",
        "null, shoes, 3",
        "Nike, shoes, 4"
    })
    void formSearch_shouldIncludeCorrectFilters(String brand, String category, int expectedFilterCount) throws Exception {
        String brandVal = "null".equals(brand) ? null : brand;
        String categoryVal = "null".equals(category) ? null : category;
        
        SearchSpec result = productSearchFormer.formSearch(
                "test", 0, 10, brandVal, categoryVal, SortType.RELEVANCE, null);

        assertNotNull(result.getFilters());
        assertEquals(expectedFilterCount, result.getFilters().size());
    }

    @Test
    void formSearch_shouldSetIndex() throws Exception {
        SearchSpec result = productSearchFormer.formSearch(
                "test", 0, 10, null, null, SortType.RELEVANCE, null);

        assertEquals(TEST_INDEX, result.getIndex());
    }

    @Test
    void formSearch_shouldSetFields() throws Exception {
        SearchSpec result = productSearchFormer.formSearch(
                "test", 0, 10, null, null, SortType.RELEVANCE, List.of("id", "title"));

        assertEquals(List.of("id", "title"), result.getFields());
    }

    @ParameterizedTest
    @CsvSource({
        "RELEVANCE, _score",
        "LEXICAL, _score",
        "PRICE_ASCENDING, price",
        "PRICE_DESCENDING, price"
    })
    void formSearch_shouldSetCorrectSortField(SortType sortType, String expectedSortField) throws Exception {
        SearchSpec result = productSearchFormer.formSearch(
                "test", 0, 10, null, null, sortType, null);

        assertNotNull(result.getSort());
        assertEquals(1, result.getSort().getSorts().size());
        assertEquals(expectedSortField, result.getSort().getSorts().get(0).getLeft());
    }

    @Test
    void formSearch_shouldNotBeHybridByDefault() throws Exception {
        SearchSpec result = productSearchFormer.formSearch(
                "test", 0, 10, null, null, SortType.RELEVANCE, null);

        assertFalse(result.isHybrid());
    }

    @Test
    void formSearch_shouldNotSetPipelineByDefault() throws Exception {
        SearchSpec result = productSearchFormer.formSearch(
                "test", 0, 10, null, null, SortType.RELEVANCE, null);

        assertNull(result.getPipeline());
    }
}
