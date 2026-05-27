package com.itheima.ops.digital.staff.integration.anythingllm;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class AnythingLLMClient {

    @Autowired
    private AnythingLLMProperties properties;

    private static final Pattern FAQ_ANSWER_PATTERN =
            Pattern.compile("答案：(.+?)(?:\n问题：|\\Z)", Pattern.DOTALL);
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.7;

    private String extractAnswer(String responseBody) {
        JSONObject respJson = JSONUtil.parseObj(responseBody);
        JSONArray sources = respJson.getJSONArray("sources");
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        JSONObject topSource = sources.getJSONObject(0);
        double score = topSource.getDouble("score", 0.0);
        if (score < HIGH_CONFIDENCE_THRESHOLD) {
            return null;
        }
        String sourceText = topSource.getStr("text", "");
        Matcher m = FAQ_ANSWER_PATTERN.matcher(sourceText);
        if (m.find()) {
            String answer = m.group(1).trim();
            log.info("高置信度匹配[{}], 直接返回知识库答案", String.format("%.3f", score));
            return answer;
        }
        return null;
    }

    public String chatCompletion(String userQuestion) {
        log.info("=== 调用AnythingLLM RAG知识库 ===");
        log.info("用户问题: {}", userQuestion);

        String rawBody = chatCompletionRaw(userQuestion);

        String directAnswer = extractAnswer(rawBody);
        if (directAnswer != null) {
            return directAnswer;
        }

        JSONObject respJson = JSONUtil.parseObj(rawBody);
        return respJson.getStr("textResponse");
    }

    public void chatCompletionStream(String userQuestion, Consumer<String> onChunk,
                                      Runnable onComplete, Consumer<Exception> onError) {
        // Step 1: query AnythingLLM to get sources and check confidence
        String rawBody;
        try {
            rawBody = chatCompletionRaw(userQuestion);
        } catch (Exception e) {
            log.error("查询AnythingLLM失败", e);
            onError.accept(e);
            return;
        }

        // Step 2: check for high-confidence FAQ match
        String directAnswer = extractAnswer(rawBody);
        if (directAnswer != null) {
            // send answer as single chunk (it's exact FAQ match, instant response)
            onChunk.accept(directAnswer);
            onComplete.run();
            return;
        }

        // Step 3: low confidence — extract context and call Ollama with token-level streaming
        JSONObject respJson = JSONUtil.parseObj(rawBody);
        JSONArray sources = respJson.getJSONArray("sources");

        StringBuilder contextBuilder = new StringBuilder();
        if (sources != null) {
            for (int i = 0; i < sources.size(); i++) {
                JSONObject source = sources.getJSONObject(i);
                String text = source.getStr("text", "");
                if (text != null && !text.isEmpty()) {
                    contextBuilder.append(text).append("\n\n");
                }
            }
        }

        String systemPrompt = "你是一个运维数字员工助手。如果用户问题与提供的知识库内容相关，请严格引用知识库中的答案。如果知识库中没有相关信息，请以专业的方式回答用户问题。";
        String userPrompt;
        if (contextBuilder.length() > 0) {
            userPrompt = "知识库参考内容：\n" + contextBuilder + "用户问题：" + userQuestion;
        } else {
            userPrompt = userQuestion;
        }

        callOllamaStream(systemPrompt, userPrompt, onChunk, onComplete, onError);
    }

    private String chatCompletionRaw(String userQuestion) {
        String url = properties.getBaseUrl() + "/v1/workspace/"
                + properties.getDefaultQueryWorkspace() + "/chat";
        JSONObject requestBody = JSONUtil.createObj();
        requestBody.set("message", userQuestion);
        requestBody.set("mode", "query");
        HttpResponse response = HttpRequest.post(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .timeout(180000)
                .body(requestBody.toString())
                .execute();
        return response.body();
    }

    private void callOllamaStream(String systemPrompt, String userPrompt,
                                   Consumer<String> onChunk, Runnable onComplete,
                                   Consumer<Exception> onError) {
        JSONObject body = JSONUtil.createObj();
        body.set("model", "qwen2.5:3b");
        body.set("stream", true);
        JSONArray messages = JSONUtil.createArray();
        messages.add(JSONUtil.createObj().set("role", "system").set("content", systemPrompt));
        messages.add(JSONUtil.createObj().set("role", "user").set("content", userPrompt));
        body.set("messages", messages);

        try (HttpResponse response = HttpRequest.post("http://localhost:11434/api/chat")
                .header("Content-Type", "application/json")
                .timeout(180000)
                .body(body.toString())
                .execute();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(response.bodyStream(), StandardCharsets.UTF_8))) {

            StringBuilder fullText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                JSONObject data = JSONUtil.parseObj(line);
                JSONObject message = data.getJSONObject("message");
                if (message != null) {
                    String content = message.getStr("content", "");
                    if (!content.isEmpty()) {
                        fullText.append(content);
                        onChunk.accept(fullText.toString());
                    }
                }
                if (data.getBool("done", false)) {
                    onComplete.run();
                    return;
                }
            }
            onComplete.run();
        } catch (Exception e) {
            log.error("Ollama流式调用失败", e);
            onError.accept(e);
        }
    }

    public boolean uploadDocument(String question, String answer) {
        String url = properties.getBaseUrl() + "/v1/document/raw-text";
        log.info("正在上传FAQ到向量知识库，question={}", question);
        try {
            JSONObject body = JSONUtil.createObj();
            body.set("textContent", "问题：" + question + "\n答案：" + answer);
            body.set("metadata", JSONUtil.createObj().set("title", question));
            body.set("addToWorkspaces", properties.getDefaultQueryWorkspace());
            HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(120000)
                    .execute();
            int status = response.getStatus();
            String resp = response.body();
            log.info("上传文档返回 status={}, response={}", status, resp);
            if (status != 200) {
                return false;
            }
            return JSONUtil.parseObj(resp).getBool("success", false);
        } catch (Exception e) {
            log.error("上传FAQ到向量知识库失败", e);
            return false;
        }
    }
}
