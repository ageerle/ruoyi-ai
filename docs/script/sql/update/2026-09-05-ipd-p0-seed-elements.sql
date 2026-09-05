-- ============================================================
-- IPD P0-8 基础种子数据：Gate 评审要素 33 项 + 国别认证模板 17 项
-- 依据（逐字，不杜撰）：
--   docs/ipd-系统说明/外部资源/IPD系统_五大Gate评审要素_v1.md（33 要素逐项表）
--   v3 BR-IPD-05b / 补漏表 M1（国别认证内置常用项）
-- 口径说明：否决项按「要素表格内含 ❌」逐项打标，共 15 个要素带否决位；
--   原文汇总行写 14（G2 计 4），与其要素表逐项 ❌ 不一致（G2 实标 5）——
--   按要素表逐项为准（实现要求#1：否决=gate_review_element.is_veto 布尔），
--   差异已记录于开发计划-看板镜像.md P0-8 卡。
-- 幂等：按 element_code / (country_code,cert_name) 唯一定位，重跑跳过。
-- ============================================================

INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090500, 'G1', 'G1-1', '市场机会真实性', '✅ ≥5 家目标客户一手验证记录；或 ≥1 家客户书面意向（二选一）。参数 gate.g1.minCustomerVerifications=5', '0', 1, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G1-1');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090501, 'G1', 'G1-2', '市场规模与目标设定', '目标销售额有自下而上推导（客户数×客单×渗透率）或可比产品对标；四项基准值（目标销售额/渠道数/NPS/场景数）已录入', '1', 2, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G1-2');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090502, 'G1', 'G1-3', '竞争格局与差异化', '差异点≥2 项且竞品 6 个月内难以复制（竞品≥3 家对比）', '0', 3, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G1-3');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090503, 'G1', 'G1-4', '技术可行性', '预研结论为可行或有条件可行+明确条件；结论不可行=否决', '1', 4, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G1-4');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090504, 'G1', 'G1-5', '商业性', '毛利率 ≥ 产品线门槛（可配置 gate.grossMarginThreshold）；毛利率为负=否决', '1', 5, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G1-5');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090505, 'G1', 'G1-6', '合规与知识产权', '认证清单已确认且无不可逾越障碍；FTO 无高风险专利；Z03 合规审查未通过/认证不可逾越=否决', '1', 6, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G1-6');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090506, 'G1', 'G1-7', '资源与组队', '双PM 均已确认承接；研发PM 未到位=否决', '1', 7, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G1-7');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090507, 'G2', 'G2-1', 'PRD 完整性', '需求条目化率 100%，每条含验收标准；PRD 未上传=否决', '1', 8, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G2-1');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090508, 'G2', 'G2-2', '需求优先级与版本规划', '首版 Must 需求可支撑核心场景闭环', '0', 9, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G2-2');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090509, 'G2', 'G2-3', '差异化卖点可交付性', '每个卖点有研发侧可实现+成本增量确认；卖点被判定不可实现=否决', '1', 10, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G2-3');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090510, 'G2', 'G2-4', '价值定价与毛利复核', '毛利率 ≥ 门槛；低于门槛且无定价调整方案=否决', '1', 11, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G2-4');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090511, 'G2', 'G2-5', '技术方案与里程碑', '里程碑日期完整（EVT/DVT/PVT/GTM）；里程碑日期缺失=否决', '1', 12, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G2-5');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090512, 'G2', 'G2-6', '认证与法规清单确认', '目标市场强制认证全部列入且周期匹配；缺失或周期冲突=否决', '1', 13, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G2-6');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090513, 'G3', 'G3-1', '进度与里程碑', '偏差 ≤5 个工作日，或已制定追赶计划', '0', 14, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G3-1');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090514, 'G3', 'G3-2', '场景完整度', '每个核心场景端到端可走通', '0', 15, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G3-2');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090515, 'G3', 'G3-3', '需求变更情况', '累计变更率 ≤15%（预警线 12%）', '0', 16, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G3-3');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090516, 'G3', 'G3-4', '技术风险与阻塞', '无 P0 级阻塞；有则已升级并明确责任人', '0', 17, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G3-4');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090517, 'G3', 'G3-5', '成本与合规跟踪', '成本偏差 ≤5%；认证进度正常', '0', 18, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G3-5');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090518, 'G4', 'G4-1', '产品就绪', 'DVT/V02 认证/V03 Beta/V06 量产准入全部通过；V02 未通过或量产准入未通过=否决', '1', 19, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G4-1');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090519, 'G4', 'G4-2', '质量与缺陷', 'P0 清零，P1 ≤3 且有 workaround；P0 未清零=否决', '1', 20, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G4-2');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090520, 'G4', 'G4-3', '供应与备货', '首批量产完成，备货满足首批订单预测', '0', 21, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G4-3');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090521, 'G4', 'G4-4', '价格与渠道体系', '价格体系已发布，目标渠道覆盖率 ≥60%', '0', 22, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G4-4');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090522, 'G4', 'G4-5', '销售工具与培训', '工具包齐备，核心渠道培训覆盖率 ≥80%（含海外分支）', '0', 23, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G4-5');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090523, 'G4', 'G4-6', '本地化与合规落地', '本地化验收清单逐项打勾完成；目标市场强制认证未取得/Z05 未完成=否决', '1', 24, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G4-6');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090524, 'G4', 'G4-7', '售后与支持', '售后维修方案/备件/话术/退换货政策已就绪', '0', 25, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G4-7');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090525, 'G4', 'G4-8', 'GTM 方案可执行性', '首批目标客户名单 ≥10 家且已分配责任人', '0', 26, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G4-8');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090526, 'G5', 'G5-1', '销售达成情况', '90 天累计达成率 ≥25%（预警线，非否决）；回款口径 Q2', '0', 27, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G5-1');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090527, 'G5', 'G5-2', '渠道与场景覆盖', '渠道覆盖 ≥50% 目标，场景覆盖 ≥50% 目标', '0', 28, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G5-2');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090528, 'G5', 'G5-3', '客户反馈与质量', 'P0 问题 100% 闭环；NPS 达目标或已制定改进计划；P0 未闭环=否决', '1', 29, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G5-3');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090529, 'G5', 'G5-4', '需求准确率复盘', '变更率统计完成，根因已归类', '0', 30, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G5-4');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090530, 'G5', 'G5-5', '上市准时性与窗口命中', '两项偏差已计量并归因（≤15 天/≤30 天）', '0', 31, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G5-5');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090531, 'G5', 'G5-6', '利润与成本复盘', '毛利率偏差 ≤5 个百分点或已归因', '0', 32, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G5-6');
INSERT INTO gate_review_elements (id, gate_code, element_code, element_name, pass_standard, is_veto, sort_order, enabled, create_by, create_time, tenant_id, del_flag)
SELECT 1948090532, 'G5', 'G5-7', '迭代与生命周期决策', '已形成明确决议（加速/迭代/扩展/限售/停产）并指定责任人；未形成决议=否决', '1', 33, '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM gate_review_elements WHERE element_code = 'G5-7');

-- ---------- 国别认证模板 18 项（v3 BR-IPD-05b 内置常用项） ----------
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090600, 'CN', '中国', 'CCC', NULL, '中国强制性产品认证', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'CN' AND cert_name = 'CCC');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090601, 'CN', '中国', 'SRRC', NULL, '无线电发射设备型号核准', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'CN' AND cert_name = 'SRRC');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090602, 'CN', '中国', '等保 2.0', NULL, '网络安全等级保护 2.0 测评', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'CN' AND cert_name = '等保 2.0');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090603, 'CN', '中国', '个保法合规', NULL, '个人信息保护法合规审查（生物特征数据处理）', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'CN' AND cert_name = '个保法合规');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090604, 'US', '北美', 'FCC', NULL, '美国联邦通信认证', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'US' AND cert_name = 'FCC');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090605, 'US', '北美', 'UL/ETL', NULL, '北美安全认证（二选一）', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'US' AND cert_name = 'UL/ETL');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090606, 'US', '北美', '加州 Prop 65', NULL, '加州 65 号提案有害物质警示', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'US' AND cert_name = '加州 Prop 65');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090607, 'EU', '欧盟', 'CE', NULL, '欧盟符合性认证', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'EU' AND cert_name = 'CE');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090608, 'EU', '欧盟', 'RoHS', NULL, '欧盟有害物质限制', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'EU' AND cert_name = 'RoHS');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090609, 'EU', '欧盟', 'REACH', NULL, '欧盟化学品注册评估授权', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'EU' AND cert_name = 'REACH');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090610, 'EU', '欧盟', 'GDPR', NULL, '欧盟通用数据保护条例（生物特征数据处理合规）', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'EU' AND cert_name = 'GDPR');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090611, 'SA', '沙特阿拉伯', 'SABER/SASO', NULL, '沙特产品符合性认证', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'SA' AND cert_name = 'SABER/SASO');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090612, 'AE', '阿联酋', 'ECAS/EQM', NULL, '阿联酋合格认证（二选一体系）', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'AE' AND cert_name = 'ECAS/EQM');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090613, 'IN', '印度', 'BIS', NULL, '印度标准局认证', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'IN' AND cert_name = 'BIS');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090614, 'KR', '韩国', 'KC', NULL, '韩国认证', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'KR' AND cert_name = 'KC');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090615, 'JP', '日本', 'PSE/MIC', NULL, '日本电气安全/无线电认证', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'JP' AND cert_name = 'PSE/MIC');
INSERT INTO cert_templates (id, country_code, country_name, cert_name, cert_authority, requirement_desc, is_mandatory, create_by, create_time, tenant_id, del_flag)
SELECT 1948090616, 'AU', '澳洲', 'RCM', NULL, '澳洲合规标志', '1', 1, NOW(), '000000', '0'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM cert_templates WHERE country_code = 'AU' AND cert_name = 'RCM');
