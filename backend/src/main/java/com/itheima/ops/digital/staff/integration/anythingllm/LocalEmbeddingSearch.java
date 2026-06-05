package com.itheima.ops.digital.staff.integration.anythingllm;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.ops.digital.staff.entity.OpsFaq;
import com.itheima.ops.digital.staff.mapper.OpsFaqMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Ollama bge-m3嵌入模型的本地FAQ向量搜索
 * 不依赖AnythingLLM，直接在本地做语义匹配
 */
@Slf4j
@Component
public class LocalEmbeddingSearch {

    @Autowired
    private OpsFaqMapper opsFaqMapper;

    @Autowired
    private OllamaProperties ollamaProperties;

    /** FAQ ID -> 嵌入向量 (1024维) */
    private final Map<Long, float[]> faqEmbeddings = new ConcurrentHashMap<>();
    /** FAQ ID -> OpsFaq */
    private final Map<Long, OpsFaq> faqCache = new ConcurrentHashMap<>();

    private volatile boolean embeddingsLoaded = false;
    private static final int MAX_RETRIES = 3;

    /**
     * 启动时异步加载FAQ嵌入，不阻塞应用启动
     */
    @PostConstruct
    public void init() {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // wait for Ollama to be ready
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            refreshEmbeddings();
        }, "faq-embedding-loader").start();
    }

    /**
     * 刷新所有FAQ嵌入向量（新增/修改FAQ后调用）
     */
    public synchronized void refreshEmbeddings() {
        List<OpsFaq> faqList = opsFaqMapper.selectList(
                new LambdaQueryWrapper<OpsFaq>().eq(OpsFaq::getStatus, 1));
        log.info("开始加载FAQ嵌入向量，共{}条", faqList.size());
        faqEmbeddings.clear();
        faqCache.clear();
        int success = 0;
        for (OpsFaq faq : faqList) {
            try {
                float[] embedding = getEmbedding(faq.getQuestion());
                if (embedding != null) {
                    faqEmbeddings.put(faq.getId(), embedding);
                    faqCache.put(faq.getId(), faq);
                    success++;
                }
            } catch (Exception e) {
                log.warn("FAQ[{}]嵌入失败: {}", faq.getId(), e.getMessage());
            }
        }
        log.info("FAQ嵌入加载完成: {}/{}", success, faqList.size());
    }

    /**
     * 为单个FAQ生成并缓存嵌入
     */
    public void addOrUpdateFaq(OpsFaq faq) {
        // 只缓存启用的FAQ
        if (faq.getStatus() != null && faq.getStatus() == 0) {
            removeFaq(faq.getId());
            return;
        }
        try {
            float[] embedding = getEmbedding(faq.getQuestion());
            if (embedding != null) {
                faqEmbeddings.put(faq.getId(), embedding);
                faqCache.put(faq.getId(), faq);
                log.info("FAQ[{}]嵌入已更新", faq.getId());
            }
        } catch (Exception e) {
            log.warn("FAQ[{}]嵌入失败: {}", faq.getId(), e.getMessage());
        }
    }

    /**
     * 从嵌入缓存中移除指定FAQ（停用时调用）
     */
    public void removeFaq(Long faqId) {
        faqEmbeddings.remove(faqId);
        faqCache.remove(faqId);
        log.info("FAQ[{}]已从嵌入缓存移除", faqId);
    }

    /**
     * 搜索最匹配的FAQ
     * @return SearchResult 包含匹配的FAQ和相似度，如果没有高匹配则返回null
     */
    public SearchResult search(String question) {
        if (faqEmbeddings.isEmpty()) {
            log.warn("FAQ嵌入向量为空，尝试刷新");
            refreshEmbeddings();
            if (faqEmbeddings.isEmpty()) {
                return null;
            }
        }

        float[] queryEmbedding = getEmbedding(question);
        if (queryEmbedding == null) {
            return null;
        }

        double bestScore = 0;
        Long bestFaqId = null;

        for (Map.Entry<Long, float[]> entry : faqEmbeddings.entrySet()) {
            double score = cosineSimilarity(queryEmbedding, entry.getValue());
            if (score > bestScore) {
                bestScore = score;
                bestFaqId = entry.getKey();
            }
        }

        if (bestFaqId != null && bestScore >= ollamaProperties.getEmbeddingSimilarityThreshold()) {
            OpsFaq matched = faqCache.get(bestFaqId);
            log.info("本地嵌入匹配成功: score={}, question={}", String.format("%.3f", bestScore), matched.getQuestion());
            return new SearchResult(matched, bestScore);
        }

        log.info("本地嵌入未找到高匹配 FAQ, bestScore={}", String.format("%.3f", bestScore));
        return null;
    }

    /**
     * 搜索top-k相关FAQ作为上下文
     */
    public List<OpsFaq> searchContext(String question, int topK) {
        if (faqEmbeddings.isEmpty()) {
            refreshEmbeddings();
        }

        float[] queryEmbedding = getEmbedding(question);
        if (queryEmbedding == null) {
            return List.of();
        }

        // 计算所有FAQ的相似度，排序取topK
        record ScoredFaq(OpsFaq faq, double score) {}
        List<ScoredFaq> scored = new ArrayList<>();

        for (Map.Entry<Long, float[]> entry : faqEmbeddings.entrySet()) {
            double score = cosineSimilarity(queryEmbedding, entry.getValue());
            OpsFaq faq = faqCache.get(entry.getKey());
            if (faq != null) {
                scored.add(new ScoredFaq(faq, score));
            }
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));

        List<OpsFaq> result = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            if (scored.get(i).score > 0.3) { // 最低相关性阈值
                result.add(scored.get(i).faq);
            }
        }
        return result;
    }

    /**
     * 调用Ollama生成嵌入向量，带重试
     */
    private float[] getEmbedding(String text) {
        String url = ollamaProperties.getBaseUrl() + "/api/embeddings";
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                var body = JSONUtil.createObj();
                body.set("model", ollamaProperties.getEmbeddingModel());
                body.set("prompt", text);

                HttpResponse response = HttpRequest.post(url)
                        .header("Content-Type", "application/json")
                        .timeout(30000)
                        .body(body.toString())
                        .execute();

                if (response.getStatus() != 200) {
                    log.warn("嵌入API返回非200: {}", response.getStatus());
                    if (attempt < MAX_RETRIES - 1) sleepRetry(attempt);
                    continue;
                }

                var json = JSONUtil.parseObj(response.body());
                var embeddingArray = json.getJSONArray("embedding");
                if (embeddingArray == null) return null;

                float[] embedding = new float[embeddingArray.size()];
                for (int i = 0; i < embeddingArray.size(); i++) {
                    embedding[i] = embeddingArray.getFloat(i).floatValue();
                }
                return embedding;
            } catch (Exception e) {
                log.warn("生成嵌入向量失败 (attempt {}/{}): {}", attempt + 1, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES - 1) sleepRetry(attempt);
            }
        }
        log.error("嵌入向量生成最终失败");
        return null;
    }

    private void sleepRetry(int attempt) {
        try {
            Thread.sleep(1000L * (attempt + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 搜索结果
     */
    public record SearchResult(OpsFaq faq, double score) {}
}
