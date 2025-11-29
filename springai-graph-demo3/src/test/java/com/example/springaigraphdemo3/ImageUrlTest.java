package com.example.springaigraphdemo3;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 测试URL处理的效果
 */
@SpringBootTest
public class ImageUrlTest {

    @Test
    public void testUrlValidation() {
        // 这里可以测试各种URL格式
        String[] testUrls = {
            "https://example.com/image.jpg",
            "https://example.com/image with spaces.png",
            "https://example.com/image%20with%20encoding.jpg",
            "file:/path/to/local/image.png"
        };

        for (String url : testUrls) {
            System.out.println("测试URL: " + url);
            // 这里可以调用ImageReviewNode的方法进行测试
        }
    }
}