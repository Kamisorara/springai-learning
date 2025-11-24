package com.example.springaigraphdemo2.config;

import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.KeyStrategyFactoryBuilder;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.example.springaigraphdemo2.graphNode.AnimalCountingNode;
import com.example.springaigraphdemo2.graphNode.MergeNode;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

@Configuration
public class AnimalCountingGraph {

    @Resource
    private AnimalCountingNode animalCountingNode;

    @Resource
    private MergeNode mergeNode;

    @Bean
    public StateGraph AnimalCountingGraph() throws GraphStateException {
        // 配置状态键的合并策略
        KeyStrategyFactory strategyFactory = new KeyStrategyFactoryBuilder()
                        .addPatternStrategy("count", new ReplaceStrategy())
                        .build();

        // 构建 Graph
        StateGraph stateGraph = new StateGraph(strategyFactory);
        // 添加节点
        stateGraph.addNode("animalCounting", node_async(animalCountingNode))
                .addNode("merge", node_async(mergeNode));

        // 配置边: START -> animalCounting -> merge -> END
        stateGraph.addEdge(START, "animalCounting")
                .addEdge("animalCounting", "merge")
                .addEdge("merge", END);

        return stateGraph;
    }
}
