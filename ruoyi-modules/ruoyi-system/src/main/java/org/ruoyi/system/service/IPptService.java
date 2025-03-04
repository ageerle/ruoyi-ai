package org.ruoyi.system.service;

import com.alibaba.fastjson.JSONObject;
import org.ruoyi.system.domain.vo.ppt.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI_PPT Service接口
 *
 * @author NSL
 * @since 2024-12-30
 */
public interface IPptService {
    /**
     * 同步流式生成 PPT
     *
     * @param title ppt 标题
     */
    void syncStreamGeneratePpt(String title);

    /**
     * 查询 PPT 列表
     *
     * @param pptQueryVo 查询参数
     */
    void selectPptList(PptAllQueryDto pptQueryVo);

    /**
     * 分页查询 PPT 模板
     * @param pptQueryVo 查询参数
     */
    JSONObject getPptTemplates(PptTemplateQueryDto pptQueryVo);

    /**
     * 生成大纲
     * @param generateOutlineDto 生成大纲参数
     */
    SseEmitter generateOutline(PptGenerateOutlineDto generateOutlineDto);

    /**
     * 生成大纲内容
     * @param generateContentDto 生成大纲内容参数
     */
    SseEmitter generateContent(PptGenerateContentDto generateContentDto);

    /**
     * 生成ppt
     * @param generatePptxDto 生成PPT参数
     */
    JSONObject generatePptx(PptGeneratePptxDto generatePptxDto);

    /**
     * 获取API Token
     * @return 文多多Token
     */
    String getApiToken();

    /**
     * 生成PPT成功回调接口
     */
    void successCallback();

}
