package com.example.springairagdemo1.service.llm;

import com.example.springairagdemo1.config.DataAgentProperties;
import com.example.springairagdemo1.service.llm.impls.BlockLlmService;
import com.example.springairagdemo1.service.llm.impls.StreamLlmService;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class LlmServiceFactory implements FactoryBean<LlmService> {

    private final DataAgentProperties properties;

    private final ChatClient chatClient;

    @Override
    public LlmService getObject() {
        if (LlmServiceEnum.BLOCK.equals(properties.getLlmServiceType())) {
            return new BlockLlmService(chatClient);
        }
        else {
            return new StreamLlmService(chatClient);
        }
    }

    @Override
    public Class<?> getObjectType() {
        return LlmService.class;
    }

}
