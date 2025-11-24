package com.example.springaigraphdemo2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnimalInfo {
    @JsonProperty("total")
    private Integer total;

    @JsonProperty("animals")
    private List<AnimalDetail> animals;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AnimalDetail {
        @JsonProperty("species")
        private String species;

        @JsonProperty("count")
        private Integer count;
    }

}
