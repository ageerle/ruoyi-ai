package org.ruoyi.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.system.domain.SysNotice;
import org.ruoyi.system.domain.SysNoticeState;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.domain.bo.SysNoticeBo;
import org.ruoyi.system.domain.vo.SysNoticeVo;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.mapper.SysNoticeMapper;
import org.ruoyi.system.mapper.SysNoticeStateMapper;
import org.ruoyi.system.mapper.SysUserMapper;
import org.ruoyi.system.service.ISysNoticeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    private final SysNoticeStateMapper noticeStateMapper;
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
    public SysNotice getNotice(SysNoticeBo notice) {
        LambdaQueryWrapper<SysNoticeState> lqwState = Wrappers.lambdaQuery();
        Long userId = LoginHelper.getLoginUser().getUserId();

        lqwState.eq(userId != null, SysNoticeState::getUserId, userId);
        // 查询未读通知
        lqwState.eq(SysNoticeState::getReadStatus, "0");
        lqwState.orderByDesc(SysNoticeState::getCreateTime); // 按创建时间降序排序
        List<SysNoticeState> states = noticeStateMapper.selectList(lqwState);  // 查询公告阅读状态
        SysNoticeState sysNoticeState = states.isEmpty() ? null : states.get(0); // 取第一条记录
        if (sysNoticeState != null) {
            return baseMapper.selectById(sysNoticeState.getNoticeId());
        }else {
            return null;
        }
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
        // 插入公告
        int insert = baseMapper.insert(notice);
        // 公告类型（1通知 2公告）
        if("1".equals(bo.getNoticeType())){
            // 将之前通知全部设为已读
            noticeStateMapper.readAllNotice();
            // 插入通知阅读状态
            List<SysUser> sysUserList = userMapper.selectList();
            List<SysNoticeState> noticeStateList = new ArrayList<>();
            for (SysUser sysUser : sysUserList) {
                SysNoticeState sysNoticeState = new SysNoticeState();
                if (notice != null) {
                    sysNoticeState.setNoticeId(notice.getNoticeId());
                    sysNoticeState.setUserId(sysUser.getUserId());
                    noticeStateList.add(sysNoticeState);
                }
            }
            noticeStateMapper.insertBatch(noticeStateList);
        }
        return insert;
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
