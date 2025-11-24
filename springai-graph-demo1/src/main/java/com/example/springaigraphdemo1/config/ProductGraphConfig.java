package com.example.springaigraphdemo1.config;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.example.springaigraphdemo1.graphNode.MarketingCopyNode;
import com.example.springaigraphdemo1.graphNode.MergeNode;
import com.example.springaigraphdemo1.graphNode.SpecificationExtractionNode;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class ProductGraphConfig {

    @Resource
    private MarketingCopyNode marketingCopyNode;
    @Resource
    private SpecificationExtractionNode specificationExtractionNode;
    @Resource
    private MergeNode mergeNode;

    @Bean
    public StateGraph productEnrichmentGraph() throws GraphStateException {
        // 配置状态键的合并策略
        // 定义各个状态字段的更新策略(这里全部使用替换策略)
        KeyStrategyFactory keyStrategyFactory = new KeyStrategyFactoryBuilder()
                .addPatternStrategy("description", new ReplaceStrategy())
                .addPatternStrategy("slogan", new ReplaceStrategy())
                .addPatternStrategy("material", new ReplaceStrategy())
                .addPatternStrategy("colors", new ReplaceStrategy())
                .addPatternStrategy("season", new ReplaceStrategy())
                .addPatternStrategy("productInfo", new ReplaceStrategy())
                .build();

        // 构建 Graph
        StateGraph graph = new StateGraph(keyStrategyFactory);

        // 添加节点(使用异步执行)
        graph.addNode("marketing", node_async(marketingCopyNode))
                .addNode("specification", node_async(specificationExtractionNode))
                .addNode("merge", node_async(mergeNode));

        // 配置边: START -> 并行执行 marketing 和 specification -> merge -> END
        graph.addEdge(START, "marketing")
                .addEdge(START, "specification")
                .addEdge("marketing", "merge")
                .addEdge("specification", "merge")
                .addEdge("merge", END);

        // 打印 Graph 流程图
        GraphRepresentation representation = graph.getGraph(
                GraphRepresentation.Type.PLANTUML,
                "Product Enrichment Graph");
        System.out.println("\n=== Product Enrichment Graph UML Flow ===");
        System.out.println(representation.content());
        System.out.println("=========================================\n");

        return graph;
    }
}
