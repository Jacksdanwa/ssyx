package com.atme.ssyx.order.controller;


import com.atme.ssyx.common.auth.AuthContextHolder;
import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.model.order.OrderInfo;
import com.atme.ssyx.order.service.OrderInfoService;
import com.atme.ssyx.vo.order.OrderConfirmVo;
import com.atme.ssyx.vo.order.OrderSubmitVo;
import com.atme.ssyx.vo.order.OrderUserQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 订单 前端控制器
 * </p>
 *
 * @author me
 * @since 2025-04-20
 */
@RestController
@RequestMapping(value = "/api/order")
public class OrderInfoController {

    @Autowired
    private OrderInfoService orderInfoService;


    //订单查询
    @GetMapping("auth/findUserOrderPage/{page}/{limit}")
    public Result findUserOrderPage(
            @ApiParam(name = "page", value = "当前页码", required = true)
            @PathVariable Long page,
            @ApiParam(name = "limit", value = "每页记录数", required = true)
            @PathVariable Long limit,
            @ApiParam(name = "orderVo", value = "查询对象", required = false)
            OrderUserQueryVo orderUserQueryVo){
        //获取用户id
        Long userId = AuthContextHolder.getUserId();
        orderUserQueryVo.setUserId(userId);

        //分页
        Page<OrderInfo> pageparam = new Page<>(page,limit);
        IPage<OrderInfo> result = orderInfoService.getOrderInfoByUserIdPage(pageparam,orderUserQueryVo);
        return Result.ok(result);
    }



    @ApiOperation("确认订单")
    @GetMapping("auth/confirmOrder")
    public Result confirm(){
        OrderConfirmVo orderConfirmVo = orderInfoService.confirmOrder();
        return Result.ok(orderConfirmVo);
    }

    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderSubmitVo orderSubmitVo){
        Long userId = AuthContextHolder.getUserId();
        Long orderId = orderInfoService.submitOrder(orderSubmitVo);
        return Result.ok(orderId);
    }

    //获取订单详情
    @GetMapping("auth/getOrderInfoById/{orderId}")
    public Result getOrderInfoById(@PathVariable("orderId") Long orderId){
        OrderInfo orderInfo = orderInfoService.getOrderInfoById(orderId);
        return Result.ok(orderInfo);
    }

    //根据OrderNo查询订单信息
    @GetMapping("inner/getOrderInfo/{orderNo}")
    public OrderInfo getOrderInfo(@PathVariable("orderNo") String orderNo){
        return orderInfoService.getOrderInfoByOrderNo(orderNo);
    }

}

