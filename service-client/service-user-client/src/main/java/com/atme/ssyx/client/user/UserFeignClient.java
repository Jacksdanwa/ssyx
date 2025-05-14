package com.atme.ssyx.client.user;


import com.atme.ssyx.vo.user.LeaderAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

//使用远程调用的原因
//1 一个模块不导入多个数据库
//2 有些方法在user模块中以及写过，没有必要重新写一遍，在新模块中调用原来模块中的数据
//
@FeignClient(name = "service-user")
public interface UserFeignClient {

    @GetMapping("/api/user/leader/inner/getUserAddressByUserId/{userId}")
    public LeaderAddressVo getleaderAddressVo(@PathVariable Long userId);

}
