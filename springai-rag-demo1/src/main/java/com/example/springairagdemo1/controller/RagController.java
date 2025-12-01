package com.example.springairagdemo1.controller;

import com.example.springairagdemo1.service.RagService;
import com.example.springairagdemo1.service.vectorstore.AgentVectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * RAG服务控制器
 * 提供向量存储和检索增强生成的REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RagController {

    private final RagService ragService;
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
     * 添加文档到向量存储
     *
     * @param request 请求体
     * @return 添加结果
     */
    @PostMapping("/documents")
    public ResponseEntity<Map<String, Object>> addDocument(@RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "文档内容不能为空"
                ));
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) request.getOrDefault("metadata", Map.of());

            String documentId = java.util.UUID.randomUUID().toString();
            Document document = new Document(documentId, content, metadata);
            vectorStoreService.addDocument(document);

            return ResponseEntity.ok(Map.of(
                    "message", "文档添加成功",
                    "documentId", document.getId(),
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
     * 上传文档文件
     *
     * @param file 上传的文件
     * @param title 文档标题（可选）
     * @param source 文档来源（可选）
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "source", required = false) String source) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "上传文件不能为空"
                ));
            }

            // 读取文件内容
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);

            // 构建元数据
            Map<String, Object> metadata = Map.of(
                    "filename", file.getOriginalFilename(),
                    "contentType", file.getContentType(),
                    "size", file.getSize(),
                    "uploadTime", System.currentTimeMillis()
            );

            // 添加可选元数据
            Map<String, Object> fullMetadata = new java.util.HashMap<>(metadata);
            if (title != null && !title.trim().isEmpty()) {
                fullMetadata.put("title", title);
            }
            if (source != null && !source.trim().isEmpty()) {
                fullMetadata.put("source", source);
            }

            // 创建文档并添加到向量存储
            String documentId = java.util.UUID.randomUUID().toString();
            Document document = new Document(documentId, content, fullMetadata);
            vectorStoreService.addDocument(document);

            log.info("文档上传成功: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

            return ResponseEntity.ok(Map.of(
                    "message", "文档上传成功",
                    "documentId", document.getId(),
                    "filename", file.getOriginalFilename(),
                    "size", file.getSize(),
                    "contentType", file.getContentType(),
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (IOException e) {
            log.error("文件读取失败: {}", file.getOriginalFilename(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "文件读取失败: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("文档上传失败: {}", file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "文档上传失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 批量上传文档文件
     *
     * @param files 上传的文件数组
     * @return 上传结果
     */
    @PostMapping("/upload/batch")
    public ResponseEntity<Map<String, Object>> uploadDocuments(
            @RequestParam("files") MultipartFile[] files) {

        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "上传文件列表不能为空"
                ));
            }

            List<Document> documents = new java.util.ArrayList<>();
            int successCount = 0;
            List<String> failedFiles = new java.util.ArrayList<>();

            for (MultipartFile file : files) {
                try {
                    if (!file.isEmpty()) {
                        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

                        Map<String, Object> metadata = Map.of(
                                "filename", file.getOriginalFilename(),
                                "contentType", file.getContentType(),
                                "size", file.getSize(),
                                "uploadTime", System.currentTimeMillis()
                        );

                        String documentId = java.util.UUID.randomUUID().toString();
                        Document document = new Document(documentId, content, metadata);
                        documents.add(document);
                        successCount++;
                    } else {
                        failedFiles.add(file.getOriginalFilename() + " (文件为空)");
                    }
                } catch (Exception e) {
                    log.error("处理文件失败: {}", file.getOriginalFilename(), e);
                    failedFiles.add(file.getOriginalFilename() + " (" + e.getMessage() + ")");
                }
            }

            // 批量添加成功的文档
            if (!documents.isEmpty()) {
                vectorStoreService.addDocuments(documents);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "批量上传完成",
                    "totalFiles", files.length,
                    "successCount", successCount,
                    "failedCount", failedFiles.size(),
                    "failedFiles", failedFiles,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("批量上传失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "批量上传失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 批量添加文档
     *
     * @param request 请求体
     * @return 添加结果
     */
    @PostMapping("/documents/batch")
    public ResponseEntity<Map<String, Object>> addDocuments(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documentsData = (List<Map<String, Object>>) request.get("documents");

            if (documentsData == null || documentsData.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "文档列表不能为空"
                ));
            }

            List<Document> documents = documentsData.stream()
                    .map(docData -> {
                        String content = (String) docData.get("content");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> metadata = (Map<String, Object>) docData.getOrDefault("metadata", Map.of());
                        String documentId = java.util.UUID.randomUUID().toString();
                        return new Document(documentId, content, metadata);
                    })
                    .toList();

            vectorStoreService.addDocuments(documents);

            return ResponseEntity.ok(Map.of(
                    "message", "批量添加成功",
                    "count", documents.size(),
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("批量添加文档失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "批量添加文档失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 删除文档
     *
     * @param documentId 文档ID
     * @return 删除结果
     */
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String documentId) {
        try {
            boolean deleted = vectorStoreService.deleteDocument(documentId);

            return ResponseEntity.ok(Map.of(
                    "message", deleted ? "文档删除成功" : "文档不存在",
                    "documentId", documentId,
                    "deleted", deleted,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("删除文档失败，ID: {}", documentId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "删除文档失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 根据元数据搜索文档
     *
     * @param metadata 元数据键值对（请求参数）
     * @return 搜索结果
     */
    @GetMapping("/documents/search")
    public ResponseEntity<Map<String, Object>> searchByMetadata(@RequestParam Map<String, Object> metadata) {
        try {
            List<Document> documents = vectorStoreService.searchByMetadata(metadata);

            return ResponseEntity.ok(Map.of(
                    "metadata", metadata,
                    "results", documents.stream().map(doc -> Map.of(
                            "id", doc.getId(),
                            "content", doc.getText(),
                            "metadata", doc.getMetadata()
                    )).toList(),
                    "count", documents.size(),
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("元数据搜索失败，条件: {}", metadata, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "元数据搜索失败: " + e.getMessage()
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
}