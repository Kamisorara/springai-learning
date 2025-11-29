package com.example.springaidemo2.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/openai-interface")
public class OpenAIInterfaceController {
    private static final String DEFAULT_PROMPT = "你是一个博学的智能聊天助手，请根据用户提问回答！";
    private final ChatClient chatClient;

    public OpenAIInterfaceController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem(DEFAULT_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }


    // 普通接口流式调用
    @GetMapping("/stream/chat")
    public Flux<String> streamChat(@RequestParam(value = "query", defaultValue = DEFAULT_PROMPT) String query,
                                   HttpServletResponse response) {

        // 避免返回乱码
        response.setCharacterEncoding("UTF-8");

        return chatClient.prompt(query).stream().content();

    }

    // 验证OpenAI兼容接口多模态能力
    @PostMapping("/multi-model/chat")
    public Flux<String> multimodalChat(@RequestParam(value = "query", defaultValue = "你是谁") String query,
                                       @RequestParam(value = "imageUrl", required = false, defaultValue = "") String imageUrl,
                                       HttpServletResponse response) throws URISyntaxException, IOException {
        // 避免返回乱码
        response.setCharacterEncoding("UTF-8");
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

        // 构建包含图片的用户消息
        UserMessage userMessage = UserMessage.builder()
                .text(query)
                .media(List.of(new Media(mimeType, imageResource)))
                .build();

        return chatClient.prompt()
                .messages(userMessage)
                .stream()
                .content();
    }

    // 简单根据文件扩展名判断 MimeType
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
