package com.example.springaigraphdemo3;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ApiTest {

    @Autowired
    private ChatClient chatClient;

    @Test
    public void testTextApi() {
        try {
            String response = chatClient.prompt("Hello, are you working?").call().content();
            System.out.println("Text API Response: " + response);
        } catch (Exception e) {
            System.err.println("Text API Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}