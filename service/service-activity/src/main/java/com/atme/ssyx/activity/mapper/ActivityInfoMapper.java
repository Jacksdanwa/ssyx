package com.atme.ssyx.activity.mapper;

import com.atme.ssyx.model.activity.ActivityInfo;
import com.atme.ssyx.model.activity.ActivityRule;
import com.atme.ssyx.model.activity.ActivitySku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 活动表 Mapper 接口
 * </p>
 *
 * @author atguigu
 * @since 2025-02-18
 */
@Mapper
public interface ActivityInfoMapper extends BaseMapper<ActivityInfo> {

    List<Long> selectIdList(@Param("skuIdList") List<Long> skuIdList);

    List<ActivityRule> findActivityRule(@Param("skuId") Long skuId);

    List<ActivitySku> selectCartActivitiy(@Param("skuIdList") List<Long> skuIdList);

}
