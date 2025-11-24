package com.example.springaigraphdemo1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecificationResult {
    private String material;
    private List<String> colors;
    private String season;
}

