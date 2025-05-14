package com.atme.ssyx.client.cart.service.Impl;

import com.atme.ssyx.client.cart.service.CartInfoService;
import com.atme.ssyx.client.product.ProductFeignClient;
import com.atme.ssyx.common.constant.RedisConst;
import com.atme.ssyx.common.exception.SsyxException;
import com.atme.ssyx.common.result.ResultCodeEnum;
import com.atme.ssyx.enums.SkuType;
import com.atme.ssyx.model.order.CartInfo;
import com.atme.ssyx.model.product.SkuInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartInfoServiceImpl implements CartInfoService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ProductFeignClient productFeignClient;

    //返回购物车中rediskey的名字
    private String getCartKey(Long userId){
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }
    //添加
    @Override
    public void addToCart(Long skuId, Integer skuNum, Long userId) {
        //购物车数据存储到redis  key中包含用户id
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> HashOperations = redisTemplate.boundHashOps(cartKey);
        CartInfo cartInfo = null;
        //根据结果判断，得到skuId和skuNum
        //判断结果中是否有skuId
        if(HashOperations.hasKey(skuId.toString())){
            cartInfo = HashOperations.get(skuId.toString());
            Integer currentSkuNum = cartInfo.getSkuNum() + skuNum;
            if (currentSkuNum < 1){
                return;
            }
            //更新cartInfo对象
            cartInfo.setSkuNum(currentSkuNum);
            cartInfo.setCurrentBuyNum(currentSkuNum);
            //
            if (cartInfo.getPerLimit() < currentSkuNum){
                throw new SsyxException(ResultCodeEnum.SKU_LIMIT_ERROR);
            }
            cartInfo.setIsChecked(1);
            cartInfo.setUpdateTime(new Date());
        }
        else {
            skuNum = 1;

            //远程调用
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            if(skuInfo == null){
                throw new SsyxException(ResultCodeEnum.DATA_ERROR);
            }

            //封装对象
            cartInfo.setSkuId(skuId);
            cartInfo.setCategoryId(skuInfo.getCategoryId());
            cartInfo.setSkuType(skuInfo.getSkuType());
            cartInfo.setIsNewPerson(skuInfo.getIsNewPerson());
            cartInfo.setUserId(userId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setCurrentBuyNum(skuNum);
            cartInfo.setSkuType(SkuType.COMMON.getCode());
            cartInfo.setPerLimit(skuInfo.getPerLimit());
            cartInfo.setImgUrl(skuInfo.getImgUrl());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setWareId(skuInfo.getWareId());
            cartInfo.setIsChecked(1);
            cartInfo.setStatus(1);
            cartInfo.setCreateTime(new Date());
            cartInfo.setUpdateTime(new Date());

            //更新缓存
            HashOperations.put(skuId.toString(),cartInfo);
            //设置过期时间
            this.setCartKeyExpire(cartKey);
        }
    }

    @Override
    public void deleteCart(Long skuId, Long userId) {
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(this.getCartKey(userId));
        if (boundHashOperations.hasKey(skuId.toString())){
            boundHashOperations.delete(skuId.toString());
        }
    }

    @Override
    public void deleteAllCart(Long userId) {
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(this.getCartKey(userId));
        List<CartInfo> values = boundHashOperations.values();
        for (CartInfo cartInfo : values){
            boundHashOperations.delete(cartInfo.getSkuId().toString());
        }
    }

    @Override
    public void batchDeleteCart(Long userId, List<SkuInfo> skuInfoList) {
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(this.getCartKey(userId));
        skuInfoList.forEach(skuId ->{
            boundHashOperations.delete(skuId.toString());
        });
    }

    @Override
    public List<CartInfo> getCartList(Long userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        if (!StringUtils.isEmpty(userId)){
            return cartInfoList;
        }
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(this.getCartKey(userId));
        cartInfoList = boundHashOperations.values();
        if (!CollectionUtils.isEmpty(cartInfoList)){
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getCreateTime().compareTo(o2.getCreateTime());
                }
            });
        }


        return cartInfoList;
    }

    //获取购物车中选中的购物项
    @Override
    public List<CartInfo> getCartCheckedList(Long userId) {
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        List<CartInfo> cartInfoList = boundHashOperations.values();
        //ischecked = 1
        List<CartInfo> infoList = cartInfoList.stream().filter(cartInfo -> {
            return cartInfo.getIsChecked().intValue() == 1;
        }).collect(Collectors.toList());
        return infoList;
    }

    @Override
    public void deleteCartChecked(Long userId) {
        //根据用户id查询购物车的记录
        List<CartInfo> cartCheckedList = this.getCartCheckedList(userId);
        //查询list集合进行遍历，得到每个skuId集合
        List<Long> list = cartCheckedList.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
        //构建key值
        String cartKey = this.getCartKey(userId);

        //根据key查询filed-value的结构
        BoundHashOperations<String,String,CartInfo> boundHashOperations = redisTemplate.boundHashOps(cartKey);

        //根据filed删除
        list.forEach(skuId -> {
            boundHashOperations.delete(skuId.toString());
        });
    }

    //设置key过期时间
    private void setCartKeyExpire(String key){
        redisTemplate.expire(key,RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }
}
