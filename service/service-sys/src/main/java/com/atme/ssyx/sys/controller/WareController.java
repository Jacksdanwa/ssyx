package com.atme.ssyx.sys.controller;


import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.model.sys.Ware;
import com.atme.ssyx.sys.service.WareService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 仓库表 前端控制器
 * </p>
 *
 * @author atguigu
 * @since 2025-02-04
 */
//@CrossOrigin
@RestController
@RequestMapping("/admin/sys/ware")
@Api(tags = "仓库接口")
public class WareController {

    @Autowired
    private WareService wareService;

    //查询所有仓库的列表
    @ApiOperation("查询所有仓库的列表")
    @GetMapping("findAllList")
    public Result findAllList(){
        List<Ware> list = wareService.list();
        return Result.ok(list);
    }
}

