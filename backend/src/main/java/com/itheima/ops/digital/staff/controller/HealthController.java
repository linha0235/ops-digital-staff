package com.itheima.ops.digital.staff.controller;

import com.itheima.ops.digital.staff.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("运维数字员工系统后端服务正常运行");
    }
}
