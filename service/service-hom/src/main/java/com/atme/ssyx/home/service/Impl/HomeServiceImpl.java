package com.atme.ssyx.home.service.Impl;

import com.atme.ssyx.client.product.ProductFeignClient;
import com.atme.ssyx.client.search.SkuFeignClient;
import com.atme.ssyx.client.user.UserFeignClient;
import com.atme.ssyx.home.service.HomeService;
import com.atme.ssyx.model.product.Category;
import com.atme.ssyx.model.product.SkuInfo;
import com.atme.ssyx.model.search.SkuEs;
import com.atme.ssyx.vo.user.LeaderAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HomeServiceImpl implements HomeService {


    @Autowired
    private SkuFeignClient skuFeignClient;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;
    @Override
    public Map<String, Object> homeData(Long userId) {
        Map<String,Object> result = new HashMap<>();
        //1 根据userId获取当前登录语句的提货信息
        //引入依赖   pom
        //远程调用service-user模块的接口获取数据
        LeaderAddressVo leaderAddressVo = userFeignClient.getleaderAddressVo(userId);
        //获取所有分类的信息
        //远程调用service-product
        List<Category> allCategoryList = productFeignClient.findAllCategoryList();
        //新人专享
        List<SkuInfo> newPersonSkuInfoList = productFeignClient.findNewPersonSkuInfoList();
        //热销好货
        //用es查询
        //hotsc降序
        List<SkuEs> hotSkuList = skuFeignClient.findHotSkuList();
        //返回
        result.put("leaderAddressVo",leaderAddressVo);
        result.put("allCategoryList",allCategoryList);
        result.put("newPersonSkuInfoList",newPersonSkuInfoList);
        result.put("hotSkuList",hotSkuList);
        return result;
    }
}
