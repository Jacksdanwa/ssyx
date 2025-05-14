package com.atme.ssyx.user.service.Impl;

import com.atme.ssyx.model.user.Leader;
import com.atme.ssyx.model.user.User;
import com.atme.ssyx.model.user.UserDelivery;
import com.atme.ssyx.user.service.UserService;
import com.atme.ssyx.user.mapper.LeaderMapper;
import com.atme.ssyx.user.mapper.UserDeliveryMapper;
import com.atme.ssyx.user.mapper.UserMapper;
import com.atme.ssyx.vo.user.LeaderAddressVo;
import com.atme.ssyx.vo.user.UserLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserDeliveryMapper userDeliveryMapper;

    @Autowired
    private LeaderMapper leaderMapper;
    //
    @Override
    public User getUserByOpenId(String openid) {
        return baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getId,openid));
    }

    @Override
    public LeaderAddressVo getLeaderByUserId(Long userId) {
        UserDelivery userDelivery = userDeliveryMapper.selectOne(new LambdaQueryWrapper<UserDelivery>()
                .eq(UserDelivery::getUserId, userId)
                .eq(UserDelivery::getIsDefault, 1));
        if (userDelivery == null){
            return null;
        }
        //查询leader表
        Leader leader = leaderMapper.selectById(userDelivery.getId());
        LeaderAddressVo leaderAddressVo = new LeaderAddressVo();
        BeanUtils.copyProperties(leader, leaderAddressVo);
        leaderAddressVo.setUserId(userId);
        leaderAddressVo.setLeaderId(leader.getId());
        leaderAddressVo.setLeaderName(leader.getName());
        leaderAddressVo.setLeaderPhone(leader.getPhone());
        leaderAddressVo.setWareId(userDelivery.getWareId());
        leaderAddressVo.setStorePath(leader.getStorePath());
        return leaderAddressVo;
    }

    @Override
    public UserLoginVo getUserLoginVo(Long id) {
        User user = baseMapper.selectById(id);
        UserLoginVo userLoginVo = new UserLoginVo();
        userLoginVo.setNickName(user.getNickName());
        userLoginVo.setUserId(id);
        userLoginVo.setPhotoUrl(user.getPhotoUrl());
        userLoginVo.setOpenId(user.getOpenId());
        userLoginVo.setIsNew(user.getIsNew());

        UserDelivery userDelivery = userDeliveryMapper.selectOne(
                new LambdaQueryWrapper<UserDelivery>()
                        .eq(UserDelivery::getUserId, id)
                        .eq(UserDelivery::getIsDefault,1)
        );
        if (userDelivery != null){
            userLoginVo.setLeaderId(userDelivery.getLeaderId());
            userLoginVo.setWareId(userDelivery.getWareId());
        } else {
            userLoginVo.setWareId(1L);
            userLoginVo.setLeaderId(1L);
        }
        return userLoginVo;
    }
}
