package com.atme.ssyx.payment.service.Impl;

import com.atme.ssyx.common.constant.MqConst;
import com.atme.ssyx.common.exception.SsyxException;
import com.atme.ssyx.common.result.ResultCodeEnum;
import com.atme.ssyx.enums.PaymentStatus;
import com.atme.ssyx.enums.PaymentType;
import com.atme.ssyx.model.order.OrderInfo;
import com.atme.ssyx.model.order.PaymentInfo;
import com.atme.ssyx.mq.service.RabbitService;
import com.atme.ssyx.order.client.OrderFeignClient;
import com.atme.ssyx.payment.mapper.PaymentInfoMapper;
import com.atme.ssyx.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public PaymentInfo getPaymentInfoByOrderNo(String orderNo) {
        return baseMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));
    }

    @Override
    public PaymentInfo savePaymentInfo(String orderNo) {
        //远程调用，根据OrderNo查询订单信息
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderNo);
        if (orderInfo == null){
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }
        //封装paymentInfo中
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setPaymentType(PaymentType.WEIXIN);
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setOrderNo(orderInfo.getOrderNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        String subject = "userId:" + orderInfo.getUserId() + "下订单";
        paymentInfo.setSubject(subject);
        paymentInfo.setTotalAmount(new BigDecimal("0.01"));
        //调用方法实现
        baseMapper.insert(paymentInfo);
        return paymentInfo;
    }

    @Override
    public void paySuccess(String outTradeNo, Map<String, String> resultmap) {
        //查询当前状态是否为已经支付，已经支付就不用改
        PaymentInfo paymentInfo = baseMapper.selectOne(
                new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, outTradeNo)
        );
        if (paymentInfo.getPaymentStatus() != PaymentStatus.UNPAID){
            return;
        }
        //如果状态是未支付的话，更新

        paymentInfo.setPaymentStatus(PaymentStatus.PAID);
        paymentInfo.setTradeNo(resultmap.get("transaction_id"));
        paymentInfo.setCallbackContent(resultmap.toString());
        baseMapper.updateById(paymentInfo);


        //TODO：RabbitMQ，修改订单状态修改库存
        rabbitService.sendMessage(MqConst.EXCHANGE_PAY_DIRECT,MqConst.ROUTING_PAY_SUCCESS,outTradeNo);

    }
}
