package com.atme.ssyx.order.service.impl;


import com.atme.ssyx.client.cart.CartFeignClient;
import com.atme.ssyx.client.activity.ActivityFeignClient;
import com.atme.ssyx.client.product.ProductFeignClient;
import com.atme.ssyx.client.user.UserFeignClient;
import com.atme.ssyx.common.auth.AuthContextHolder;
import com.atme.ssyx.common.constant.MqConst;
import com.atme.ssyx.common.constant.RedisConst;
import com.atme.ssyx.common.exception.SsyxException;
import com.atme.ssyx.common.result.ResultCodeEnum;
import com.atme.ssyx.common.utils.DateUtil;
import com.atme.ssyx.enums.*;
import com.atme.ssyx.model.activity.ActivityRule;
import com.atme.ssyx.model.activity.CouponInfo;
import com.atme.ssyx.model.order.CartInfo;
import com.atme.ssyx.model.order.OrderInfo;
import com.atme.ssyx.model.order.OrderItem;
import com.atme.ssyx.mq.service.RabbitService;
import com.atme.ssyx.order.mapper.OrderInfoMapper;
import com.atme.ssyx.order.mapper.OrderItemMapper;
import com.atme.ssyx.order.service.OrderInfoService;
import com.atme.ssyx.vo.order.CartInfoVo;
import com.atme.ssyx.vo.order.OrderConfirmVo;
import com.atme.ssyx.vo.order.OrderSubmitVo;
import com.atme.ssyx.vo.order.OrderUserQueryVo;
import com.atme.ssyx.vo.product.SkuStockLockVo;
import com.atme.ssyx.vo.user.LeaderAddressVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 订单 服务实现类
 * </p>
 *
 * @author me
 * @since 2025-04-20
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ActivityFeignClient activityFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private OrderItemMapper orderItemMapper;
    //确认订单
    @Override
    public OrderConfirmVo confirmOrder() {
        //获取id
        Long userId = AuthContextHolder.getUserId();

        //获取团长id
        LeaderAddressVo leaderAddressVo = userFeignClient.getleaderAddressVo(userId);

        //购物车中选中的商品
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);

        //唯一标识订单
        String orderNow = System.currentTimeMillis() + "";
        redisTemplate.opsForValue().set(RedisConst.ORDER_REPEAT+orderNow,orderNow,24, TimeUnit.HOURS);

        //获取购物车中满足条件的营销活动和优惠券相关信息
        OrderConfirmVo cartActivityAndCoupon = activityFeignClient.findCartActivityAndCoupon(cartCheckedList, userId);

        //封装其他值
        cartActivityAndCoupon.setLeaderAddressVo(leaderAddressVo);
        cartActivityAndCoupon.setOrderNo(orderNow);

        return cartActivityAndCoupon;
    }


    //生成订单
    @Override
    public Long submitOrder(OrderSubmitVo orderSubmitVo) {
        //给那个用户生成订单  设置OrderSubmitVo 中的 userId
        Long userId = AuthContextHolder.getUserId();
        orderSubmitVo.setUserId(userId);

        //订单不能重复提交，做重复提交验证
        //TODO:通过redis + lua脚本进行判断   lua脚本保证原子性
        //1 从orderSubmitVo中得到唯一标识
         
        String orderNo = orderSubmitVo.getOrderNo();
        if(StringUtils.isEmpty(orderNo)){
            throw new SsyxException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        //2 用orderNo查询
        String script = "if(redis.call('get', KEYS[1]) == ARGV[1]) then " +
                "return redis.call('del', KEYS[1]) else return 0 end";

        //3 如果出现相同的，表示正常提交订单，把redisOrderNo标识删除
        Boolean flag = (Boolean)redisTemplate.execute(new DefaultRedisScript(script, Boolean.class), Arrays.asList(RedisConst.ORDER_REPEAT + orderNo), orderNo);
        //4 如果没出现，不正常，重复提交了，不能往后进行
        if (!flag){
            throw new SsyxException(ResultCodeEnum.REPEAT_SUBMIT);
        }
        //第三步 验证库存 并且 锁定库存
        //1 验证库存  查询是否有充足余量
        //2 锁定库存 如果库存充足，对你买的数量进行锁定（没有真正减库存）
        //1 远程调用service-cart 获取购物车中的商品（选中的购物项）
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
        //2 购物车有很多商品 商品不同类型  重点处理普通类型的商品
        List<CartInfo> commonSkuList = cartCheckedList.stream().filter(cartInfo -> cartInfo.getSkuType() == SkuType.COMMON.getCode()).collect(Collectors.toList());
        //3 把获取购物车里面的普通类型的商品list集合  转换
        if (!CollectionUtils.isEmpty(commonSkuList)){
            List<SkuStockLockVo> stockLockVoList = commonSkuList.stream().map(item -> {
                SkuStockLockVo skuStockLockVo = new SkuStockLockVo();
                skuStockLockVo.setSkuId(item.getSkuId());
                skuStockLockVo.setSkuNum(item.getSkuNum());
                return skuStockLockVo;
            }).collect(Collectors.toList());
            //4 远程调用service-product模块实现商品的锁定
            //// 验证库存并且锁定库存  保证原子性
            Boolean isLockSuccess= productFeignClient.checkAndLock(stockLockVoList, orderNo);
            if (!isLockSuccess){
                //库存锁定失败
                throw new SsyxException(ResultCodeEnum.ORDER_STOCK_FALL);
            }
        }
        //第四步
        //向两张表添加数据
        Long orderId = this.saveOrder(orderSubmitVo,cartCheckedList);

        //下单完成，删除购物车中的数据
        //发送mq消息
        rabbitService.sendMessage(MqConst.EXCHANGE_ORDER_DIRECT,MqConst.ROUTING_DELETE_CART,orderSubmitVo.getUserId());
        //返回订单id
        return orderId;

    }

    @Override
    public OrderInfo getOrderInfoByOrderNo(String orderNo) {
        return baseMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
    }

    @Override
    public void orderPay(String orderNo) {
        //查询订单状态是否已经修改完成
        OrderInfo orderInfo = this.getOrderInfoByOrderNo(orderNo);
        if (orderInfo == null || orderInfo.getOrderStatus() != OrderStatus.UNPAID){
            return;
        }
        //更新订单
        this.updateOrderStatus(orderInfo.getId());

        //扣减库存
        rabbitService.sendMessage(MqConst.EXCHANGE_ORDER_DIRECT,MqConst.ROUTING_MINUS_STOCK,orderNo);
    }

    @Override
    public IPage<OrderInfo> getOrderInfoByUserIdPage(Page<OrderInfo> pageparam, OrderUserQueryVo orderUserQueryVo) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId, orderUserQueryVo.getUserId());
        wrapper.eq(OrderInfo::getOrderStatus, orderUserQueryVo.getOrderStatus());
        Page<OrderInfo> pageModel = baseMapper.selectPage(pageparam, wrapper);

        //获取到每个订单，封装订单项
        List<OrderInfo> orderInfoList = pageModel.getRecords();
        for (OrderInfo orderInfo : orderInfoList){
            //根据订单id查询
            List<OrderItem> orderItemList = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getSkuId, orderInfo.getId()));
            orderInfo.setOrderItemList(orderItemList);
            //封装订单状态
            orderInfo.getParam().put("OrderStatusName",orderInfo.getOrderStatus().getComment());
        }

        return pageModel;
    }

    private void updateOrderStatus(Long id) {
        OrderInfo orderInfo = baseMapper.selectById(id);
        orderInfo.setOrderStatus(OrderStatus.WAITING_DELEVER);
        orderInfo.setProcessStatus(ProcessStatus.WAITING_DELEVER);
        baseMapper.updateById(orderInfo);
    }

    @Transactional(rollbackFor = {Exception.class})
    public Long saveOrder(OrderSubmitVo orderSubmitVo, List<CartInfo> cartCheckedList) {
        if (CollectionUtils.isEmpty(cartCheckedList)){
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }
        //查询用户提货点和团长的相关信息
        Long userId = AuthContextHolder.getUserId();
        LeaderAddressVo leaderAddressVo = userFeignClient.getleaderAddressVo(userId);
        if (leaderAddressVo == null){
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }
        //计算相关金额
        //营销活动 与 优惠券
        Map<String, BigDecimal> map = this.computeActivitySplitAmount(cartCheckedList);
        //优惠券金额
        Map<String, BigDecimal> map1 = this.computeCouponInfoSplitAmount(cartCheckedList, orderSubmitVo.getCouponId());

        //封装订单项
        List<OrderItem> orderItemList = new ArrayList<>();
        OrderItem orderItem = new OrderItem();
        for (CartInfo cartInfo : cartCheckedList){
            if(cartInfo.getSkuType() == SkuType.COMMON.getCode()) {
                orderItem.setSkuType(SkuType.COMMON);
            } else {
                orderItem.setSkuType(SkuType.SECKILL);
            }
            orderItem.setSkuId(cartInfo.getSkuId());
            orderItem.setSkuName(cartInfo.getSkuName());
            orderItem.setSkuPrice(cartInfo.getCartPrice());
            orderItem.setImgUrl(cartInfo.getImgUrl());
            orderItem.setSkuNum(cartInfo.getSkuNum());
            orderItem.setLeaderId(orderSubmitVo.getLeaderId());
            //营销活动的金额
            BigDecimal activityAmount = map.get("activity:" + orderItem.getSkuId());
            if (activityAmount == null){
                activityAmount = new BigDecimal(0);
            }
            orderItem.setSplitActivityAmount(activityAmount);
            //优惠券金额
            BigDecimal couponAmount = map1.get("coupon:" + orderItem.getSkuId());
            orderItem.setSplitCouponAmount(couponAmount);
            //总金额
            BigDecimal skuTotalAmount = orderItem.getSkuPrice().multiply(new BigDecimal(orderItem.getSkuNum()));

            //优惠之后的金额
            BigDecimal splitTotalAmount = skuTotalAmount.subtract(activityAmount).subtract(couponAmount);


            orderItem.setSplitTotalAmount(splitTotalAmount);
            orderItemList.add(orderItem);

        }

        //封装订单OrderInfo的数据
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUserId(userId); // 用户id
        orderInfo.setOrderNo(orderSubmitVo.getOrderNo());// 订单号 唯一标识
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setLeaderId(orderSubmitVo.getLeaderId());
        orderInfo.setLeaderName(leaderAddressVo.getLeaderName());
        orderInfo.setLeaderPhone(leaderAddressVo.getLeaderPhone());
        orderInfo.setTakeName(leaderAddressVo.getTakeName());
        orderInfo.setReceiverName(orderSubmitVo.getReceiverName());
        orderInfo.setReceiverPhone(orderSubmitVo.getReceiverPhone());
        orderInfo.setReceiverProvince(leaderAddressVo.getProvince());
        orderInfo.setReceiverCity(leaderAddressVo.getCity());
        orderInfo.setReceiverDistrict(leaderAddressVo.getDistrict());
        orderInfo.setReceiverAddress(leaderAddressVo.getDetailAddress());
        orderInfo.setWareId(cartCheckedList.get(0).getWareId());
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);

        //计算订单金额
        BigDecimal originalTotalAmount = this.computeTotalAmount(cartCheckedList);
        BigDecimal activityAmount = map.get("activity:total");
        if(null == activityAmount) activityAmount = new BigDecimal(0);
        BigDecimal couponAmount = map1.get("coupon:total");
        if(null == couponAmount) couponAmount = new BigDecimal(0);
        BigDecimal totalAmount = originalTotalAmount.subtract(activityAmount).subtract(couponAmount);

        //计算订单金额
        orderInfo.setOriginalTotalAmount(originalTotalAmount);
        orderInfo.setActivityAmount(activityAmount);
        orderInfo.setCouponAmount(couponAmount);
        orderInfo.setTotalAmount(totalAmount);

        baseMapper.insert(orderInfo);

        //添加订单项
        orderItemList.forEach(orderItem1 -> {
            orderItem1.setOrderId(orderInfo.getId());
            orderItemMapper.insert(orderItem1);
        });


        //如果当前订单使用优惠券，更新状态
        if (orderInfo.getCouponId() != null){
            activityFeignClient.updateCouponInfoUseStatus(orderInfo.getCouponId(),userId,orderInfo.getId());
        }

        //下单成功之后，记录用户的数量，redis
        //hash类型  key(userId) - field(skuId) - value(skuNum)
        String orderSkuKey = RedisConst.ORDER_SKU_MAP + orderSubmitVo.getUserId();
        BoundHashOperations<String, String, Integer> hashOperations = redisTemplate.boundHashOps(orderSkuKey);
        cartCheckedList.forEach(cartInfo -> {
            if(hashOperations.hasKey(cartInfo.getSkuId().toString())) {
                Integer orderSkuNum = hashOperations.get(cartInfo.getSkuId().toString()) + cartInfo.getSkuNum();
                hashOperations.put(cartInfo.getSkuId().toString(), orderSkuNum);
            }
        });
        redisTemplate.expire(orderSkuKey, DateUtil.getCurrentExpireTimes(), TimeUnit.SECONDS);
        //订单id


        return orderInfo.getId();

    }

    //订单详情
    @Override
    public OrderInfo getOrderInfoById(Long orderId) {
        //根据OrderId查询订单基本信息
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        //根据订单id查询订单项的list列表
        List<OrderItem> orderItemList = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        //查询出的订单项封装到每个订单项里面
        orderInfo.setOrderItemList(orderItemList);
        return orderInfo;
    }

    //计算总金额
    private BigDecimal computeTotalAmount(List<CartInfo> cartInfoList) {
        BigDecimal total = new BigDecimal(0);
        for (CartInfo cartInfo : cartInfoList) {
            BigDecimal itemTotal = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
            total = total.add(itemTotal);
        }
        return total;
    }

    /**
     * 计算购物项分摊的优惠减少金额
     * 打折：按折扣分担
     * 现金：按比例分摊
     * @param cartInfoParamList
     * @return
     */
    private Map<String, BigDecimal> computeActivitySplitAmount(List<CartInfo> cartInfoParamList) {
        Map<String, BigDecimal> activitySplitAmountMap = new HashMap<>();

        //促销活动相关信息
        List<CartInfoVo> cartInfoVoList = activityFeignClient.findCartActivityList(cartInfoParamList);

        //活动总金额
        BigDecimal activityReduceAmount = new BigDecimal(0);
        if(!CollectionUtils.isEmpty(cartInfoVoList)) {
            for(CartInfoVo cartInfoVo : cartInfoVoList) {
                ActivityRule activityRule = cartInfoVo.getActivityRule();
                List<CartInfo> cartInfoList = cartInfoVo.getCartInfoList();
                if(null != activityRule) {
                    //优惠金额， 按比例分摊
                    BigDecimal reduceAmount = activityRule.getReduceAmount();
                    activityReduceAmount = activityReduceAmount.add(reduceAmount);
                    if(cartInfoList.size() == 1) {
                        activitySplitAmountMap.put("activity:"+cartInfoList.get(0).getSkuId(), reduceAmount);
                    } else {
                        //总金额
                        BigDecimal originalTotalAmount = new BigDecimal(0);
                        for(CartInfo cartInfo : cartInfoList) {
                            BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                            originalTotalAmount = originalTotalAmount.add(skuTotalAmount);
                        }
                        //记录除最后一项是所有分摊金额， 最后一项=总的 - skuPartReduceAmount
                        BigDecimal skuPartReduceAmount = new BigDecimal(0);
                        if (activityRule.getActivityType() == ActivityType.FULL_REDUCTION) {
                            for(int i=0, len=cartInfoList.size(); i<len; i++) {
                                CartInfo cartInfo = cartInfoList.get(i);
                                if(i < len -1) {
                                    BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                                    //sku分摊金额
                                    BigDecimal skuReduceAmount = skuTotalAmount.divide(originalTotalAmount, 2, RoundingMode.HALF_UP).multiply(reduceAmount);
                                    activitySplitAmountMap.put("activity:"+cartInfo.getSkuId(), skuReduceAmount);

                                    skuPartReduceAmount = skuPartReduceAmount.add(skuReduceAmount);
                                } else {
                                    BigDecimal skuReduceAmount = reduceAmount.subtract(skuPartReduceAmount);
                                    activitySplitAmountMap.put("activity:"+cartInfo.getSkuId(), skuReduceAmount);
                                }
                            }
                        } else {
                            for(int i=0, len=cartInfoList.size(); i<len; i++) {
                                CartInfo cartInfo = cartInfoList.get(i);
                                if(i < len -1) {
                                    BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));

                                    //sku分摊金额
                                    BigDecimal skuDiscountTotalAmount = skuTotalAmount.multiply(activityRule.getBenefitDiscount().divide(new BigDecimal("10")));
                                    BigDecimal skuReduceAmount = skuTotalAmount.subtract(skuDiscountTotalAmount);
                                    activitySplitAmountMap.put("activity:"+cartInfo.getSkuId(), skuReduceAmount);

                                    skuPartReduceAmount = skuPartReduceAmount.add(skuReduceAmount);
                                } else {
                                    BigDecimal skuReduceAmount = reduceAmount.subtract(skuPartReduceAmount);
                                    activitySplitAmountMap.put("activity:"+cartInfo.getSkuId(), skuReduceAmount);
                                }
                            }
                        }
                    }
                }
            }
        }
        activitySplitAmountMap.put("activity:total", activityReduceAmount);
        return activitySplitAmountMap;
    }

    //优惠卷优惠金额
    private Map<String, BigDecimal> computeCouponInfoSplitAmount(List<CartInfo> cartInfoList, Long couponId) {
        Map<String, BigDecimal> couponInfoSplitAmountMap = new HashMap<>();

        if(null == couponId) return couponInfoSplitAmountMap;
        CouponInfo couponInfo = activityFeignClient.findRangeSkuIdList(cartInfoList, couponId);

        if(null != couponInfo) {
            //sku对应的订单明细
            Map<Long, CartInfo> skuIdToCartInfoMap = new HashMap<>();
            for (CartInfo cartInfo : cartInfoList) {
                skuIdToCartInfoMap.put(cartInfo.getSkuId(), cartInfo);
            }
            //优惠券对应的skuId列表
            List<Long> skuIdList = couponInfo.getSkuIdList();
            if(CollectionUtils.isEmpty(skuIdList)) {
                return couponInfoSplitAmountMap;
            }
            //优惠券优化总金额
            BigDecimal reduceAmount = couponInfo.getAmount();
            if(skuIdList.size() == 1) {
                //sku的优化金额
                couponInfoSplitAmountMap.put("coupon:"+skuIdToCartInfoMap.get(skuIdList.get(0)).getSkuId(), reduceAmount);
            } else {
                //总金额
                BigDecimal originalTotalAmount = new BigDecimal(0);
                for (Long skuId : skuIdList) {
                    CartInfo cartInfo = skuIdToCartInfoMap.get(skuId);
                    BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                    originalTotalAmount = originalTotalAmount.add(skuTotalAmount);
                }
                //记录除最后一项是所有分摊金额， 最后一项=总的 - skuPartReduceAmount
                BigDecimal skuPartReduceAmount = new BigDecimal(0);
                if (couponInfo.getCouponType() == CouponType.CASH || couponInfo.getCouponType() == CouponType.FULL_REDUCTION) {
                    for(int i=0, len=skuIdList.size(); i<len; i++) {
                        CartInfo cartInfo = skuIdToCartInfoMap.get(skuIdList.get(i));
                        if(i < len -1) {
                            BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                            //sku分摊金额
                            BigDecimal skuReduceAmount = skuTotalAmount.divide(originalTotalAmount, 2, RoundingMode.HALF_UP).multiply(reduceAmount);
                            couponInfoSplitAmountMap.put("coupon:"+cartInfo.getSkuId(), skuReduceAmount);

                            skuPartReduceAmount = skuPartReduceAmount.add(skuReduceAmount);
                        } else {
                            BigDecimal skuReduceAmount = reduceAmount.subtract(skuPartReduceAmount);
                            couponInfoSplitAmountMap.put("coupon:"+cartInfo.getSkuId(), skuReduceAmount);
                        }
                    }
                }
            }
            couponInfoSplitAmountMap.put("coupon:total", couponInfo.getAmount());
        }
        return couponInfoSplitAmountMap;
    }
}
