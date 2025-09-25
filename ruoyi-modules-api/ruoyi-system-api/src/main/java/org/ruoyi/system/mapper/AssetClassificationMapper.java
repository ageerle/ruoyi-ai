package org.ruoyi.system.mapper;

import org.apache.ibatis.annotations.Param;
import org.ruoyi.core.mapper.BaseMapperPlus;
import org.ruoyi.system.domain.AssetClassification;
import org.ruoyi.system.domain.vo.AssetClassificationVo;

/**
 * 高等学校固定资产分类与代码Mapper接口
 *
 * @author cass
 * @date 2025-09-24
 */
public interface AssetClassificationMapper extends BaseMapperPlus<AssetClassification, AssetClassificationVo> {

    /**
     * 根据分类代码查询
     *
     * @param classificationCode 分类代码
     * @return 高等学校固定资产分类与代码
     */
    AssetClassification queryByClassificationCode(@Param("classificationCode") String classificationCode);
}
