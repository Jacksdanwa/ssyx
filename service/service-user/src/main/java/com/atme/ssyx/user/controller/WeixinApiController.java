package com.atme.ssyx.user.controller;

import com.alibaba.fastjson2.JSONObject;
import com.atme.ssyx.common.constant.RedisConst;
import com.atme.ssyx.common.exception.SsyxException;
import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.common.result.ResultCodeEnum;
import com.atme.ssyx.common.utils.JwtHelper;
import com.atme.ssyx.enums.UserType;
import com.atme.ssyx.model.user.User;
import com.atme.ssyx.user.service.UserService;
import com.atme.ssyx.user.utils.ConstantPropertiesUtil;
import com.atme.ssyx.user.utils.HttpClientUtils;
import com.atme.ssyx.vo.user.LeaderAddressVo;
import com.atme.ssyx.vo.user.UserLoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/weixin")
public class WeixinApiController {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @GetMapping("/wxLogin/{code}")
    public Result loginWX(@PathVariable String code){
        //1 得到微信返回的code临时票据
        //2 拿着code + 小程序id + 小程序密钥 请求微信接口服务
        ////使用HttpClient请求
        String wxOpenAppId = ConstantPropertiesUtil.WX_OPEN_APP_ID;
        String wxOpenAppSecret = ConstantPropertiesUtil.WX_OPEN_APP_SECRET;
        StringBuffer url = new StringBuffer()
                .append("https://api.weixin.qq.com/sns/jscode2session")
                .append("?appid=%s")
                .append("&secret=%s")
                .append("&js_code=%s")
                .append("&grant_type=authorization_code");
        String tokenUrl = String.format(url.toString(), wxOpenAppId, wxOpenAppSecret, code);
        //HttpClient发送get请求
        String result = null;
        try {
            result = HttpClientUtils.get(tokenUrl);
        } catch (Exception e) {
            throw new SsyxException(ResultCodeEnum.FETCH_ACCESSTOKEN_FAILD);
        }

        //3 请求微信接口服务 返回两个值
        JSONObject jsonObject = JSONObject.parseObject(result,null);
        String sessionKey = jsonObject.getString("session_key");
        String openid = jsonObject.getString("openid");

        //4 将微信的值添加到数据库中去
        //操作User表  判断方式 openid
        User user = userService.getUserByOpenId(openid);
        if (user == null) {
            user = new User();
            user.setOpenId(openid);
            user.setNickName(openid);
            user.setPhotoUrl("");
            user.setUserType(UserType.USER);
            user.setIsNew(0);
            userService.save(user);
        }
        //5 根据UserId查询提货点
        LeaderAddressVo leaderAddressVo = userService.getLeaderByUserId(user.getId());
        //6 JWT工具生成字符串
        String token = JwtHelper.createToken(user.getId(), user.getNickName());

        //redis
        UserLoginVo userLoginVo = userService.getUserLoginVo(user.getId());
        redisTemplate.opsForValue()
                .set(RedisConst.USER_LOGIN_KEY_PREFIX + user.getId(),
                        userLoginVo,RedisConst.USERKEY_TIMEOUT,
                        TimeUnit.DAYS);
        //封装数据

        Map<String,Object> map = new HashMap<>();
        map.put("user",user);
        map.put("token",token);
        map.put("LeaderAddressVO",leaderAddressVo);
        return Result.ok(map);
    }
}
