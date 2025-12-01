package com.example.springairagdemo1.service.vectorstore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class AgentVectorStoreServiceTest {

    @Autowired
    private AgentVectorStoreService vectorStoreService;

    private final String TEST_DOC_ID = "test-doc-001";
    private final String TEST_CONTENT = "这是一个测试文档，包含了关于人工智能和机器学习的内容。";

    @BeforeEach
    void setUp() {
        // 清理测试数据
        try {
            vectorStoreService.deleteDocument(TEST_DOC_ID);
        } catch (Exception e) {
            // 忽略删除失败的异常
        }
    }

    @Test
    void testAddAndGetDocument() {
        // 准备测试数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "tech");
        metadata.put("author", "test");

        Document document = new Document(TEST_DOC_ID, TEST_CONTENT, metadata);

        // 添加文档
        assertDoesNotThrow(() -> vectorStoreService.addDocument(document));

        // 验证文档总数
        long count = vectorStoreService.getDocumentCount();
        assertTrue(count > 0, "文档总数应该大于0");
    }

    @Test
    void testBatchAddDocuments() {
        // 准备测试数据
        List<Document> documents = List.of(
                new Document("test-doc-001", "机器学习是人工智能的一个分支", Map.of("category", "AI")),
                new Document("test-doc-002", "深度学习使用神经网络进行学习", Map.of("category", "AI")),
                new Document("test-doc-003", "自然语言处理处理人类语言", Map.of("category", "NLP"))
        );

        // 批量添加文档
        assertDoesNotThrow(() -> vectorStoreService.addDocuments(documents));

        // 验证文档总数
        long count = vectorStoreService.getDocumentCount();
        assertTrue(count >= 3, "文档总数应该至少为3");
    }

    @Test
    void testSimilaritySearch() {
        // 先添加测试文档
        Document document = new Document(TEST_DOC_ID, TEST_CONTENT, Map.of("category", "tech"));
        vectorStoreService.addDocument(document);

        // 相似度搜索
        List<Document> results = vectorStoreService.similaritySearch("人工智能");

        assertNotNull(results, "搜索结果不应为空");
        assertFalse(results.isEmpty(), "应该找到相关文档");

        // 验证搜索结果包含我们添加的文档
        boolean found = results.stream()
                .anyMatch(doc -> TEST_DOC_ID.equals(doc.getId()));
        assertTrue(found, "搜索结果应该包含测试文档");
    }

    @Test
    void testSimilaritySearchWithParameters() {
        // 添加测试文档
        vectorStoreService.addDocuments(List.of(
                new Document("test-doc-001", "机器学习算法", Map.of("category", "ML")),
                new Document("test-doc-002", "深度学习框架", Map.of("category", "DL")),
                new Document("test-doc-003", "人工智能应用", Map.of("category", "AI"))
        ));

        // 自定义参数搜索
        List<Document> results = vectorStoreService.similaritySearch("机器学习", 2, 0.5);

        assertNotNull(results, "搜索结果不应为空");
        assertTrue(results.size() <= 2, "结果数量不应超过2");
    }

    @Test
    void testSearchByMetadata() {
        // 添加测试文档
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "tech");
        metadata.put("author", "test-author");

        Document document = new Document(TEST_DOC_ID, TEST_CONTENT, metadata);
        vectorStoreService.addDocument(document);

        // 根据元数据搜索
        Map<String, Object> searchMetadata = Map.of("author", "test-author");
        List<Document> results = vectorStoreService.searchByMetadata(searchMetadata);

        assertNotNull(results, "搜索结果不应为空");
        assertFalse(results.isEmpty(), "应该找到匹配的文档");
    }

    @Test
    void testDeleteDocument() {
        // 添加测试文档
        Document document = new Document(TEST_DOC_ID, TEST_CONTENT, Map.of("category", "test"));
        vectorStoreService.addDocument(document);

        // 删除文档
        boolean deleted = vectorStoreService.deleteDocument(TEST_DOC_ID);
        assertTrue(deleted, "删除操作应该成功");

        // 验证文档已被删除
        Document retrieved = vectorStoreService.getDocumentById(TEST_DOC_ID);
        assertNull(retrieved, "删除后应该无法获取文档");
    }

    @Test
    void testBatchDeleteDocuments() {
        // 添加测试文档
        List<String> docIds = List.of("test-doc-001", "test-doc-002", "test-doc-003");
        List<Document> documents = docIds.stream()
                .map(id -> new Document(id, "测试内容 " + id, Map.of("batch", "test")))
                .toList();

        vectorStoreService.addDocuments(documents);

        // 批量删除
        int deletedCount = vectorStoreService.deleteDocuments(docIds);
        assertEquals(docIds.size(), deletedCount, "删除数量应该匹配");
    }

    @Test
    void testDeleteByMetadata() {
        // 添加测试文档
        List<Document> documents = List.of(
                new Document("test-doc-001", "内容1", Map.of("toDelete", "true")),
                new Document("test-doc-002", "内容2", Map.of("toDelete", "true")),
                new Document("test-doc-003", "内容3", Map.of("toKeep", "true"))
        );

        vectorStoreService.addDocuments(documents);

        // 根据元数据删除
        boolean deleted = vectorStoreService.deleteByMetadata(Map.of("toDelete", "true"));
        assertTrue(deleted, "删除操作应该成功");
    }

    @Test
    void testGetDocumentCount() {
        long initialCount = vectorStoreService.getDocumentCount();

        // 添加文档
        vectorStoreService.addDocuments(List.of(
                new Document("test-doc-001", "内容1", Map.of()),
                new Document("test-doc-002", "内容2", Map.of())
        ));

        long finalCount = vectorStoreService.getDocumentCount();
        assertTrue(finalCount >= initialCount + 2, "文档数量应该增加");
    }

    @Test
    void testClearAll() {
        // 添加一些测试文档
        vectorStoreService.addDocuments(List.of(
                new Document("test-doc-001", "内容1", Map.of()),
                new Document("test-doc-002", "内容2", Map.of())
        ));

        // 清空所有文档（这个测试比较危险，需要小心使用）
        // vectorStoreService.clearAll();

        // 验证清空结果
        // long count = vectorStoreService.getDocumentCount();
        // assertEquals(0, count, "清空后文档数量应该为0");

        // 注意：实际测试时请谨慎使用clearAll方法
    }
}