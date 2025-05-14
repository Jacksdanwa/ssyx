package com.atme.ssyx.acl.service.impl;


import com.atme.ssyx.acl.mapper.PermissionMapper;
import com.atme.ssyx.acl.service.PermissionService;
import com.atme.ssyx.acl.service.RolePermissionService;
import com.atme.ssyx.acl.utils.PermissionHelper;
import com.atme.ssyx.model.acl.Permission;
import com.atme.ssyx.model.acl.RolePermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    @Autowired
    private RolePermissionService rolePermissionService;

    @Override
    public List<Permission> queryAllPermission() {
        //查询所有菜单
        List<Permission> allPermissionList = baseMapper.selectList(null);
        //转换要求的数据格式
        List<Permission> result = PermissionHelper.buildPermission(allPermissionList);

        return result;
    }

    @Override
    public void removechildById(Long id) {
        //创建集合
        List<Long> idlist = new ArrayList<>();
        //由id查询idlist
        this.DeletePermissionChildList(id, idlist);
        //调用方法删除
        baseMapper.deleteBatchIds(idlist);
    }

    private void DeletePermissionChildList(Long id, List<Long> idlist) {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getPid, id);
        List<Permission> childlist = baseMapper.selectList(wrapper);


        //递归查询是否还有子菜单
        childlist.stream().forEach(item -> {
            //封装菜单id到idlist集合中
            idlist.add(item.getId());
            //递归
            this.DeletePermissionChildList(item.getId(), idlist);
        });

    }

    @Override
    public Map<String, Object> getPermissionByRoleId(Long roleId) {
        //查询所有权限
        List<Permission> permissionList = baseMapper.selectList(null);

        //2 根据角色id查询角色已经拥有的权限
        // 创建一个查询类
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();

        //比较类中roleid相同的值
        wrapper.eq(RolePermission::getRoleId, roleId);
        //取出后放到List集合中
        List<RolePermission> rolePermissionList = rolePermissionService.list(wrapper);
        //通过第一步返回的集合，获取所有权限的id列表 List<Long>
        List<Long> RolePermissionList = rolePermissionList.stream()
                .map(item -> item.getPermissionId())
                .collect(Collectors.toList());

        //创建空的为所有的权限准备的表
        List<Permission> permissionList1 = new ArrayList<>();

        for (Permission p : permissionList) {
            if (RolePermissionList.contains(p.getId())) {
                permissionList1.add(p);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("permissionList", permissionList);
        result.put("PermissionList1", permissionList1);
        return result;


    }

    //给某个角色授权
    @Override
    public void saveRolePermission(Long roleId, Long[] permissionId) {
        //删除角色授权
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, roleId);
        rolePermissionService.remove(wrapper);

        //角色授权
        List<RolePermission> rolePermissionList = new ArrayList<>();
        for (Long permissionId1 : permissionId){
            RolePermission rolePermission = new RolePermission();
            rolePermission.setPermissionId(permissionId1);
            rolePermission.setRoleId(roleId);
            rolePermissionList.add(rolePermission);
        }

        rolePermissionService.saveBatch(rolePermissionList);
    }


}
