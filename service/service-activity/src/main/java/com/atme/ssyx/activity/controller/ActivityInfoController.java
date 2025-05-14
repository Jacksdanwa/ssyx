package com.atme.ssyx.activity.controller;


import com.atme.ssyx.activity.service.ActivityInfoService;
import com.atme.ssyx.common.result.Result;
import com.atme.ssyx.model.activity.ActivityInfo;
import com.atme.ssyx.model.product.SkuInfo;
import com.atme.ssyx.vo.activity.ActivityRuleVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 活动表 前端控制器
 * </p>
 *
 * @author atguigu
 * @since 2025-02-18
 */
@RestController
@RequestMapping("/admin/activity/activityInfo")
//@CrossOrigin

public class ActivityInfoController {

    @Autowired
    private ActivityInfoService activityInfoService;

    @GetMapping("{page}/{limit}")
    private Result list(@PathVariable Long page,
                              @PathVariable Long limit){
        Page<ActivityInfo> pageParam = new Page<>(page,limit);
        IPage<ActivityInfo> pageModel = activityInfoService.selectPage(pageParam);
        return Result.ok(pageModel);
    }

    @GetMapping("save")
    private Result save(@RequestBody ActivityInfo activityInfo){
        activityInfoService.save(activityInfo);
        return Result.ok(null);
    }

    @ApiOperation("修改用户")
    @PutMapping("update")
    public Result update(@RequestBody ActivityInfo activityInfo){
        activityInfoService.updateById(activityInfo);
        return Result.ok(null);
    }

    //5 删除用户
    @ApiOperation("删除用户")
    @DeleteMapping("remove/{id}")
    public Result deteleById(@PathVariable Long id){
        activityInfoService.removeById(id);
        return Result.ok(null);
    }

    //6 批量删除用户
    @ApiOperation("批量删除用户")
    @DeleteMapping("batchremove")
    public Result batchRemove(@RequestBody List<Long> idlist){
        activityInfoService.removeByIds(idlist);
        return Result.ok(null);
    }

    //营销活动规则相关接口
    //根据活动id获取规则数据
    @GetMapping("findActivityRuleList/{id}")
    public Result findActivityRuleList(@PathVariable Long id){
        Map<String,Object> ActivityRuleType = activityInfoService.findActivityRuleList(id);
        return Result.ok(ActivityRuleType);
    }

    //在活动里面添加数据
    @PostMapping("saveActivityRule")
    public Result saveActivityRule(@RequestBody ActivityRuleVo activityRuleVo){
        activityInfoService.saveActivityRule(activityRuleVo);
        return Result.ok(null);
    }
    //根据关键字查询匹配sku信息
    @GetMapping("findSkuInfoByKeyword/{keyword}")
    public Result findSkuInfoByKeyword(@PathVariable String keyword){
        List<SkuInfo> skuInfoList = activityInfoService.findSkuInfoByKeyword(keyword);
        return Result.ok(skuInfoList);
    }

    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id){
        ActivityInfo activityInfo = activityInfoService.getById(id);
        activityInfo.setActivityTypeString(activityInfo.getActivityType().getComment());
        return Result.ok(activityInfo);
    }

}

