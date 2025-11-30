package com.example.springaigraphdemo3.util;

public class JsonCleanerUtil {

    /**
     * 清理模型返回的字符串，移除各种可能的标记，提取有效 JSON 部分。
     *
     * @param response 原始响应字符串
     * @return 清理后的JSON字符串
     */
    public static String cleanJsonString(String response) {
        if (response == null || response.isBlank()) {
            return "{}";
        }

        String cleaned = response.trim();

        // 1. 移除所有类型的特殊标记
        cleaned = cleaned.replaceAll("<\\|[^|]*\\|>", ""); // 移除 <|xxx|> 格式
        cleaned = cleaned.replaceAll("```json\\s*", "");   // 移除 ```json
        cleaned = cleaned.replaceAll("```\\s*", "");       // 移除 ```
        cleaned = cleaned.replaceAll("^[^{\\[]*", "");     // 移除开头非 JSON 字符
        cleaned = cleaned.replaceAll("[^}\\]]*$", "");     // 移除末尾非 JSON 字符

        // 2. 提取第一个完整的 JSON 对象或数组
        int start = -1;
        int end = -1;
        char startChar = '\0';

        // 查找起始位置
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '{' || c == '[') {
                start = i;
                startChar = c;
                break;
            }
        }

        if (start == -1) {
            return "{}";
        }

        // 匹配对应的结束符
        char endChar = (startChar == '{') ? '}' : ']';
        int depth = 0;
        boolean inString = false;
        boolean escapeNext = false;

        for (int i = start; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);

            if (escapeNext) {
                escapeNext = false;
                continue;
            }

            if (c == '\\') {
                escapeNext = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == startChar) {
                    depth++;
                } else if (c == endChar) {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
        }

        if (end > start) {
            return cleaned.substring(start, end + 1);
        }

        return "{}";
    }

    /**
     * 验证字符串是否为有效的 JSON 格式
     *
     * @param str 待验证的字符串
     * @return true 如果是有效的 JSON，否则 false
     */
    public static boolean isValidJson(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        String trimmed = str.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
}
