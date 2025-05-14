package com.atme.ssyx.sys.controller;


import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.model.sys.Region;
import com.atme.ssyx.sys.service.RegionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 地区表 前端控制器
 * </p>
 *
 * @author atguigu
 * @since 2025-02-04
 */
@Api(tags = "地区接口")
@RestController
@RequestMapping("/admin/sys/region")
//@CrossOrigin
public class RegionController {

    @Autowired
    private RegionService regionService;

    //根据区域关键字查询区域信息
    @ApiOperation("根据根据区域关键字查询区域信息")
    @GetMapping("findRegionByKeyword/{keyword}")
    public Result findRegionByKeyword(@PathVariable("keyword") String keyword){
        List<Region> list = regionService.getRegionByKeyword(keyword);
        return Result.ok(list);
    }

}

