package com.atme.ssyx.product.service.impl;

import com.atme.ssyx.common.constant.MqConst;
import com.atme.ssyx.common.constant.RedisConst;
import com.atme.ssyx.common.exception.SsyxException;
import com.atme.ssyx.common.result.ResultCodeEnum;
import com.atme.ssyx.model.product.SkuAttrValue;
import com.atme.ssyx.model.product.SkuImage;
import com.atme.ssyx.model.product.SkuInfo;
import com.atme.ssyx.model.product.SkuPoster;
import com.atme.ssyx.mq.service.RabbitService;
import com.atme.ssyx.product.mapper.SkuInfoMapper;
import com.atme.ssyx.product.service.SkuAttrValueService;
import com.atme.ssyx.product.service.SkuImageService;
import com.atme.ssyx.product.service.SkuInfoService;
import com.atme.ssyx.product.service.SkuPosterService;
import com.atme.ssyx.vo.product.SkuInfoQueryVo;
import com.atme.ssyx.vo.product.SkuInfoVo;
import com.atme.ssyx.vo.product.SkuStockLockVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 * sku信息 服务实现类
 * </p>
 *
 * @author atguigu
 * @since 2025-02-12
 */
@Service
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo> implements SkuInfoService {

    //图片
    @Autowired
    private SkuImageService skuImageService;

    //海报
    @Autowired
    private SkuPosterService skuPosterService;

    //属性
    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public IPage<SkuInfo> selectPageSkuInfo(Page<SkuInfo> pageParam, SkuInfoQueryVo skuInfoQueryVo) {
        Long categoryId = skuInfoQueryVo.getCategoryId();
        String keyword = skuInfoQueryVo.getKeyword();
        String skuType = skuInfoQueryVo.getSkuType();

        LambdaQueryWrapper<SkuInfo> wrapper = new LambdaQueryWrapper<>();
        if (!StringUtils.isEmpty(keyword)){
            wrapper.like(SkuInfo::getSkuName,keyword);
        }
        if (!StringUtils.isEmpty(categoryId)){
            wrapper.eq(SkuInfo::getCategoryId,categoryId);
        }
        if (!StringUtils.isEmpty(skuType)){
            wrapper.like(SkuInfo::getSkuType,skuType);
        }

        IPage<SkuInfo> pageModel = baseMapper.selectPage(pageParam, wrapper);
        return pageModel;
    }

    @Override
    public void saveSkuInfo(SkuInfoVo skuInfoVo) {
        //1、添加sku基本信息
        //将vo中的信息复制到skuInfo中去
        SkuInfo skuInfo = new SkuInfo();
        BeanUtils.copyProperties(skuInfoVo,skuInfo);
        baseMapper.insert(skuInfo);

        //保存海报的信息
        List<SkuPoster> skuPosterList = skuInfoVo.getSkuPosterList();
        if (!CollectionUtils.isEmpty(skuPosterList)){
            //遍历 向每个海报添加商品的sku_id
            for (SkuPoster skuPoster : skuPosterList ){
                skuPoster.setId(skuInfo.getId());
            }
            skuPosterService.saveBatch(skuPosterList);
        }

        //保存图片信息
        List<SkuImage> skuImagesList = skuInfoVo.getSkuImagesList();
        if (!CollectionUtils.isEmpty(skuImagesList)){
            for (SkuImage skuImage: skuImagesList) {
                skuImage.setId(skuInfo.getId());
            }
            skuImageService.saveBatch(skuImagesList);
        }

        //保存属性信息
        List<SkuAttrValue> skuAttrValueList = skuInfoVo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)){
            for (SkuAttrValue skuAttrValue: skuAttrValueList) {
                skuAttrValue.setId(skuInfo.getId());
            }
            skuAttrValueService.saveBatch(skuAttrValueList);
        }


    }

    @Override
    public SkuInfoVo getSkuInfo(Long id) {
        SkuInfoVo skuInfoVo = new SkuInfoVo();

        //根据sku查询基本信息
        SkuInfo skuInfo = baseMapper.selectById(id);
        List<SkuImage> skuImageList = skuImageService.getImageListBySkuId(id);
        List<SkuPoster> skuPosterList = skuPosterService.getPosterListBySkuId(id);
        List<SkuAttrValue> skuAttrValueList = skuAttrValueService.getAttrValueBySkuId(id);

        BeanUtils.copyProperties(skuInfo,skuInfoVo);
        skuInfoVo.setSkuImagesList(skuImageList);
        skuInfoVo.setSkuAttrValueList(skuAttrValueList);
        skuInfoVo.setSkuPosterList(skuPosterList);
        return skuInfoVo;
    }

    @Override
    public void updateSkuInfo(SkuInfoVo skuInfoVo) {
        //修改基本信息
        SkuInfo skuInfo = new SkuInfo();
        BeanUtils.copyProperties(skuInfoVo,skuInfo);
        baseMapper.updateById(skuInfo);

        Long skuId = skuInfoVo.getId();
        //海报信息
        LambdaQueryWrapper<SkuPoster> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkuPoster::getSkuId,skuId);
        skuPosterService.remove(wrapper);

        List<SkuPoster> skuPosterList = skuInfoVo.getSkuPosterList();
        if (!CollectionUtils.isEmpty(skuPosterList)){
            //遍历 向每个海报添加商品的sku_id
            for (SkuPoster skuPoster : skuPosterList ){
                skuPoster.setId(skuId);
            }
            skuPosterService.saveBatch(skuPosterList);
        }
        skuImageService.remove(new LambdaQueryWrapper<SkuImage>().eq(SkuImage::getSkuId,skuId));
        List<SkuImage> skuImagesList = skuInfoVo.getSkuImagesList();
        if (!CollectionUtils.isEmpty(skuImagesList)){
            for (SkuImage skuImage: skuImagesList) {
                skuImage.setId(skuId);
            }
            skuImageService.saveBatch(skuImagesList);
        }

        skuAttrValueService.remove(new LambdaQueryWrapper<SkuAttrValue>().eq(SkuAttrValue::getSkuId,skuId));
        List<SkuAttrValue> skuAttrValueList = skuInfoVo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)){
            for (SkuAttrValue skuAttrValue: skuAttrValueList) {
                skuAttrValue.setId(skuId);
            }
            skuAttrValueService.saveBatch(skuAttrValueList);
        }
    }

    @Override
    public void check(Long skuId, Integer status) {
        SkuInfo skuInfo = baseMapper.selectById(skuId);
        skuInfo.setCheckStatus(status);
        baseMapper.updateById(skuInfo);
    }

    @Override
    public void publish(Long skuId, Integer status) {
        if (status == 1){ //上架
            SkuInfo skuInfo = baseMapper.selectById(skuId);
            skuInfo.setPublishStatus(status);
            baseMapper.updateById(skuInfo);
            //整合mq
            //三个参数，前面两个在常量类中有所体现，最后一个meassage是skuId
            rabbitService.sendMessage(MqConst.EXCHANGE_GOODS_DIRECT,MqConst.ROUTING_GOODS_UPPER,skuId);
        } else {
            SkuInfo skuInfo = baseMapper.selectById(skuId);
            skuInfo.setPublishStatus(status);
            baseMapper.updateById(skuInfo);
            rabbitService.sendMessage(MqConst.EXCHANGE_GOODS_DIRECT,MqConst.ROUTING_GOODS_LOWER,skuId);
        }

    }

    @Override
    public void isNewPerson(Long skuId, Integer status) {
        SkuInfo skuInfo = baseMapper.selectById(skuId);
        skuInfo.setIsNewPerson(status);
        baseMapper.updateById(skuInfo);
    }

    @Override
    public List<SkuInfo> findSkuInfoList(List<Long> skuIdList) {
        return baseMapper.selectBatchIds(skuIdList);
    }

    @Override
    public List<SkuInfo> findSkuInfoByKeyword(String keyword) {
        return baseMapper.selectList(new LambdaQueryWrapper<SkuInfo>().like(SkuInfo::getSkuName,keyword));
    }

    @Override
    public List<SkuInfo> findNewPersonSkuInfoList() {
        //条件1： is_new_person = 1
        //条件2： publish_status = 1
        //条件3： 显示其中的三个
        //分页实现
        //获取第一页的数据
        Page<SkuInfo> page = new Page<>(1,3);
        LambdaQueryWrapper<SkuInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkuInfo::getIsNewPerson,1);
        wrapper.eq(SkuInfo::getPublishStatus,1);
        wrapper.orderByDesc(SkuInfo::getStock);//库存排序
        //调用方法查询
        Page<SkuInfo> skuInfoPage = baseMapper.selectPage(page, wrapper);
        List<SkuInfo> records = skuInfoPage.getRecords();
        return records;
    }

    @Override
    public SkuInfoVo getSkuInfoVo(Long skuId) {
        SkuInfoVo skuInfoVo = new SkuInfoVo();
        //skuId查询SkuInfo
        SkuInfo skuInfo = baseMapper.selectById(skuId);

        //skuId查询图片
        List<SkuImage> imageListBySkuId = skuImageService.getImageListBySkuId(skuId);
        //skuId查询海报
        List<SkuPoster> posterListBySkuId = skuPosterService.getPosterListBySkuId(skuId);
        //skuId得到属性相关信息
        List<SkuAttrValue> attrValueBySkuId = skuAttrValueService.getAttrValueBySkuId(skuId);

        BeanUtils.copyProperties(skuInfo,skuInfoVo);
        skuInfoVo.setSkuImagesList(imageListBySkuId);
        skuInfoVo.setSkuPosterList(posterListBySkuId);
        skuInfoVo.setSkuAttrValueList(attrValueBySkuId);

        return skuInfoVo;
    }

    //验证与锁定库存
    @Override
    public Boolean checkAndLock(List<SkuStockLockVo> skuStockLockVos, String orderNo) {
        //1 判断skuStockLockVos是否为空
        if (!CollectionUtils.isEmpty(skuStockLockVos)){
            throw new SsyxException(ResultCodeEnum.DATA_ERROR);
        }

        //2 遍历skuStockLockVos得到其中的每个商品，验证库存，具备原子性
        skuStockLockVos.forEach(skuStockLockVo ->{
            this.checkLock(skuStockLockVo);
        });

        //3 只要有一个商品锁定失败的情况下，所有锁定成功的商品都解锁
        boolean flag = skuStockLockVos.stream().anyMatch(skuStockLockVo -> !skuStockLockVo.getIsLock());
        if (flag){
            skuStockLockVos.stream()
                    .filter(SkuStockLockVo::getIsLock)
                    .forEach(skuStockLockVo -> baseMapper.unlockStock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum()));
            return false;
        }
        //4 如果所有商品都成功了,redis缓存，为了方便后面减库存
        redisTemplate.opsForValue().set(RedisConst.STOCK_INFO + orderNo,skuStockLockVos);
        return true;
    }

    @Override
    public void minusStock(String orderNo) {
        //从redis获取锁定库存信息
        List<SkuStockLockVo> skuStockLockVoList = (List<SkuStockLockVo>) redisTemplate.opsForValue().get(RedisConst.STOCK_INFO + orderNo);
        if (CollectionUtils.isEmpty(skuStockLockVoList)){
            return;
        }
        //遍历集合得到对象减库存
        skuStockLockVoList.forEach(skuStockLockVo ->{
            baseMapper.minusStock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum());
        });

        redisTemplate.delete(RedisConst.STOCK_INFO + orderNo);
    }

    public void checkLock(SkuStockLockVo skuStockLockVo){
        //获取锁
        //公平锁
        RLock rLock = this.redissonClient.getFairLock(RedisConst.SKUKEY_PREFIX + skuStockLockVo.getSkuId());

        //加锁
        rLock.lock();

        try{
            //验证库存
            SkuInfo skuInfo = baseMapper.checkStock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum());
            //如果没有满足条件的商品，设置isLock值返回
            if (skuInfo == null){
                skuStockLockVo.setIsLock(false);
                return;
            }
            //有满足条件的商品
            //锁定库存
            //rows是影响行数
            Integer rows = baseMapper.lockStock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum());
            if (rows == 1){
                skuStockLockVo.setIsLock(true);
            }
        }finally {
            //解锁
            rLock.unlock();
        }
    }
}
