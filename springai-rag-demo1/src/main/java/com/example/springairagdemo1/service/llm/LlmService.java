package com.example.springairagdemo1.service.llm;

import com.example.springairagdemo1.util.ChatResponseUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallbackProvider;
import reactor.core.CorePublisher;
import reactor.core.publisher.Flux;

public interface LlmService {
    Flux<ChatResponse> call(String system, String user);

    Flux<ChatResponse> callSystem(String system);

    Flux<ChatResponse> callUser(String user);

    /**
     * 带工具调用的聊天接口
     *
     * @param system 系统提示
     * @param user   用户提示
     * @param tools  工具回调提供者
     * @return 聊天响应流
     */
    Flux<ChatResponse> callWithTools(String system, String user, ToolCallbackProvider tools);

    /**
     * 带工具调用的聊天接口（仅用户提示）
     *
     * @param user  用户提示
     * @param tools 工具回调提供者
     * @return 聊天响应流
     */
    Flux<ChatResponse> callUserWithTools(String user, ToolCallbackProvider tools);

    /**
     * 带工具调用的聊天接口（仅系统提示）
     *
     * @param system 系统提示
     * @param tools  工具回调提供者
     * @return 聊天响应流
     */
    Flux<ChatResponse> callSystemWithTools(String system, ToolCallbackProvider tools);

    default Flux<String> toStringFlux(Flux<ChatResponse> responseFlux) {
        return responseFlux.map(ChatResponseUtil::getText);
    }

    @Deprecated
    default String blockToString(Flux<ChatResponse> responseFlux) {
        return toStringFlux(responseFlux).collect(StringBuilder::new, StringBuilder::append)
                .map(StringBuilder::toString)
                .block();
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

    /**
     * 生成文本响应（带工具调用）
     *
     * @param system 系统提示
     * @param user   用户提示
     * @param tools  工具回调提供者
     * @return 生成的文本内容
     */
    default String generateTextWithTools(String system, String user, ToolCallbackProvider tools) {
        return toStringFlux(callWithTools(system, user, tools)).collectList().block()
                .stream()
                .reduce("", (a, b) -> a + b);
    }

    /**
     * 生成文本响应（带工具调用，仅用户提示）
     *
     * @param user  用户提示
     * @param tools 工具回调提供者
     * @return 生成的文本内容
     */
    default String generateTextWithTools(String user, ToolCallbackProvider tools) {
        return toStringFlux(callUserWithTools(user, tools)).collectList().block()
                .stream()
                .reduce("", (a, b) -> a + b);
    }

    /**
     * 生成文本响应流（带工具调用）
     * @param enhancedPrompt
     * @param tools
     * @return
     */
    default Flux<String> generateTextWithToolsStream(String enhancedPrompt, ToolCallbackProvider tools) {
        return toStringFlux(callUserWithTools(enhancedPrompt, tools));
    }
}
