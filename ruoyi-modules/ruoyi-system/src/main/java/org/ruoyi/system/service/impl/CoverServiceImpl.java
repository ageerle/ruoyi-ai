package org.ruoyi.system.service.impl;

import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.apache.commons.lang3.math.NumberUtils;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.wechat.itchat4j.utils.enums.ResultEnum;
import org.ruoyi.system.cofing.OkHttpConfig;
import org.ruoyi.system.domain.Cover;
import org.ruoyi.system.domain.CoverPromptAudio;
import org.ruoyi.system.domain.model.ApiResult;
import org.ruoyi.system.domain.vo.cover.*;
import org.ruoyi.system.mapper.CoverMapper;
import org.ruoyi.system.mapper.CoverPromptAudioMapper;
import org.ruoyi.system.service.IChatCostService;
import org.ruoyi.system.service.ICoverService;
import org.ruoyi.system.util.WeChatScanHttpUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.ruoyi.common.satoken.utils.LoginHelper.getLoginUser;

/**
 * 翻唱Service业务层处理
 *
 * @author NSL
 * @since 2024-12-25
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class CoverServiceImpl implements ICoverService {

    private final CoverMapper coverMapper;
    private final CoverPromptAudioMapper coverPromptAudioMapper;
    private final ConfigService configService;
    private final IChatCostService chatCostService;
    private final WeChatScanHttpUtil weChatScanHttpUtil;
    private final OkHttpConfig okHttpConfig;

    @Override
    public List<MusicVo> searchMusic(String musicName) {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("keyword", musicName);
        String json = JSONUtil.toJsonStr(queryMap);
        Request postRequest = weChatScanHttpUtil.createPostRequest(getKey("search_music_url"), json);
        String body = weChatScanHttpUtil.executeRequest(postRequest);
        ApiResult apiResult = JSONUtil.toBean(body, ApiResult.class);
        Object data = apiResult.getData();
        Map<String, List<MusicVo>> userMap = JSONUtil.toBean(JSONUtil.toJsonStr(data), new TypeReference<>() {}, true);
        return userMap.get("musicList");
    }

    @Override
    public void callback(CoverCallbackVo coverCallbackVo) {
        Cover cover = BeanUtil.copyProperties(coverCallbackVo, Cover.class);
        coverMapper.insertOrUpdate(cover);
    }

    @Override
    public void saveCoverTask(CoverParamVo coverParamVo) {
        Long userId = null;
        try {
            userId = getLoginUser().getUserId();
        } catch (Exception e) {
            throw new NotLoginException(NotLoginException.NOT_TOKEN_MESSAGE, NotLoginException.NOT_TOKEN, NotLoginException.NOT_TOKEN);
        }
        List<String> promptAudioUrl = coverParamVo.getPromptAudioUrl();
        Cover cover = BeanUtil.copyProperties(coverParamVo, Cover.class);
        cover.setUserId(userId);
        coverMapper.insertOrUpdate(cover);
        if (CollUtil.isEmpty(promptAudioUrl)) {
            // 查询用户最近一次翻唱记录
            List<CoverPromptAudioVo> coverPromptAudioVoList = coverPromptAudioMapper.selectLatestVoByUserId(userId);
            if (CollUtil.isEmpty(coverPromptAudioVoList)) {
                throw new BaseException("未查询到翻唱记录，请上传参考音频！");
            }
                List<String> promptAudioUrlList = coverPromptAudioVoList.stream().map(CoverPromptAudioVo::getPromptAudioUrl).collect(Collectors.toList());
            coverParamVo.setPromptAudioUrl(promptAudioUrlList);
        } else {
            // 保存用户翻唱记录
            for (String audioUrl : promptAudioUrl) {
                CoverPromptAudio coverPromptAudio = new CoverPromptAudio();
                coverPromptAudio.setPromptAudioUrl(audioUrl);
                coverPromptAudio.setUserId(userId);
                coverPromptAudio.setCoverId(cover.getId());
                coverPromptAudioMapper.insertOrUpdate(coverPromptAudio);
            }
        }
        coverParamVo.setUserId(String.valueOf(userId));
        coverParamVo.setOrderId(String.valueOf(cover.getId()));
        coverParamVo.setUserGender("");
        log.info("发起翻唱任务,入参{}", JSONUtil.toJsonStr(coverParamVo));
        // 调用翻唱API
        Request postRequest = weChatScanHttpUtil.createPostRequest(getKey("cover_url"), JSONUtil.toJsonStr(coverParamVo));
        String body = weChatScanHttpUtil.executeRequest(postRequest);
        ApiResult apiResult = JSONUtil.toBean(body, ApiResult.class);
        if (ResultEnum.SUCCESS.getCode().equals(String.valueOf(apiResult.getCode()))) {
            // 扣费
            chatCostService.taskDeduct("cover", "token", NumberUtils.toDouble(okHttpConfig.getGenerate(), 2.5));
        }
        log.info("调用翻唱结果,出参{}", body);
    }

    @Override
    public TableDataInfo<CoverVo> searchCoverRecord(PageQuery pageQuery) {
        LambdaQueryWrapper<Cover> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(Cover::getUserId, getLoginUser().getUserId());
        queryWrapper.orderByDesc(Cover::getCreateTime);
        Page<CoverVo> result = coverMapper.selectVoPage(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(result);
    }

    public String getKey(String key) {
        return configService.getConfigValue("cover", key);
    }

}
