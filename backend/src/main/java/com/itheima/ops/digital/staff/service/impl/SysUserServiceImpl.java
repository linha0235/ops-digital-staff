package com.itheima.ops.digital.staff.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.ops.digital.staff.entity.SysUser;
import com.itheima.ops.digital.staff.mapper.SysUserMapper;
import com.itheima.ops.digital.staff.service.SysUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public IPage<SysUser> queryPage(Integer pageNum, Integer pageSize, String username, String realName, Integer status) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(username), SysUser::getUsername, username)
                .like(StringUtils.isNotBlank(realName), SysUser::getRealName, realName)
                .eq(status != null, SysUser::getStatus, status);
        return page(page, wrapper);
    }

    @Override
    public void createUser(SysUser user) {
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        user.setStatus(1);
        save(user);
    }

    @Override
    public void updateUser(SysUser user) {
        updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        removeById(id);
    }

    @Override
    public void freezeUser(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(0);
        updateById(user);
    }
}
