package org.ruoyi.system.service;

import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.vo.cover.CoverCallbackVo;
import org.ruoyi.system.domain.vo.cover.CoverParamVo;
import org.ruoyi.system.domain.vo.cover.CoverVo;
import org.ruoyi.system.domain.vo.cover.MusicVo;

import java.util.List;

/**
 * 翻唱Service接口
 *
 * @author NSL
 * @since 2024-12-25
 */
public interface ICoverService {

    /**
     * 查找歌曲
     *
     * @param musicName 歌曲名称
     * @return 匹配的歌曲信息集合
     */
    List<MusicVo> searchMusic(String musicName);

    /**
     * 翻唱回调
     *
     * @param coverCallbackVo 回调信息
     */
    void callback(CoverCallbackVo coverCallbackVo);

    /**
     * 翻唱歌曲
     *
     * @param coverParamVo 翻唱信息
     */
    void saveCoverTask(CoverParamVo coverParamVo);

    /**
     * 查询用户的翻唱记录
     *
     * @return 翻唱记录
     */
    TableDataInfo<CoverVo> searchCoverRecord(PageQuery pageQuery);

}
