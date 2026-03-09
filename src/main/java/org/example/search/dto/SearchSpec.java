package org.example.search.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchSpec {
    private String index;
    private String pipeline;
    private int from;
    private int size;
    private boolean hybrid = false;
    private List<SearchFilter> filters;
    private SortSpec sort;
    private List<String> fields;
}
