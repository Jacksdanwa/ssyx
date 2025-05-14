package com.atme.ssyx.acl.controller;

import com.atme.ssyx.acl.service.RoleService;
import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.model.acl.Role;
import com.atme.ssyx.vo.acl.RoleQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "角色接口")
@RestController
@RequestMapping("/admin/acl/role")
//@CrossOrigin

public class RoleController {

    @Autowired
    private RoleService roleService;
    //1 角色列表 条件分页查询

    /**
     * 查询
     * @param current 当前页
     * @param limit  每页记录数
     * @param roleQueryVo 条件对象，根据名称查询
     * @return Result
     */
    @ApiOperation(value = "角色条件分页查询")
    @GetMapping("{current}/{limit}")
    public Result pageList(@PathVariable Long current,
                           @PathVariable Long limit,
                           RoleQueryVo roleQueryVo) {
        //1 创建page对象，传递每页的参数以及记录数
        //通过对象传递给前端叫params
        Page<Role> pageParam = new Page<>(current,limit);

        //2 调用service方法实现条件分页查询
        IPage<Role> pageModel = roleService.selectRolePage(pageParam,roleQueryVo);
        return Result.ok(pageModel);
    }

    //2 根据id查询角色
    @ApiOperation("根据id查询角色")
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id) {
        Role role = roleService.getById(id);
        return Result.ok(role);
    }

    //3 添加角色
    @ApiOperation("添加角色")
    @PostMapping("save")
    //通过@RequestBody注解，将前端传递的json数据封装到实体类中
    public Result save(@RequestBody Role role){
        boolean is_success = roleService.save(role);
        if (is_success) {
            return Result.ok(null);
        } else {
            return Result.fail(null);
        }
    }

    //4 修改角色
    @ApiOperation("修改角色")
    @PutMapping("update")
    public Result update(@RequestBody Role role) {
        boolean is_success = roleService.updateById(role);
        if (is_success) {
            return Result.ok(null);
        } else {
            return Result.fail(null);
        }
    }

    //5 根据id删除角色
    @ApiOperation("根据id删除角色")
    @DeleteMapping("remove/{id}")
    public Result deleteById(@PathVariable Long id) {
        boolean is_success = roleService.removeById(id);
        if (is_success) {
            return Result.ok(null);
        } else {
            return Result.fail(null);
        }
    }

    //6 批量删除角色
    //json的数组格式对应的是java中数组的集合
    @ApiOperation("批量删除角色")
    @DeleteMapping("batchremove")
    public Result batchRemove(@RequestBody List<Long> idList){
        boolean is_success = roleService.removeByIds(idList);
        if (is_success) {
            return Result.ok(null);
        } else {
            return Result.fail(null);
        }
    }
}
