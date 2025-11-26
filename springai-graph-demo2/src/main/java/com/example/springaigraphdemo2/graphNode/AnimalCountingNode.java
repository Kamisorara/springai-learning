package com.example.springaigraphdemo2.graphNode;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnimalCountingNode implements NodeAction {

    private final ChatClient chatClient;

    public AnimalCountingNode(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        try {
            String imageUrl = (String) state.value("imageUrl")
                    .orElseThrow(() -> new IllegalArgumentException("图片 URL 不能为空"));

            imageUrl = imageUrl.trim();
            System.out.println("处理的图片 URL: " + imageUrl);

            // 获取图片资源
            Resource imageResource;
            MimeType mimeType;

            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                // 对于 HTTP URL，下载图片内容到内存，避免 URL 特殊字符问题
                URI uri = new URI(imageUrl);
                org.springframework.core.io.UrlResource urlResource = new org.springframework.core.io.UrlResource(uri);
                
                // 读取图片内容到字节数组
                byte[] imageBytes;
                try (InputStream inputStream = urlResource.getInputStream()) {
                    imageBytes = inputStream.readAllBytes();
                }
                
                // 创建 ByteArrayResource
                imageResource = new ByteArrayResource(imageBytes);
                mimeType = getMimeTypeFromPath(imageUrl);
                System.out.println("从 URL 下载图片到内存，大小: " + imageBytes.length + " bytes");
            } else if (imageUrl.startsWith("file:")) {
                String localPath = imageUrl.startsWith("file://") 
                    ? imageUrl.substring(7) 
                    : imageUrl.substring(5);
                imageResource = new FileSystemResource(localPath);
                mimeType = getMimeTypeFromPath(localPath);
            } else {
                imageResource = new ClassPathResource(imageUrl);
                mimeType = getMimeTypeFromPath(imageUrl);
            }

            System.out.println("图片资源准备完成，MimeType: " + mimeType);

            String promptText = """
                请作为专业的图像识别助手,分析这张图片并统计其中的动物数量。
                要求:
                1. 识别图片中所有可见的动物
                2. 按物种分类统计数量
                3. 直接返回纯JSON格式,不要使用Markdown代码块包裹
                4. 不要添加任何注释或说明文字

                返回格式:
                {"animals":[{"species":"狗","count":2}],"total":2}
                """;

            List<Media> mediaList = List.of(new Media(mimeType, imageResource));

            UserMessage message = UserMessage.builder()
                    .text(promptText)
                    .media(mediaList)
                    .metadata(new HashMap<>())
                    .build();

            message.getMetadata().put(DashScopeApiConstants.MESSAGE_FORMAT, "image");

            Prompt prompt = new Prompt(
                    message,
                    DashScopeChatOptions.builder().withMultiModel(true).build()
            );

            ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
            String response = chatResponse.getResult().getOutput().getText();

            if (response == null || response.trim().isEmpty()) {
                response = "未能获取有效响应";
            }

            return Map.of("animalCount", response);
        } catch (Exception e) {
            System.err.println("[AnimalCountingNode] 调用异常，imageUrl: "
                    + state.value("imageUrl").orElse("null"));
            e.printStackTrace();
            return Map.of("animalCount", "调用失败: " + e.getMessage());
        }
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
