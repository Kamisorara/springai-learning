package com.example.springaidemo1.controller;

import com.example.springaidemo1.resp.ChatResponse;
import com.example.springaidemo1.service.JsonAgentService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat-test")
public class TestController {
    @Resource
    private JsonAgentService jsonAgentService;
    private static final String DEFAULT_PROMPT = "你是一个博学的智能聊天助手，请根据用户提问回答！";

    private final ChatClient chatClient;

    public TestController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem(DEFAULT_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @GetMapping("/query")
    public String queryChatModel(@RequestParam String query) {
        return chatClient.prompt(query).call().content();
    }

    @GetMapping("/streaming-query")
    public Flux<String> streamingChat(@RequestParam(value = "query", defaultValue = DEFAULT_PROMPT) String query, HttpServletResponse httpServletResponse) {
        httpServletResponse.setCharacterEncoding("UTF-8");
        return chatClient.prompt(query).stream().content();
    }

    @GetMapping("/json-agent")
    public ChatResponse jsonAgent(@RequestParam String query) {
        return jsonAgentService.processQuery(query);
    }

    @GetMapping("/raw-json")
    public String rawJson(@RequestParam String query) {
        return jsonAgentService.getRawJsonResponse(query);
    }
}
