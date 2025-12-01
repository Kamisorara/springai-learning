package com.example.springairagdemo1.service.vectorstore.impl;

import com.example.springairagdemo1.config.DataAgentProperties;
import com.example.springairagdemo1.service.vectorstore.AgentVectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Milvus向量存储服务实现类
 */
@Slf4j
@Service
public class AgentVectorStoreServiceImpl implements AgentVectorStoreService {

    private static final String DEFAULT = "default";

    private final VectorStore vectorStore;
    private final DataAgentProperties dataAgentProperties;

    // 使用构造函数注入，避免循环依赖
    public AgentVectorStoreServiceImpl(VectorStore vectorStore, DataAgentProperties dataAgentProperties) {
        this.vectorStore = vectorStore;
        this.dataAgentProperties = dataAgentProperties;
    }

    @Override
    public void addDocument(Document document) {
        try {
            // 添加详细的文档信息日志
            log.info("准备添加文档到向量存储，文档ID: {}, 内容长度: {}, 元数据: {}",
                    document.getId(),
                    document.getText() != null ? document.getText().length() : 0,
                    document.getMetadata());

            // 验证文档内容
            if (document.getText() == null || document.getText().trim().isEmpty()) {
                log.warn("文档内容为空，文档ID: {}", document.getId());
            }

            vectorStore.add(List.of(document));
            log.info("成功添加文档到向量存储，文档ID: {}, 向量维度配置: {}",
                    document.getId(),
                    dataAgentProperties.getVectorStore().getEmbeddingDimension());
        } catch (io.milvus.exception.ParamException e) {
            log.error("Milvus参数错误，文档ID: {}, 错误信息: {}, 可能原因：向量维度不匹配，当前配置维度: {}",
                    document.getId(),
                    e.getMessage(),
                    dataAgentProperties.getVectorStore().getEmbeddingDimension(), e);
            throw new RuntimeException("向量维度配置错误，请检查embedding模型和Milvus配置", e);
        }  catch (Exception e) {
            log.error("添加文档到向量存储失败，文档ID: {}, 错误类型: {}, 错误信息: {}, 配置信息: embeddingModel={}, embeddingDimension={}",
                    document.getId(),
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    "embedding-3",
                    dataAgentProperties.getVectorStore().getEmbeddingDimension(), e);
            throw new RuntimeException("向量存储添加失败", e);
        }
    }

    @Override
    public void addDocuments(List<Document> documents) {
        try {
            vectorStore.add(documents);
            log.info("成功批量添加{}个文档到向量存储", documents.size());
        } catch (Exception e) {
            log.error("批量添加文档到向量存储失败，文档数量: {}", documents.size(), e);
            throw new RuntimeException("向量存储批量添加失败", e);
        }
    }

    @Override
    public List<Document> similaritySearch(String query) {

        List<Document> batch;
        try {
            // 简化实现：使用基本的相似度搜索，过滤基于文档内容
            batch = vectorStore.similaritySearch(query);

            // 根据相似度阈值和数量限制进行过滤
            if (batch.size() > dataAgentProperties.getVectorStore().getBatchDelTopkLimit()) {
                batch = batch.subList(0, dataAgentProperties.getVectorStore().getBatchDelTopkLimit());
            }
            log.info("相似度搜索完成，查询: '{}', 返回{}个结果", query, batch.size());
            return batch;
        } catch (Exception e) {
            log.error("相似度搜索失败，查询: '{}'", query, e);
            throw new RuntimeException("相似度搜索失败", e);
        }
    }

    @Override
    public List<Document> similaritySearch(String query, int topK, double similarityThreshold) {
        try {
            // 使用VectorStore的相似度搜索方法
            List<Document> results = vectorStore.similaritySearch(query);

            // 由于VectorStore接口限制，这里简化处理
            if (results.size() > topK) {
                results = results.subList(0, topK);
            }

            log.info("相似度搜索完成，查询: '{}', topK: {}, 阈值: {}, 返回{}个结果",
                    query, topK, similarityThreshold, results.size());
            return results;
        } catch (Exception e) {
            log.error("相似度搜索失败，查询: '{}', topK: {}, 阈值: {}", query, topK, similarityThreshold, e);
            throw new RuntimeException("相似度搜索失败", e);
        }
    }

    @Override
    public List<Document> searchByMetadata(Map<String, Object> metadata) {
        try {
            // 简化版元数据搜索：先获取所有文档，再过滤
            List<Document> allDocs = vectorStore.similaritySearch("");

            // 获取配置的topK限制
            int topKLimit = dataAgentProperties.getVectorStore().getTopkLimit();

            List<Document> filteredDocs = allDocs.stream()
                    .filter(doc -> matchesMetadata(doc, metadata))
                    .limit(dataAgentProperties.getVectorStore().getTopkLimit())
                    .toList();

            log.info("元数据搜索完成，条件: {}, 返回{}个结果", metadata, filteredDocs.size());
            return filteredDocs;
        } catch (Exception e) {
            log.error("元数据搜索失败，条件: {}", metadata, e);
            throw new RuntimeException("元数据搜索失败", e);
        }
    }

    @Override
    public Document getDocumentById(String id) {
        try {
            // 根据ID获取文档的具体实现可能因向量存储而异
            // 这里使用元数据查询的方式
            Map<String, Object> metadataFilter = Map.of("_id", id);
            List<Document> results = searchByMetadata(metadataFilter);

            if (!results.isEmpty()) {
                Document document = results.get(0);
                log.info("成功获取文档，ID: {}", id);
                return document;
            } else {
                log.warn("未找到文档，ID: {}", id);
                return null;
            }
        } catch (Exception e) {
            log.error("获取文档失败，ID: {}", id, e);
            throw new RuntimeException("获取文档失败", e);
        }
    }

    @Override
    public boolean deleteDocument(String id) {
        try {
            // 从向量存储中删除指定ID的文档
            List<String> idsToDelete = List.of(id);
            vectorStore.delete(idsToDelete);
            log.info("成功删除文档，ID: {}", id);
            return true;
        } catch (Exception e) {
            log.error("删除文档失败，ID: {}", id, e);
            throw new RuntimeException("删除文档失败", e);
        }
    }

    @Override
    public int deleteDocuments(List<String> ids) {
        try {
            int batchSize = dataAgentProperties.getVectorStore().getBatchDelTopkLimit();
            int totalDeleted = 0;

            // 分批删除，避免一次删除过多
            for (int i = 0; i < ids.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, ids.size());
                List<String> batchIds = ids.subList(i, endIndex);

                vectorStore.delete(batchIds);
                totalDeleted += batchIds.size();

                log.info("批量删除文档完成，批次大小: {}, 累计删除: {}", batchIds.size(), totalDeleted);
            }

            log.info("批量删除完成，总共删除{}个文档", totalDeleted);
            return totalDeleted;
        } catch (Exception e) {
            log.error("批量删除文档失败，数量: {}", ids.size(), e);
            throw new RuntimeException("批量删除文档失败", e);
        }
    }

    @Override
    public boolean deleteByMetadata(Map<String, Object> metadata) {
        try {
            // 先查询匹配的文档
            List<Document> documentsToDelete = searchByMetadata(metadata);

            if (documentsToDelete.isEmpty()) {
                log.info("没有找到匹配的文档进行删除，条件: {}", metadata);
                return true;
            }

            // 提取文档ID
            List<String> idsToDelete = documentsToDelete.stream()
                    .map(Document::getId)
                    .toList();

            // 批量删除
            deleteDocuments(idsToDelete);

            log.info("根据元数据删除完成，条件: {}, 删除数量: {}", metadata, idsToDelete.size());
            return true;
        } catch (Exception e) {
            log.error("根据元数据删除失败，条件: {}", metadata, e);
            throw new RuntimeException("根据元数据删除失败", e);
        }
    }

    @Override
    public long getDocumentCount() {
        try {
            // 获取向量存储中的文档总数
            // 注意：由于VectorStore API限制，这里返回一个估算值
            // 在实际应用中，应该使用向量存储的计数API

            // 尝试搜索所有文档（使用空字符串搜索通常会返回所有文档）
            List<Document> allDocs = vectorStore.similaritySearch("");
            long count = allDocs.size();
            log.info("向量存储文档总数: {}", count);
            return count;
        } catch (Exception e) {
            log.error("获取文档总数失败", e);
            throw new RuntimeException("获取文档总数失败", e);
        }
    }

    @Override
    public void clearAll() {
        try {
            // 清空向量存储中的所有文档
            // 注意：这个操作比较危险，实际使用时需要谨慎
            List<Document> allDocs = vectorStore.similaritySearch("");

            if (!allDocs.isEmpty()) {
                List<String> allIds = allDocs.stream()
                        .map(Document::getId)
                        .toList();

                deleteDocuments(allIds);
                log.info("已清空向量存储，删除{}个文档", allIds.size());
            } else {
                log.info("向量存储已经是空的");
            }
        } catch (Exception e) {
            log.error("清空向量存储失败", e);
            throw new RuntimeException("清空向量存储失败", e);
        }
    }

    /**
     * 创建元数据过滤表达式
     */
    private String createFilterExpression(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }

        StringBuilder filter = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (!first) {
                filter.append(" AND ");
            }
            filter.append(entry.getKey()).append(" == ");

            Object value = entry.getValue();
            if (value instanceof String) {
                filter.append("'").append(value).append("'");
            } else {
                filter.append(value);
            }

            first = false;
        }

        return filter.toString();
    }

    /**
     * 检查文档是否匹配元数据条件
     */
    private boolean matchesMetadata(Document document, Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return true;
        }

        Map<String, Object> docMetadata = document.getMetadata();

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            Object expectedValue = entry.getValue();
            Object actualValue = docMetadata.get(key);

            if (expectedValue == null) {
                return actualValue == null;
            }

            if (actualValue == null || !expectedValue.equals(actualValue)) {
                return false;
            }
        }

        return true;
    }
}