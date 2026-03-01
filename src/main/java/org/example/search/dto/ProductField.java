package org.example.search.dto;

public enum ProductField {
    ID("id"),
    FTS("fts"),
    TITLE("title"),
    DESCRIPTION("description"),
    BRAND("brand"),
    CATEGORY("category");

    private final String value;

    ProductField(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
