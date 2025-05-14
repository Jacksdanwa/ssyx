package com.atme.ssyx.user.api;

import com.atme.ssyx.user.service.UserService;
import com.atme.ssyx.vo.user.LeaderAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/leader")
public class LeaderAddressApiController {

    @Autowired
    private UserService userService;

    @GetMapping("/inner/getUserAddressByUserId/{userId}")
    public LeaderAddressVo leaderAddressVo(@PathVariable Long userId){
        return userService.getLeaderByUserId(userId);
    }

}
