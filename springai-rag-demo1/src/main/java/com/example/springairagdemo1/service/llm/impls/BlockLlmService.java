package com.example.springairagdemo1.service.llm.impls;

import com.example.springairagdemo1.service.llm.LlmService;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class BlockLlmService implements LlmService {

    private final ChatClient chatClient;

    @Override
    public Flux<ChatResponse> call(String system, String user) {
        return Mono.fromCallable(() -> chatClient.prompt().system(system).user(user).call().chatResponse()).flux();
    }

    @Override
    public Flux<ChatResponse> callSystem(String system) {
        return Mono.fromCallable(() -> chatClient.prompt().system(system).call().chatResponse()).flux();
    }

    @Override
    public Flux<ChatResponse> callUser(String user) {
        return Mono.fromCallable(() -> chatClient.prompt().user(user).call().chatResponse()).flux();
    }

}
