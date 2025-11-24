package com.example.springaigraphdemo2.controller;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.example.springaigraphdemo2.model.AnimalInfo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/animal-counting")
public class AnimalCountingController {

    private final CompiledGraph compiledGraph;
    private static final String UPLOAD_DIR = "uploads/images/";

    public AnimalCountingController(@Qualifier("AnimalCountingGraph") StateGraph animalCountingGraph) throws Exception {
        SaverConfig saverConfig = SaverConfig.builder()
                .register(new MemorySaver())
                .build();

        this.compiledGraph = animalCountingGraph.compile(
                CompileConfig.builder().saverConfig(saverConfig).build()
        );

        Files.createDirectories(Paths.get(UPLOAD_DIR));
    }

    @PostMapping("/count-by-url")
    public AnimalInfo countAnimalsByUrl(@RequestBody Map<String, String> request) throws Exception {
        String imageUrl = request.get("imageUrl");
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("图片 URL 不能为空");
        }

        Map<String, Object> input = Map.of("imageUrl", imageUrl);
        RunnableConfig runnableConfig = RunnableConfig.builder().build();
        Optional<OverAllState> result = compiledGraph.invoke(input, runnableConfig);

        // 修改为获取 animalInfo 对象
        return (AnimalInfo) result.get().value("animalInfo")
                .orElseThrow(() -> new RuntimeException("未能获取统计结果"));
    }

    @PostMapping("/count-by-upload")
    public AnimalInfo countAnimalsByUpload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("只支持图片文件");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = Paths.get(UPLOAD_DIR + filename);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String localPath = filePath.toAbsolutePath().toString();
        Map<String, Object> input = Map.of("imageUrl", "file:" + localPath);
        RunnableConfig runnableConfig = RunnableConfig.builder().build();
        Optional<OverAllState> result = compiledGraph.invoke(input, runnableConfig);

        // 修改为获取 animalInfo 对象
        return (AnimalInfo) result.get().value("animalInfo")
                .orElseThrow(() -> new RuntimeException("未能获取统计结果"));
    }

    @PostMapping("/count")
    public AnimalInfo countAnimals(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "imageUrl", required = false) String imageUrl) throws Exception {

        if (file != null && !file.isEmpty()) {
            return countAnimalsByUpload(file);
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            return countAnimalsByUrl(Map.of("imageUrl", imageUrl));
        } else {
            throw new IllegalArgumentException("请提供图片文件或图片 URL");
        }
    }
}
