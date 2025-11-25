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

    // 审核单张图片
    private boolean reviewSingleImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return false;
        }
        try {
            Resource imageResource = resolveImageResource(imageUrl.trim());
            MimeType mimeType = resolveMimeType(imageUrl);
            System.out.println("ImageReviewNode 处理的图片 URL: [" + imageUrl + "]");
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

            List<Media> mediaList = List.of(new Media(mimeType, imageResource));

            UserMessage message = UserMessage.builder()
                    .text(reviewPrompt)
                    .media(mediaList)
                    .metadata(new HashMap<>())
                    .build();

            message.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, "image");

            Prompt prompt = new Prompt(
                    message,
                    DashScopeChatOptions.builder()
                            .withModel("qwen-vl-plus")
                    .withMultiModel(true).build()
            );

            ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
            String modelOutput = chatResponse.getResult().getOutput().getText();

            return parseReviewResult(modelOutput);
        } catch (Exception ex) {
            ex.printStackTrace(); // 建议打印完整堆栈信息
            return false;
        }
    }

    // 解析图片资源
    private Resource resolveImageResource(String imageUrl) throws Exception {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            Resource imageResource = new org.springframework.core.io.UrlResource(new URI(imageUrl));
            return imageResource;
        }
        if (imageUrl.startsWith("file:")) {
            return new FileSystemResource(imageUrl.substring(5));
        }
        return new ClassPathResource(imageUrl);
    }

    // 根据文件路径后缀解析 MimeType
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

    // 解析模型返回的审核结果
    private boolean parseReviewResult(String modelOutput) throws Exception {
        if (modelOutput == null || modelOutput.isBlank()) {
            return false;
        }
        // **关键修复：在解析前清理字符串**
        String cleanJson = cleanJsonString(modelOutput);
        JsonNode root = objectMapper.readTree(cleanJson);
        return root.path("pass").asBoolean(false);
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
