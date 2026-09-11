package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ReceiptLedger;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ReceiptLedgerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 销售回款台账服务（P3-4.1 AC-INC-16b/16c/16d/31/31b/32）
 *
 * <p>核心规则：
 * <ul>
 *   <li>AC-INC-16b：达成率口径 = 回款（RECEIPT），不是出库/开票</li>
 *   <li>AC-INC-16c：月度录入金额 + 上传凭证附件</li>
 *   <li>AC-INC-16d：窗口外数据不计入达成率</li>
 *   <li>AC-INC-31：窗口外退款不做回溯扣减</li>
 *   <li>AC-INC-31b：窗口内退款当期冲减</li>
 *   <li>AC-INC-32：6自然月窗口以上市日期为起算点</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ReceiptLedgerService {

    private final ReceiptLedgerMapper receiptLedgerMapper;
    private final ProjectMapper projectMapper;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String MONTH_PATTERN = "\\d{4}-(0[1-9]|1[0-2])";

    /**
     * 录入回款（AC-INC-16c）
     * 校验：source 必须为 RECEIPT；月份在窗口内才计入达成率
     */
    @Transactional(rollbackFor = Exception.class)
    public ReceiptLedger recordReceipt(ReceiptLedger ledger) {
        if (ledger.getReceiptMonth() == null || !ledger.getReceiptMonth().matches(MONTH_PATTERN)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "回款月份必须为 YYYY-MM 格式（1-12 月）");
        }
        if (!"RECEIPT".equals(ledger.getSource())) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "AC-INC-16b：达成率口径必须是 RECEIPT（回款），不是 " + ledger.getSource());
        }
        if (ledger.getReceiptAmount() == null || ledger.getReceiptAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "回款金额必须为正数");
        }
        if (ledger.getRefundAmount() == null) {
            ledger.setRefundAmount(BigDecimal.ZERO);
        }
        // AC-INC-32：项目存在性校验 + 上市日期为起算点
        Project project = projectMapper.selectById(ledger.getProjectId());
        if (project == null || "1".equals(project.getDelFlag())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "项目不存在: " + ledger.getProjectId());
        }
        if (ledger.getWindowStart() == null && project.getLaunchDate() != null) {
            ledger.setWindowStart(project.getLaunchDate());
        }
        // 2026-09-08 P-BACKLOG-1：月份重复业务校验——真库唯一键 uk_receipt_project_month
        // 防线前移，真活复跑曾暴露 Duplicate entry SQL 异常逃逸为 90001 ISE，
        // 现收敛为 STATE_CONFLICT 业务拒绝，不再泄漏系统内部错误码
        ReceiptLedger duplicated = receiptLedgerMapper.selectOne(
            new LambdaQueryWrapper<ReceiptLedger>()
                .eq(ReceiptLedger::getProjectId, ledger.getProjectId())
                .eq(ReceiptLedger::getReceiptMonth, ledger.getReceiptMonth())
        );
        if (duplicated != null) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "该月份已有回款记录，不可重复录入: " + ledger.getReceiptMonth());
        }
        // 计算窗口
        computeWindow(ledger);
        ledger.setCreateTime(new Date());
        receiptLedgerMapper.insert(ledger);
        return ledger;
    }

    /**
     * 退款冲减（AC-INC-31/31b）
     * 窗口内退款 → 当期冲减；窗口外退款 → 不回溯扣减（拒绝）
     */
    @Transactional(rollbackFor = Exception.class)
    public ReceiptLedger recordRefund(Long projectId, String month, BigDecimal refundAmount) {
        ReceiptLedger existing = receiptLedgerMapper.selectOne(
            new LambdaQueryWrapper<ReceiptLedger>()
                .eq(ReceiptLedger::getProjectId, projectId)
                .eq(ReceiptLedger::getReceiptMonth, month)
        );
        if (existing == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "该月份无回款记录，不可冲减: " + month);
        }
        // AC-INC-31：窗口外退款不做回溯扣减
        if (!isInWindow(existing)) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT, "AC-INC-31：窗口外退款不做回溯扣减，月份=" + month);
        }
        // AC-INC-31b：窗口内退款当期冲减
        existing.setRefundAmount(existing.getRefundAmount().add(refundAmount));
        receiptLedgerMapper.updateById(existing);
        return existing;
    }

    /**
     * 计算达成率（AC-INC-16b/16d）
     * 达成率 = SUM(窗口内净回款) / 目标销售额
     */
    public BigDecimal calculateAchievementRate(Long projectId, BigDecimal targetSales) {
        List<ReceiptLedger> all = receiptLedgerMapper.selectList(
            new LambdaQueryWrapper<ReceiptLedger>()
                .eq(ReceiptLedger::getProjectId, projectId)
                .eq(ReceiptLedger::getSource, "RECEIPT")
        );
        BigDecimal totalInWindow = BigDecimal.ZERO;
        for (ReceiptLedger r : all) {
            if (isInWindow(r)) {
                BigDecimal net = r.getReceiptAmount().subtract(
                    r.getRefundAmount() != null ? r.getRefundAmount() : BigDecimal.ZERO);
                totalInWindow = totalInWindow.add(net);
            }
            // AC-INC-16d：窗口外数据不计入
        }
        if (targetSales == null || targetSales.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalInWindow.divide(targetSales, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    /**
     * 查询项目回款台账列表
     */
    public List<ReceiptLedger> listByProject(Long projectId) {
        return receiptLedgerMapper.selectList(
            new LambdaQueryWrapper<ReceiptLedger>()
                .eq(ReceiptLedger::getProjectId, projectId)
                .orderByDesc(ReceiptLedger::getReceiptMonth)
        );
    }

    /**
     * AC-INC-32：6自然月窗口以上市日期为起算点
     */
    private void computeWindow(ReceiptLedger ledger) {
        if (ledger.getWindowStart() != null) {
            LocalDate start = ledger.getWindowStart().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate end = start.plusMonths(6).minusDays(1);
            ledger.setWindowEnd(Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
    }

    /**
     * 判断回款月份是否在6自然月窗口内
     */
    private boolean isInWindow(ReceiptLedger ledger) {
        if (ledger.getWindowStart() == null || ledger.getWindowEnd() == null) {
            return true; // 无窗口约束时默认计入
        }
        if (ledger.getReceiptMonth() == null) {
            return false;
        }
        LocalDate monthStart = LocalDate.parse(ledger.getReceiptMonth() + "-01");
        LocalDate wStart = ledger.getWindowStart().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate wEnd = ledger.getWindowEnd().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return !monthStart.isBefore(wStart) && !monthStart.isAfter(wEnd);
    }
}
