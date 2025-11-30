package com.example.springaigraphdemo3.graphNode;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.example.springaigraphdemo3.util.JsonCleanerUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.InputStream;
import java.net.URI;
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
                    你是严格的内容安全审核员。**必须且仅返回纯JSON,不要添加任何标记、说明或额外文本**。
                    JSON格式:
                    {"pass": true/false, "violations": ["类型1", "类型2"]}
                    审核标准:
                    1. 禁止裸露、性暗示、敏感隐私暴露。
                    2. 禁止血腥、肢解、明显暴力或虐待场景。
                    3. 禁止恐怖组织、爆炸物、极端主义宣传。
                    任一条触发即pass=false并列出violations。
                    """;

            UserMessage userMessage = UserMessage.builder()
                    .text(reviewPrompt)
                    .media(List.of(new Media(mimeType, imageResource)))
                    .build();

            System.out.println("开始调用 OpenAI API...");
            ChatResponse chatResponse = chatClient.prompt()
                    .messages(userMessage)
                    .call()
                    .chatResponse();
            System.out.println("OpenAI API 调用成功");
            String modelOutput = chatResponse.getResult().getOutput().getText();

            return parseReviewResult(modelOutput);
        } catch (org.springframework.ai.retry.NonTransientAiException ex) {
            // 处理模型内容审查异常 (如阿里云 1301、OpenAI moderation 等)
            System.err.println("模型内容审查拒绝: " + imageUrl);
            System.err.println("原因: " + ex.getMessage());
            return false; // 直接判定为审核不通过

        } catch (javax.net.ssl.SSLHandshakeException ex) {
            // 网络连接问题
            System.err.println("SSL 连接失败: " + imageUrl);
            return false; // 网络问题也视为审核失败
        } catch (Exception ex) {
            // 其他异常 (JSON 解析失败、超时等)
            System.err.println("ImageReviewNode 处理失败: " + imageUrl);
            System.err.println("错误: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            return false;
        }
    }

    private boolean parseReviewResult(String modelOutput) throws Exception {
        if (modelOutput == null || modelOutput.isBlank()) {
            return false;
        }
        String cleanJson = JsonCleanerUtil.cleanJsonString(modelOutput);
        JsonNode root = objectMapper.readTree(cleanJson);
        return root.path("pass").asBoolean(false);
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
