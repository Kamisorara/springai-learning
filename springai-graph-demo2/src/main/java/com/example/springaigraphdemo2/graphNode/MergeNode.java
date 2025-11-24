package com.example.springaigraphdemo2.graphNode;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.springaigraphdemo2.model.AnimalInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MergeNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String animalCountStr = (String) state.value("animalCount")
                .orElse("{}");

        String cleanedJson = cleanJsonString(animalCountStr);
        ObjectMapper mapper = new ObjectMapper();

        // 直接反序列化为 AnimalInfo 对象
        // { "animals" : [ { "species" : "狗", "count" : 2 } ], "total" : 2 }
        AnimalInfo animalInfo = mapper.readValue(cleanedJson, AnimalInfo.class);

        return Map.of(
                "animalCount", animalInfo.getTotal(),
                "animalInfo", animalInfo
        );
    }

    private String cleanJsonString(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return "{}";
        }

        // 移除 Markdown 代码块标记
        String cleaned = jsonStr.trim();

        // 移除开头的 ```json 或 ```
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        // 移除结尾的 ```
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }
}
