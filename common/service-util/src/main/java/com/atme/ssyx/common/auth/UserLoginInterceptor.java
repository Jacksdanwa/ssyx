package com.atme.ssyx.common.auth;

import com.atme.ssyx.common.constant.RedisConst;
import com.atme.ssyx.common.utils.JwtHelper;
import com.atme.ssyx.vo.user.UserLoginVo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserLoginInterceptor implements HandlerInterceptor {

    //不自动注入的原因是因为当前类没有被spring管理
    private RedisTemplate redisTemplate;
    public UserLoginInterceptor(RedisTemplate redisTemplate){
        this.redisTemplate = redisTemplate;
    }


    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        this.getUserLoginVo(request);
        return true;
    }

    private void getUserLoginVo(HttpServletRequest request) {
        //从请求头中获取token
        String token = request.getHeader("token");
        //判断
        if (!StringUtils.isEmpty(token)){
            Long userId = JwtHelper.getUserId(token);
            UserLoginVo userLoginVo = (UserLoginVo) redisTemplate.opsForValue().get(RedisConst.USER_LOGIN_KEY_PREFIX + userId);
            if (userLoginVo != null) {
                AuthContextHolder.setWareId(userLoginVo.getWareId());
                AuthContextHolder.setUserId(userLoginVo.getUserId());
                AuthContextHolder.setUserLoginVo(userLoginVo);
            }
        }
    }
}
