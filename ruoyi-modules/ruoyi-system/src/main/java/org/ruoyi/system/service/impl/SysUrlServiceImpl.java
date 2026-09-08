package org.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.system.domain.SysUrl;
import org.ruoyi.system.domain.bo.SysUrlBo;
import org.ruoyi.system.domain.vo.SysUrlShortcutVo;
import org.ruoyi.system.domain.vo.SysUrlVo;
import org.ruoyi.system.mapper.SysUrlMapper;
import org.ruoyi.system.service.ISysUrlService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * URL 管理 服务层实现
 *
 * @author ruoyi
 */
@RequiredArgsConstructor
@Service
public class SysUrlServiceImpl implements ISysUrlService {

    /**
     * 状态：正常
     */
    private static final String STATUS_NORMAL = "0";

    private final SysUrlMapper baseMapper;

    /**
     * 分页查询 URL 列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return URL 分页列表
     */
    @Override
    public TableDataInfo<SysUrlVo> selectPageUrlList(SysUrlBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysUrl> lqw = buildQueryWrapper(bo);
        Page<SysUrlVo> page = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    /**
     * 根据 ID 查询 URL 详情
     *
     * @param urlId 链接ID
     * @return URL 信息
     */
    @Override
    public SysUrlVo selectUrlById(Long urlId) {
        SysUrlVo vo = baseMapper.selectVoById(urlId);
        if (vo == null) {
            throw new ServiceException("链接不存在或已被删除");
        }
        return vo;
    }

    /**
     * 新增 URL
     *
     * @param bo URL 信息
     * @return 结果
     */
    @Override
    public int insertUrl(SysUrlBo bo) {
        validateUrl(bo.getUrl());
        SysUrl url = MapstructUtils.convert(bo, SysUrl.class);
        if (url.getSortOrder() == null) {
            url.setSortOrder(0);
        }
        if (StringUtils.isBlank(url.getStatus())) {
            url.setStatus(STATUS_NORMAL);
        }
        return baseMapper.insert(url);
    }

    /**
     * 修改 URL
     *
     * @param bo URL 信息
     * @return 结果
     */
    @Override
    public int updateUrl(SysUrlBo bo) {
        validateUrl(bo.getUrl());
        if (bo.getUrlId() == null || baseMapper.selectById(bo.getUrlId()) == null) {
            throw new ServiceException("链接不存在或已被删除");
        }
        SysUrl url = MapstructUtils.convert(bo, SysUrl.class);
        return baseMapper.updateById(url);
    }

    /**
     * 批量删除 URL
     *
     * @param urlIds 需要删除的链接ID
     * @return 结果
     */
    @Override
    public int deleteUrlByIds(Long[] urlIds) {
        return baseMapper.deleteByIds(Arrays.asList(urlIds));
    }

    /**
     * 查询当前租户已启用的快捷入口列表（按排序升序）
     *
     * @return 快捷入口列表
     */
    @Override
    public List<SysUrlShortcutVo> selectShortcutList() {
        LambdaQueryWrapper<SysUrl> lqw = Wrappers.<SysUrl>lambdaQuery()
            .eq(SysUrl::getStatus, STATUS_NORMAL)
            .orderByAsc(SysUrl::getSortOrder)
            .orderByAsc(SysUrl::getUrlId);
        return baseMapper.selectVoList(lqw).stream()
            .map(this::toShortcut)
            .toList();
    }

    private LambdaQueryWrapper<SysUrl> buildQueryWrapper(SysUrlBo bo) {
        LambdaQueryWrapper<SysUrl> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getName()), SysUrl::getName, bo.getName());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), SysUrl::getStatus, bo.getStatus());
        lqw.orderByAsc(SysUrl::getSortOrder);
        lqw.orderByAsc(SysUrl::getUrlId);
        return lqw;
    }

    private SysUrlShortcutVo toShortcut(SysUrlVo vo) {
        SysUrlShortcutVo shortcut = new SysUrlShortcutVo();
        shortcut.setUrlId(vo.getUrlId());
        shortcut.setName(vo.getName());
        shortcut.setUrl(vo.getUrl());
        shortcut.setDescription(vo.getDescription());
        shortcut.setSortOrder(vo.getSortOrder());
        return shortcut;
    }

    /**
     * 校验 URL 地址是否合法：仅允许 http/https 协议，且 host 必须存在且有效
     *
     * @param url 待校验的 URL
     */
    private void validateUrl(String url) {
        if (StringUtils.isBlank(url)) {
            throw new ServiceException("链接地址不能为空");
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new ServiceException("链接地址格式不正确");
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new ServiceException("链接地址必须以 http:// 或 https:// 开头");
        }
        String host = uri.getHost();
        if (StringUtils.isBlank(host)) {
            throw new ServiceException("链接地址的主机名无效");
        }
    }
}
