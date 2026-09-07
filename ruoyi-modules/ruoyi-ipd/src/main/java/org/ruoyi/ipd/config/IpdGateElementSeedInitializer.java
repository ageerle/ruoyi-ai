package org.ruoyi.ipd.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.mapper.GateElementMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gate 评审要素种子（33 项 / 14 否决位，BR-IPD-06 配套；仅 dev profile）。
 *
 * <p>根因修复：此前 gate_review_elements 表初始为空，前端 gate-panel.vue 在 API 返回 0 项时
 * 被迫用 FALLBACK_GATE_ELEMENTS 硬编码兜底（33 项静态常量），导致前后端要素定义双轨漂移。
 * 本初始化器在 dev 环境启动时幂等注入权威 33 项要素，前端兜底仅作为 API 异常时的降级展示。
 *
 * <p>要素分布：G1 概念决策(7项/3否决) + G2 计划决策(6项/2否决) + G3 开发阶段(5项/2否决)
 * + G4 验证确认(8项/4否决) + G5 上市发布(7项/3否决) = 33 项，14 否决位。
 *
 * <p>幂等：按 gateCode+elementCode 判存在即跳过；已存在行不覆盖（超管可在管理端增删改）。
 * 与前端 FALLBACK_GATE_ELEMENTS 内容对齐（2026-09-07 磁盘核实），如权威规格 v1 更新需同步修改此处与前端兜底。
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class IpdGateElementSeedInitializer implements ApplicationRunner {

    private final GateElementMapper gateElementMapper;

    /** 要素定义：{gateCode, elementCode, elementName, passStandard, isVeto(Y/N), sortOrder} */
    private static final List<String[]> SEED_ELEMENTS = List.of(
        // G1 概念决策评审（7 项，3 否决）
        new String[]{"G1", "G1-01", "市场机会与用户痛点验证", "细分市场 TAM ≥ 项目立项最低门槛，有定量访谈或调研佐证", "N", "1"},
        new String[]{"G1", "G1-02", "商业模式可行性", "LTV/CAC ≥ 3，回收周期 ≤ 18 个月", "Y", "2"},
        new String[]{"G1", "G1-03", "技术可行性评估", "关键技术风险点有明确攻关路径与责任人", "Y", "3"},
        new String[]{"G1", "G1-04", "竞品与替代方案分析", "形成竞品矩阵，识别至少 2 个差异化卖点", "N", "4"},
        new String[]{"G1", "G1-05", "初步财务评估", "NPV ≥ 0，关键假设有书面依据", "Y", "5"},
        new String[]{"G1", "G1-06", "法规与合规预审", "形成合规清单，无 P0 不可接受项", "N", "6"},
        new String[]{"G1", "G1-07", "立项建议书（Charter）", "Charter 通过 PM 双签会签", "N", "7"},
        // G2 计划决策评审（6 项，2 否决）
        new String[]{"G2", "G2-01", "项目计划书与里程碑", "里程碑可量化，阶段 Gate 与交付物一一对应", "N", "1"},
        new String[]{"G2", "G2-02", "资源预算与人力配置", "预算偏差 ≤ 10%，关键岗位 HR 已确认", "Y", "2"},
        new String[]{"G2", "G2-03", "项目风险评估与应对", "P0/P1 风险有 Owner 与缓解计划", "N", "3"},
        new String[]{"G2", "G2-04", "开发与运营流程", "研发流程文档发布，CI/CD 链路 demo 通过", "N", "4"},
        new String[]{"G2", "G2-05", "团队组建与能力盘点", "关键岗位到位率 ≥ 90%", "N", "5"},
        new String[]{"G2", "G2-06", "立项评审决议", "三方签字齐全，无保留意见", "Y", "6"},
        // G3 开发阶段评审（5 项，2 否决）
        new String[]{"G3", "G3-01", "关键功能实现", "P0 需求 100% 实现，P1 ≥ 95%", "Y", "1"},
        new String[]{"G3", "G3-02", "系统集成完成度", "端到端核心链路 demo 通过", "Y", "2"},
        new String[]{"G3", "G3-03", "内部测试报告", "无 P0/P1 遗留缺陷", "N", "3"},
        new String[]{"G3", "G3-04", "代码质量与安全审查", "无 Critical 安全漏洞，Sonar 评级达标", "N", "4"},
        new String[]{"G3", "G3-05", "性能基准", "核心接口 P95 ≤ 目标值", "N", "5"},
        // G4 验证/确认评审（8 项，4 否决）
        new String[]{"G4", "G4-01", "UAT 用户验收测试", "客户代表签字通过，无 P0 遗留", "Y", "1"},
        new String[]{"G4", "G4-02", "安全评估与渗透测试", "无 High 及以上未修复漏洞", "Y", "2"},
        new String[]{"G4", "G4-03", "性能压力与稳定性", "峰值容量 ≥ 业务预期 2 倍", "N", "3"},
        new String[]{"G4", "G4-04", "兼容性验证", "目标矩阵 100% 覆盖", "N", "4"},
        new String[]{"G4", "G4-05", "文档完整性", "文档齐备并通过内部评审", "N", "5"},
        new String[]{"G4", "G4-06", "培训与知识转移", "关键角色培训通过率 100%", "N", "6"},
        new String[]{"G4", "G4-07", "部署与回滚方案", "演练通过，回滚 ≤ 30 分钟", "Y", "7"},
        new String[]{"G4", "G4-08", "上市与运营就绪", "上市 checklist 100% 完成", "Y", "8"},
        // G5 上市发布评审（7 项，3 否决）
        new String[]{"G5", "G5-01", "上市发布就绪", "发布条件齐备，监管绿灯", "Y", "1"},
        new String[]{"G5", "G5-02", "客服与支持体系", "客服首问负责率 ≥ 95%", "N", "2"},
        new String[]{"G5", "G5-03", "营销与渠道就绪", "主渠道签约完成，PR 时间表锁定", "N", "3"},
        new String[]{"G5", "G5-04", "运营监控与告警", "监控大盘上线，告警演练通过", "N", "4"},
        new String[]{"G5", "G5-05", "应急预案", "应急预案演练通过", "Y", "5"},
        new String[]{"G5", "G5-06", "监管与法务终审", "法务终审通过，无 P0 风险", "Y", "6"},
        new String[]{"G5", "G5-07", "上市复盘计划", "复盘计划批准，关键指标已埋点", "N", "7"}
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        int inserted = 0;
        int skipped = 0;
        for (String[] def : SEED_ELEMENTS) {
            String gateCode = def[0];
            String elementCode = def[1];
            Long exist = gateElementMapper.selectCount(new LambdaQueryWrapper<GateElement>()
                .eq(GateElement::getGateCode, gateCode)
                .eq(GateElement::getElementCode, elementCode));
            if (exist != null && exist > 0) {
                skipped++;
                continue;
            }
            GateElement e = GateElement.builder()
                .gateCode(gateCode)
                .elementCode(elementCode)
                .elementName(def[2])
                .passStandard(def[3])
                .isVeto(def[4])
                .sortOrder(Integer.parseInt(def[5]))
                .enabled("Y")
                .status("PUBLISHED")
                .version(0)
                .vetoDualRequired("N")
                .build();
            gateElementMapper.insert(e);
            inserted++;
        }
        log.info("[GateElementSeed] 33 项要素种子初始化完成：新增 {} 项，跳过已存在 {} 项", inserted, skipped);
    }
}
