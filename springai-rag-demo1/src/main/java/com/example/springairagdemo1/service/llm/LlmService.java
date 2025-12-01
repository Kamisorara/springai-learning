package com.example.springairagdemo1.service.llm;

import com.example.springairagdemo1.util.ChatResponseUtil;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

public interface LlmService {
    Flux<ChatResponse> call(String system, String user);

    Flux<ChatResponse> callSystem(String system);

    Flux<ChatResponse> callUser(String user);

    default Flux<String> toStringFlux(Flux<ChatResponse> responseFlux) {
        return responseFlux.map(ChatResponseUtil::getText);
    }

    /**
     * 生成文本响应（同步方法，用于RAG等场景）
     *
     * @param prompt 提示词
     * @return 生成的文本内容
     */
    default String generateText(String prompt) {
        return toStringFlux(callUser(prompt)).collectList().block()
                .stream()
                .reduce("", (a, b) -> a + b);
    }

    /**
     * 生成文本响应（带系统提示）
     *
     * @param system 系统提示
     * @param user   用户提示
     * @return 生成的文本内容
     */
    default String generateText(String system, String user) {
        return toStringFlux(call(system, user)).collectList().block()
                .stream()
                .reduce("", (a, b) -> a + b);
    }
}
