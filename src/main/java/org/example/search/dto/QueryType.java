package org.example.search.dto;

public enum QueryType {
    MUST_MATCH,
    KNN_MATCH,
    SHOULD_MATCH,
    FILTER_TERM,
    FILTER_MATCH
}
