package com.atme.ssyx.acl.service.impl;

import com.atme.ssyx.acl.mapper.RolePermissionMapper;
import com.atme.ssyx.acl.service.RolePermissionService;
import com.atme.ssyx.model.acl.RolePermission;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission> implements RolePermissionService {
}
