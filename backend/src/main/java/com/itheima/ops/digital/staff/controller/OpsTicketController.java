package com.itheima.ops.digital.staff.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.ops.digital.staff.common.result.Result;
import com.itheima.ops.digital.staff.entity.OpsTicket;
import com.itheima.ops.digital.staff.service.OpsTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "运维工单管理")
@RestController
@RequestMapping("/api/ticket")
public class OpsTicketController {

    @Autowired
    private OpsTicketService opsTicketService;

    @Operation(summary = "分页查询工单列表")
    @GetMapping("/page")
    public Result<IPage<OpsTicket>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          Integer status) {
        return Result.success(opsTicketService.pageQuery(pageNum, pageSize, status));
    }

    @Operation(summary = "用户提交报障工单")
    @PostMapping
    public Result<?> create(@RequestBody OpsTicket ticket) {
        opsTicketService.createTicket(ticket);
        return Result.success();
    }

    @Operation(summary = "处理工单")
    @PutMapping("/handle/{id}")
    public Result<?> handle(@PathVariable Long id, Long handlerId, String handleResult) {
        opsTicketService.handleTicket(id, handlerId, handleResult);
        return Result.success();
    }

    @Operation(summary = "回访添加满意度")
    @PutMapping("/satisfaction/{id}")
    public Result<?> satisfaction(@PathVariable Long id, Integer satisfaction) {
        opsTicketService.addSatisfaction(id, satisfaction);
        return Result.success();
    }
}
