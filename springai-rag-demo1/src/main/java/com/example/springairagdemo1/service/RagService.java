package com.example.springairagdemo1.service;

import com.example.springairagdemo1.service.llm.LlmService;
import com.example.springairagdemo1.service.vectorstore.AgentVectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * RAG服务 - 结合向量存储和LLM的检索增强生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final AgentVectorStoreService vectorStoreService;
    private final LlmService llmService;
    private final ResourceLoader resourceLoader;

    /**
     * 从文本文件加载文档到向量存储
     *
     * @param filePath 文件路径
     * @param metadata 元数据
     * @return 加载的文档数量
     */
    public int loadTextFileToVectorStore(String filePath, Map<String, Object> metadata) {
        try {
            Path path = Paths.get(filePath);
            String content = Files.readString(path);

            // 创建文档
            Document document = new Document(
                    UUID.randomUUID().toString(),
                    content,
                    Map.of(
                            "source", filePath,
                            "type", "text",
                            "filename", path.getFileName().toString()
                    )
            );

            // 合并用户提供的元数据
            if (metadata != null) {
                document.getMetadata().putAll(metadata);
            }

            // 添加到向量存储
            vectorStoreService.addDocument(document);

            log.info("成功加载文本文件到向量存储: {}, 文档ID: {}", filePath, document.getId());
            return 1;

        } catch (IOException e) {
            log.error("加载文本文件失败: {}", filePath, e);
            throw new RuntimeException("加载文本文件失败", e);
        }
    }

    /**
     * 从PDF文件加载文档到向量存储
     *
     * @param resourcePath PDF文件路径（classpath路径）
     * @param metadata     元数据
     * @return 加载的文档数量
     */
    public int loadPdfFileToVectorStore(String resourcePath, Map<String, Object> metadata) {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + resourcePath);

            // 简化版PDF处理 - 在实际项目中可能需要使用专门的PDF解析库
            // 这里提供基本的PDF处理示例
            Document document = new Document(
                    UUID.randomUUID().toString(),
                    "PDF文件内容: " + resource.getFilename(),
                    Map.of(
                            "source", resourcePath,
                            "type", "pdf",
                            "filename", resource.getFilename()
                    )
            );

            // 合并用户提供的元数据
            if (metadata != null) {
                document.getMetadata().putAll(metadata);
            }

            vectorStoreService.addDocument(document);

            log.info("成功加载PDF文件到向量存储: {}", resourcePath);
            return 1;

        } catch (Exception e) {
            log.error("加载PDF文件失败: {}", resourcePath, e);
            throw new RuntimeException("加载PDF文件失败", e);
        }
    }

    /**
     * 从JSON文件加载文档到向量存储
     *
     * @param resourcePath JSON文件路径
     * @param metadata     元数据
     * @return 加载的文档数量
     */
    public int loadJsonFileToVectorStore(String resourcePath, Map<String, Object> metadata) {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + resourcePath);

            // 配置JSON阅读器
            JsonReader reader = new JsonReader(resource, "content", "metadata");

            // 读取文档
            List<Document> documents = reader.get();

            // 为每个文档添加元数据
            documents.forEach(doc -> {
                doc.getMetadata().put("source", resourcePath);
                doc.getMetadata().put("type", "json");
                doc.getMetadata().put("filename", resource.getFilename());

                if (metadata != null) {
                    doc.getMetadata().putAll(metadata);
                }
            });

            // 批量添加到向量存储
            vectorStoreService.addDocuments(documents);

            log.info("成功加载JSON文件到向量存储: {}, 文档数: {}", resourcePath, documents.size());
            return documents.size();

        } catch (Exception e) {
            log.error("加载JSON文件失败: {}", resourcePath, e);
            throw new RuntimeException("加载JSON文件失败", e);
        }
    }

    /**
     * 加载目录下的所有文本文件到向量存储
     *
     * @param directoryPath 目录路径
     * @param metadata       元数据
     * @return 加载的文档数量
     */
    public int loadDirectoryToVectorStore(String directoryPath, Map<String, Object> metadata) {
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            int totalLoaded = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return fileName.endsWith(".txt") || fileName.endsWith(".md");
                    })
                    .mapToInt(path -> {
                        try {
                            String content = Files.readString(path);
                            Document document = new Document(
                                    UUID.randomUUID().toString(),
                                    content,
                                    Map.of(
                                            "source", path.toString(),
                                            "type", "text",
                                            "filename", path.getFileName().toString(),
                                            "directory", directoryPath
                                    )
                            );

                            if (metadata != null) {
                                document.getMetadata().putAll(metadata);
                            }

                            vectorStoreService.addDocument(document);
                            return 1;
                        } catch (IOException e) {
                            log.error("读取文件失败: {}", path, e);
                            return 0;
                        }
                    })
                    .sum();

            log.info("成功加载目录到向量存储: {}, 文档数: {}", directoryPath, totalLoaded);
            return totalLoaded;

        } catch (IOException e) {
            log.error("遍历目录失败: {}", directoryPath, e);
            throw new RuntimeException("遍历目录失败", e);
        }
    }

    /**
     * 检索增强生成问答
     *
     * @param question 用户问题
     * @return 基于检索内容的回答
     */
    public String ragQuestionAnswering(String question) {
        try {
            // 1. 从向量存储中检索相关文档
            List<Document> relevantDocs = vectorStoreService.similaritySearch(question, 5, 0.7);

            if (relevantDocs.isEmpty()) {
                return "抱歉，我在知识库中没有找到与您问题相关的信息。";
            }

            // 2. 构建上下文
            String context = relevantDocs.stream()
                    .map(doc -> String.format("[来源: %s]\n%s",
                            doc.getMetadata().get("source"),
                            doc.getText()))
                    .reduce((a, b) -> a + "\n\n" + b)
                    .orElse("");

            // 3. 构建增强提示
            String enhancedPrompt = String.format(
                    "基于以下上下文信息回答问题。如果上下文中没有相关信息，请诚实地说明。\n\n" +
                    "上下文信息：\n%s\n\n" +
                    "问题：%s\n\n" +
                    "请基于上述上下文提供详细、准确的回答：",
                    context, question
            );

            // 4. 调用LLM生成回答
            String answer = llmService.generateText(enhancedPrompt);

            log.info("RAG问答完成，问题: {}, 检索文档数: {}, 回答长度: {}",
                    question, relevantDocs.size(), answer.length());

            return answer;

        } catch (Exception e) {
            log.error("RAG问答失败，问题: {}", question, e);
            return "抱歉，处理您的问题时遇到了错误，请稍后重试。";
        }
    }

    /**
     * 获取知识库统计信息
     *
     * @return 统计信息
     */
    public Map<String, Object> getKnowledgeBaseStats() {
        try {
            long totalDocuments = vectorStoreService.getDocumentCount();

            return Map.of(
                    "totalDocuments", totalDocuments,
                    "vectorStore", "Milvus",
                    "status", "online"
            );
        } catch (Exception e) {
            log.error("获取知识库统计信息失败", e);
            return Map.of(
                    "totalDocuments", 0,
                    "vectorStore", "Milvus",
                    "status", "error",
                    "error", e.getMessage()
            );
        }
    }

    /**
     * 清空知识库
     * 注意：这是一个危险操作，请谨慎使用
     */
    public void clearKnowledgeBase() {
        try {
            vectorStoreService.clearAll();
            log.warn("知识库已清空");
        } catch (Exception e) {
            log.error("清空知识库失败", e);
            throw new RuntimeException("清空知识库失败", e);
        }
    }

    /**
     * 搜索相关文档
     *
     * @param query 查询文本
     * @param topK  返回结果数量
     * @return 相关文档列表
     */
    public List<Document> searchRelevantDocuments(String query, int topK) {
        try {
            return vectorStoreService.similaritySearch(query, topK, 0.8);
        } catch (Exception e) {
            log.error("搜索相关文档失败，查询: {}", query, e);
            throw new RuntimeException("搜索相关文档失败", e);
        }
    }
}