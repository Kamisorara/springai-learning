package com.example.springairagdemo1.util;

import com.example.springairagdemo1.enums.TextType;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

public class ChatResponseUtil {

    public static ChatResponse createResponse(String statusMessage) {
        return createPureResponse(statusMessage + "\n");
    }

    public static ChatResponse createPureResponse(String message) {
        AssistantMessage assistantMessage = new AssistantMessage(message);
        Generation generation = new Generation(assistantMessage);
        return new ChatResponse(List.of(generation));
    }

    public static ChatResponse createTrimResponse(String message, TextType textType) {
        return createPureResponse(message.replace(textType.getStartSign(), "").replace(textType.getEndSign(), ""));
    }

    public static String getText(ChatResponse chatResponse) {
        Generation result = chatResponse.getResult();
        if (result == null) {
            return "";
        }
        AssistantMessage output = result.getOutput();
        if (output == null) {
            return "";
        }
        return output.getText() == null ? "" : output.getText();
    }

}
