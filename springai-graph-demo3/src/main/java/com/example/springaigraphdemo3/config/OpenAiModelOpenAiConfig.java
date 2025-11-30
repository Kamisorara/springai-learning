package com.example.springaigraphdemo3.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 用于修改 OpenAI 模型的请求 URL，将 /v1/chat/completions 替换为 /chat/completions 增强切换模型API的兼容性
 */
@Configuration
public class OpenAiModelOpenAiConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder.requestInterceptor(
                (request, body, execution) -> {
                    String uri = request.getURI().toString();
                    // 将 /v1/chat/completions 替换为 /chat/completions
                    if (uri.contains("/v1/chat/completions")) {
                        uri = uri.replace("/v1/chat/completions", "/chat/completions");
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
