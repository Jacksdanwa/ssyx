package com.atme.ssyx.acl.service;

import com.atme.ssyx.model.acl.Role;
import com.atme.ssyx.vo.acl.RoleQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface RoleService extends IService<Role> {
    //1 角色列表 条件分页查询
    IPage<Role> selectRolePage(Page<Role> pageParam, RoleQueryVo roleQueryVo);
    //获取用户角色数据
    Map<String, Object> getRoleByAdminId(Long adminId);
    //分配角色
    void saveAdminRole(Long adminId, Long[] roleIds);
}
