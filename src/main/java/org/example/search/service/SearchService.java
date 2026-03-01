package org.example.search.service;

import org.example.search.dto.SearchResult;
import org.example.search.model.IndexableDocument;
import org.example.search.dto.SearchSpec;

import java.io.IOException;

public interface SearchService {
    String clusterHealth();
    <T extends IndexableDocument> SearchResult search(SearchSpec searchSpec, Class<T> clazz) throws IOException;
}
