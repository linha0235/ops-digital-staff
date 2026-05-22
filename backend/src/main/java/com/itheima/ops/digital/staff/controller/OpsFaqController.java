package com.itheima.ops.digital.staff.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.ops.digital.staff.common.result.Result;
import com.itheima.ops.digital.staff.entity.OpsFaq;
import com.itheima.ops.digital.staff.service.OpsFaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "运维知识库管理")
@RestController
@RequestMapping("/api/faq")
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
    public Result<String> chat(@RequestParam String question) {
        return Result.success(opsFaqService.chatQuery(question));
    }

    @Operation(summary = "同步FAQ到向量知识库")
    @PostMapping("/sync/{id}")
    public Result<Boolean> sync(@PathVariable Long id) {
        return Result.success(opsFaqService.syncFaqToVector(id));
    }
}
