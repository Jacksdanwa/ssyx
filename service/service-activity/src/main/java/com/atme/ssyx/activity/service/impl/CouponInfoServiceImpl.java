package com.atme.ssyx.activity.service.impl;

import com.atme.ssyx.activity.mapper.CouponInfoMapper;
import com.atme.ssyx.activity.mapper.CouponRangeMapper;
import com.atme.ssyx.activity.mapper.CouponUseMapper;
import com.atme.ssyx.activity.service.CouponInfoService;
import com.atme.ssyx.client.product.ProductFeignClient;
import com.atme.ssyx.enums.CouponRangeType;
import com.atme.ssyx.enums.CouponStatus;
import com.atme.ssyx.model.activity.CouponInfo;
import com.atme.ssyx.model.activity.CouponRange;
import com.atme.ssyx.model.activity.CouponUse;
import com.atme.ssyx.model.order.CartInfo;
import com.atme.ssyx.model.product.Category;
import com.atme.ssyx.model.product.SkuInfo;
import com.atme.ssyx.vo.activity.CouponRuleVo;
import com.atme.ssyx.vo.product.SkuInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 优惠券信息 服务实现类
 * </p>
 *
 * @author atme
 * @since 2025-02-18
 */
@Service
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {


    @Autowired
    private CouponRangeMapper couponRangeMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private CouponUseMapper couponUseMapper;
    @Override
    public IPage<CouponInfo> selectlist(Page<CouponInfo> pageParam) {
        Page<CouponInfo> couponInfoPage = baseMapper.selectPage(pageParam, null);
        List<CouponInfo> couponInfoList = couponInfoPage.getRecords();
        couponInfoList.stream().forEach(item -> {
            item.setCouponTypeString(item.getCouponType().getComment());
            if (item.getRangeType() != null){
                item.setRangeTypeString(item.getRangeType().getComment());
            }
        });
        return couponInfoPage;
    }

    @Override
    public CouponInfo getCouponById(Long id) {
        CouponInfo couponInfo = baseMapper.selectById(id);
        couponInfo.setCouponTypeString(couponInfo.getCouponType().getComment());
        if (couponInfo.getRangeType() != null){
            couponInfo.setRangeTypeString(couponInfo.getRangeType().getComment());
        }
        return couponInfo;
    }

    @Override
    public Map<String, Object> findCouponRuleList(Long id) {
        CouponInfo couponInfo = baseMapper.selectById(id);

        List<CouponRange> list = couponRangeMapper.selectList(new LambdaQueryWrapper<CouponRange>().eq(CouponRange::getCouponId,id));
        //获取所有的rangeId
        List<Long> rangeList = list.stream().map(CouponRange::getRangeId).collect(Collectors.toList());

        Map<String, Object> map = new HashMap<>();
        if (!CollectionUtils.isEmpty(rangeList)){
            //规则类型为Sku  获取Skuid
            if (couponInfo.getRangeType() == CouponRangeType.SKU){
                List<SkuInfo> skuInfoList = productFeignClient.findSkuInfoList(rangeList);
                map.put("skuInfoList",skuInfoList);
            } else if (couponInfo.getRangeType() == CouponRangeType.CATEGORY){
                //规则类型为CateGory  获取categoryid
                List<Category> categoryList = productFeignClient.findCategoryList(rangeList);
                map.put("categoryList",categoryList);
            }
        }
        return map;
    }

    @Override
    public void saveCouponRule(CouponRuleVo couponRuleVo) {
        //根据优惠券id删除规则数据
        couponRangeMapper.delete(new LambdaQueryWrapper<CouponRange>().eq(CouponRange::getCouponId,couponRuleVo.getCouponId()));

        //更新优惠券基本信息
        CouponInfo couponInfo = baseMapper.selectById(couponRuleVo.getCouponId());
        couponInfo.setRangeType(couponRuleVo.getRangeType());
        couponInfo.setConditionAmount(couponRuleVo.getConditionAmount());
        couponInfo.setAmount(couponRuleVo.getAmount());
        couponInfo.setConditionAmount(couponRuleVo.getConditionAmount());
        couponInfo.setRangeDesc(couponRuleVo.getRangeDesc());
        baseMapper.updateById(couponInfo);
        //添加优惠券新规则数据
        List<CouponRange> couponRangeList = couponRuleVo.getCouponRangeList();
        for (CouponRange couponRange: couponRangeList){
            couponRange.setCouponId(couponRuleVo.getCouponId());
            couponRangeMapper.insert(couponRange);
        }
    }

    //根据skuId和userId查询优惠卷信息
    @Override
    public List<CouponInfo> findCouponInfoList(Long skuId, Long userId) {
        //远程调用
        SkuInfoVo skuInfoVo = productFeignClient.getSkuInfoVo(skuId);
        //根据条件查询
        List<CouponInfo> couponInfoList = baseMapper.selectCouponInfoList(skuId,skuInfoVo.getCategoryId(),userId);
        return couponInfoList;
    }

    //获取优惠券
    @Override
    public List<CouponInfo> findCartCouponInfo(List<CartInfo> cartInfoList, Long userId) {
        List<CouponInfo> couponInfoList = baseMapper.selectCartCouponInfoList(userId);
        if (CollectionUtils.isEmpty(couponInfoList)){
            return null;
        }
        //获取id列表
        List<Long> couponInfoIdList = couponInfoList.stream().map(couponInfo -> couponInfo.getId()).collect(Collectors.toList());
        //查询对应的范围
        LambdaQueryWrapper<CouponRange> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CouponRange::getCouponId,couponInfoIdList);
        List<CouponRange> rangeList = couponRangeMapper.selectList(wrapper);

        //获取优惠券id
        Map<Long,List<Long>> couponIdListToSkuIdMap = this.findCouponIdListToSkuIdMap(cartInfoList,rangeList);
        CouponInfo optimalCouponInfo = null;

        BigDecimal reduceAmount = new BigDecimal(0);
        for (CouponInfo couponInfo : couponInfoList){
            if (CouponRangeType.ALL == couponInfo.getRangeType()){
                BigDecimal totalAmount = computeTotalAmount(cartInfoList);
                if(totalAmount.subtract(couponInfo.getConditionAmount()).doubleValue() >= 0){
                    couponInfo.setIsSelect(1);
                }
            } else {
                List<Long> skuIdList = couponIdListToSkuIdMap.get(couponInfo.getId());
                //满足使用范围的购物项
                List<CartInfo> cartInfoList1 = cartInfoList.stream().filter(cartInfo -> skuIdList.contains(cartInfo.getSkuId())).collect(Collectors.toList());
                BigDecimal totalAmount = computeTotalAmount(cartInfoList1);
                if(totalAmount.subtract(couponInfo.getConditionAmount()).doubleValue() >= 0){
                    couponInfo.setIsSelect(1);
                }
            }
            if (couponInfo.getIsSelect().intValue() == 1 && couponInfo.getAmount().subtract(reduceAmount).doubleValue() > 0) {
                reduceAmount = couponInfo.getAmount();
                optimalCouponInfo = couponInfo;
            }
        }
        if(null != optimalCouponInfo) {
            optimalCouponInfo.setIsOptimal(1);
        }
        return couponInfoList;

    }

    @Override
    public CouponInfo findRangeSkuIdList(List<CartInfo> cartInfoList, Long couponId) {
        //根据优惠券id查询基本信息
        CouponInfo couponInfo = baseMapper.selectById(couponId);
        if (couponInfo == null){
            return null;
        }
        //根据优惠券id查询对应的range
        List<CouponRange> rangeList = couponRangeMapper.selectList(
                new LambdaQueryWrapper<CouponRange>().eq(CouponRange::getCouponId, couponId)
        );
        //对应sku信息
        Map<Long, List<Long>> couponIdListToSkuIdMap = this.findCouponIdListToSkuIdMap(cartInfoList, rangeList);
        List<Long> skuIdList = couponIdListToSkuIdMap.entrySet().iterator().next().getValue();
        couponInfo.setSkuIdList(skuIdList);
        return couponInfo;
    }

    @Override
    public void updateCouponInfoUseStatus(Long couponId, Long userId, Long orderId) {
        CouponUse couponUse = couponUseMapper.selectOne(new LambdaQueryWrapper<CouponUse>()
                .eq(CouponUse::getCouponId, couponId)
                .eq(CouponUse::getUserId, userId)
                .eq(CouponUse::getOrderId, orderId));

        //设置修改值
        couponUse.setCouponStatus(CouponStatus.USED);

        //调用方法进行修改
        couponUseMapper.updateById(couponUse);
    }

    private Map<Long, List<Long>> findCouponIdListToSkuIdMap(List<CartInfo> cartInfoList, List<CouponRange> rangeList) {
        Map<Long, List<Long>> map = new HashMap<>();
        Set<Long> skuIdSet = new HashSet<>();

        Map<Long, List<CouponRange>> couponRangeMap = rangeList.stream().collect(Collectors.groupingBy(couponRange -> couponRange.getCouponId()));
        Iterator<Map.Entry<Long, List<CouponRange>>> iterator = couponRangeMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<Long, List<CouponRange>> next = iterator.next();
            Long key = next.getKey();
            List<CouponRange> value = next.getValue();

            //创建集合
            for (CartInfo cartInfo : cartInfoList){
                for (CouponRange couponRange : value){
                    //判断
                    if (couponRange.getRangeType() == CouponRangeType.SKU && couponRange.getRangeId().longValue() == cartInfo.getSkuId()){
                        skuIdSet.add(cartInfo.getSkuId());
                    } else if (couponRange.getRangeType() == CouponRangeType.CATEGORY && couponRange.getRangeId().longValue() == cartInfo.getCategoryId()){
                        skuIdSet.add(cartInfo.getSkuId());
                    }
                }
            }

            map.put(key, new ArrayList<>(skuIdSet));
        }



        return map;
    }


    private BigDecimal computeTotalAmount(List<CartInfo> cartInfoList) {
        BigDecimal total = new BigDecimal("0");
        for (CartInfo cartInfo : cartInfoList) {
            //是否选中
            if(cartInfo.getIsChecked().intValue() == 1) {
                BigDecimal itemTotal = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                total = total.add(itemTotal);
            }
        }
        return total;
    }
}
