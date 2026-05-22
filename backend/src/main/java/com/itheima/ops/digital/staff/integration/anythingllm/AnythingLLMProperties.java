package com.itheima.ops.digital.staff.integration.anythingllm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "anything-llm")
public class AnythingLLMProperties {
    private String baseUrl;
    private String apiKey;
    private String defaultQueryWorkspace;
}
