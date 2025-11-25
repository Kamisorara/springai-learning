package com.example.springaigraphdemo3.config;

import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.KeyStrategyFactoryBuilder;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.example.springaigraphdemo3.graphNode.ImageReviewNode;
import com.example.springaigraphdemo3.graphNode.ReviewMergeNode;
import com.example.springaigraphdemo3.graphNode.TextReviewNode;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

@Configuration
public class ContentReviewGraph {

    @Resource
    private TextReviewNode textReviewNode;

    @Resource
    private ImageReviewNode imageReviewNode;

    @Resource
    private ReviewMergeNode reviewMergeNode;

    @Bean
    public StateGraph ContentReviewGraph() throws GraphStateException {
        KeyStrategyFactory strategyFactory = new KeyStrategyFactoryBuilder()
                .addPatternStrategy("text_review_result", new ReplaceStrategy())
                .addPatternStrategy("image_review_result", new ReplaceStrategy())
                .addPatternStrategy("final_review_result", new ReplaceStrategy())
                .build();

        StateGraph stateGraph = new StateGraph(strategyFactory);

        // 添加节点
        stateGraph.addNode("textReview", node_async(textReviewNode))
                .addNode("imageReview", node_async(imageReviewNode))
                .addNode("reviewMerge", node_async(reviewMergeNode));

        // 并行开始:START 同时连接到两个审查节点
        stateGraph.addEdge(START, "textReview")
                .addEdge(START, "imageReview");

        // 两个审查节点都连接到 merge 节点
        stateGraph.addEdge("textReview", "reviewMerge")
                .addEdge("imageReview", "reviewMerge");

        // merge 节点连接到 END
        stateGraph.addEdge("reviewMerge", END);

        return stateGraph;
    }
}
