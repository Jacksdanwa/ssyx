package com.atme.ssyx.search.controller;

import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.model.search.SkuEs;
import com.atme.ssyx.search.service.SkuService;
import com.atme.ssyx.vo.search.SkuEsQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/search/sku")
public class SkuApiController {

    @Autowired
    private SkuService skuService;

    //上架
    @GetMapping("inner/upperSku/{skuId}")
    public Result upperSku(@PathVariable Long skuId){
        skuService.upperSku(skuId);
        return Result.ok(null);
    }

    //下架
    @GetMapping("inner/lowerSku/{skuId}")
    public Result lowerSku(@PathVariable Long skuId){
        skuService.lowerSku(skuId);
        return Result.ok(null);
    }

    //获取爆款商品
    @GetMapping("inner/findHotSkuList")
    public List<SkuEs> findHotSkuList() {
        return skuService.findHotSkuList();
    }

    //查询分类商品
    @GetMapping("{page}/{limit}")
    public Result list(@PathVariable Integer page,
                       @PathVariable Integer limit,
                       SkuEsQueryVo skuEsQueryVo){
        //创建pageable对象，0代表第一页
        Pageable pageable = PageRequest.of(page - 1,limit);
        Page<SkuEs> skuEsPage = skuService.search(pageable,skuEsQueryVo);
        return Result.ok(skuEsPage);
    }

    //更新商品热度
    @GetMapping("inner/incrHotScore/{skuId}")
    public Boolean incrHotScore(@PathVariable Long skuId){
        skuService.incrHotScore(skuId);
        return true;
    }

}
