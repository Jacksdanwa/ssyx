package com.atme.ssyx.home.controller;

import com.atme.ssyx.common.auth.AuthContextHolder;
import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.home.service.HomeService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Api(tags = "首页接口")
@RestController
@RequestMapping("api/home")
public class HomeApiController {

    @Autowired
    private HomeService homeService;

    @GetMapping("index")
    public Result index(HttpServletRequest servletRequest){
        Long userId = AuthContextHolder.getUserId();
        Map <String,Object> result = homeService.homeData(userId);
        return Result.ok(result);
    }

    //查询分类的商品


}
