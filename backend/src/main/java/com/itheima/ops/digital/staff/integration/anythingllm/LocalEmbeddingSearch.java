package com.itheima.ops.digital.staff.integration.anythingllm;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
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

    /** FAQ ID -> 嵌入向量 (1024维) */
    private final Map<Long, float[]> faqEmbeddings = new ConcurrentHashMap<>();
    /** FAQ ID -> OpsFaq */
    private final Map<Long, OpsFaq> faqCache = new ConcurrentHashMap<>();

    private static final double SIMILARITY_THRESHOLD = 0.75;
    private static final String EMBEDDING_MODEL = "bge-m3:latest";
    private static final String OLLAMA_EMBED_URL = "http://localhost:11434/api/embeddings";

    /**
     * 启动时加载所有FAQ嵌入
     */
    @PostConstruct
    public void init() {
        refreshEmbeddings();
    }

    /**
     * 刷新所有FAQ嵌入向量（新增/修改FAQ后调用）
     */
    public synchronized void refreshEmbeddings() {
        List<OpsFaq> faqList = opsFaqMapper.selectList(null);
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

        if (bestFaqId != null && bestScore >= SIMILARITY_THRESHOLD) {
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
     * 调用Ollama生成嵌入向量
     */
    private float[] getEmbedding(String text) {
        try {
            var body = JSONUtil.createObj();
            body.set("model", EMBEDDING_MODEL);
            body.set("prompt", text);

            HttpResponse response = HttpRequest.post(OLLAMA_EMBED_URL)
                    .header("Content-Type", "application/json")
                    .timeout(30000)
                    .body(body.toString())
                    .execute();

            var json = JSONUtil.parseObj(response.body());
            var embeddingArray = json.getJSONArray("embedding");
            if (embeddingArray == null) return null;

            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.getFloat(i).floatValue();
            }
            return embedding;
        } catch (Exception e) {
            log.error("生成嵌入向量失败", e);
            return null;
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
