package com.example.springairagdemo1.service.vectorstore;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * 向量存储服务接口
 * 提供基于Milvus的向量存储和检索功能
 */
public interface AgentVectorStoreService {

    /**
     * 添加单个文档到向量存储
     *
     * @param document 要添加的文档
     */
    void addDocument(Document document);

    /**
     * 批量添加文档到向量存储
     *
     * @param documents 要添加的文档列表
     */
    void addDocuments(List<Document> documents);

    /**
     * 相似度搜索 - 使用默认参数
     *
     * @param query 查询文本
     * @return 相似文档列表
     */
    List<Document> similaritySearch(String query);

    /**
     * 相似度搜索 - 自定义参数
     *
     * @param query             查询文本
     * @param topK              返回结果数量
     * @param similarityThreshold 相似度阈值
     * @return 相似文档列表
     */
    List<Document> similaritySearch(String query, int topK, double similarityThreshold);

    /**
     * 根据元数据搜索文档
     *
     * @param metadata 元数据条件
     * @return 匹配的文档列表
     */
    List<Document> searchByMetadata(Map<String, Object> metadata);

    /**
     * 根据文档ID获取文档
     *
     * @param id 文档ID
     * @return 文档对象，如果未找到返回null
     */
    Document getDocumentById(String id);

    /**
     * 删除指定ID的文档
     *
     * @param id 文档ID
     * @return 删除是否成功
     */
    boolean deleteDocument(String id);

    /**
     * 批量删除文档
     *
     * @param ids 要删除的文档ID列表
     * @return 实际删除的文档数量
     */
    int deleteDocuments(List<String> ids);

    /**
     * 根据元数据删除匹配的文档
     *
     * @param metadata 元数据条件
     * @return 删除是否成功
     */
    boolean deleteByMetadata(Map<String, Object> metadata);

    /**
     * 获取向量存储中的文档总数
     *
     * @return 文档总数
     */
    long getDocumentCount();

    /**
     * 清空向量存储中的所有文档
     * 注意：这是一个危险操作，请谨慎使用
     */
    void clearAll();
}
