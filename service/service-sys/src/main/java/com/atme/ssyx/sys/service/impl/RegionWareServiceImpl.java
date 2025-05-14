package com.atme.ssyx.sys.service.impl;

import com.atme.ssyx.common.exception.SsyxException;
import com.atme.ssyx.common.result.ResultCodeEnum;
import com.atme.ssyx.model.sys.RegionWare;
import com.atme.ssyx.sys.mapper.RegionWareMapper;
import com.atme.ssyx.sys.service.RegionWareService;
import com.atme.ssyx.vo.sys.RegionWareQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * <p>
 * 城市仓库关联表 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2025-02-04
 */
@Service
public class RegionWareServiceImpl extends ServiceImpl<RegionWareMapper, RegionWare> implements RegionWareService {

    @Override
    public IPage<RegionWare> selectPage(Page<RegionWare> page1, RegionWareQueryVo regionWareQueryVo) {
        //获取查询条件值
        String keyword = regionWareQueryVo.getKeyword();
        //判断条件值是否为空，不为空封装条件
        LambdaQueryWrapper<RegionWare> wrapper = new LambdaQueryWrapper<>();
        if (!StringUtils.isEmpty(keyword)){
            wrapper.like(RegionWare::getRegionName,keyword)
                    .or()
                    .like(RegionWare::getWareName, keyword);
        }
        //调用方法实现条件分页查询
        Page<RegionWare> regionWarePage = baseMapper.selectPage(page1, wrapper);
        //数据返回
        return regionWarePage;
    }

    //添加开通区域
    @Override
    public void saveRegionWare(RegionWare regionWare) {
        LambdaQueryWrapper<RegionWare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RegionWare::getRegionId,regionWare.getRegionId());
        // >0表示有数据  =0表示没有数据
        Integer count = baseMapper.selectCount(wrapper);
        if (count > 0){
            //抛出异常
            throw new SsyxException(ResultCodeEnum.REGION_OPEN);
        }
        baseMapper.insert(regionWare);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        RegionWare regionWare = baseMapper.selectById(id);
        regionWare.setStatus(status);
        baseMapper.updateById(regionWare);
    }
}
