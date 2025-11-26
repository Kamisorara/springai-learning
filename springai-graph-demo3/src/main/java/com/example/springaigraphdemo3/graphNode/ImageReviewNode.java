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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.io.InputStream;
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
            System.err.println("ImageReviewNode: imageUrl 为空");
            return false;
        }
        try {
            String normalizedUrl = imageUrl.trim();
            System.out.println("ImageReviewNode 开始处理图片 URL: [" + normalizedUrl + "]");
            System.out.println("URL 长度: " + normalizedUrl.length());

            Resource imageResource;
            MimeType mimeType;

            if (normalizedUrl.startsWith("http://") || normalizedUrl.startsWith("https://")) {
                // 对于 HTTP URL，下载图片内容到内存，避免 URL 特殊字符问题
                URI uri = new URI(normalizedUrl);
                org.springframework.core.io.UrlResource urlResource = new org.springframework.core.io.UrlResource(uri);
                
                // 读取图片内容到字节数组
                byte[] imageBytes;
                try (InputStream inputStream = urlResource.getInputStream()) {
                    imageBytes = inputStream.readAllBytes();
                }
                
                // 创建 ByteArrayResource
                imageResource = new ByteArrayResource(imageBytes);
                mimeType = getMimeTypeFromPath(normalizedUrl);
                System.out.println("从 URL 下载图片到内存，大小: " + imageBytes.length + " bytes");
            } else if (normalizedUrl.startsWith("file:")) {
                String localPath = normalizedUrl.startsWith("file://") 
                    ? normalizedUrl.substring(7) 
                    : normalizedUrl.substring(5);
                imageResource = new FileSystemResource(localPath);
                mimeType = getMimeTypeFromPath(localPath);
            } else {
                imageResource = new ClassPathResource(normalizedUrl);
                mimeType = getMimeTypeFromPath(normalizedUrl);
            }

            System.out.println("图片资源准备完成");
            System.out.println("MimeType: " + mimeType);

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
                    DashScopeChatOptions.builder().withMultiModel(true).build()
            );

            System.out.println("开始调用 DashScope API...");
            ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
            System.out.println("DashScope API 调用成功");
            String modelOutput = chatResponse.getResult().getOutput().getText();

            return parseReviewResult(modelOutput);
        } catch (Exception ex) {
            System.err.println("ImageReviewNode 处理图片失败: " + imageUrl);
            System.err.println("错误类型: " + ex.getClass().getName());
            System.err.println("错误信息: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    private boolean parseReviewResult(String modelOutput) throws Exception {
        if (modelOutput == null || modelOutput.isBlank()) {
            return false;
        }
        String cleanJson = cleanJsonString(modelOutput);
        JsonNode root = objectMapper.readTree(cleanJson);
        return root.path("pass").asBoolean(false);
    }

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

    private MimeType getMimeTypeFromPath(String path) {
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(".png")) {
            return MimeTypeUtils.IMAGE_PNG;
        } else if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return MimeTypeUtils.IMAGE_JPEG;
        } else if (lowerPath.endsWith(".gif")) {
            return MimeTypeUtils.IMAGE_GIF;
        } else if (lowerPath.endsWith(".webp")) {
            return MimeType.valueOf("image/webp");
        } else {
            return MimeTypeUtils.IMAGE_JPEG;
        }
    }
}
