package com.atme.ssyx.client.cart.service;

import com.atme.ssyx.model.order.CartInfo;
import com.atme.ssyx.model.product.SkuInfo;

import java.util.List;

public interface CartInfoService {
    void addToCart(Long skuId, Integer skuNum, Long userId);

    void deleteCart(Long skuId, Long userId);

    void deleteAllCart(Long userId);

    void batchDeleteCart(Long userId, List<SkuInfo> skuInfoList);

    List<CartInfo> getCartList(Long userId);

    List<CartInfo> getCartCheckedList(Long userId);

    void deleteCartChecked(Long userId);
}
