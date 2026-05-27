package com.itheima.ops.digital.staff.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.ops.digital.staff.common.result.Result;
import com.itheima.ops.digital.staff.entity.SysUser;
import com.itheima.ops.digital.staff.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "运维账号管理")
@RestController
@RequestMapping("/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Operation(summary = "分页查询账号列表")
    @GetMapping("/page")
    public Result<IPage<SysUser>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       String username, String realName, Integer status) {
        return Result.success(sysUserService.queryPage(pageNum, pageSize, username, realName, status));
    }

    @Operation(summary = "新增运维账号")
    @PostMapping
    public Result<?> create(@RequestBody SysUser user) {
        sysUserService.createUser(user);
        return Result.success();
    }

    @Operation(summary = "修改运维账号信息")
    @PutMapping
    public Result<?> update(@RequestBody SysUser user) {
        sysUserService.updateUser(user);
        return Result.success();
    }

    @Operation(summary = "删除运维账号")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "冻结运维账号")
    @PutMapping("/freeze/{id}")
    public Result<?> freeze(@PathVariable Long id) {
        sysUserService.freezeUser(id);
        return Result.success();
    }
}
