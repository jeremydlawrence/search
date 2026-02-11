package org.example.search.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.example.search.service.OpenSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    private final OpenSearchService openSearchService;

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
}
