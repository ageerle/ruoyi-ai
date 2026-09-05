package org.ruoyi.ipd.seed;

import org.ruoyi.ipd.domain.ActionDef;

import java.util.List;

/**
 * IPD 六阶段标准动作清单 v3 目录（69 动作 = 深管 42 / 轻管 27，阻断 38 / 非阻断 31）
 * 来源：docs/ipd-系统说明/外部资源/IPD系统_六阶段标准动作清单_v3.md（已按 Gavin 全部决策定稿）
 *
 * 设计说明：动作编码体系（C01/P01/D05/C12/D11/V10...）与阶段/深度/阻断属性是流程定义事实，
 * 编译期固化于本目录；项目等级裁剪规则（A 级必做集、B 级清单）由超管在 system_configs 后台配置（v3 原文），
 * B 级清单因 v3 有权威定义而内置为常量（见 B_LEVEL_BLOCKING_CODES）。
 *
 * ownerRole 在 MARKET_PM|RD_PM|BOTH 之外补充 GROUP_LEADER（K01-K04 共担KPI 归集动作主责=产品组长，
 * I5 决策；LC09 R=系统自动，业务责任 A=产品组长，归组到 GROUP_LEADER）。
 * K01-K04 无独立阶段，挂 LIFECYCLE（上市后 6 个月归集窗口）。
 */
public final class ActionCatalog {

    private ActionCatalog() {
    }

    public static final List<ActionDef> ALL = List.of(
        // ===== 阶段一 概念 CONCEPT（12：深 11 / 轻 1；阻断 10）=====
        new ActionDef("C01", "市场机会与痛点调研", "CONCEPT", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("C02", "竞品分析", "CONCEPT", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("C03", "目标客户与细分市场定义", "CONCEPT", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("C04", "区域市场准入与需求差异调研", "CONCEPT", "MARKET_PM", "DEEP", true, "OVERSEAS", "", false, ""),
        new ActionDef("C05", "技术可行性预研", "CONCEPT", "RD_PM", "LIGHT", false, "ALL", "", false, ""),
        new ActionDef("C06", "产品概念与差异化定位", "CONCEPT", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("C07", "成本/定价/毛利初步测算", "CONCEPT", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("C08", "销量预测与商业目标(四项基准值录入)", "CONCEPT", "MARKET_PM", "DEEP", true, "ALL", "BASELINE", false, ""),
        new ActionDef("C09", "项目等级评定与差异化系数", "CONCEPT", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("C10", "知识产权与合规预检(含专利FTO)", "CONCEPT", "RD_PM", "DEEP", false, "ALL", "", false, ""),
        new ActionDef("C11", "Charter立项评审会", "CONCEPT", "BOTH", "DEEP", true, "ALL", "", false, "G1"),
        new ActionDef("C12", "生物特征数据合规审查", "CONCEPT", "MARKET_PM", "DEEP", true, "ALL", "", true, ""),
        // ===== 阶段二 计划 PLAN（13：深 4 / 轻 9；阻断 5）=====
        new ActionDef("P01", "产品需求规格定义PRD", "PLAN", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("P02", "需求优先级排序与版本规划", "PLAN", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("P03", "总体技术方案与系统架构设计", "PLAN", "RD_PM", "LIGHT", false, "ALL", "", false, ""),
        new ActionDef("P04", "ID/结构/硬件/固件方案设计", "PLAN", "RD_PM", "LIGHT", false, "HW", "", false, ""),
        new ActionDef("P05", "软件概要设计", "PLAN", "RD_PM", "LIGHT", false, "SW", "", false, ""),
        new ActionDef("P06", "解决方案场景设计与集成方案", "PLAN", "RD_PM", "LIGHT", false, "SOL", "", false, ""),
        new ActionDef("P07", "关键器件选型与供应链评估", "PLAN", "RD_PM", "LIGHT", false, "HW", "", false, ""),
        new ActionDef("P08", "项目计划与里程碑排期", "PLAN", "RD_PM", "LIGHT", false, "ALL", "", false, ""),
        new ActionDef("P09", "资源与预算评估", "PLAN", "RD_PM", "LIGHT", false, "ALL", "", false, ""),
        new ActionDef("P10", "认证与法规清单确认", "PLAN", "RD_PM", "LIGHT", true, "ALL", "", false, ""),
        new ActionDef("P11", "风险识别与应对计划", "PLAN", "RD_PM", "LIGHT", false, "ALL", "", false, ""),
        new ActionDef("P12", "差异化卖点确认与价值定价", "PLAN", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("P13", "差异化确认评审会", "PLAN", "BOTH", "DEEP", true, "ALL", "", false, "G2"),
        // ===== 阶段三 开发 DEV（11：深 2 / 轻 9；阻断 2）=====
        new ActionDef("D01", "详细设计", "DEV", "RD_PM", "LIGHT", false, "ALL", "", false, ""),
        new ActionDef("D02", "首版BOM冻结与采购下单", "DEV", "RD_PM", "LIGHT", false, "HW", "", false, ""),
        new ActionDef("D03", "手板/EVT样机制作", "DEV", "RD_PM", "LIGHT", false, "HW", "", false, ""),
        new ActionDef("D04", "软件开发与单元测试", "DEV", "RD_PM", "LIGHT", false, "SW", "", false, ""),
        new ActionDef("D05", "双周开发评审", "DEV", "BOTH", "DEEP", true, "ALL", "", false, "G3"),
        new ActionDef("D06", "需求变更评估与审批", "DEV", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("D07", "模具开发与T1试模", "DEV", "RD_PM", "LIGHT", false, "HW", "", false, ""),
        new ActionDef("D08", "开发阶段成本复核", "DEV", "MARKET_PM", "LIGHT", false, "ALL", "", false, ""),
        new ActionDef("D09", "内测版本发布Alpha", "DEV", "RD_PM", "LIGHT", false, "ALL", "", false, ""),
        new ActionDef("D10", "解决方案联调与集成测试环境搭建", "DEV", "RD_PM", "LIGHT", false, "SOL", "", false, ""),
        new ActionDef("D11", "BioCV算法训练与评测", "DEV", "RD_PM", "LIGHT", false, "BIOCV", "FAR,FRR", true, ""),
        // ===== 阶段四 验证 VALID（12：深 6 / 轻 6；阻断 7）=====
        new ActionDef("V01", "DVT设计验证测试", "VALID", "RD_PM", "LIGHT", false, "HW", "", false, ""),
        new ActionDef("V02", "认证测试送检", "VALID", "RD_PM", "LIGHT", true, "ALL", "CERT_NO,CERT_DATE", false, ""),
        new ActionDef("V03", "Beta客户试用与反馈收集", "VALID", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("V04", "软件系统测试与缺陷收敛", "VALID", "RD_PM", "LIGHT", false, "SW", "", false, ""),
        new ActionDef("V05", "试产PVT/小批量", "VALID", "RD_PM", "LIGHT", false, "HW", "", false, ""),
        new ActionDef("V06", "量产准入评审", "VALID", "RD_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("V07", "包装说明书快速指南定稿", "VALID", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("V08", "售后与维修方案准备", "VALID", "MARKET_PM", "LIGHT", false, "ALL", "", false, ""),
        new ActionDef("V09", "解决方案试点客户交付验证", "VALID", "MARKET_PM", "DEEP", true, "SOL", "", false, ""),
        new ActionDef("V10", "跨人种跨年龄适配验证", "VALID", "MARKET_PM", "DEEP", true, "BIOCV", "", true, ""),
        new ActionDef("V11", "平台兼容性与SDK-API对接验证", "VALID", "RD_PM", "LIGHT", false, "SOL", "", false, ""),
        new ActionDef("V12", "海外市场本地化适配验证", "VALID", "MARKET_PM", "DEEP", true, "OVERSEAS", "", false, ""),
        // ===== 阶段五 发布 LAUNCH（8：深 7 / 轻 1；阻断 7）=====
        new ActionDef("L01", "GTM上市策略", "LAUNCH", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("L02", "销售渠道与价格体系发布", "LAUNCH", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("L03", "销售工具包", "LAUNCH", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("L04", "销售与渠道培训", "LAUNCH", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("L05", "首批量产与备货", "LAUNCH", "RD_PM", "LIGHT", false, "HW", "", false, ""),
        new ActionDef("L06", "系统上架", "LAUNCH", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("L07", "GTM就绪评审", "LAUNCH", "BOTH", "DEEP", true, "ALL", "", false, "G4"),
        new ActionDef("L08", "正式上市发布(录入上市日期)", "LAUNCH", "MARKET_PM", "DEEP", true, "ALL", "LAUNCH_DATE", false, ""),
        // ===== 阶段六 生命周期 LIFECYCLE（9：深 8 / 轻 1；阻断 7）=====
        new ActionDef("LC01", "上市后销售与回款跟踪", "LIFECYCLE", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("LC02", "上市后90天复盘", "LIFECYCLE", "BOTH", "DEEP", true, "ALL", "", false, "G5"),
        new ActionDef("LC03", "上市后6个月终算(回款达成率+奖金池)", "LIFECYCLE", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("LC04", "双PM贡献度评定", "LIFECYCLE", "BOTH", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("LC05", "客户反馈与质量问题处理", "LIFECYCLE", "MARKET_PM", "DEEP", false, "ALL", "", false, ""),
        new ActionDef("LC06", "版本迭代与维护发布", "LIFECYCLE", "RD_PM", "LIGHT", false, "ALL", "", false, ""),
        new ActionDef("LC07", "生命周期状态维护", "LIFECYCLE", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("LC08", "停产评估与公告", "LIFECYCLE", "MARKET_PM", "DEEP", true, "ALL", "", false, ""),
        new ActionDef("LC09", "项目归档(系统自动,业务责任产品组长)", "LIFECYCLE", "GROUP_LEADER", "DEEP", true, "ALL", "", false, ""),
        // ===== 共担KPI 归集（4：深 4 / 轻 0；阻断 0，主责=产品组长 I5 决策）=====
        new ActionDef("K01", "销量出货量达成率归集", "LIFECYCLE", "GROUP_LEADER", "DEEP", false, "ALL", "", false, ""),
        new ActionDef("K02", "渠道商覆盖达成率归集", "LIFECYCLE", "GROUP_LEADER", "DEEP", false, "ALL", "", false, ""),
        new ActionDef("K03", "客户NPS调研归集", "LIFECYCLE", "GROUP_LEADER", "DEEP", false, "ALL", "", false, ""),
        new ActionDef("K04", "场景覆盖率归集", "LIFECYCLE", "GROUP_LEADER", "DEEP", false, "ALL", "", false, ""));

    /**
     * B 级项目阻断集（v3 权威定义）：Gate 关联动作 7 项 + P10/V02 法规认证 + C12 生物特征合规（全等级）= 10 项。
     * A 级清单 v3 未定死（约 25），由超管后台配置，此处不内置（不造无源数据）。
     */
    public static final List<String> B_LEVEL_BLOCKING_CODES = List.of(
        "C11", "P12", "P13", "D05", "L07", "L08", "LC02", "P10", "V02", "C12");

    /** 主 Prompt v3 L513-517 别名对：Z 系为同一动作第二编码（64+5 建制=69 历史口径） */
    public static final java.util.Map<String, String> ALIASES = java.util.Map.of(
        "Z01", "D11", "Z02", "V10", "Z03", "C12", "Z04", "V11", "Z05", "V12");

    /** 别名归一：Z 系编码解析为权威编码；未知编码原样返回 */
    public static String resolveCode(String code) {
        return ALIASES.getOrDefault(code, code);
    }

    public static List<ActionDef> byStage(String stage) {
        return ALL.stream().filter(a -> a.stage().equals(stage)).toList();
    }

    public static ActionDef byCode(String code) {
        return ALL.stream().filter(a -> a.code().equals(code)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("动作编码不存在: " + code));
    }
}