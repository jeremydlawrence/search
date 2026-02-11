package org.example.search.service;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OpenSearchService {
    private static final Logger logger = LoggerFactory.getLogger(OpenSearchService.class);

    private final OpenSearchClient client;

    @Autowired
    public OpenSearchService(final OpenSearchClient client) {
        this.client = client;
    }

    public String clusterHealth() {
        try {
            final HealthStatus health = client.cluster().health().status();
            return health.toString();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }
}
