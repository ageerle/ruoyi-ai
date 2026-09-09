package org.ruoyi.system.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.bo.SysUrlBo;
import org.ruoyi.system.domain.vo.SysUrlShortcutVo;
import org.ruoyi.system.domain.vo.SysUrlVo;

import java.util.List;

/**
 * URL 管理 服务层
 *
 * @author ruoyi
 */
public interface ISysUrlService {

    /**
     * 分页查询 URL 列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return URL 分页列表
     */
    TableDataInfo<SysUrlVo> selectPageUrlList(SysUrlBo bo, PageQuery pageQuery);

    /**
     * 根据 ID 查询 URL 详情
     *
     * @param urlId 链接ID
     * @return URL 信息
     */
    SysUrlVo selectUrlById(Long urlId);

    /**
     * 新增 URL
     *
     * @param bo URL 信息
     * @return 结果
     */
    int insertUrl(SysUrlBo bo);

    /**
     * 修改 URL
     *
     * @param bo URL 信息
     * @return 结果
     */
    int updateUrl(SysUrlBo bo);

    /**
     * 批量删除 URL
     *
     * @param urlIds 需要删除的链接ID
     * @return 结果
     */
    int deleteUrlByIds(Long[] urlIds);

    /**
     * 查询当前租户已启用的快捷入口列表（按排序升序）
     *
     * @return 快捷入口列表
     */
    List<SysUrlShortcutVo> selectShortcutList();

}
