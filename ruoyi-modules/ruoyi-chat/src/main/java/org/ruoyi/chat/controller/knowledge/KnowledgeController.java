package org.ruoyi.chat.controller.knowledge;

import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.ruoyi.domain.PdfFileContentResult;
import org.ruoyi.domain.bo.KnowledgeAttachBo;
import org.ruoyi.domain.bo.KnowledgeFragmentBo;
import org.ruoyi.domain.bo.KnowledgeInfoBo;
import org.ruoyi.domain.bo.KnowledgeInfoUploadBo;
import org.ruoyi.domain.vo.KnowledgeAttachVo;
import org.ruoyi.domain.vo.KnowledgeFragmentVo;
import org.ruoyi.domain.vo.KnowledgeInfoVo;
import org.ruoyi.service.IKnowledgeAttachService;
import org.ruoyi.service.IKnowledgeFragmentService;
import org.ruoyi.service.IKnowledgeInfoService;
import org.ruoyi.service.PdfImageExtractService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 知识库管理
 *
 * @author ageerle
 * @date 2025-05-03
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController extends BaseController {

  private final IKnowledgeInfoService knowledgeInfoService;

  private final IKnowledgeAttachService attachService;

  private final IKnowledgeFragmentService fragmentService;

  private final PdfImageExtractService pdfImageExtractService;

  /**
   * 根据用户信息查询本地知识库
   */
  @GetMapping("/list")
  public TableDataInfo<KnowledgeInfoVo> list(KnowledgeInfoBo bo, PageQuery pageQuery) {
    if (!StpUtil.isLogin()) {
      throw new SecurityException("请先去登录!");
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
  public R<String> remove(@PathVariable String id) {
    knowledgeInfoService.removeKnowledge(id);
    return R.ok("删除知识库成功!");
  }

  /**
   * 修改知识库
   */
  @Log(title = "知识库", businessType = BusinessType.UPDATE)
  @PostMapping("/edit")
  public R<Void> edit(@RequestBody KnowledgeInfoBo bo) {
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
  public TableDataInfo<KnowledgeAttachVo> attach(KnowledgeAttachBo bo, PageQuery pageQuery,
      @PathVariable String kid) {
    bo.setKid(kid);
    return attachService.queryPageList(bo, pageQuery);
  }

  /**
   * 上传知识库附件
   */
  @PostMapping(value = "/attach/upload")
  public R<String> upload(KnowledgeInfoUploadBo bo) throws Exception {
    knowledgeInfoService.upload(bo);
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
   */
  @PostMapping("attach/remove/{kid}")
  public R<Void> removeAttach(@NotEmpty(message = "主键不能为空")
  @PathVariable String kid) {
    attachService.removeKnowledgeAttach(kid);
    return R.ok();
  }


  /**
   * 查询知识片段
   */
  @GetMapping("/fragment/list/{docId}")
  public TableDataInfo<KnowledgeFragmentVo> fragmentList(KnowledgeFragmentBo bo,
      PageQuery pageQuery, @PathVariable String docId) {
    bo.setDocId(docId);
    return fragmentService.queryPageList(bo, pageQuery);
  }

  /**
   * 上传文件翻译
   */
  @PostMapping("/translationByFile")
  @ResponseBody
  public String translationByFile(@RequestParam("file") MultipartFile file, String targetLanguage) {
    return attachService.translationByFile(file, targetLanguage);
  }

  /**
   * 提取PDF中的图片并调用gpt-4o-mini,识别图片内容并返回
   *
   * @param file PDF文件
   * @return 文件名称和图片内容
   */
  @PostMapping("/extract-images")
  @Operation(summary = "提取PDF中的图片并调用大模型,识别图片内容并返回", description = "提取PDF中的图片并调用gpt-4o-mini,识别图片内容并返回")
  public R<List<PdfFileContentResult>> extractImages(
      @RequestPart("file") MultipartFile file
  ) throws IOException {
    return R.ok(pdfImageExtractService.extractImages(file));
  }
}
