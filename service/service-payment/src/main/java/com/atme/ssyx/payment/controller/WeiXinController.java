package com.atme.ssyx.payment.controller;


import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.common.result.ResultCodeEnum;
import com.atme.ssyx.payment.service.PaymentInfoService;
import com.atme.ssyx.payment.service.WeiXinService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "微信支付接口")
@RestController
@RequestMapping("/api/payment/weixin")
@Slf4j
public class WeiXinController {

    @Autowired
    private WeiXinService weiXinService;

    @Autowired
    private PaymentInfoService paymentInfoService;

    //调用微信支付系统生成代码
    @GetMapping("createJsapi/{orderNo}")
    public Result createJsapi(@PathVariable("orderNo") String orderNo){
        Map<String,String> map = weiXinService.createJsapi(orderNo);
        return Result.ok(map);
    }

    //查询订单支付状态
    @GetMapping("/queryPayStatus/{orderNo}")
    public Result queryPayStatus(@PathVariable("orderNo") String orderNo){
        //调用微信支付的接口查询支付状态
        Map<String,String> resultmap = weiXinService.queryPayStatus(orderNo);
        //判断是否为空
        if (resultmap == null){
            return Result.build(null, ResultCodeEnum.PAYMENT_FAIL);
        }
        //支付成功，修改状态
        if ("SUCCESS".equals(resultmap.get("trade_state"))){
            String outTradeNo = resultmap.get("out_trade_no");
            //订单记录变为已支付，库存-1
            paymentInfoService.paySuccess(outTradeNo, resultmap);
            return Result.ok(null);
        }

        //支付中进行等待
        return Result.build(null,ResultCodeEnum.PAYMENT_WAITING);
    }


}
