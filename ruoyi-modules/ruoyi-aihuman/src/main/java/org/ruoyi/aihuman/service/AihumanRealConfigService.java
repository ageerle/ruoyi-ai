package org.ruoyi.aihuman.service;

import org.ruoyi.aihuman.domain.bo.AihumanRealConfigBo;
import org.ruoyi.aihuman.domain.vo.AihumanRealConfigVo;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * 真人交互数字人配置Service接口
 *
 * @author ageerle
 * @date Tue Oct 21 11:46:52 GMT+08:00 2025
 */
public interface AihumanRealConfigService {

    /**
     * 查询真人交互数字人配置
     */
    AihumanRealConfigVo queryById(Integer id);

    /**
     * 查询真人交互数字人配置列表
     */
    TableDataInfo<AihumanRealConfigVo> queryPageList(AihumanRealConfigBo bo, PageQuery pageQuery);

    /**
     * 查询真人交互数字人配置列表
     */
    List<AihumanRealConfigVo> queryList(AihumanRealConfigBo bo);

    /**
     * 新增真人交互数字人配置
     */
    Boolean insertByBo(AihumanRealConfigBo bo);

    /**
     * 修改真人交互数字人配置
     */
    Boolean updateByBo(AihumanRealConfigBo bo);

    /**
     * 执行真人交互数字人配置
     */
    Boolean runByBo(AihumanRealConfigBo bo);

    /**
     * 校验并批量删除真人交互数字人配置信息
     */
    Boolean deleteWithValidByIds(Collection<Integer> ids, Boolean isValid);

    // 在AihumanRealConfigService接口中添加
    Boolean stopByBo(AihumanRealConfigBo bo);
}