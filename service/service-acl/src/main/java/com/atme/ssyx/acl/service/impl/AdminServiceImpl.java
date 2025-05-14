package com.atme.ssyx.acl.service.impl;

import com.atme.ssyx.acl.mapper.AdminMapper;
import com.atme.ssyx.acl.service.AdminService;
import com.atme.ssyx.model.acl.Admin;
import com.atme.ssyx.vo.acl.AdminQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {
    @Override
    public IPage<Admin> selectAdminPage(Page<Admin> pageParam, AdminQueryVo adminQueryVo) {
        String username = adminQueryVo.getUsername();
        String name = adminQueryVo.getName();
        LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
        if (!StringUtils.isEmpty(username)){
            queryWrapper.eq(Admin::getUsername,username);
        }
        if (!StringUtils.isEmpty(name)){
            queryWrapper.like(Admin::getName,name);
        }
        Page<Admin> adminPage = baseMapper.selectPage(pageParam, queryWrapper);
        return adminPage;
    }
}
