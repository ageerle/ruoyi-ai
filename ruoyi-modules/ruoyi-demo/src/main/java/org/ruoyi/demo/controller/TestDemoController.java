package org.ruoyi.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.ValidatorUtils;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.core.validate.QueryGroup;
import org.ruoyi.common.excel.core.ExcelResult;
import org.ruoyi.common.excel.utils.ExcelUtil;
import org.ruoyi.common.idempotent.annotation.RepeatSubmit;
import org.ruoyi.common.log.annotation.Log;
import org.ruoyi.common.log.enums.BusinessType;
import org.ruoyi.common.mybatis.core.page.PageQuery;
import org.ruoyi.common.mybatis.core.page.TableDataInfo;
import org.ruoyi.common.web.core.BaseController;
import org.ruoyi.demo.domain.TestDemo;
import org.ruoyi.demo.domain.bo.TestDemoBo;
import org.ruoyi.demo.domain.bo.TestDemoImportVo;
import org.ruoyi.demo.domain.vo.TestDemoVo;
import org.ruoyi.demo.service.ITestDemoService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 测试单表Controller
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/demo")
public class TestDemoController extends BaseController {

    private final ITestDemoService testDemoService;

    /**
     * 查询测试单表列表
     */
    @SaCheckPermission("demo:demo:list")
    @GetMapping("/list")
    public TableDataInfo<TestDemoVo> list(@Validated(QueryGroup.class) TestDemoBo bo, PageQuery pageQuery) {
        return testDemoService.queryPageList(bo, pageQuery);
    }

    /**
     * 自定义分页查询
     */
    @SaCheckPermission("demo:demo:list")
    @GetMapping("/page")
    public TableDataInfo<TestDemoVo> page(@Validated(QueryGroup.class) TestDemoBo bo, PageQuery pageQuery) {
        return testDemoService.customPageList(bo, pageQuery);
    }

    /**
     * 导入数据
     *
     * @param file 导入文件
     */
    @Log(title = "测试单表", businessType = BusinessType.IMPORT)
    @SaCheckPermission("demo:demo:import")
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file) throws Exception {
        ExcelResult<TestDemoImportVo> excelResult = ExcelUtil.importExcel(file.getInputStream(), TestDemoImportVo.class, true);
        List<TestDemo> list = MapstructUtils.convert(excelResult.getList(), TestDemo.class);
        testDemoService.saveBatch(list);
        return R.ok(excelResult.getAnalysis());
    }

    /**
     * 导出测试单表列表
     */
    @SaCheckPermission("demo:demo:export")
    @Log(title = "测试单表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@Validated TestDemoBo bo, HttpServletResponse response) {
        List<TestDemoVo> list = testDemoService.queryList(bo);
        // 测试雪花id导出
//        for (TestDemoVo vo : list) {
//            vo.setId(1234567891234567893L);
//        }
        ExcelUtil.exportExcel(list, "测试单表", TestDemoVo.class, response);
    }

    /**
     * 获取测试单表详细信息
     *
     * @param id 测试ID
     */
    @SaCheckPermission("demo:demo:query")
    @GetMapping("/{id}")
    public R<TestDemoVo> getInfo(@NotNull(message = "主键不能为空")
                                 @PathVariable("id") Long id) {
        return R.ok(testDemoService.queryById(id));
    }

    /**
     * 新增测试单表
     */
    @SaCheckPermission("demo:demo:add")
    @Log(title = "测试单表", businessType = BusinessType.INSERT)
    @RepeatSubmit(interval = 2, timeUnit = TimeUnit.SECONDS, message = "{repeat.submit.message}")
    @PostMapping()
    public R<Void> add(@RequestBody TestDemoBo bo) {
        // 使用校验工具对标 @Validated(AddGroup.class) 注解
        // 用于在非 Controller 的地方校验对象
        ValidatorUtils.validate(bo, AddGroup.class);
        return toAjax(testDemoService.insertByBo(bo));
    }

    /**
     * 修改测试单表
     */
    @SaCheckPermission("demo:demo:edit")
    @Log(title = "测试单表", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody TestDemoBo bo) {
        return toAjax(testDemoService.updateByBo(bo));
    }

    /**
     * 删除测试单表
     *
     * @param ids 测试ID串
     */
    @SaCheckPermission("demo:demo:remove")
    @Log(title = "测试单表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(testDemoService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
