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
        if (StringUtils.isBlank(user.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (StringUtils.isBlank(user.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (lambdaQuery().eq(SysUser::getUsername, user.getUsername()).count() > 0) {
            throw new IllegalArgumentException("用户名已存在: " + user.getUsername());
        }
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        user.setStatus(1);
        save(user);
    }

    @Override
    public void updateUser(SysUser user) {
        if (StringUtils.isNotBlank(user.getPassword())) {
            user.setPassword(BCrypt.hashpw(user.getPassword()));
        }
        // 检查用户名唯一性（排除自身）
        if (StringUtils.isNotBlank(user.getUsername())) {
            long count = lambdaQuery()
                    .eq(SysUser::getUsername, user.getUsername())
                    .ne(SysUser::getId, user.getId())
                    .count();
            if (count > 0) {
                throw new IllegalArgumentException("用户名已存在: " + user.getUsername());
            }
        }
        updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("无效的用户ID");
        }
        removeById(id);
    }

    @Override
    public void freezeUser(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("无效的用户ID");
        }
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(0);
        updateById(user);
    }
}
