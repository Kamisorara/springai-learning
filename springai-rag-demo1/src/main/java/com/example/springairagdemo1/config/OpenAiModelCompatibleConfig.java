package com.example.springairagdemo1.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用于修改 OpenAI 模型的请求 URL，移除 /v1 前缀以增强切换模型API的兼容性
 */
@Configuration
public class OpenAiModelCompatibleConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder.requestInterceptor(
                (request, body, execution) -> {
                    String uri = request.getURI().toString();

                    // 移除 /v1 前缀的所有 OpenAI API 端点
                    if (uri.contains("/v1/")) {
                        // Chat Completions - 聊天补全
                        uri = uri.replace("/v1/chat/completions", "/chat/completions");

                        // Completions - 文本补全
                        uri = uri.replace("/v1/completions", "/completions");

                        // Embeddings - 向量嵌入
                        uri = uri.replace("/v1/embeddings", "/embeddings");

                        // Images - 图像生成
                        uri = uri.replace("/v1/images/generations", "/images/generations");
                        uri = uri.replace("/v1/images/edits", "/images/edits");
                        uri = uri.replace("/v1/images/variations", "/images/variations");

                        // Audio - 音频转录和翻译
                        uri = uri.replace("/v1/audio/transcriptions", "/audio/transcriptions");
                        uri = uri.replace("/v1/audio/translations", "/audio/translations");
                        uri = uri.replace("/v1/audio/speech", "/audio/speech");

                        // Files - 文件管理
                        uri = uri.replace("/v1/files", "/files");

                        // Fine-tuning - 微调
                        uri = uri.replace("/v1/fine_tuning/jobs", "/fine_tuning/jobs");

                        // Models - 模型列表
                        uri = uri.replace("/v1/models", "/models");

                        // Moderations - 内容审核
                        uri = uri.replace("/v1/moderations", "/moderations");

                        String finalUri = uri;
                        request = new org.springframework.http.client.support.HttpRequestWrapper(request) {
                            @NotNull
                            @Override
                            public java.net.URI getURI() {
                                return java.net.URI.create(finalUri);
                            }
                        };
                    }
                    return execution.execute(request, body);
                }
        );
    }
}
