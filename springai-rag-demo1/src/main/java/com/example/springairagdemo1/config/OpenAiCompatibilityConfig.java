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

    private static String removeTargetV1Segment(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String[] segs = path.split("/", -1); // 保留空段
        List<String> parts = new ArrayList<>(Arrays.asList(segs));
        // 找到第一个非空段索引
        int firstNonEmpty = -1;
        for (int i = 0; i < parts.size(); i++) {
            if (!parts.get(i).isEmpty()) {
                firstNonEmpty = i;
                break;
            }
        }
        for (int i = 0; i < parts.size(); i++) {
            if ("v1".equals(parts.get(i))) {
                boolean isLeading = (i == firstNonEmpty);
                boolean followsVersion = (i > firstNonEmpty && VERSION_PATTERN.matcher(parts.get(i - 1)).matches());
                if (isLeading || followsVersion) {
                    parts.remove(i);
                }
                break; // 只移除第一个匹配的 v1
            }
        }
        // 重新拼接，保留开头的斜杠（split 带来的空段）
        return String.join("/", parts);
    }

    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> restClientBuilder.requestInterceptor((request, body, execution) -> {
            URI uri = request.getURI();
            String path = uri.getPath();
            String newPath = removeTargetV1Segment(path);
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
            String newPath = removeTargetV1Segment(path);
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
