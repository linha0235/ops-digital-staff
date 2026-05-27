package com.itheima.ops.digital.staff.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.ops.digital.staff.common.result.Result;
import com.itheima.ops.digital.staff.entity.OpsFaq;
import com.itheima.ops.digital.staff.service.OpsFaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Slf4j
@Tag(name = "运维知识库管理")
@RestController
@RequestMapping("/faq")
public class OpsFaqController {

    @Autowired
    private OpsFaqService opsFaqService;

    @Operation(summary = "分页查询FAQ列表")
    @GetMapping("/page")
    public Result<IPage<OpsFaq>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      String category, String keyword) {
        return Result.success(opsFaqService.pageQuery(pageNum, pageSize, category, keyword));
    }

    @Operation(summary = "新增FAQ")
    @PostMapping
    public Result<?> create(@RequestBody OpsFaq faq) {
        opsFaqService.save(faq);
        return Result.success();
    }

    @Operation(summary = "修改FAQ")
    @PutMapping
    public Result<?> update(@RequestBody OpsFaq faq) {
        opsFaqService.updateById(faq);
        return Result.success();
    }

    @Operation(summary = "删除FAQ")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        opsFaqService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "自助问答")
    @GetMapping("/chat")
    public Result<String> chatGet(@RequestParam(defaultValue = "") String question) {
        return Result.success(opsFaqService.chatQuery(question));
    }

    @Operation(summary = "自助问答")
    @PostMapping("/chat")
    public Result<String> chatPost(@RequestBody(required = false) java.util.Map<String,String> req) {
        String question = "";
        if(req != null) {
            question = req.getOrDefault("question", "");
        }
        return Result.success(opsFaqService.chatQuery(question));
    }

    @Operation(summary = "自助问答(流式)")
    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@RequestBody java.util.Map<String, String> req) {
        String question = req != null ? req.getOrDefault("question", "") : "";
        SseEmitter emitter = new SseEmitter(180000L);
        new Thread(() -> {
            try {
                opsFaqService.chatQueryStream(question,
                        text -> {
                            try {
                                emitter.send(SseEmitter.event().data(text));
                            } catch (IOException e) {
                                log.error("SSE发送失败", e);
                            }
                        },
                        () -> emitter.complete(),
                        ex -> emitter.completeWithError(ex));
            } catch (Exception e) {
                log.error("流式问答异常", e);
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }

    @Operation(summary = "同步FAQ到向量知识库")
    @PostMapping("/sync/{id}")
    public Result<Boolean> sync(@PathVariable Long id) {
        return Result.success(opsFaqService.syncFaqToVector(id));
    }

    @Operation(summary = "批量同步FAQ到向量知识库")
    @PostMapping("/syncAll")
    public Result<java.util.Map<String, Object>> syncAll() {
        return Result.success(opsFaqService.syncAllFaqToVector());
    }
}
