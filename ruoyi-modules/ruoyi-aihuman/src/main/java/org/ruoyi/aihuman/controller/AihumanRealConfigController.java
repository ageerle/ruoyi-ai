package org.ruoyi.aihuman.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.aihuman.domain.bo.AihumanRealConfigBo;
import org.ruoyi.aihuman.domain.vo.AihumanRealConfigVo;
import org.ruoyi.aihuman.service.AihumanRealConfigService;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 真人交互数字人配置
 *
 * @author ageerle
 * @date Tue Oct 21 11:46:52 GMT+08:00 2025
 */
//临时免登录
@SaIgnore

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/aihuman/aihumanRealConfig")
public class AihumanRealConfigController extends BaseController {

    private final AihumanRealConfigService aihumanRealConfigService;

    /**
     * 查询真人交互数字人配置列表
     */
    @SaCheckPermission("aihuman:aihumanRealConfig:list")
    @GetMapping("/list")
    public TableDataInfo<AihumanRealConfigVo> list(AihumanRealConfigBo bo, PageQuery pageQuery) {
        return aihumanRealConfigService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出真人交互数字人配置列表
     */
    @SaCheckPermission("aihuman:aihumanRealConfig:export")
    @Log(title = "真人交互数字人配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(AihumanRealConfigBo bo, HttpServletResponse response) {
        List<AihumanRealConfigVo> list = aihumanRealConfigService.queryList(bo);
        ExcelUtil.exportExcel(list, "真人交互数字人配置", AihumanRealConfigVo.class, response);
    }

    /**
     * 获取真人交互数字人配置详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("aihuman:aihumanRealConfig:query")
    @GetMapping("/{id}")
    public R<AihumanRealConfigVo> getInfo(@NotNull(message = "主键不能为空")
                                          @PathVariable Integer id) {
        return R.ok(aihumanRealConfigService.queryById(id));
    }

    /**
     * 新增真人交互数字人配置
     */
    @SaCheckPermission("aihuman:aihumanRealConfig:add")
    @Log(title = "真人交互数字人配置", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody AihumanRealConfigBo bo) {
        return toAjax(aihumanRealConfigService.insertByBo(bo));
    }

    /**
     * 修改真人交互数字人配置
     */
    @SaCheckPermission("aihuman:aihumanRealConfig:edit")
    @Log(title = "真人交互数字人配置", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody AihumanRealConfigBo bo) {
        return toAjax(aihumanRealConfigService.updateByBo(bo));
    }

    /**
     * 删除真人交互数字人配置
     *
     * @param ids 主键串
     */
    @SaCheckPermission("aihuman:aihumanRealConfig:remove")
    @Log(title = "真人交互数字人配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Integer[] ids) {
        return toAjax(aihumanRealConfigService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 1.执行以下命令：
     * cd F:\Projects\AI-Human\LiveTalking
     * conda activate D:\zg117\C\Users\zg117\.conda\envs\livetalking_new
     * python app.py --transport webrtc --model wav2lip --avatar_id wav2lip256_avatar1
     * <p>
     * 2.监听 python app.py --transport webrtc --model wav2lip --avatar_id wav2lip256_avatar1 执行情况
     * <p>
     * 3.返回执行结果并打开页面
     * http://127.0.0.1:8010/webrtcapi-diy.html
     */
    @SaCheckPermission("aihuman:aihumanRealConfig:run")
    //@Log(title = "真人交互数字人配置", businessType = BusinessType.UPDATE, operatorType = OperatorType.OTHER)
    @RepeatSubmit()
    @PutMapping("/run")
    public R<String> run(@Validated(EditGroup.class) @RequestBody AihumanRealConfigBo bo) {
        boolean result = aihumanRealConfigService.runByBo(bo);
        if (result) {
            // 返回前端页面URL，前端可以根据这个URL跳转或打开新页面
            // http://127.0.0.1:8010/webrtcapi-diy.html 其中的 http://127.0.0.1 获取当前java服务的IP地址
            // return R.ok("http://127.0.0.1:8010/webrtcapi-diy.html");
            // 运行状态
            bo.setRunStatus("1");
            return R.ok("http://127.0.0.1:8010/webrtcapi-diy.html");
        } else {
            return R.fail("启动真人交互数字人失败");
        }
    }

    /**
     * 停止真人交互数字人配置任务
     */
    @SaCheckPermission("aihuman:aihumanRealConfig:stop")
    //@Log(title = "真人交互数字人配置", businessType = BusinessType.UPDATE, operatorType = OperatorType.OTHER)
    @RepeatSubmit()
    @PutMapping("/stop")
    public R<String> stop(@Validated(EditGroup.class) @RequestBody AihumanRealConfigBo bo) {
        boolean result = aihumanRealConfigService.stopByBo(bo);
        if (result) {
            // 运行状态
            bo.setRunStatus("0");
            return R.ok("真人交互数字人任务已停止");
        } else {
            return R.fail("停止真人交互数字人任务失败或没有正在运行的任务");
        }
    }
}