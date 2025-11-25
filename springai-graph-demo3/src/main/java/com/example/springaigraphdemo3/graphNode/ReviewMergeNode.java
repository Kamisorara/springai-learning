package com.example.springaigraphdemo3.graphNode;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReviewMergeNode implements NodeAction {

    private final ChatClient chatClient;

    public ReviewMergeNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Boolean textResult = (Boolean) state.value("text_review_result").orElse(Boolean.FALSE);
        Boolean imageResult = (Boolean) state.value("image_review_result").orElse(Boolean.FALSE);
        Integer processedCount = (Integer) state.value("image_review_processed_count").orElse(0);
        String failedUrl = (String) state.value("image_review_failed_url").orElse("");

        boolean finalResult = Boolean.TRUE.equals(textResult) && Boolean.TRUE.equals(imageResult);

        return Map.of(
                "text_review_result", textResult,
                "image_review_result", imageResult,
                "image_review_processed_count", processedCount,
                "image_review_failed_url", failedUrl,
                "final_review_result", finalResult
        );
    }
}
