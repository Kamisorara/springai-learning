package com.example.springairagdemo1.service.llm.impls;

import com.example.springairagdemo1.service.llm.LlmService;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallbackProvider;
import reactor.core.publisher.Flux;

@AllArgsConstructor
public class StreamLlmService implements LlmService {
    private final ChatClient chatClient;

    @Override
    public Flux<ChatResponse> call(String system, String user) {
        return chatClient.prompt().system(system).user(user).stream().chatResponse();
    }

    @Override
    public Flux<ChatResponse> callSystem(String system) {
        return chatClient.prompt().system(system).stream().chatResponse();
    }

    @Override
    public Flux<ChatResponse> callUser(String user) {
        return chatClient.prompt().user(user).stream().chatResponse();
    }

    @Override
    public Flux<ChatResponse> callWithTools(String system, String user, ToolCallbackProvider tools) {
        return chatClient.prompt()
                .system(system)
                .user(user)
                .toolCallbacks(tools)
                .stream()
                .chatResponse();
    }

    @Override
    public Flux<ChatResponse> callUserWithTools(String user, ToolCallbackProvider tools) {
        return chatClient.prompt()
                .user(user)
                .toolCallbacks(tools)
                .stream()
                .chatResponse();
    }

    @Override
    public Flux<ChatResponse> callSystemWithTools(String system, ToolCallbackProvider tools) {
        return chatClient.prompt()
                .system(system)
                .toolCallbacks(tools)
                .stream()
                .chatResponse();
    }

}
