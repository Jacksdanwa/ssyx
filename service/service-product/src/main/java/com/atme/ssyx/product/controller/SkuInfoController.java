package com.atme.ssyx.product.controller;


import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.model.product.SkuInfo;
import com.atme.ssyx.product.service.SkuInfoService;
import com.atme.ssyx.vo.product.SkuInfoQueryVo;
import com.atme.ssyx.vo.product.SkuInfoVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * sku信息 前端控制器
 * </p>
 *
 * @author atguigu
 * @since 2025-02-12
 */
@RestController
@RequestMapping("/admin/product/skuInfo")
//@CrossOrigin

@Api(tags = "sku端接口")
public class SkuInfoController {

    @Autowired
    private SkuInfoService skuInfoservice;

    @ApiOperation("sku列表")
    @GetMapping("{page}/{limit}")
    public Result list(@PathVariable Long page,
                       @PathVariable Long limit,
                       SkuInfoQueryVo skuInfoQueryVo) {
        Page<SkuInfo> pageParam = new Page<>(page, limit);
        IPage<SkuInfo> pageModel = skuInfoservice.selectPageSkuInfo(pageParam, skuInfoQueryVo);
        return Result.ok(pageModel);
    }

    @ApiOperation("添加商品的sku信息")
    @PostMapping("save")
    public Result save(@RequestBody SkuInfoVo skuInfoVo) {
        skuInfoservice.saveSkuInfo(skuInfoVo);
        return Result.ok(null);
    }

    @ApiOperation("根据id获取商品sku信息")
    @GetMapping("get/{id}")
    public Result getSkuInfoById(@PathVariable Long id) {
        SkuInfoVo skuInfoVo = skuInfoservice.getSkuInfo(id);
        return Result.ok(skuInfoVo);
    }

    @ApiOperation("修改sku")
    @PutMapping("update")
    public Result update(@RequestBody SkuInfoVo skuInfoVo) {
        skuInfoservice.updateSkuInfo(skuInfoVo);
        return Result.ok(null);
    }

    @ApiOperation(value = "删除")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        skuInfoservice.removeById(id);
        return Result.ok(null);
    }

    @ApiOperation(value = "根据id列表删除")
    @DeleteMapping("batchRemove")
    public Result batchRemove(@RequestBody List<Long> idList) {
        skuInfoservice.removeByIds(idList);
        return Result.ok(null);
    }

    @ApiOperation("商品审核")
    @GetMapping("check/{skuId}/{status}")
    public Result check(@PathVariable Long skuId,
                        @PathVariable Integer status){
        skuInfoservice.check(skuId,status);
        return Result.ok(null);
    }


    @ApiOperation("商品上架")
    @GetMapping("publish/{skuId}/{status}")
    public Result publish(@PathVariable Long skuId,
                          @PathVariable Integer status){
        skuInfoservice.publish(skuId,status);
        return Result.ok(null);
    }

    @ApiOperation("新人专享")
    @GetMapping("isNewPerson/{skuId}/{status}")
    public Result isNewPerson(@PathVariable Long skuId,
                              @PathVariable Integer status) {
        skuInfoservice.isNewPerson(skuId, status);
        return Result.ok(null);
    }
}

