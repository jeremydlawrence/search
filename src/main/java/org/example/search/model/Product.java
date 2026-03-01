package org.example.search.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product implements IndexableDocument {
    private String id;
    private String title;
    private String description;
    private String brand;
    private List<String> category;
    private BigDecimal price;
    private List<String> image;
}
