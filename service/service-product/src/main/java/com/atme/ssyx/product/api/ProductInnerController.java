package com.atme.ssyx.product.api;

import com.atme.ssyx.model.product.Category;
import com.atme.ssyx.model.product.SkuInfo;
import com.atme.ssyx.product.service.CategoryService;
import com.atme.ssyx.product.service.SkuInfoService;
import com.atme.ssyx.vo.product.SkuInfoVo;
import com.atme.ssyx.vo.product.SkuStockLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/product")
public class ProductInnerController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SkuInfoService skuInfoService;
    //根据分类获取分类的数据
    @GetMapping("inner/getCategory/{categoryId}")
    public Category getCategory(@PathVariable Long categoryId){
        return categoryService.getById(categoryId);
    }

    //根据sku_id获取sku信息
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId) {
        return skuInfoService.getById(skuId);
    }

    //根据sku列表得到sku信息列表
    @PostMapping("inner/findSkuInfoList")
    public List<SkuInfo> findSkuInfoList(@RequestBody List<Long> skuIdList){
        return skuInfoService.findSkuInfoList(skuIdList);
    }

    @PostMapping("inner/findCategoryList")
    public List<Category> findCategoryList(@RequestBody List<Long> CategoryList){
        return categoryService.listByIds(CategoryList);
    }

    @GetMapping("inner/findSkuInfoByKeyword/{keyword}")
    public List<SkuInfo> findSkuInfoByKeyword(@PathVariable String keyword){
        return skuInfoService.findSkuInfoByKeyword(keyword);
    }

    @GetMapping("inner/findAllCategoryList")
    public List<Category> findAllCategoryList(){
        return categoryService.list();
    }

    @GetMapping("inner/findNewPersonSkuInfoList")
    public List<SkuInfo> findNewPersonSkuInfoList(){
        return skuInfoService.findNewPersonSkuInfoList();
    }

    //根据skuid获取信息
    @GetMapping("inner/getSkuInfoVo/{skuId}")
    public SkuInfoVo getSkuInfoVo(@PathVariable Long skuId){
        return skuInfoService.getSkuInfoVo(skuId);
    }

    //验证锁定库存
    @PostMapping("inner/CheckAndLock/{orderNo}")
    public Boolean checkAndLock(@RequestBody List<SkuStockLockVo> skuStockLockVos,@PathVariable String orderNo){
        return skuInfoService.checkAndLock(skuStockLockVos,orderNo);
    }
}
