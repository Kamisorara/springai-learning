package com.example.springaigraphdemo1.graphNode;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.springaigraphdemo1.model.SpecificationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component
public class SpecificationExtractionNode implements NodeAction {

    private final ChatClient chatClient;

    private final ObjectMapper objectMapper;

    public SpecificationExtractionNode(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    private static final String EXTRACTION_PROMPT = """
            你是一位商品信息提取专家。请从以下商品描述中提取结构化的商品属性。
            
            商品描述：{description}
            
            请以 JSON 格式返回，包含以下字段：
            - material: 材质（字符串）
            - colors: 颜色列表（数组）
            - season: 适用季节（字符串）
            
            只返回 JSON，不要添加任何其他文字说明。
            """;


    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 获取Description
        String description = String.valueOf(state.value("description"));
        PromptTemplate promptTemplate = new PromptTemplate(EXTRACTION_PROMPT);
        // 构建提示词
        String prompt = promptTemplate.render(Map.of("description", description));
        String jsonResp = chatClient.prompt(prompt).call().content().trim();
        // 清理可能的 markdown 代码块标记
        jsonResp = jsonResp.replaceAll("```json\\n?", "").replaceAll("```", "").trim();
        SpecificationResult spec = objectMapper.readValue(jsonResp, SpecificationResult.class);


        return Map.of(
                "material", spec.getMaterial(),
                "colors", spec.getColors(),
                "season", spec.getSeason()
        );
    }
}
