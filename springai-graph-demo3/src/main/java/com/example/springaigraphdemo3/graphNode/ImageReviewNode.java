package com.example.springaigraphdemo3.graphNode;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ImageReviewNode implements NodeAction {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ImageReviewNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        List<String> imageUrls = (List<String>) state.value("image_url_lists").orElse(List.of());

        if (imageUrls == null || imageUrls.isEmpty()) {
            return Map.of(
                    "image_review_result", true,
                    "image_review_processed_count", 0
            );
        }

        int index = 0;
        while (index < imageUrls.size()) {
            String imageUrl = imageUrls.get(index);
            if (!reviewSingleImage(imageUrl)) {
                return Map.of(
                        "image_review_result", false,
                        "image_review_failed_url", imageUrl,
                        "image_review_processed_count", index + 1
                );
            }
            index++;
        }

        return Map.of(
                "image_review_result", true,
                "image_review_processed_count", imageUrls.size()
        );
    }

    private boolean reviewSingleImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return false;
        }
        try {
            Resource imageResource = resolveImageResource(imageUrl.trim());
            MimeType mimeType = resolveMimeType(imageUrl);
            System.out.println("图片大小: " + imageResource.contentLength() + " bytes");
            String reviewPrompt = """
                    你是严格的内容安全审核员，请仅返回JSON：
                    {"pass":true|false,"violations":["NONE"或违规点数组],"confidence":0~1}
                    审核标准：
                    1. 禁止裸露、性暗示、敏感隐私暴露。
                    2. 禁止血腥、肢解、明显暴力或虐待场景。
                    3. 禁止恐怖组织、爆炸物、极端主义宣传。
                    任一条触发即pass=false并列出violations。禁止添加多余文本。
                    """;

            UserMessage message = UserMessage.builder()
                    .text(reviewPrompt)
                    .media(List.of(new Media(mimeType, imageResource)))
                    .metadata(new HashMap<>())
                    .build();
            message.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, "image");

            Prompt prompt = new Prompt(message,
                    DashScopeChatOptions.builder().withModel("qwen2.5-vl-7b-instruct").withMultiModel(true)
                    .build());

            ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
            String modelOutput = chatResponse.getResult().getOutput().getText();

            return parseReviewResult(modelOutput);
        } catch (Exception ex) {
            System.out.println(ex);
            return false;
        }
    }

    private Resource resolveImageResource(String imageUrl) throws Exception {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return new UrlResource(new URI(imageUrl));
        }
        if (imageUrl.startsWith("file:")) {
            return new FileSystemResource(imageUrl.substring(5));
        }
        return new ClassPathResource(imageUrl);
    }

    private MimeType resolveMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) {
            return MimeTypeUtils.IMAGE_PNG;
        } else if (lower.endsWith(".gif")) {
            return MimeTypeUtils.IMAGE_GIF;
        } else if (lower.endsWith(".webp")) {
            return MimeType.valueOf("image/webp");
        }
        return MimeTypeUtils.IMAGE_JPEG;
    }

    private boolean parseReviewResult(String modelOutput) throws Exception {
        if (modelOutput == null || modelOutput.isBlank()) {
            return false;
        }
        JsonNode root = objectMapper.readTree(modelOutput);
        return root.path("pass").asBoolean(false);
    }
}
