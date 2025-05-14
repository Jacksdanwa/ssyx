package com.atme.ssyx.client.cart.controller;

import com.atme.ssyx.client.cart.service.CartInfoService;
import com.atme.ssyx.client.activity.ActivityFeignClient;
import com.atme.ssyx.common.auth.AuthContextHolder;
import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.model.order.CartInfo;
import com.atme.ssyx.model.product.SkuInfo;
import com.atme.ssyx.vo.order.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartApiController {

    @Autowired
    private CartInfoService cartInfoService;

    @Autowired
    private ActivityFeignClient activityFeignClient;


    @GetMapping("activityCartList")
    public Result activityCartList() {
        // 获取用户Id
        Long userId = AuthContextHolder.getUserId();
        List<CartInfo> cartInfoList = cartInfoService.getCartList(userId);

        OrderConfirmVo orderTradeVo = activityFeignClient.findCartActivityAndCoupon(cartInfoList, userId);
        return Result.ok(orderTradeVo);
    }

    //添加商品
    @GetMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum){
        Long userId = AuthContextHolder.getUserId();
        cartInfoService.addToCart(skuId,skuNum,userId);
        return Result.ok(null);
    }

    @GetMapping("cartList")
    public Result cartList(){
        //获取userId
        Long userId = AuthContextHolder.getUserId();
        List<CartInfo> cartInfoList = cartInfoService.getCartList(userId);
        return Result.ok(cartInfoList);
    }


    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCartById(@PathVariable Long skuId) {
        Long userId = AuthContextHolder.getUserId();
        cartInfoService.deleteCart(skuId,userId);
        return Result.ok(null);
    }

    @DeleteMapping("deleteAllCart")
    public Result deleteAllCart(){
        Long userId = AuthContextHolder.getUserId();
        cartInfoService.deleteAllCart(userId);
        return Result.ok(null);
    }

    @DeleteMapping("batchDeleteCart")
    public Result batchDeleteCart(@RequestBody List<SkuInfo> skuInfoList){
        Long userId = AuthContextHolder.getUserId();
        cartInfoService.batchDeleteCart(userId,skuInfoList);
        return Result.ok(null);
    }

    //获取用户购物车中的商品
    @GetMapping("inner/getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable Long userId){
        return cartInfoService.getCartCheckedList(userId);
    }


}
