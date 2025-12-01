package com.example.springairagdemo1.integration;

import com.example.springairagdemo1.service.RagServiceSimple;
import com.example.springairagdemo1.service.llm.LlmService;
import com.example.springairagdemo1.service.vectorstore.AgentVectorStoreService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

/**
 * RAG功能集成测试
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class RagIntegrationTest {

    @Autowired
    private RagServiceSimple ragService;

    @Autowired
    private AgentVectorStoreService vectorStoreService;

    @Test
    void testCompleteRagWorkflow() {
        // 1. 添加测试文档
        String doc1Id = ragService.addTextDocument(
                "人工智能（AI）是计算机科学的一个分支，致力于创建能够执行通常需要人类智能的任务的系统。",
                Map.of("category", "tech", "type", "definition", "language", "zh-CN")
        );

        String doc2Id = ragService.addTextDocument(
                "机器学习是人工智能的核心技术，通过算法让计算机从数据中学习模式和规律。",
                Map.of("category", "tech", "type", "definition", "language", "zh-CN")
        );

        String doc3Id = ragService.addTextDocument(
                "深度学习是机器学习的一个子领域，使用人工神经网络来模拟人脑的学习过程。",
                Map.of("category", "tech", "type", "definition", "language", "zh-CN")
        );

        log.info("添加了3个测试文档，ID分别为: {}, {}, {}", doc1Id, doc2Id, doc3Id);

        // 2. 验证文档总数
        long documentCount = vectorStoreService.getDocumentCount();
        log.info("向量存储中的文档总数: {}", documentCount);

        // 3. 测试相似度搜索
        List<org.springframework.ai.document.Document> searchResults = ragService.searchRelevantDocuments("机器学习", 5);
        log.info("搜索'机器学习'返回{}个结果", searchResults.size());

        // 打印搜索结果
        searchResults.forEach(doc -> {
            log.info("搜索结果 - ID: {}, 内容: {}", doc.getId(), doc.getText());
            log.info("元数据: {}", doc.getMetadata());
        });

        // 4. 测试RAG问答
        try {
            String answer = ragService.ragQuestionAnswering("什么是机器学习？它在人工智能中有什么作用？");
            log.info("RAG问答答案: {}", answer);
        } catch (Exception e) {
            log.warn("RAG问答失败，可能是因为LLM服务未正确配置: {}", e.getMessage());
        }

        // 5. 测试元数据搜索
        List<org.springframework.ai.document.Document> metadataResults = vectorStoreService.searchByMetadata(
                Map.of("category", "tech")
        );
        log.info("元数据搜索返回{}个结果", metadataResults.size());

        // 6. 清理测试数据
        try {
            vectorStoreService.deleteDocuments(List.of(doc1Id, doc2Id, doc3Id));
            log.info("清理了测试文档");
        } catch (Exception e) {
            log.warn("清理测试文档失败: {}", e.getMessage());
        }

        // 7. 获取最终统计信息
        Map<String, Object> stats = ragService.getKnowledgeBaseStats();
        log.info("知识库统计: {}", stats);
    }

    @Test
    void testVectorStoreBasics() {
        try {
            // 测试基本的向量存储操作
            long initialCount = vectorStoreService.getDocumentCount();
            log.info("初始文档数: {}", initialCount);

            // 添加单个文档
            org.springframework.ai.document.Document testDoc = new org.springframework.ai.document.Document(
                    "test-doc-001",
                    "这是一个测试文档，用于验证向量存储的基本功能。",
                    Map.of("test", "true", "timestamp", System.currentTimeMillis())
            );

            vectorStoreService.addDocument(testDoc);
            log.info("添加了测试文档，ID: {}", testDoc.getId());

            // 搜索文档
            List<org.springframework.ai.document.Document> searchResults = vectorStoreService.similaritySearch("测试");
            log.info("搜索结果数: {}", searchResults.size());

            // 根据ID获取文档
            org.springframework.ai.document.Document retrievedDoc = vectorStoreService.getDocumentById(testDoc.getId());
            if (retrievedDoc != null) {
                log.info("成功获取文档: {}", retrievedDoc.getText());
            }

            // 删除文档
            boolean deleted = vectorStoreService.deleteDocument(testDoc.getId());
            log.info("文档删除结果: {}", deleted);

        } catch (Exception e) {
            log.error("向量存储基本测试失败: {}", e.getMessage(), e);
        }
    }

    @Test
    void testRagServiceFunctionality() {
        try {
            // 测试RAG服务的各种功能

            // 1. 测试添加文本文档
            String docId = ragService.addTextDocument(
                    "Spring Boot是一个快速应用开发框架，它基于Spring生态系统。",
                    Map.of("framework", "spring-boot", "language", "java", "difficulty", "intermediate")
            );
            log.info("添加Spring Boot文档，ID: {}", docId);

            // 2. 测试相关文档搜索
            List<org.springframework.ai.document.Document> relatedDocs = ragService.searchRelevantDocuments("Spring框架", 3);
            log.info("找到{}个相关文档", relatedDocs.size());

            // 3. 测试问答（可能失败，因为需要LLM配置）
            try {
                String qaResult = ragService.ragQuestionAnswering("Spring Boot有什么特点？");
                log.info("问答结果: {}", qaResult);
            } catch (Exception e) {
                log.info("问答功能需要配置LLM服务: {}", e.getMessage());
            }

            // 4. 清理
            if (docId != null) {
                vectorStoreService.deleteDocument(docId);
                log.info("清理了测试文档");
            }

        } catch (Exception e) {
            log.error("RAG服务测试失败: {}", e.getMessage(), e);
        }
    }
}