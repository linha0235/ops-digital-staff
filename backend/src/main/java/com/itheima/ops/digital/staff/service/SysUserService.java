package com.itheima.ops.digital.staff.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.ops.digital.staff.entity.SysUser;

public interface SysUserService extends IService<SysUser> {
    IPage<SysUser> queryPage(Integer pageNum, Integer pageSize, String username, String realName, Integer status);
    void createUser(SysUser user);
    void updateUser(SysUser user);
    void deleteUser(Long id);
    void freezeUser(Long id);
}
