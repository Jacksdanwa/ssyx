package com.atme.ssyx.acl.controller;

import com.atme.ssyx.acl.service.PermissionService;
import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.model.acl.Permission;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "权限管理接口")
@RestController
@RequestMapping("/admin/acl/permission")
//@CrossOrigin

public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @ApiOperation("查看某个角色的权限列表")
    @GetMapping("toAssgin/{roleId}")
    public Result toAssgin(@PathVariable Long roleId){
        Map<String,Object> map = permissionService.getPermissionByRoleId(roleId);
        return Result.ok(map);
    }

    @ApiOperation("给某个角色授权")
    @PostMapping("doAssign")
    public Result doAssign(@RequestParam Long roleId, Long[] permissionId){
        permissionService.saveRolePermission(roleId, permissionId);
        return Result.ok(null);
    }



    @ApiOperation("查询所有菜单")
    @GetMapping
    public Result list(){
        List<Permission> list = permissionService.queryAllPermission();
        return Result.ok(list);
    }

    @ApiOperation("添加菜单")
    @PostMapping("save")
    public Result save(@RequestBody Permission permission){
        permissionService.save(permission);
        return Result.ok(null);
    }

    @ApiOperation("修改菜单")
    @PutMapping("update")
    public Result update(@RequestBody Permission permission){
        permissionService.updateById(permission);
        return Result.ok(null);
    }

    @ApiOperation("递归删除菜单")
    @DeleteMapping("remove/{id }")
    public Result delete(@PathVariable Long id){
        permissionService.removechildById(id);
        return Result.ok(null);
    }

}
