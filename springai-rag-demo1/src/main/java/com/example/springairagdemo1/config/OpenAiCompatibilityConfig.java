// java
package com.example.springairagdemo1.config;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.reactive.function.client.ClientRequest;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OpenAI 兼容性配置
 */
@Configuration
public class OpenAiCompatibilityConfig {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^v\\d+$");

    private static String removeSpringAiAddedV1(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        // 处理 DashScope: /compatible-mode/v1/v1/* -> /compatible-mode/v1/*
        if (path.contains("/compatible-mode/v1/v1/")) {
            return path.replaceFirst("/compatible-mode/v1/v1/", "/compatible-mode/v1/");
        }

        // 处理 BigModel: /api/paas/v4/v1/* -> /api/paas/v4/*
        if (path.contains("/api/paas/v4/v1/")) {
            return path.replaceFirst("/api/paas/v4/v1/", "/api/paas/v4/");
        }

        // 通用情况：如果路径中已经包含版本号（v1、v4等），Spring AI又添加了/v1，则移除Spring AI添加的/v1
        String[] segments = path.split("/");
        List<String> result = new ArrayList<>();
        boolean skipNextV1 = false;

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];

            if (segment.isEmpty()) {
                // 保留开头的空段（保持路径的斜杠）
                result.add(segment);
                continue;
            }

            // 检查前一个段是否是版本号模式
            String prevSegment = i > 0 ? segments[i - 1] : "";
            boolean prevIsVersion = Pattern.matches("^v\\d+$", prevSegment) ||
                                   prevSegment.equals("compatible-mode") ||
                                   prevSegment.equals("paas");

            // 如果当前段是v1，且前一个段是版本号或特定路径，则跳过这个v1
            if ("v1".equals(segment) && prevIsVersion) {
                skipNextV1 = true;
                continue;
            }

            result.add(segment);
        }

        return String.join("/", result);
    }


    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder.requestInterceptor((request, body, execution) -> {
            URI uri = request.getURI();
            String path = uri.getPath();
            String newPath = removeSpringAiAddedV1(path);
            if (!newPath.equals(path)) {
                try {
                    URI newUri = new URI(
                            uri.getScheme(),
                            uri.getAuthority(),
                            newPath,
                            uri.getQuery(),
                            uri.getFragment()
                    );
                    request = new HttpRequestWrapper(request) {
                        @Override
                        public java.net.URI getURI() {
                            return newUri;
                        }
                    };
                } catch (URISyntaxException e) {
                    throw new IOException(e);
                }
            }
            return execution.execute(request, body);
        });
    }

    @Bean
    public WebClientCustomizer webClientCustomizer() {
        return builder -> builder.filter((request, next) -> {
            URI uri = request.url();
            String path = uri.getPath();
            String newPath = removeSpringAiAddedV1(path);
            if (!newPath.equals(path)) {
                try {
                    URI newUri = new URI(
                            uri.getScheme(),
                            uri.getAuthority(),
                            newPath,
                            uri.getQuery(),
                            uri.getFragment()
                    );
                    ClientRequest newRequest = ClientRequest.from(request)
                            .url(newUri)
                            .build();
                    return next.exchange(newRequest);
                } catch (URISyntaxException e) {
                    return Mono.error(e);
                }
            }
            return next.exchange(request);
        });
    }
}
