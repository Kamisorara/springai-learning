package com.example.springaigraphdemo1.controller;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.example.springaigraphdemo1.model.ProductInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final CompiledGraph compiledGraph;

    public ProductController(@Qualifier("productEnrichmentGraph") StateGraph productEnrichmentGraph) throws GraphStateException {
        // 配置检查点保存器 - 使用内存保存器存储图的执行状态
        SaverConfig saverConfig = SaverConfig.builder()
                .register(new MemorySaver())
                .build();

        // 编译状态图 - 将定义好的图转换为可执行的编译图
        this.compiledGraph = productEnrichmentGraph.compile(
                CompileConfig.builder().saverConfig(saverConfig).build()
        );
    }

    @PostMapping("/enrich")
    public ProductInfo enrichProduct(@RequestBody String description) throws GraphRunnerException {
        Map<String, Object> initialState = Map.of("description", description);
        // 配置运行参数
        RunnableConfig runnableConfig = RunnableConfig.builder().build();
        // 调用编译图执行工作流 - 返回最终状态
        Optional<OverAllState> result = compiledGraph.invoke(initialState, runnableConfig);
        // 从结果状态中提取产品信息并返回
        return (ProductInfo) result.get().value("productInfo").orElseThrow();
    }

    @GetMapping("/test")
    public String test() {
        return "Product Enrichment API is running!";
    }
}
