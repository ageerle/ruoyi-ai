package org.ruoyi.system.service;

import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.system.domain.SysNotice;
import org.ruoyi.system.domain.bo.SysNoticeBo;
import org.ruoyi.system.domain.vo.SysNoticeVo;

/**
 * 公告 服务层
 *
 * @author Lion Li
 */
public interface ISysNoticeService {


    TableDataInfo<SysNoticeVo> selectPageNoticeList(SysNoticeBo notice, PageQuery pageQuery);


    /**
     * 查询公告信息
     *
     * @param noticeId 公告ID
     * @return 公告信息
     */
    SysNoticeVo selectNoticeById(Long noticeId);

    /**
     * 查询公告列表
     *
     * @param notice 公告信息
     * @return 公告集合
     */
    SysNotice getNotice(SysNoticeBo notice);

    /**
     * 新增公告
     *
     * @param bo 公告信息
     * @return 结果
     */
    int insertNotice(SysNoticeBo bo);

    /**
     * 修改公告
     *
     * @param bo 公告信息
     * @return 结果
     */
    int updateNotice(SysNoticeBo bo);

    /**
     * 删除公告信息
     *
     * @param noticeId 公告ID
     * @return 结果
     */
    int deleteNoticeById(Long noticeId);

    /**
     * 批量删除公告信息
     *
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    int deleteNoticeByIds(Long[] noticeIds);
}
