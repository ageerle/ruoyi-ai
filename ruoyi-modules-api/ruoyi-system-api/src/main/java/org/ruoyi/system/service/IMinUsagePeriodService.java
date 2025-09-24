package org.ruoyi.system.service;

import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.MinUsagePeriod;
import org.ruoyi.system.domain.bo.MinUsagePeriodBo;
import org.ruoyi.system.domain.vo.MinUsagePeriodVo;
import org.ruoyi.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 最低使用年限表Service接口
 *
 * @author cass
 * @date 2025-09-24
 */
public interface IMinUsagePeriodService {

    /**
     * 查询最低使用年限表
     */
    MinUsagePeriodVo queryById(Long id);

    /**
     * 根据国标代码查询最低使用年限表
     */
    MinUsagePeriod queryByGbCode(String gbCode);

    /**
     * 查询最低使用年限表列表
     */
    TableDataInfo<MinUsagePeriodVo> queryPageList(MinUsagePeriodBo bo, PageQuery pageQuery);

    /**
     * 查询最低使用年限表列表
     */
    List<MinUsagePeriodVo> queryList(MinUsagePeriodBo bo);

    /**
     * 新增最低使用年限表
     */
    Boolean insertByBo(MinUsagePeriodBo bo);

    /**
     * 修改最低使用年限表
     */
    Boolean updateByBo(MinUsagePeriodBo bo);

    /**
     * 校验并批量删除最低使用年限表信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 批量导入最低使用年限表数据
     */
    String importData(List<MinUsagePeriodVo> dataList, Boolean isUpdateSupport, String operName);

}
