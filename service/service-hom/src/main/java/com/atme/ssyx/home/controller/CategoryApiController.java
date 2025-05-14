package com.atme.ssyx.home.controller;

import com.atme.ssyx.client.product.ProductFeignClient;
import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.model.product.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/home")
public class CategoryApiController {

    @Autowired
    private ProductFeignClient productFeignClient;

    @GetMapping("category")
    public Result categoryList(){
        List<Category> allCategoryList = productFeignClient.findAllCategoryList();
        return Result.ok(allCategoryList);
    }

}
