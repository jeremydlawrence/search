package org.example.search.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

@Builder
@Data
public class SortSpec {
   @Singular
   private List<Pair<String, String>> sorts;
   private String pipeline;
}
