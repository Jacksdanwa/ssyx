package com.atme.ssyx.acl.service;

import com.atme.ssyx.model.acl.Permission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;


public interface PermissionService extends IService<Permission> {
    //查询所有菜单
    List<Permission> queryAllPermission();

    //递归删除菜单
    void removechildById(Long id);

    Map<String, Object> getPermissionByRoleId(Long roleId);

    void saveRolePermission(Long roleId, Long[] permissionId);
}
