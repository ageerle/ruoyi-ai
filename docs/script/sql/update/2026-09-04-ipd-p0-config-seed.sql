-- =====================================================================
-- IPD 系统参数种子（G-05/G-08：全部可配置，禁止硬编码）
-- 来源：v3 §7 参数表（L1127-1177）+ 开发说明书 D.0.7（双源核对一致，54 键）
-- 涉钱 6 项切换开关见 bonus.poolBase / salesSource / performanceScoreStrategy /
--      coefficientDecider / multiProjectSplit / launchAnchor（§8.1 Q1-Q6）
-- 日期：2026-09-04
-- =====================================================================

insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090401, 'allowance.L1', '1000', 'NUMBER', '1000', '月度津贴标准 L1（超管配映射表非个人等级，B6）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090402, 'allowance.L2', '1500', 'NUMBER', '1500', '月度津贴标准 L2', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090403, 'allowance.L3', '2000', 'NUMBER', '2000', '月度津贴标准 L3', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090404, 'allowance.L4', '2500', 'NUMBER', '2500', '月度津贴标准 L4', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090405, 'allowance.L5', '3000', 'NUMBER', '3000', '月度津贴标准 L5', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090406, 'allowance.capMultiplier', '2', 'NUMBER', '2', '月津贴总额封顶倍数（多项目叠加≤2倍）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090407, 'allowance.projectCountThreshold', '3', 'NUMBER', '3', '需评级委员会审批的项目数阈值（组长上限同3，A4）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090408, 'allowance.noOutputMonths', '2', 'NUMBER', '2', '无实质产出停发阈值（月）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090409, 'allowance.scoreStopThreshold', '60', 'NUMBER', '60', '当月停发分数线（<60 停发）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090410, 'allowance.levelEffectiveRule', 'BY_BIND_TIME', 'STRING', 'BY_BIND_TIME', '评级更新后额度适用规则：按项目绑定时锁定（B6）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090411, 'bonus.poolRate', '0.05', 'NUMBER', '0.05', '奖金池提取比例 5%', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090412, 'bonus.poolBase', 'TARGET_SALES', 'STRING', 'TARGET_SALES', '奖金池基数（Q1：目标销售额）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090413, 'bonus.salesSource', 'RECEIPT', 'STRING', 'RECEIPT', '达成率统计口径（Q2：回款）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090414, 'bonus.performanceScoreStrategy', 'PROJECT_SCORE', 'STRING', 'PROJECT_SCORE', '绩效系数取数（Q3：项目维度概念→发布）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090415, 'bonus.multiProjectSplit', 'NONE', 'STRING', 'NONE', '多项目池分摊（Q5：不分摊，产品项目1:1）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090416, 'bonus.launchAnchor', 'L08_ACTION', 'STRING', 'L08_ACTION', '上市锚点（Q6：L08 动作录入的上市日期）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090417, 'bonus.coefficientDecider', 'G1_DUAL_SIGN', 'STRING', 'G1_DUAL_SIGN', '系数定值（Q4：双PM联合提议+组长确认）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090418, 'bonus.coefficient.S', '1.5', 'NUMBER', '1.5', 'S 级系数（区间1.5-2.0取下限，E21）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090419, 'bonus.coefficient.A', '1.0', 'NUMBER', '1.0', 'A 级系数（固定）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090420, 'bonus.coefficient.B', '0.8', 'NUMBER', '0.8', 'B 级系数（区间0.6-0.8取上限，E21）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090421, 'bonus.coefficientRange.S', '1.5-2.0', 'STRING', '1.5-2.0', 'S 级系数合法区间（校验用）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090422, 'bonus.coefficientRange.B', '0.6-0.8', 'STRING', '0.6-0.8', 'B 级系数合法区间（校验用）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090423, 'bonus.achievementTiers', '[{"threshold":120,"multiplier":1.2,"match":"ABOVE"},{"threshold":100,"multiplier":1,"match":"GTE"},{"threshold":85,"multiplier":0.8,"match":"GTE"},{"threshold":70,"multiplier":0.6,"match":"GTE"},{"threshold":50,"multiplier":0.3,"match":"GTE"},{"threshold":0,"multiplier":0,"match":"GTE"}]', 'JSON', '[{"threshold":120,"multiplier":1.2,"match":"ABOVE"},{"threshold":100,"multiplier":1,"match":"GTE"},{"threshold":85,"multiplier":0.8,"match":"GTE"},{"threshold":70,"multiplier":0.6,"match":"GTE"},{"threshold":50,"multiplier":0.3,"match":"GTE"},{"threshold":0,"multiplier":0,"match":"GTE"}]', '达成率阶梯6档区间制（E22；ABOVE=严格大于，GTE=大于等于；按序从高到低首中即停）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090424, 'bonus.performanceTiers', '[{"threshold":95,"multiplier":1},{"threshold":85,"multiplier":0.8},{"threshold":70,"multiplier":0.6},{"threshold":60,"multiplier":0.3},{"threshold":0,"multiplier":0}]', 'JSON', '[{"threshold":95,"multiplier":1},{"threshold":85,"multiplier":0.8},{"threshold":70,"multiplier":0.6},{"threshold":60,"multiplier":0.3},{"threshold":0,"multiplier":0}]', '绩效系数映射5档（E23；按综合得分从高到低 GTE 匹配）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090425, 'bonus.contribution.marketRange', '40-65', 'STRING', '40-65', '市场PM 贡献度区间%（与研发和恒100）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090426, 'bonus.contribution.rdRange', '35-60', 'STRING', '35-60', '研发PM 贡献度区间%', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090427, 'bonus.monthsWindow', '6', 'NUMBER', '6', '回款统计窗口（月，自上市日起）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090428, 'kpi.functionalWeight', '0.6', 'NUMBER', '0.6', '功能KPI 权重', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090429, 'kpi.sharedWeight', '0.4', 'NUMBER', '0.4', '共担KPI 权重', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090430, 'kpi.reviewWeights', '{"self":0.2,"marketLeader":0.4,"rdLeader":0.4}', 'JSON', '{"self":0.2,"marketLeader":0.4,"rdLeader":0.4}', '项目绩效评定权重（Q3衍生：自评20%+市场组长40%+研发组长40%）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090431, 'kpi.monthlyDeadlineDay', '5', 'NUMBER', '5', '共担KPI 月度归集截止日（次月第5个工作日18:00，I5衍生）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090432, 'kpi.collectorRole', 'GROUP_LEADER', 'STRING', 'GROUP_LEADER', '共担KPI 录入复核角色（I5：产品组长）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090433, 'kpi.reviewDaysAfterLaunch', '30', 'NUMBER', '30', '项目绩效评定完成期限（上市后30日内）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090434, 'gate.g1.minCustomerVerifications', '5', 'NUMBER', '5', 'G1 立项≥5家客户一手验证或1家书面意向（I3衍生）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090435, 'gate.signDeadlineDays', '3', 'NUMBER', '3', '双签期限（自然日，D17）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090436, 'gate.signExtendMaxTimes', '3', 'NUMBER', '3', '超管延长Gate期限次数上限（D17）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090437, 'gate.reviewRoundEscalation', '3', 'NUMBER', '3', '第N轮升级组长列席（D18）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090438, 'gate.reviewRoundSuperAdmin', '5', 'NUMBER', '5', '第N轮超管介入（D18）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090439, 'gate.reviewDays90', '90', 'NUMBER', '90', '上市后复盘天数', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090440, 'bid.expireWarnDays', '3', 'NUMBER', '3', '招标到期提醒提前天数（C12）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090441, 'bid.selectDeadlineDays', '7', 'NUMBER', '7', '待遴选挂起升级天数（C12）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090442, 'bid.assignAfterDays', '30', 'NUMBER', '30', '超管可指派天数阈值（C13）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090443, 'handover.resignDeadlineDays', '15', 'NUMBER', '15', '离职移交期限（自然日，B8）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090444, 'handover.freezeBeforeDisable', 'true', 'BOOL', 'true', '先冻结待移交→移交完成→才禁用（B8）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090445, 'deletion.leaderDeadlineDays', '2', 'NUMBER', '2', '删除初审期限（工作日，F29）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090446, 'deletion.adminDeadlineDays', '2', 'NUMBER', '2', '删除终审期限（工作日，F29）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090447, 'deletion.withdrawHours', '24', 'NUMBER', '24', '删除申请撤回时限（小时）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090448, 'noOutput.days', '60', 'NUMBER', '60', '实质产出判定窗口（天，E27）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090449, 'hr.syncCron', '0 2 1 * *', 'STRING', '0 2 1 * *', '人员同步定时（每月1日02:00，B6）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090450, 'hr.syncLeaderRole', 'true', 'BOOL', 'true', '组长角色随API同步（A3）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090451, 'hr.enableProxyLeader', 'false', 'BOOL', 'false', '不做代理组长/代审人（A3）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090452, 'hr.allowCrossRole', 'false', 'BOOL', 'false', '不允许跨角色（B7 角色固定）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090453, 'hr.levelSource', 'API_ONLY', 'STRING', 'API_ONLY', 'L1-L5 API 唯一权威源（B6，超管不可改）', '000000', now());
insert into system_configs (id, config_key, config_value, value_type, default_value, description, tenant_id, create_time)
values (1948090454, 'req.guestOtherAssignDays', '5', 'NUMBER', '5', '游客「其他」需求未指派提醒天数（G31）', '000000', now());
