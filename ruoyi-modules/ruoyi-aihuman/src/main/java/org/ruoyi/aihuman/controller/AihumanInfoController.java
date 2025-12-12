package org.ruoyi.aihuman.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import org.ruoyi.aihuman.domain.AihumanInfo;
import org.ruoyi.aihuman.domain.vo.AihumanInfoVo;
import org.ruoyi.aihuman.service.IAihumanInfoService;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.core.page.PageQuery;
import org.ruoyi.core.page.TableDataInfo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * AI人类交互信息Controller
 * 免登录接口，方便验证
 *
 * @author QingYunAI
 */
@SaIgnore
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/aihuman/info")
public class AihumanInfoController {

    private final IAihumanInfoService aihumanInfoService;

    /**
     * 获取AI人类交互信息详情
     */
    @GetMapping("/{id}")
    public R<AihumanInfoVo> getInfo(@PathVariable Long id) {
        return R.ok(aihumanInfoService.queryById(id));
    }

    /**
     * 查询AI人类交互信息列表
     */
    @GetMapping("/list")
    public R<TableDataInfo<AihumanInfoVo>> list(AihumanInfo aihumanInfo, PageQuery pageQuery) {
        TableDataInfo<AihumanInfoVo> tableDataInfo = aihumanInfoService.queryPageList(aihumanInfo, pageQuery);
        return R.ok(tableDataInfo);
    }

    /**
     * 新增AI人类交互信息
     */
    @PostMapping
    public R<Integer> add(@Validated @RequestBody AihumanInfo aihumanInfo) {
        return R.ok(aihumanInfoService.insert(aihumanInfo));
    }

    /**
     * 修改AI人类交互信息
     */
    @PutMapping
    public R<Integer> edit(@Validated @RequestBody AihumanInfo aihumanInfo) {
        return R.ok(aihumanInfoService.update(aihumanInfo));
    }

    /**
     * 删除AI人类交互信息
     */
    @DeleteMapping("/{ids}")
    public R<Integer> remove(@PathVariable Long[] ids) {
        return R.ok(aihumanInfoService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

    /**
     * 测试接口
     * 提供一个简单的GET接口用于快速验证控制器是否正常工作
     */
    @GetMapping("/test")
    public R<String> test() {
        return R.ok("AI Human Controller is working!");
    }
}