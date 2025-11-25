package com.example.springaigraphdemo3.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUploads {
    @JsonProperty("text")
    private String text;
    @JsonProperty("image_url_lists")
    private List<String> imageUrlLists;
}
