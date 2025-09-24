package org.ruoyi.system.listener;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.excel.core.ExcelListener;
import org.ruoyi.common.excel.core.ExcelResult;
import org.ruoyi.system.domain.MinUsagePeriod;
import org.ruoyi.system.domain.bo.MinUsagePeriodBo;
import org.ruoyi.system.domain.vo.MinUsagePeriodImportVo;
import org.ruoyi.system.service.IMinUsagePeriodService;

import java.util.ArrayList;
import java.util.List;

/**
 * 最低使用年限表自定义导入监听器
 *
 * @author cass
 * @date 2025-09-24
 */
@Slf4j
public class MinUsagePeriodImportListener extends AnalysisEventListener<MinUsagePeriodImportVo> implements ExcelListener<MinUsagePeriodImportVo> {

    private final IMinUsagePeriodService minUsagePeriodService;

    private final Boolean isUpdateSupport;

    private final String operName;

    private final List<MinUsagePeriodImportVo> list = new ArrayList<>();

    private int successNum = 0;
    private int failureNum = 0;
    private final StringBuilder successMsg = new StringBuilder();
    private final StringBuilder failureMsg = new StringBuilder();

    public MinUsagePeriodImportListener(Boolean isUpdateSupport) {
        this.minUsagePeriodService = SpringUtils.getBean(IMinUsagePeriodService.class);
        this.isUpdateSupport = isUpdateSupport;
        this.operName = "系统";
    }

    @Override
    public void invoke(MinUsagePeriodImportVo data, AnalysisContext context) {
        list.add(data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("所有数据解析完成！");
        // 这里可以添加批量处理逻辑
        for (MinUsagePeriodImportVo data : list) {
            try {
                // 验证是否存在这个数据
                MinUsagePeriod existData = minUsagePeriodService.queryByGbCode(data.getGbCode());
                if (ObjectUtil.isNull(existData)) {
                    minUsagePeriodService.insertByBo(BeanUtil.toBean(data, MinUsagePeriodBo.class));
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、国标代码 " + data.getGbCode() + " 导入成功");
                } else if (isUpdateSupport) {
                    BeanUtil.copyProperties(data, existData);
                    minUsagePeriodService.updateByBo(BeanUtil.toBean(existData, MinUsagePeriodBo.class));
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、国标代码 " + data.getGbCode() + " 更新成功");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、国标代码 " + data.getGbCode() + " 已存在");
                }
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、国标代码 " + data.getGbCode() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
    }

    @Override
    public ExcelResult<MinUsagePeriodImportVo> getExcelResult() {
        return new ExcelResult<MinUsagePeriodImportVo>() {
            @Override
            public List<MinUsagePeriodImportVo> getList() {
                return list;
            }

            @Override
            public List<String> getErrorList() {
                return new ArrayList<>();
            }

            @Override
            public String getAnalysis() {
                if (failureNum > 0) {
                    failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
                    return failureMsg.toString();
                } else {
                    successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
                    return successMsg.toString();
                }
            }
        };
    }
}
