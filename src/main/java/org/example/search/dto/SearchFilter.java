package org.example.search.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchFilter {
    private String fieldName;
    private Object fieldValue;
    private QueryType queryType;
    private QueryOperator queryOperator;
    private Float boost;
}
