package org.example.search.dto;

public enum SortType {
    RELEVANCE("relevance"),
    LEXICAL("lexical"),
    SEMANTIC("semantic"),
    HYBRID("hybrid"),
    PRICE_ASCENDING("price_asc"),
    PRICE_DESCENDING("price_desc");

    private final String value;

    SortType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static SortType fromValue(String value) {
        for (SortType sortType : SortType.values()) {
            if (sortType.value.equals(value)) {
                return sortType;
            }
        }
        return RELEVANCE;
    }
}
