package com.atme.ssyx.product.controller;


import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.model.product.Category;
import com.atme.ssyx.product.service.CategoryService;
import com.atme.ssyx.vo.product.CategoryQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 商品三级分类 前端控制器
 * </p>
 *
 * @author atguigu
 * @since 2025-02-12
 */
@Api(tags = "目录接口")
@RestController
@RequestMapping("/admin/product/category")
//@CrossOrigin

public class CategoryController {

    @Autowired
    public CategoryService categoryService;

    //商品分类列表
    @ApiOperation("商品分类列表")
    @GetMapping("{page}/{limit}")
    public Result list(@PathVariable Long page,
                       @PathVariable Long limit,
                       CategoryQueryVo categoryQueryVo){
        Page<Category> pageParam = new Page<>(page, limit);
        IPage<Category> pageModel = categoryService.selectPageCategory(pageParam, categoryQueryVo);
        return Result.ok(pageModel);
    }

    @ApiOperation("获取商品分类信息")
    @GetMapping("{id}")
    public Result idlist(@PathVariable Long id){
        Category category = categoryService.getById(id);
        return Result.ok(category);
    }

    @ApiOperation("新增商品分类")
    @PostMapping("save")
    public Result save(@RequestBody Category category){
        categoryService.save(category);
        return Result.ok(null);
    }

    @ApiOperation("修改商品分类")
    @PutMapping("update")
    public Result update(@RequestBody Category category){
        categoryService.updateById(category);
        return Result.ok(null);
    }

    @ApiOperation("删除商品分类")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id){
        categoryService.removeById(id);
        return Result.ok(null);
    }

    @ApiOperation("根据id列表删除商品分类")
    @DeleteMapping("batchRemove")
    public Result batchRemove(@RequestBody List<Long> ids){
        categoryService.removeByIds(ids);
        return Result.ok(null);
    }

    @ApiOperation("查询所有商品的分类")
    @GetMapping("findAllList")
    public Result findAllList(){
        List<Category> list = categoryService.list();
        return Result.ok(list);
    }
}

