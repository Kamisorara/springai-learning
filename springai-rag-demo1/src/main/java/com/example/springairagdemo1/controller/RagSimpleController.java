package com.example.springairagdemo1.controller;

import com.example.springairagdemo1.service.RagServiceSimple;
import com.example.springairagdemo1.service.vectorstore.AgentVectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 简化版RAG服务控制器
 * 提供向量存储和检索增强生成的核心REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/rag-simple")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RagSimpleController {

    private final RagServiceSimple ragService;
    private final AgentVectorStoreService vectorStoreService;

    /**
     * 检索增强生成问答
     *
     * @param request 请求体
     * @return 回答
     */
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, String> request) {
        try {
            String question = request.get("question");
            if (question == null || question.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "问题不能为空"
                ));
            }

            String answer = ragService.ragQuestionAnswering(question);

            return ResponseEntity.ok(Map.of(
                    "question", question,
                    "answer", answer,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("问答失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "处理问题时发生错误: " + e.getMessage()
            ));
        }
    }

    /**
     * 相似度搜索
     *
     * @param query 查询文本
     * @param topK  返回结果数量（可选，默认10）
     * @return 相关文档列表
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> similaritySearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int topK) {

        try {
            if (query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "查询内容不能为空"
                ));
            }

            List<Document> documents = ragService.searchRelevantDocuments(query, topK);

            return ResponseEntity.ok(Map.of(
                    "query", query,
                    "results", documents.stream().map(doc -> Map.of(
                            "id", doc.getId(),
                            "content", doc.getText(),
                            "metadata", doc.getMetadata()
                    )).toList(),
                    "count", documents.size(),
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("搜索失败，查询: {}", query, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "搜索失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 添加文本文档到向量存储
     *
     * @param request 请求体
     * @return 添加结果
     */
    @PostMapping("/documents")
    public ResponseEntity<Map<String, Object>> addTextDocument(@RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "文档内容不能为空"
                ));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) request.getOrDefault("metadata", Map.of());

            String documentId = ragService.addTextDocument(content, metadata);

            return ResponseEntity.ok(Map.of(
                    "message", "文档添加成功",
                    "documentId", documentId,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("添加文档失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "添加文档失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取知识库统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = ragService.getKnowledgeBaseStats();

            return ResponseEntity.ok(Map.of(
                    "stats", stats,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "获取统计信息失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取文档总数
     *
     * @return 文档总数
     */
    @GetMapping("/documents/count")
    public ResponseEntity<Map<String, Object>> getDocumentCount() {
        try {
            long count = vectorStoreService.getDocumentCount();

            return ResponseEntity.ok(Map.of(
                    "count", count,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("获取文档总数失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "获取文档总数失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 清空知识库
     * 注意：这是一个危险操作，请谨慎使用
     *
     * @return 操作结果
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearKnowledgeBase() {
        try {
            ragService.clearKnowledgeBase();

            return ResponseEntity.ok(Map.of(
                    "message", "知识库已清空",
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("清空知识库失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "清空知识库失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 健康检查接口
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            long documentCount = vectorStoreService.getDocumentCount();
            return ResponseEntity.ok(Map.of(
                    "status", "healthy",
                    "vectorStore", "Milvus",
                    "documentCount", documentCount,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                    "status", "unhealthy",
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }
}