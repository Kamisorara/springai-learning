package com.example.springaidemo1.service;

import com.example.springaidemo1.resp.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;

@Service
public class JsonAgentService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public JsonAgentService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一个专业的 JSON 数据处理助手。请根据用户需求生成规范的 JSON 格式响应，包含 status、message、data 和 timestamp 字段。")
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public ChatResponse processQuery(String query) {
        try {
            String response = chatClient.prompt()
                    .user(query + "\n请返回纯 JSON 格式（以纯文本输出 json，请不要包含任何多余的文字——包括 markdown 格式），包含 status、message 和 data 字段。")
                    .call()
                    .content();

            return parseJsonResponse(response);
        } catch (Exception e) {
            return new ChatResponse("error", "处理失败：" + e.getMessage(), null);
        }
    }


    private ChatResponse parseJsonResponse(String response) {
        try {
            String jsonStr = response.trim();

            // 提取 Markdown 代码块中的 JSON
            if (jsonStr.contains("```json")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```")).trim();
            } else if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
                if (jsonStr.contains("```")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("```")).trim();
                }
            }

            // JSON 格式完整，直接反序列化
            if (jsonStr.startsWith("{")) {
                ChatResponse result = objectMapper.readValue(jsonStr, ChatResponse.class);

                // 确保 timestamp 有值
                if (result.getTimestamp() == null) {
                    result.setTimestamp(System.currentTimeMillis());
                }

                return result;
            }

            return new ChatResponse("success", response, null);
        } catch (Exception e) {
            // 异常时返回清晰的错误信息，不返回原始响应
            return new ChatResponse("error", "JSON 解析失败：" + e.getMessage(), null);
        }
    }


    public String getRawJsonResponse(String query) {
        return chatClient.prompt()
                .user(query + "\n请返回纯 JSON 格式，不要包含其他文本。")
                .call()
                .content();
    }
}
