package com.itheima.ops.digital.staff.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.ops.digital.staff.common.result.Result;
import com.itheima.ops.digital.staff.entity.OpsTicket;
import com.itheima.ops.digital.staff.service.OpsFaqService;
import com.itheima.ops.digital.staff.service.OpsTicketService;
import com.itheima.ops.digital.staff.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "仪表盘统计")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private OpsFaqService opsFaqService;
    @Autowired
    private OpsTicketService opsTicketService;

    @Operation(summary = "获取首页统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = new HashMap<>();

        long userCount = sysUserService.count();
        long faqCount = opsFaqService.count();
        long pendingTicketCount = opsTicketService.count(
                new LambdaQueryWrapper<OpsTicket>().eq(OpsTicket::getStatus, 0));

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        long todayHandledCount = opsTicketService.count(
                new LambdaQueryWrapper<OpsTicket>()
                        .eq(OpsTicket::getStatus, 2)
                        .between(OpsTicket::getUpdateTime, todayStart, todayEnd));

        data.put("userCount", userCount);
        data.put("faqCount", faqCount);
        data.put("pendingTicketCount", pendingTicketCount);
        data.put("todayHandledCount", todayHandledCount);
        return Result.success(data);
    }
}
