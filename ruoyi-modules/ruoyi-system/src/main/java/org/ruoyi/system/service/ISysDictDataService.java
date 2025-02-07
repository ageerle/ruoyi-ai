package org.ruoyi.system.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.SysDictDataBo;
import org.ruoyi.system.domain.vo.SysDictDataVo;

import java.util.List;

/**
 * 字典 业务层
 *
 * @author Lion Li
 */
public interface ISysDictDataService {


    TableDataInfo<SysDictDataVo> selectPageDictDataList(SysDictDataBo dictData, PageQuery pageQuery);

    /**
     * 根据条件分页查询字典数据
     *
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    List<SysDictDataVo> selectDictDataList(SysDictDataBo dictData);

    /**
     * 根据字典类型和字典键值查询字典数据信息
     *
     * @param dictType  字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    String selectDictLabel(String dictType, String dictValue);

    /**
     * 根据字典数据ID查询信息
     *
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    SysDictDataVo selectDictDataById(Long dictCode);

    /**
     * 批量删除字典数据信息
     *
     * @param dictCodes 需要删除的字典数据ID
     */
    void deleteDictDataByIds(Long[] dictCodes);

    /**
     * 新增保存字典数据信息
     *
     * @param bo 字典数据信息
     * @return 结果
     */
    List<SysDictDataVo> insertDictData(SysDictDataBo bo);

    /**
     * 修改保存字典数据信息
     *
     * @param bo 字典数据信息
     * @return 结果
     */
    List<SysDictDataVo> updateDictData(SysDictDataBo bo);
}
