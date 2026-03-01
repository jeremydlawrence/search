package org.example.search.dto;

import lombok.Builder;
import lombok.Data;
import org.example.search.model.IndexableDocument;

import java.util.List;

@Builder
@Data
public class SearchResult<T extends IndexableDocument> {
    private Long took;
    private Long total;
    private Integer start;
    private List<T> results;
}
