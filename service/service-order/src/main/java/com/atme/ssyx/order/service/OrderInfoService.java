package com.atme.ssyx.order.service;

import com.atme.ssyx.model.order.OrderInfo;
import com.atme.ssyx.vo.order.OrderConfirmVo;
import com.atme.ssyx.vo.order.OrderSubmitVo;
import com.atme.ssyx.vo.order.OrderUserQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 订单 服务类
 * </p>
 *
 * @author me
 * @since 2025-04-20
 */
public interface OrderInfoService extends IService<OrderInfo> {

    OrderConfirmVo confirmOrder();


    OrderInfo getOrderInfoById(Long orderId);

    Long submitOrder(OrderSubmitVo orderSubmitVo);

    OrderInfo getOrderInfoByOrderNo(String orderNo);

    void orderPay(String orderNo);

    IPage<OrderInfo> getOrderInfoByUserIdPage(Page<OrderInfo> pageparam, OrderUserQueryVo orderUserQueryVo);
}
