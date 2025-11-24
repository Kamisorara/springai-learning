package com.example.springaigraphdemo1.graphNode;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component
public class MarketingCopyNode implements NodeAction {

    private final ChatClient chatClient;

    public MarketingCopyNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    private static final String MARKETING_PROMPT = """
            你是一位专业的营销文案专家。请根据以下商品描述，生成一句吸引人的、简短的营销口号（Slogan）。

            要求：
            - 口号要朗朗上口，富有感染力
            - 长度控制在20字以内
            - 突出商品的核心卖点

            商品描述：{description}

            请直接返回营销口号，不要添加其他说明文字也不要返回Markdown格式的文字。
            """;
    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 获取营销文案
        String description = String.valueOf(state.value("description"));
        // 提示词
        PromptTemplate promptTemplate = new PromptTemplate(MARKETING_PROMPT);
        String prompt = promptTemplate.render(Map.of("description", description));
        String slogan = chatClient.prompt(prompt).call().content().trim();

        // 调用
        return Map.of("slogan", slogan);
    }
}
