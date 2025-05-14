package com.atme.ssyx.sys.controller;


import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.model.sys.RegionWare;
import com.atme.ssyx.sys.service.RegionWareService;
import com.atme.ssyx.vo.sys.RegionWareQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 城市仓库关联表 前端控制器
 * </p>
 *
 * @author atguigu
 * @since 2025-02-04
 */
@Api(tags = "开通区域接口")
@RestController
@RequestMapping("/admin/sys/regionWare")
//@CrossOrigin

public class RegionWareController {

    @Autowired
    private RegionWareService regionWareService;

    //开通区域列表
    @ApiOperation("开通区域列表")
    @GetMapping("{page}/{limit}")
    public Result getPageList(@PathVariable Long page,
                              @PathVariable Long limit,
                              RegionWareQueryVo regionWareQueryVo){
        Page<RegionWare> page1 = new Page<>(page,limit);
        IPage<RegionWare> pagemodel = regionWareService.selectPage(page1,regionWareQueryVo);
        return Result.ok(pagemodel);
    }

    //添加开通区域
    @ApiOperation("添加开通区域")
    @PostMapping("save")
    public Result addRegionWare(@RequestBody RegionWare regionWare){
        regionWareService.saveRegionWare(regionWare);
        return Result.ok(null);
    }

    //删除开通区域
    @ApiOperation("删除开通区域")
    @DeleteMapping("remove/{id}")
    public Result removeRegionById(@PathVariable Long id){
        regionWareService.removeById(id);
        return Result.ok(null);
    }


    //取消开通区域
    @ApiOperation("取消开通区域")
    @PostMapping("updateStatus/{id}/{status}")
    public Result updateStatus(@PathVariable Long id, Integer status){
        regionWareService.updateStatus(id,status);
        return Result.ok(null);
    }

}

