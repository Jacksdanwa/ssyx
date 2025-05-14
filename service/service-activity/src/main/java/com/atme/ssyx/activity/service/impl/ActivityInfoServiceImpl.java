package com.atme.ssyx.activity.service.impl;

import com.atme.ssyx.activity.mapper.ActivityInfoMapper;
import com.atme.ssyx.activity.mapper.ActivityRuleMapper;
import com.atme.ssyx.activity.mapper.ActivitySkuMapper;
import com.atme.ssyx.activity.service.ActivityInfoService;
import com.atme.ssyx.activity.service.CouponInfoService;
import com.atme.ssyx.client.product.ProductFeignClient;
import com.atme.ssyx.enums.ActivityType;
import com.atme.ssyx.model.activity.ActivityInfo;
import com.atme.ssyx.model.activity.ActivityRule;
import com.atme.ssyx.model.activity.ActivitySku;
import com.atme.ssyx.model.activity.CouponInfo;
import com.atme.ssyx.model.order.CartInfo;
import com.atme.ssyx.model.product.SkuInfo;
import com.atme.ssyx.vo.activity.ActivityRuleVo;
import com.atme.ssyx.vo.order.CartInfoVo;
import com.atme.ssyx.vo.order.OrderConfirmVo;
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
 * 活动表 服务实现类
 * </p>
 *
 * @author atme
 * @since 2025-02-18
 */
@Service
public class ActivityInfoServiceImpl extends ServiceImpl<ActivityInfoMapper, ActivityInfo> implements ActivityInfoService {

    @Autowired
    private ActivityRuleMapper activityRuleMapper;

    @Autowired
    private ActivitySkuMapper activitySkuMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private CouponInfoService couponInfoService;
    @Override
    public IPage<ActivityInfo> selectPage(Page<ActivityInfo> pageParam) {
        Page<ActivityInfo> activityInfoPage = baseMapper.selectPage(pageParam, null);
        List<ActivityInfo> records = activityInfoPage.getRecords();
        //遍历集合，得到每个对象，向对象中封装活动类型
        //向对象封装活动类型到属性中
        records.stream().forEach(item -> {
            item.setActivityTypeString(item.getActivityType().getComment());
        });
        return activityInfoPage;
    }

    @Override
    public Map<String, Object> findActivityRuleList(Long id) {
        Map<String, Object> result = new HashMap<>();
        LambdaQueryWrapper<ActivityRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityRule::getActivityId,id);
        List<ActivityRule> activityRuleList = activityRuleMapper.selectList(wrapper);
        result.put("activityRuleList",activityRuleList);

        List<ActivitySku> activitySkuList = activitySkuMapper.selectList(new LambdaQueryWrapper<ActivitySku>().eq(ActivitySku::getActivityId, id));
        List<Long> SkuIdList = activitySkuList.stream().map(ActivitySku::getSkuId).collect(Collectors.toList());
        List<SkuInfo> skuInfoList = productFeignClient.findSkuInfoList(SkuIdList);//远程调用
        result.put("skuInfoList",skuInfoList);
        return result;

    }

    @Override
    public void saveActivityRule(ActivityRuleVo activityRuleVo) {
        //根据活动id删除之前的数据
        //删除ActivityRule
        Long activityId = activityRuleVo.getActivityId();
        activityRuleMapper.delete(new LambdaQueryWrapper<ActivityRule>().eq(ActivityRule::getActivityId,activityId));
        activitySkuMapper.delete(new LambdaQueryWrapper<ActivitySku>().eq(ActivitySku::getActivityId,activityId));

        //获取活动规则的列表
        List<ActivityRule> activityRuleList = activityRuleVo.getActivityRuleList();
        ActivityInfo activityInfo = baseMapper.selectById(activityId);
        for (ActivityRule activityRule : activityRuleList){
            activityRule.setActivityId(activityId);
            activityRule.setActivityType(activityInfo.getActivityType());
            activityRuleMapper.insert(activityRule);
        }

        //获取规则的范围
        List<ActivitySku> activitySkuList = activityRuleVo.getActivitySkuList();
        for (ActivitySku activitySku : activitySkuList){
            activitySku.setActivityId(activityId);
            activitySkuMapper.insert(activitySku);
        }
    }

    @Override
    public List<SkuInfo> findSkuInfoByKeyword(String keyword) {
        //根据输入的关键字查询sku中匹配的内容列表
        //(1) service-product模块中创建接口
        //(2) service-activity调用得到sku内容的列表
        List<SkuInfo> skuInfoList = productFeignClient.findSkuInfoByKeyword(keyword);

        //判断：根据关键字查询不到内容
        if (skuInfoList.isEmpty()){
            return skuInfoList;
        }

        //判断商品是否之前参加过活动,如果参加过并且活动正在进行中,排除商品
        List<Long> skuLists = baseMapper.selectIdList(skuInfoList.stream().map(SkuInfo::getId).collect(Collectors.toList()));
        //查两张表判断  activity-info 和 activity-sku
        List<SkuInfo> finalList = new ArrayList<>();
        //遍历列表
        for (SkuInfo skuInfo: skuInfoList){
            if (!skuLists.contains(skuInfo.getId())){
                finalList.add(skuInfo);
            }
        }
        return finalList;
    }

    @Override
    public Map<Long, List<String>> findActivity(List<Long> skuIdList) {
        Map<Long, List<String>> map = new HashMap<>();
        //遍历skuId，得到每个
        skuIdList.forEach(skuId -> {
            //根据skuId查询每个对应的活动列表
            List<ActivityRule> activityRuleList = baseMapper.findActivityRule(skuId);
            //数据封装，规则名称
            if (!CollectionUtils.isEmpty(activityRuleList)){
                List<String> ruleList = new ArrayList<>();
                for(ActivityRule activityRule :activityRuleList){
                    ruleList.add(this.getRuleDesc(activityRule));
                }
                map.put(skuId,ruleList);
            }
        });
        return map;
    }

    @Override
    public Map<String, Object> findActivityAndCoupon(Long skuId, Long userId) {
        Map<String,Object> result = new HashMap<>();

        //1 根据skuId获取营销活动，一个活动有多个规则
        List<ActivityRule> activityRuleList =this.ruleActivityRuleList(skuId);
        //2 根据skuId和userId查询优惠卷信息
        List<CouponInfo> infoList = couponInfoService.findCouponInfoList(skuId,userId);
        //3 封装到map集合中进行返回
        result.put("infoList",infoList);
        result.put("activityRuleList",activityRuleList);
        return result;

    }

    @Override
    public List<ActivityRule> ruleActivityRuleList(Long skuId) {
        List<ActivityRule> activityRuleList = baseMapper.findActivityRule(skuId);
        for(ActivityRule activityRule : activityRuleList ){
            String ruleDesc = this.getRuleDesc(activityRule);
            activityRule.setRuleDesc(ruleDesc);
        }
        return activityRuleList;
    }

    @Override
    public OrderConfirmVo findCartActivityAndCoupon(List<CartInfo> cartInfoList, Long userId) {
        //1、获取购物车
        List<CartInfoVo> cartActivityList = this.findCartActivityList(cartInfoList);
        //2、计算活动之后的金额
        BigDecimal activityReduceAmount = cartActivityList.stream()
                .filter(cartInfoVo -> cartInfoVo.getActivityRule() != null)
                .map(cartInfoVo -> cartInfoVo.getActivityRule().getReduceAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        //3、获取可以使用的优惠券
        List<CouponInfo> couponInfoList = couponInfoService.findCartCouponInfo(cartInfoList,userId);

        //4、计算使用之后的金额，一次一张
        BigDecimal couponReduceAmount = new BigDecimal(0);
        if (!CollectionUtils.isEmpty(couponInfoList)) {
           couponReduceAmount = couponInfoList.stream().filter(couponInfo -> couponInfo.getIsOptimal().intValue() == 1).map(couponInfo -> couponInfo.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        //5、计算没有参与活动
        BigDecimal originaltTotalAmount = cartInfoList.stream().filter(cartInfo -> cartInfo.getIsChecked() == 1).map(cartInfo -> cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()))).reduce(BigDecimal.ZERO, BigDecimal::add);

        //6、参与活动，使用优惠券总金额
        BigDecimal totalAmount = originaltTotalAmount.subtract(activityReduceAmount).subtract(couponReduceAmount);
        //7、防撞数据
        OrderConfirmVo orderTradeVo = new OrderConfirmVo();
        orderTradeVo.setCarInfoVoList(cartActivityList);
        orderTradeVo.setActivityReduceAmount(activityReduceAmount);
        orderTradeVo.setCouponInfoList(couponInfoList);
        orderTradeVo.setCouponReduceAmount(couponReduceAmount);
        orderTradeVo.setOriginalTotalAmount(originaltTotalAmount);
        orderTradeVo.setTotalAmount(totalAmount);
        return orderTradeVo;
    }

    @Override
    public List<CartInfoVo> findCartActivityList(List<CartInfo> cartInfoList) {
        List<CartInfoVo>  cartInfoVoList = new ArrayList<>();
        //获取所有sku
        List<Long> skuIdList = cartInfoList.stream().map(CartInfo::getSkuId).collect(Collectors.toList());
        //根据所有skuid获取参与的活动
        List<ActivitySku> activitySkuList = baseMapper.selectCartActivitiy(skuIdList);
        //数据分组，每个活动有那些skuId
        //map里面的key是分组字段 活动id
        //value是每组里面的数据
        Map<Long, Set<Long>> activityIdToSkuIdListMap = activitySkuList.stream().collect
                (Collectors.groupingBy(ActivitySku::getActivityId,
                Collectors.mapping(ActivitySku::getSkuId, Collectors.toSet())));

        //获取活动规则数据
        //key活动id，value规则数据
        Map<Long,List<ActivityRule>> activityIdToActivitiyRuleMap = new HashMap<>();
        Set<Long> set = activitySkuList.stream().map(ActivitySku::getSkuId).collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(set)){
            LambdaQueryWrapper<ActivityRule> wrapper = new LambdaQueryWrapper<>();
            //根据前面的先排序，如果相同根据后面的排序
            wrapper.orderByDesc(ActivityRule::getConditionAmount,ActivityRule::getConditionNum);
            wrapper.in(ActivityRule::getActivityId,set);
            List<ActivityRule> activityRuleList = activityRuleMapper.selectList(wrapper);
            //封装到map
            Map<Long, List<ActivityRule>> setMap = activityRuleList.stream().collect
                    (Collectors.groupingBy
                            (activityRule -> activityRule.getActivityId()
                    ));

        }
        //有活动购物项目
        Set<Long> activtiySkuIdSet = new HashSet<>();
        if (!CollectionUtils.isEmpty(activityIdToSkuIdListMap)){
            //map遍历
            Iterator<Map.Entry<Long, Set<Long>>> iterator = activityIdToSkuIdListMap.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<Long, Set<Long>> entry = iterator.next();
                //活动id
                Long activityId = entry.getKey();
                //每个集合中对应的sku列表
                Set<Long> currentIdSet = entry.getValue();

                //获取当前活动对应的购物项列表
                List<CartInfo> currentActivityCartInfoList = cartInfoList.stream().filter(cartInfo -> currentIdSet.contains(cartInfo.getSkuId())).collect(Collectors.toList());

                BigDecimal activityTotalAmount = this.computeTotalAmount(currentActivityCartInfoList);
                int activityTotalNum = this.computeCartNum(currentActivityCartInfoList);

                //计算活动对应规则
                List<ActivityRule> currentactivityRuleList = activityIdToActivitiyRuleMap.get(activityId);
                ActivityType activityType = currentactivityRuleList.get(0).getActivityType();
                ActivityRule activityRule = null;
                //判断活动类型
                if (activityType == ActivityType.FULL_REDUCTION){
                    activityRule = this.computeFullReduction(activityTotalAmount, currentactivityRuleList);
                }else {
                    activityRule = this.computeFullDiscount(activityTotalNum, activityTotalAmount, currentactivityRuleList);
                }

                CartInfoVo cartInfoVo = new CartInfoVo();
                cartInfoVo.setActivityRule(activityRule);
                cartInfoVo.setCartInfoList(currentActivityCartInfoList);
                cartInfoVoList.add(cartInfoVo);

                //记录哪些购物项参与了
                activtiySkuIdSet.addAll(currentIdSet);
            }
        }

        //哪些没有参加
        skuIdList.removeAll(activtiySkuIdSet);
        if (!CollectionUtils.isEmpty(skuIdList)){
            //skuid对应购物项
            Map<Long, CartInfo> skuIdCartInfoMap = cartInfoList.stream().collect(Collectors.toMap(CartInfo::getSkuId, CartInfo -> CartInfo));

            CartInfoVo cartInfoVo = new CartInfoVo();
            List<CartInfo> cartInfos = new ArrayList<>();

            for (Long skuId : skuIdList){
                cartInfoVo.setActivityRule(null);

                cartInfos.add(skuIdCartInfoMap.get(skuId));

                cartInfoVo.setCartInfoList(cartInfos);
            }
        }
        return cartInfoVoList;
    }

    private String getRuleDesc(ActivityRule activityRule) {
        ActivityType activityType = activityRule.getActivityType();
        StringBuffer ruleDesc = new StringBuffer();
        if (activityType == ActivityType.FULL_REDUCTION) {
            ruleDesc
                    .append("满")
                    .append(activityRule.getConditionAmount())
                    .append("元减")
                    .append(activityRule.getBenefitAmount())
                    .append("元");
        } else {
            ruleDesc
                    .append("满")
                    .append(activityRule.getConditionNum())
                    .append("元打")
                    .append(activityRule.getBenefitDiscount())
                    .append("折");
        }
        return ruleDesc.toString();
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

    private int computeCartNum(List<CartInfo> cartInfoList) {
        int total = 0;
        for (CartInfo cartInfo : cartInfoList) {
            //是否选中
            if(cartInfo.getIsChecked().intValue() == 1) {
                total += cartInfo.getSkuNum();
            }
        }
        return total;
    }

    private ActivityRule computeFullReduction(BigDecimal totalAmount, List<ActivityRule> activityRuleList) {
        ActivityRule optimalActivityRule = null;
        //该活动规则skuActivityRuleList数据，已经按照优惠金额从大到小排序了
        for (ActivityRule activityRule : activityRuleList) {
            //如果订单项金额大于等于满减金额，则优惠金额
            if (totalAmount.compareTo(activityRule.getConditionAmount()) > -1) {
                //优惠后减少金额
                activityRule.setReduceAmount(activityRule.getBenefitAmount());
                optimalActivityRule = activityRule;
                break;
            }
        }
        if(null == optimalActivityRule) {
            //如果没有满足条件的取最小满足条件的一项
            optimalActivityRule = activityRuleList.get(activityRuleList.size()-1);
            optimalActivityRule.setReduceAmount(new BigDecimal("0"));
            optimalActivityRule.setSelectType(1);

            StringBuffer ruleDesc = new StringBuffer()
                    .append("满")
                    .append(optimalActivityRule.getConditionAmount())
                    .append("元减")
                    .append(optimalActivityRule.getBenefitAmount())
                    .append("元，还差")
                    .append(totalAmount.subtract(optimalActivityRule.getConditionAmount()))
                    .append("元");
            optimalActivityRule.setRuleDesc(ruleDesc.toString());
        } else {
            StringBuffer ruleDesc = new StringBuffer()
                    .append("满")
                    .append(optimalActivityRule.getConditionAmount())
                    .append("元减")
                    .append(optimalActivityRule.getBenefitAmount())
                    .append("元，已减")
                    .append(optimalActivityRule.getReduceAmount())
                    .append("元");
            optimalActivityRule.setRuleDesc(ruleDesc.toString());
            optimalActivityRule.setSelectType(2);
        }
        return optimalActivityRule;
    }

    private ActivityRule computeFullDiscount(Integer totalNum, BigDecimal totalAmount, List<ActivityRule> activityRuleList) {
        ActivityRule optimalActivityRule = null;
        //该活动规则skuActivityRuleList数据，已经按照优惠金额从大到小排序了
        for (ActivityRule activityRule : activityRuleList) {
            //如果订单项购买个数大于等于满减件数，则优化打折
            if (totalNum.intValue() >= activityRule.getConditionNum()) {
                BigDecimal skuDiscountTotalAmount = totalAmount.multiply(activityRule.getBenefitDiscount().divide(new BigDecimal("10")));
                BigDecimal reduceAmount = totalAmount.subtract(skuDiscountTotalAmount);
                activityRule.setReduceAmount(reduceAmount);
                optimalActivityRule = activityRule;
                break;
            }
        }
        if(null == optimalActivityRule) {
            //如果没有满足条件的取最小满足条件的一项
            optimalActivityRule = activityRuleList.get(activityRuleList.size()-1);
            optimalActivityRule.setReduceAmount(new BigDecimal("0"));
            optimalActivityRule.setSelectType(1);

            StringBuffer ruleDesc = new StringBuffer()
                    .append("满")
                    .append(optimalActivityRule.getConditionNum())
                    .append("元打")
                    .append(optimalActivityRule.getBenefitDiscount())
                    .append("折，还差")
                    .append(totalNum-optimalActivityRule.getConditionNum())
                    .append("件");
            optimalActivityRule.setRuleDesc(ruleDesc.toString());
        } else {
            StringBuffer ruleDesc = new StringBuffer()
                    .append("满")
                    .append(optimalActivityRule.getConditionNum())
                    .append("元打")
                    .append(optimalActivityRule.getBenefitDiscount())
                    .append("折，已减")
                    .append(optimalActivityRule.getReduceAmount())
                    .append("元");
            optimalActivityRule.setRuleDesc(ruleDesc.toString());
            optimalActivityRule.setSelectType(2);
        }
        return optimalActivityRule;
    }
}
