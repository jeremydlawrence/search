package org.example.search.dto;

public enum ProductField {
    ID("id"),
    FTS("fts"),
    FTS_EMBEDDING("fts_embedding"),
    TITLE("title"),
    DESCRIPTION("description"),
    BRAND("brand"),
    CATEGORY("category"),
    PRICE("price"),
    IMAGE("image");

    private final String value;

    ProductField(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
