package com.atme.ssyx.search.service.Impl;

import com.atme.ssyx.client.activity.ActivityFeignClient;
import com.atme.ssyx.client.product.ProductFeignClient;
import com.atme.ssyx.common.auth.AuthContextHolder;
import com.atme.ssyx.enums.SkuType;
import com.atme.ssyx.model.product.Category;
import com.atme.ssyx.model.product.SkuInfo;
import com.atme.ssyx.model.search.SkuEs;
import com.atme.ssyx.search.repository.SkuRepository;
import com.atme.ssyx.search.service.SkuService;
import com.atme.ssyx.vo.search.SkuEsQueryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    private SkuRepository skuRepository;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ActivityFeignClient activityFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void upperSku(Long skuId) {
        //通过远程调用,根据skuid获取相关信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);

        if (skuInfo == null){
            return ;
        }

        Category category = productFeignClient.getCategory(skuInfo.getCategoryId());
        //获取的数据封装到对象中去
        SkuEs skuEs = new SkuEs();
        if (category != null){
            skuEs.setCategoryId(category.getId());
            skuEs.setCategoryName(category.getName());
        }
        skuEs.setId(skuInfo.getId());
        skuEs.setKeyword(skuInfo.getSkuName()+","+skuEs.getCategoryName());
        skuEs.setWareId(skuInfo.getWareId());
        skuEs.setIsNewPerson(skuInfo.getIsNewPerson());
        skuEs.setImgUrl(skuInfo.getImgUrl());
        skuEs.setTitle(skuInfo.getSkuName());
        if(skuInfo.getSkuType() == SkuType.COMMON.getCode()) {
            skuEs.setSkuType(0);
            skuEs.setPrice(skuInfo.getPrice().doubleValue());
            skuEs.setStock(skuInfo.getStock());
            skuEs.setSale(skuInfo.getSale());
            skuEs.setPerLimit(skuInfo.getPerLimit());
        } else {
            //TODO 待完善-秒杀商品
        }
        skuRepository.save(skuEs);

    }

    @Override
    public void lowerSku(Long skuId) {
        skuRepository.deleteById(skuId);
    }

    @Override
    public List<SkuEs> findHotSkuList() {

        //以find read get 开头
        //关联条件的关键字
        //springdata中的东西
        Pageable pageable = PageRequest.of(0,10);
        Page<SkuEs> page = skuRepository.findByOrderByHotScoreDesc(pageable);
        List<SkuEs> skuEsList = page.getContent();
        return skuEsList;
    }

    @Override
    public Page<SkuEs> search(Pageable pageable, SkuEsQueryVo skuEsQueryVo) {

        //1 设置一个条件 向skuEs中设置id
        skuEsQueryVo.setWareId(AuthContextHolder.getWareId());

        Page<SkuEs> pageModel = null;
        //2 调用sku，根据springdata命名方法查询，
        //                     不为空  用仓库id + 分类id + keyword查询
        String keyword = skuEsQueryVo.getKeyword();
        if (!StringUtils.isEmpty(keyword)){
            // 判断keyword是否为空 为空,根据wareId + 分类id查询
            pageModel = skuRepository.findByCategoryIdAndWareId(skuEsQueryVo.getCategoryId(),
                                                                skuEsQueryVo.getWareId(),
                                                                pageable
                    );
        } else {
            pageModel = skuRepository.findByKeywordAndWareId(skuEsQueryVo.getKeyword(), skuEsQueryVo.getWareId(),pageable);
        }
        //3 查询商品营销活动
        List<SkuEs> skuEsList = pageModel.getContent();
        if(!CollectionUtils.isEmpty(skuEsList)){
            //遍历，得到所有Id
            List<Long> list = skuEsList.stream().map(item -> item.getId()).collect(Collectors.toList());
            //根据skuid列表远程调用，调用service-activity里面的接口
            //返回Map<>
            Map<Long,List<String>> skuIdToRuleListMap = activityFeignClient.findActivitiy(list);//TODO: 远程调用
            //封装数据
            if (skuIdToRuleListMap != null){
                skuEsList.forEach(skuEs -> {
                    skuEs.setRuleList(skuIdToRuleListMap.get(skuEs.getId()));
                });
            }
        }
        //4 返回数据
        return pageModel;
    }

    //更新热度
    @Override
    public void incrHotScore(Long skuId) {
        String key = "hotscore";
        //redis保存数据，每次+1
        Double hotscore = redisTemplate.opsForZSet().incrementScore(key, "skuId" + skuId, 1);
        //规则
        if (hotscore % 10 == 0){
            //更新es
            Optional<SkuEs> optional = skuRepository.findById(skuId);
            SkuEs skuEs = optional.get();
            skuEs.setHotScore(Math.round(hotscore));
            skuRepository.save(skuEs);
        }
    }
}
