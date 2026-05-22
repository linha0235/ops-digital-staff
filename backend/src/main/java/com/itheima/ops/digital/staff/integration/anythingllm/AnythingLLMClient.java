package com.itheima.ops.digital.staff.integration.anythingllm;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnythingLLMClient {

    @Autowired
    private AnythingLLMProperties properties;

    public String chatCompletion(String query) {
        String url = properties.getBaseUrl() + "/v1/workspace/" + properties.getDefaultQueryWorkspace() + "/chat";
        JSONObject body = JSONUtil.createObj();
        body.set("message", query);
        body.set("mode", "chat");
        String result = HttpRequest.post(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(60000)
                .execute()
                .body();
        return JSONUtil.parseObj(result).getStr("textResponse");
    }

    public boolean uploadDocument(String question, String answer) {
        String url = properties.getBaseUrl() + "/v1/document/raw-text";
        JSONObject body = JSONUtil.createObj();
        body.set("textContent", "问题：" + question + "\n答案：" + answer);
        body.set("metadata", JSONUtil.createObj().set("title", question));
        String resp = HttpRequest.post(url)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .header("Content-Type", "application/json")
                .body(body.toString())
                .timeout(30000)
                .execute()
                .body();
        return JSONUtil.parseObj(resp).getBool("success", false);
    }
}
