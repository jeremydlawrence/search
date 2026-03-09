package org.example.search.service;

import org.example.search.config.EmbeddingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.http.HttpRequest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmbeddingServiceTests {

    @Mock
    private EmbeddingProperties configProperties;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        when(configProperties.getProtocol()).thenReturn("http");
        when(configProperties.getHost()).thenReturn("localhost");
        when(configProperties.getPort()).thenReturn(8080);
        when(configProperties.getPath()).thenReturn("embed");
        when(configProperties.getCharLimit()).thenReturn(1000);
        embeddingService = new EmbeddingService(configProperties);
    }

    @ParameterizedTest
    @CsvSource({
        "hello, hello",
        "hello world, hello world",
        "hello   spaces, hello spaces",
        "'hello\"quote', 'hello\\\"quote'",
        "'hello\\\\backslash', 'hello\\\\\\\\backslash'",
        "'hello\nnewline', 'hello newline'",
        "'hello\ttab', 'hello tab'",
        "'hello\rreturn', 'hello return'"
    })
    void sanitizeText_shouldSanitizeInput(String input, String expected) {
        String result = embeddingService.sanitizeText(input);
        assertEquals(expected, result);
    }

    @Test
    void sanitizeText_testCharLimit() {
        when(configProperties.getCharLimit()).thenReturn(3);

        String result = embeddingService.sanitizeText("1234");

        assertEquals("123", result);
    }

    @Test
    void buildRequest_shouldCreateCorrectRequest() {
        HttpRequest request = embeddingService.buildRequest("{\"test\": true}");

        assertNotNull(request);
        assertTrue(request.uri().toString().contains("http://localhost:8080/embed"));
    }

    @Test
    void parseResponse_shouldParseJsonCorrectly() throws Exception {
        String responseJson = "{\"vectors\": [[1.0, 2.0, 3.0], [4.0, 5.0, 6.0]]}";

        List<List<Float>> result = embeddingService.parseResponse(responseJson);

        assertEquals(2, result.size());
        assertEquals(3, result.get(0).size());
        assertEquals(1.0f, result.get(0).get(0));
        assertEquals(4.0f, result.get(1).get(0));
    }

    @ParameterizedTest
    @CsvSource({
        "null",
        "''"
    })
    void getEmbeddings_shouldReturnEmptyListWhenInputNullOrEmpty(String input) {
        List<String> texts = "null".equals(input) ? null : List.of();

        List<List<Float>> result = embeddingService.getEmbeddings(texts);

        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "hello",
        "hello world"
    })
    void buildRequestBody_shouldBuildCorrectJson(String text) {
        String result = embeddingService.buildRequestBody(List.of(text));

        assertTrue(result.contains("\"texts\""));
        assertTrue(result.contains(text));
    }

    @Test
    void buildRequestBody_shouldHandleMultipleTexts() {
        String result = embeddingService.buildRequestBody(List.of("text1", "text2", "text3"));

        assertTrue(result.contains("text1"));
        assertTrue(result.contains("text2"));
        assertTrue(result.contains("text3"));
    }
}
