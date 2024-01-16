package com.xmzs.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xmzs.common.core.utils.MapstructUtils;
import com.xmzs.common.core.utils.StringUtils;
import com.xmzs.common.mybatis.core.page.PageQuery;
import com.xmzs.common.mybatis.core.page.TableDataInfo;
import com.xmzs.system.domain.SysNotice;
import com.xmzs.system.domain.bo.SysNoticeBo;
import com.xmzs.system.domain.vo.SysNoticeVo;
import com.xmzs.system.domain.vo.SysUserVo;
import com.xmzs.system.mapper.SysNoticeMapper;
import com.xmzs.system.mapper.SysUserMapper;
import com.xmzs.system.service.ISysNoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 公告 服务层实现
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysNoticeServiceImpl implements ISysNoticeService {

    private final SysNoticeMapper baseMapper;
    private final SysUserMapper userMapper;

    @Override
    public TableDataInfo<SysNoticeVo> selectPageNoticeList(SysNoticeBo notice, PageQuery pageQuery) {
        LambdaQueryWrapper<SysNotice> lqw = buildQueryWrapper(notice);
        Page<SysNoticeVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * 查询公告信息
     *
     * @param noticeId 公告ID
     * @return 公告信息
     */
    @Override
    public SysNoticeVo selectNoticeById(Long noticeId) {
        return baseMapper.selectVoById(noticeId);
    }

    /**
     * 查询公告列表
     *
     * @param notice 公告信息
     * @return 公告集合
     */
    @Override
    public List<SysNoticeVo> selectNoticeList(SysNoticeBo notice) {
        LambdaQueryWrapper<SysNotice> lqw = buildQueryWrapper(notice);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<SysNotice> buildQueryWrapper(SysNoticeBo bo) {
        LambdaQueryWrapper<SysNotice> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getNoticeTitle()), SysNotice::getNoticeTitle, bo.getNoticeTitle());
        lqw.eq(StringUtils.isNotBlank(bo.getNoticeType()), SysNotice::getNoticeType, bo.getNoticeType());
        if (StringUtils.isNotBlank(bo.getCreateByName())) {
            SysUserVo sysUser = userMapper.selectUserByUserName(bo.getCreateByName());
            lqw.eq(SysNotice::getCreateBy, ObjectUtil.isNotNull(sysUser) ? sysUser.getUserId() : null);
        }
        return lqw;
    }

    /**
     * 新增公告
     *
     * @param bo 公告信息
     * @return 结果
     */
    @Override
    public int insertNotice(SysNoticeBo bo) {
        SysNotice notice = MapstructUtils.convert(bo, SysNotice.class);
        return baseMapper.insert(notice);
    }

    /**
     * 修改公告
     *
     * @param bo 公告信息
     * @return 结果
     */
    @Override
    public int updateNotice(SysNoticeBo bo) {
        SysNotice notice = MapstructUtils.convert(bo, SysNotice.class);
        return baseMapper.updateById(notice);
    }

    /**
     * 删除公告对象
     *
     * @param noticeId 公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeById(Long noticeId) {
        return baseMapper.deleteById(noticeId);
    }

    /**
     * 批量删除公告信息
     *
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeByIds(Long[] noticeIds) {
        return baseMapper.deleteBatchIds(Arrays.asList(noticeIds));
    }
}
