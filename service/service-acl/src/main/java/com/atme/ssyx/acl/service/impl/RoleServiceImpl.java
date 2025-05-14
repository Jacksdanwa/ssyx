package com.atme.ssyx.acl.service.impl;

import com.atme.ssyx.acl.mapper.RoleMapper;
import com.atme.ssyx.acl.service.AdminRoleService;
import com.atme.ssyx.acl.service.RolePermissionService;
import com.atme.ssyx.acl.service.RoleService;
import com.atme.ssyx.model.acl.AdminRole;
import com.atme.ssyx.model.acl.Role;
import com.atme.ssyx.vo.acl.RoleQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    //用户角色关系
    @Autowired
    private AdminRoleService adminRoleService;

    @Autowired
    private RolePermissionService rolePermissionService;


    @Override
    public IPage<Role> selectRolePage(Page<Role> pageParam, RoleQueryVo roleQueryVo) {
        //获取条件值
        String roleName = roleQueryVo.getRoleName();
        //封装对象
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        //判断条件值是否为空，为空查全部，不为空封装查询条件
        if(!roleName.isEmpty()) {
            wrapper.like(Role::getRoleName,roleName);
        }

        //调用方法实现条件分页查询
        //不使用autowired注入的原因是我们引入的ServiceImpl实现类中已经提前引入了mapper接口，所以不需要再注入
        IPage<Role> rolePage = baseMapper.selectPage(pageParam, wrapper);
        //返回分页对象
        return rolePage;
    }

    @Override
    public Map<String, Object> getRoleByAdminId(Long adminId) {
        //1 查询所有的角色
        List<Role> roleList = baseMapper.selectList(null);

        //2 根据用户id查询用户已经分配的角色列表
        //2.1 根据用户id查询 用户角色关系表  admin_role 查询角色分配角色id列表
        //返回List<AdminRole>
        LambdaQueryWrapper<AdminRole> queryWrapper = new LambdaQueryWrapper<>();
        //设置查询条件，根据adminId完成
        queryWrapper.eq(AdminRole::getAdminId, adminId);
        List<AdminRole> adminRoleslist = adminRoleService.list(queryWrapper);
        //2.2 通过第一步返回的集合，获取所有角色的id列表 List<Long>
        List<Long> roleIdList = adminRoleslist.stream()
                     .map(item -> item.getRoleId())
                     .collect(Collectors.toList());
        //2.3 创建一个新的list集合，用于存储用户已经分配的角色
        List<Role> assginRoleList = new ArrayList<>();
        //2.4 遍历所有角色列表 allRolesList, 得到每个角色
        for (Role role : roleList) {
            if (roleIdList.contains(role.getId())) {
                assginRoleList.add(role);
            }
        }
         //判断所有角色里面是否包含已经分配的角色id，封装到新的list集合中

        Map<String, Object> result = new HashMap<>();
        result.put("allRolesList",roleList);
        result.put("assignRoles" ,assginRoleList);
        return result;
    }

    //为用户进行分配
    @Override
    public void saveAdminRole(Long adminId, Long[] roleIds) {
        //删除用户已经分配过的角色数据
        //根据用户id删除admin_role表中对应的数据
        LambdaQueryWrapper<AdminRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AdminRole::getAdminId,adminId);
        adminRoleService.remove(queryWrapper);

        //2 重新分配
        // 先构建一个List来存储所有要插入的AdminRole对象
        List<AdminRole> adminRoles = new ArrayList<>();
        for (Long roleId : roleIds) {
            AdminRole adminRole = new AdminRole();
            adminRole.setAdminId(adminId);
            adminRole.setRoleId(roleId);
            adminRoles.add(adminRole);
        }
        // 批量保存所有角色与用户的关系
        adminRoleService.saveBatch(adminRoles);
    }
}