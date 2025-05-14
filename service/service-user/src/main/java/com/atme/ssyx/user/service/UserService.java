package com.atme.ssyx.user.service;


import com.atme.ssyx.model.user.User;
import com.atme.ssyx.vo.user.LeaderAddressVo;
import com.atme.ssyx.vo.user.UserLoginVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {


    User getUserByOpenId(String openid);

    LeaderAddressVo getLeaderByUserId(Long userId);

    UserLoginVo getUserLoginVo(Long id);
}
