package com.atme.ssyx.acl.service.impl;

import com.atme.ssyx.acl.mapper.AdminRoleMapper;
import com.atme.ssyx.acl.service.AdminRoleService;
import com.atme.ssyx.model.acl.AdminRole;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AdminRoleServiceImpl extends ServiceImpl<AdminRoleMapper, AdminRole> implements AdminRoleService {
}
