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
}
