package org.ruoyi.system.service;

import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.AssetClassification;
import org.ruoyi.system.domain.bo.AssetClassificationBo;
import org.ruoyi.system.domain.vo.AssetClassificationVo;

import java.util.Collection;
import java.util.List;

/**
 * 高等学校固定资产分类与代码Service接口
 *
 * @author cass
 * @date 2025-09-24
 */
public interface IAssetClassificationService {

    /**
     * 查询高等学校固定资产分类与代码
     */
    AssetClassificationVo queryById(Long id);

    /**
     * 根据分类代码查询
     */
    AssetClassification queryByClassificationCode(String classificationCode);

    /**
     * 查询高等学校固定资产分类与代码列表
     */
    TableDataInfo<AssetClassificationVo> queryPageList(AssetClassificationBo bo);

    /**
     * 查询高等学校固定资产分类与代码列表
     */
    List<AssetClassificationVo> queryList(AssetClassificationBo bo);

    /**
     * 新增高等学校固定资产分类与代码
     */
    Boolean insertByBo(AssetClassificationBo bo);

    /**
     * 修改高等学校固定资产分类与代码
     */
    Boolean updateByBo(AssetClassificationBo bo);

    /**
     * 校验并批量删除高等学校固定资产分类与代码信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 导入数据
     *
     * @param dataList 数据列表
     * @param isUpdateSupport 是否更新支持
     * @param operName 操作用户
     * @return 结果
     */
    String importData(List<AssetClassificationBo> dataList, Boolean isUpdateSupport, String operName);
}
