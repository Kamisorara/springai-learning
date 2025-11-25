package com.example.springaigraphdemo3.graphNode;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TextReviewNode implements NodeAction {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TextReviewNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String text = (String) state.value("text").orElse("");
        boolean pass = text.isBlank() || reviewText(text);
        return Map.of("text_review_result", pass);
    }

    private boolean reviewText(String text) {
        try {
            String reviewPrompt = """
                    你是严格的文本内容安全审核员。请仅返回JSON：
                    {"pass":true|false,"violations":["NONE"或违规点数组],"confidence":0~1}
                    审核规则：
                    1. 禁止出现反政府、恐怖组织、极端主义或煽动暴力的言论。
                    2. 禁止描绘恐怖袭击、爆炸物制造、组织策划等敏感内容。
                    任意违规即pass=false，并在violations中列出命中的条款。不要添加额外文字。
                    待审核文本：%s
                    """.formatted(text);

            Prompt prompt = new Prompt(
                    UserMessage.builder().text(reviewPrompt).build(),
                    DashScopeChatOptions.builder().build()
            );

            ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
            String output = chatResponse.getResult().getOutput().getText();
            if (output == null || output.isBlank()) {
                return false;
            }

            // **关键修复：在解析前清理字符串**
            String cleanJson = cleanJsonString(output);

            JsonNode root = objectMapper.readTree(cleanJson);
            return root.path("pass").asBoolean(false);
        } catch (Exception ex) {
            System.out.println(ex);
            return false;
        }
    }

    /**
     * 清理模型返回的字符串，移除Markdown代码块标记。
     * @param response 原始响应字符串
     * @return 清理后的JSON字符串
     */
    private String cleanJsonString(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }
}
