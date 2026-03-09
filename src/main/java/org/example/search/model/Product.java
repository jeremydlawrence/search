package org.example.search.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Product implements IndexableDocument {
    private String id;
    private String title;
    private String description;
    private String brand;
    private List<String> category;
    private BigDecimal price;
    private List<String> image;
}
