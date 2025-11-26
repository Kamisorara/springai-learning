package com.example.springaigraphdemo3.controller;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.example.springaigraphdemo3.model.UserUploads;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

@RestController
@RequestMapping("/review")
public class ContentReviewController {
    private final CompiledGraph compiledGraph;
    private static final String UPLOAD_DIR = "uploads/images/";

    public ContentReviewController(@Qualifier("ContentReviewGraph") StateGraph contentReviewGraph) throws Exception {
        SaverConfig saverConfig = SaverConfig.builder()
                .register(new MemorySaver())
                .build();

        this.compiledGraph = contentReviewGraph.compile(
                CompileConfig.builder().saverConfig(saverConfig).build()
        );

        Files.createDirectories(Paths.get(UPLOAD_DIR));
    }

    @PostMapping("/content")
    public Map<String, Object> reviewContent(@RequestBody UserUploads userUploads) throws Exception {
        // 需要运行并行节点，所以要额外在RunnableConfig中指定并行节点的执行器
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .addParallelNodeExecutor("imageReview", ForkJoinPool.commonPool())
                .addParallelNodeExecutor("textReview", ForkJoinPool.commonPool())
                .build();

        Map<String, Object> input = Map.of(
                "text", userUploads.getText(),
                "image_url_lists", userUploads.getImageUrlLists()
        );

        Optional<OverAllState> result = compiledGraph.invoke(input, runnableConfig);
        OverAllState state = result.orElseThrow(() -> new RuntimeException("未能获取审查结果"));

        Map<String, Object> response = new HashMap<>();
        response.put("text_review_result", state.value("text_review_result").orElse(Boolean.FALSE));
        response.put("image_review_result", state.value("image_review_result").orElse(Boolean.FALSE));
        response.put("image_review_processed_count", state.value("image_review_processed_count").orElse(0));
        response.put("image_review_failed_url", state.value("image_review_failed_url").orElse(null));
        response.put("final_review_result", state.value("final_review_result")
                .orElseThrow(() -> new RuntimeException("未能获取最终审查结果")));

        return response;
    }
}
