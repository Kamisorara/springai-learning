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
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

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
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String imageUrl = (String) state.value("imageUrl")
                .orElseThrow(() -> new IllegalArgumentException("图片 URL 不能为空"));

        imageUrl = imageUrl.trim();
        System.out.println("处理的图片 URL: " + imageUrl);

        // 获取图片资源
        Resource imageResource;
        MimeType mimeType;

        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            // HTTP URL - 使用 URI 转换
            imageResource = new org.springframework.core.io.UrlResource(new URI(imageUrl));
            mimeType = getMimeTypeFromPath(imageUrl);
        } else if (imageUrl.startsWith("file:")) {
            // 本地文件路径
            String localPath = imageUrl.substring(5);
            imageResource = new FileSystemResource(localPath);
            mimeType = getMimeTypeFromPath(localPath);
        } else {
            // ClassPath 资源
            imageResource = new ClassPathResource(imageUrl);
            mimeType = getMimeTypeFromPath(imageUrl);
        }

        if (!imageResource.exists()) {
            throw new IllegalArgumentException("图片资源不存在: " + imageUrl);
        }

        System.out.println("图片大小: " + imageResource.contentLength() + " bytes");

        // 创建 Media 对象
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

        // 使用字符串常量 "image" 而不是 MessageFormat.IMAGE
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
