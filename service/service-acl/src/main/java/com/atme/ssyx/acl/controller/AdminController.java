package com.atme.ssyx.acl.controller;

import com.atme.ssyx.acl.service.AdminService;
import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.common.utils.MD5;
import com.atme.ssyx.model.acl.Admin;
import com.atme.ssyx.vo.acl.AdminQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "用户接口")
@RestController
@RequestMapping("/admin/acl/user")
//@CrossOrigin
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private com.atme.ssyx.acl.service.RoleService RoleService;

    //为用户分配角色
    @ApiOperation("为用户分配角色")
    @PostMapping("doAssign")
    public Result doassign(@RequestParam Long adminId, Long[] roleIds){
        RoleService.saveAdminRole(adminId,roleIds);
        return Result.ok(null);
    }

    //获取所有的角色，根据用户id查询已经分配的角色列表
    @ApiOperation("获取所有的角色")
    @GetMapping("toAssign/{adminId}")
    public Result toAssign(@PathVariable Long adminId){
        //map是键值对，适合这样的传参。
        //map返回的数据中会包含两部分数据：  所有角色 和 为用户已经分配的角色列表
        Map<String, Object> map = RoleService.getRoleByAdminId(adminId);
        return Result.ok(map);
    }


    //1 用户列表
    @ApiOperation("用户列表")
    @GetMapping("{current}/{limit}")
    public Result pageList(@PathVariable Long current,
                           @PathVariable Long limit,
                           AdminQueryVo adminQueryVo) {
        Page<Admin> pageParam = new Page<>(current, limit);
        IPage<Admin> pageModel = adminService.selectAdminPage(pageParam, adminQueryVo);
        return Result.ok(pageModel);
    }

    //2 id查询用户
    @ApiOperation("id查询用户")
    @GetMapping("get/{id}")
    public Result getById(@PathVariable Long id) {
        Admin admin = adminService.getById(id);
        return Result.ok(admin);
    }

    //3 添加用户
    @ApiOperation("添加用户")
    @PostMapping("save")
    public Result save(@RequestBody Admin admin){
        //获取输入的密码
        String adminPassword = admin.getPassword();
        //对md5进行加密 MD5
        String passwordMD5 = MD5.encrypt(adminPassword);
        //设置到admin对象中去
        admin.setPassword(passwordMD5);
        //调用方法进行添加
        boolean is_success = adminService.save(admin);
        if (is_success) {
            return Result.ok(null);
        } else {
            return Result.fail(null);
        }
    }
    //4 修改用户
    @ApiOperation("修改用户")
    @PutMapping("update")
    public Result update(@RequestBody Admin admin){
        boolean is_success = adminService.updateById(admin);
        if (is_success) {
            return Result.ok(null);
        } else {
            return Result.fail(null);
        }
    }

    //5 删除用户
    @ApiOperation("删除用户")
    @DeleteMapping("remove/{id}")
    public Result deteleById(@PathVariable Long id){
        boolean is_success = adminService.removeById(id);
        if (is_success) {
            return Result.ok(null);
        } else {
            return Result.fail(null);
        }
    }

    //6 批量删除用户
    @ApiOperation("批量删除用户")
    @DeleteMapping("batchremove")
    public Result batchRemove(@RequestBody List<Long> idlist){
        boolean is_success = adminService.removeByIds(idlist);
        if (is_success) {
            return Result.ok(null);
        } else {
            return Result.fail(null);
        }
    }
}
