package org.ruoyi.aihuman.service;

import org.ruoyi.aihuman.domain.bo.AihumanConfigBo;
import org.ruoyi.aihuman.domain.vo.AihumanConfigVo;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

/**
 * 交互数字人配置Service接口
 *
 * @author ageerle
 * @date Fri Sep 26 22:27:00 GMT+08:00 2025
 */
public interface AihumanConfigService {

    /**
     * 查询交互数字人配置
     */
    AihumanConfigVo queryById(Integer id);

    /**
     * 查询交互数字人配置列表
     */
    TableDataInfo<AihumanConfigVo> queryPageList(AihumanConfigBo bo, PageQuery pageQuery);

    /**
     * 查询交互数字人配置列表
     */
    List<AihumanConfigVo> queryList(AihumanConfigBo bo);

    /**
     * 新增交互数字人配置
     */
    Boolean insertByBo(AihumanConfigBo bo);

    /**
     * 修改交互数字人配置
     */
    Boolean updateByBo(AihumanConfigBo bo);

    /**
     * 校验并批量删除交互数字人配置信息
     */
    Boolean deleteWithValidByIds(Collection<Integer> ids, Boolean isValid);
}
