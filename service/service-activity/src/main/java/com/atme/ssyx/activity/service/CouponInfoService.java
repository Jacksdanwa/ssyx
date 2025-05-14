package com.atme.ssyx.activity.service;

import com.atme.ssyx.model.activity.CouponInfo;
import com.atme.ssyx.model.order.CartInfo;
import com.atme.ssyx.vo.activity.CouponRuleVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 优惠券信息 服务类
 * </p>
 *
 * @author atme
 * @since 2025-02-18
 */
public interface CouponInfoService extends IService<CouponInfo> {

    IPage<CouponInfo> selectlist(Page<CouponInfo> pageParam);

    CouponInfo getCouponById(Long id);

    Map<String, Object> findCouponRuleList(Long id);

    void saveCouponRule(CouponRuleVo couponRuleVo);

    List<CouponInfo> findCouponInfoList(Long skuId, Long userId);

    List<CouponInfo> findCartCouponInfo(List<CartInfo> cartInfoList, Long userId);

    CouponInfo findRangeSkuIdList(List<CartInfo> cartInfoList, Long couponId);

    void updateCouponInfoUseStatus(Long couponId, Long userId, Long orderId);
}
