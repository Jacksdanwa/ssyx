package com.atme.ssyx.activity.controller;


import com.atme.ssyx.activity.service.CouponInfoService;
import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.model.activity.CouponInfo;
import com.atme.ssyx.vo.activity.CouponRuleVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * <p>
 * 优惠券信息 前端控制器
 * </p>
 *
 * @author atguigu
 * @since 2025-02-18
 */
@RestController
@RequestMapping("/admin/activity/couponInfo")
//@CrossOrigin
public class CouponInfoController {

    @Autowired
    private CouponInfoService couponInfoService;

    //优惠劵分页查询接口
    @GetMapping("{page}/{limit}")
    public Result list(@PathVariable Long page,
                       @PathVariable Long limit){
        Page<CouponInfo> pageParam = new Page<>(page, limit);
        IPage<CouponInfo> pageModel = couponInfoService.selectlist(pageParam);
        return Result.ok(pageModel);
    }

    //添加优惠券接口
    @PostMapping("save")
    public Result save(@RequestBody CouponInfo couponInfo){
        couponInfoService.save(couponInfo);
        return Result.ok(null);
    }

    //根据id查询优惠券
    @GetMapping("get/{id}")
    public Result getById(@PathVariable Long id){
        CouponInfo couponInfo = couponInfoService.getCouponById(id);
        return Result.ok(couponInfo);
    }

    //根据优惠券的id查询规则数据
    @GetMapping("findCouponRuleList/{id}")
    public Result findCouponRuleList(@PathVariable Long id){
        Map<String,Object> map = couponInfoService.findCouponRuleList(id);
        return Result.ok(map);
    }

    //添加规则数据
    @PostMapping("saveCouponRule")
    public Result saveCouponRule(@RequestBody CouponRuleVo couponRuleVo){
        couponInfoService.saveCouponRule(couponRuleVo);
        return Result.ok(null);
    }
}

