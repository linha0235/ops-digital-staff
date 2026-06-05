package com.itheima.ops.digital.staff.integration.anythingllm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {
    private String baseUrl = "http://localhost:11434";
    private String chatModel = "qwen2.5:3b";
    private String embeddingModel = "bge-m3:latest";
    private int timeout = 300000;
    private int numPredict = 2048;
    private double temperature = 0.7;
    private double embeddingSimilarityThreshold = 0.75;
}
