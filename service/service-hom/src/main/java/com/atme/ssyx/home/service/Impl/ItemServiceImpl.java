package com.atme.ssyx.home.service.Impl;

import com.atme.ssyx.client.activity.ActivityFeignClient;
import com.atme.ssyx.client.product.ProductFeignClient;
import com.atme.ssyx.client.search.SkuFeignClient;
import com.atme.ssyx.home.service.ItemService;
import com.atme.ssyx.vo.product.SkuInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ActivityFeignClient activityFeignClient;

    @Autowired
    private SkuFeignClient skuFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Override
    public Map<String, Object> item(Long skuId, Long userId) {
        Map<String, Object> result = new HashMap<>();

//        ExecutorService executorService = Executors.newFixedThreadPool(3);
        //skuId查询信息
        CompletableFuture<SkuInfoVo> SkuInfoCompleteableFuture = CompletableFuture.supplyAsync(() -> {
            //远程调用获取sku对应的数据


            SkuInfoVo skuInfoVo = productFeignClient.getSkuInfoVo(skuId);
            result.put("skuInfoVo", skuInfoVo);
            return skuInfoVo;
        }, threadPoolExecutor);

        //Sku对应的优惠券信息
        CompletableFuture<Void> ActivitiyCompleteableFuture = CompletableFuture.runAsync(() -> {
            //远程调用获取优惠券
            Map<String,Object> activitiyMap = activityFeignClient.findActivityAndCoupon(skuId,userId);
            result.putAll(activitiyMap);
        }, threadPoolExecutor);

        //更新商品热度
        CompletableFuture<Void> HotCompleteableFuture = CompletableFuture.runAsync(() -> {
            //远程调用更新
            skuFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);

        //任务组合
        CompletableFuture.allOf(SkuInfoCompleteableFuture,ActivitiyCompleteableFuture,HotCompleteableFuture).join();
        return result;
    }
}
