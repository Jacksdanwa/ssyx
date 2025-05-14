package com.atme.ssyx.product.mapper;

import com.atme.ssyx.model.product.Category;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 商品三级分类 Mapper 接口
 * </p>
 *
 * @author atguigu
 * @since 2025-02-12
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

}
