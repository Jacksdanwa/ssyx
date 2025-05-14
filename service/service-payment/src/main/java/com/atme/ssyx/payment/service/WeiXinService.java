package com.atme.ssyx.payment.service;

import java.util.Map;

public interface WeiXinService {
    Map<String, String> createJsapi(String orderNo);

    Map<String, String> queryPayStatus(String orderNo);
}
