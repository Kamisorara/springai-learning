package com.example.springaimcpserverdemo1.config;


import com.example.springaimcpserverdemo1.tool.TimeTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具注册
 */
@Configuration
public class McpConfig {
    @Bean
    public ToolCallbackProvider mcpTools(TimeTool timeTool){
        return MethodToolCallbackProvider.builder().toolObjects(timeTool).build();
    }
}
