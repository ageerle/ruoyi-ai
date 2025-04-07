package org.ruoyi.controller;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.chat.domain.request.ChatRequest;
import org.ruoyi.common.chat.entity.chat.Message;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.knowledge.chain.vectorstore.VectorStore;
import org.ruoyi.knowledge.domain.bo.KnowledgeAttachBo;
import org.ruoyi.knowledge.domain.bo.KnowledgeFragmentBo;
import org.ruoyi.knowledge.domain.bo.KnowledgeInfoBo;
import org.ruoyi.knowledge.domain.req.KnowledgeInfoUploadRequest;
import org.ruoyi.knowledge.domain.vo.KnowledgeAttachVo;
import org.ruoyi.knowledge.domain.vo.KnowledgeFragmentVo;
import org.ruoyi.knowledge.domain.vo.KnowledgeInfoVo;
import org.ruoyi.knowledge.service.EmbeddingService;
import org.ruoyi.knowledge.service.IKnowledgeAttachService;
import org.ruoyi.knowledge.service.IKnowledgeFragmentService;
import org.ruoyi.knowledge.service.IKnowledgeInfoService;
import org.ruoyi.system.service.ISseService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;


/**
 * 知识库
 *
 * @author Lion Li
 * @date 2024-10-21
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController extends BaseController {

    private final IKnowledgeInfoService knowledgeInfoService;

    private final VectorStore vectorStore;

    private final IKnowledgeAttachService attachService;

    private final IKnowledgeFragmentService fragmentService;

    private final EmbeddingService embeddingService;

    private final ISseService sseService;

    /**
     * 知识库对话
     */
    @PostMapping("/send")
    public SseEmitter send(@RequestBody @Valid ChatRequest chatRequest, HttpServletRequest request) {
        List<Message> messages = chatRequest.getMessages();
        // 获取知识库信息
        Message message = messages.get(messages.size() - 1);
        StringBuilder sb = new StringBuilder(message.getContent().toString());
        List<String> nearestList;
        List<Double> queryVector = embeddingService.getQueryVector(message.getContent().toString(), chatRequest.getKid());
        nearestList = vectorStore.nearest(queryVector, chatRequest.getKid());
        for (String prompt : nearestList) {
            sb.append("\n####").append(prompt);
        }
        sb.append( (nearestList.size() > 0 ? "\n\n注意：回答问题时，须严格根据我给你的系统上下文内容原文进行回答，请不要自己发挥,回答时保持原来文本的段落层级" : ""));
        message.setRole(Message.Role.USER.getName());
        message.setContent(sb.toString());
        return sseService.sseChat(chatRequest, request);
    }

    /**
     * 根据用户信息查询本地知识库
     */
    @GetMapping("/list")
    public TableDataInfo<KnowledgeInfoVo> list(KnowledgeInfoBo bo, PageQuery pageQuery) {
        if(!StpUtil.isLogin()){
            return null;
        }
        bo.setUid(LoginHelper.getUserId());
        return knowledgeInfoService.queryPageList(bo, pageQuery);
    }

    /**
     * 新增知识库
     */
    @Log(title = "知识库", businessType = BusinessType.INSERT)
    @PostMapping("/save")
    public R<Void> save(@Validated(AddGroup.class) @RequestBody KnowledgeInfoBo bo) {
        knowledgeInfoService.saveOne(bo);
        return R.ok();
    }

    /**
     * 删除知识库
     */
    @PostMapping("/remove/{id}")
    public R<String> remove(@PathVariable String id){
        knowledgeInfoService.removeKnowledge(id);
        return R.ok("删除知识库成功!");
    }

    /**
     * 修改知识库
     */
    @Log(title = "知识库", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public R<Void> edit( @RequestBody KnowledgeInfoBo bo) {
        return toAjax(knowledgeInfoService.updateByBo(bo));
    }

    /**
     * 导出知识库列表
     */
    @Log(title = "知识库", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(KnowledgeInfoBo bo, HttpServletResponse response) {
        List<KnowledgeInfoVo> list = knowledgeInfoService.queryList(bo);
        ExcelUtil.exportExcel(list, "知识库", KnowledgeInfoVo.class, response);
    }

    /**
     * 查询知识附件信息
     */
    @GetMapping("/detail/{kid}")
    public TableDataInfo<KnowledgeAttachVo> attach(KnowledgeAttachBo bo, PageQuery pageQuery,@PathVariable String kid){
        bo.setKid(kid);
        return attachService.queryPageList(bo, pageQuery);
    }

    /**
     * 上传知识库附件
     */
    @PostMapping(value = "/attach/upload")
    public R<String> upload(KnowledgeInfoUploadRequest request){
        knowledgeInfoService.upload(request);
        return R.ok("上传知识库附件成功!");
    }

    /**
     * 获取知识库附件详细信息
     *
     * @param id 主键
     */
    @GetMapping("attach/info/{id}")
    public R<KnowledgeAttachVo> getAttachInfo(@NotNull(message = "主键不能为空")
                                              @PathVariable Long id) {
        return R.ok(attachService.queryById(id));
    }

    /**
     * 删除知识库附件
     *
     */
    @PostMapping("attach/remove/{docId}")
    public R<Void> removeAttach(@NotEmpty(message = "主键不能为空") @PathVariable String docId) {
        attachService.removeKnowledgeAttach(docId);
        return R.ok();
    }


    /**
     * 查询知识片段
     */
    @GetMapping("/fragment/list/{docId}")
    public TableDataInfo<KnowledgeFragmentVo> fragmentList(KnowledgeFragmentBo bo, PageQuery pageQuery, @PathVariable String docId) {
        bo.setDocId(docId);
        return fragmentService.queryPageList(bo, pageQuery);
    }

}
