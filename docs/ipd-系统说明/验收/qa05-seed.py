#!/usr/bin/env python3
# QA-05 性能基准造数脚本：向隔离库 ipd_perf 灌隔离数据
# 规模：500 persons / 2000 projects+products / 12000 project_stages /
#       138000 stage_actions（2000×69 对齐 P1-3.1 目录）/ 500000 audit_logs
# 密码策略：复用 ipd_dev 主库已知明文账号行的 BCrypt 哈希（不新发明明文），
#          must_change_pwd=0 规避首登强制改密
# 用法：python3 qa05-seed.py   （仅写 ipd_perf，主库只读）
import json
import random
import subprocess
import time

MYSQL = "/Users/mac/Documents/ruoyi-ai/.codex/ipd-dev/software/mysql-8.0.46-macos15-arm64/bin/mysql"
CNF = "/Users/mac/Documents/ruoyi-ai/.codex/ipd-dev/config/mysql-app.cnf"
DB = "ipd_perf"
random.seed(20260905)

# 69 动作模板（对齐 P1-3.1 目录：code/name/owner_role/depth/is_blocking）
ACTIONS = [
    ("C01", "市场机会与痛点调研", "MARKET_PM", "DEEP", 1), ("C02", "竞品分析", "MARKET_PM", "DEEP", 1),
    ("C03", "目标客户与细分市场定义", "MARKET_PM", "DEEP", 1), ("C04", "区域市场准入与需求差异调研", "MARKET_PM", "DEEP", 1),
    ("C05", "技术可行性预研", "RD_PM", "LIGHT", 0), ("C06", "产品概念与差异化定位", "MARKET_PM", "DEEP", 1),
    ("C07", "成本/定价/毛利初步测算", "MARKET_PM", "DEEP", 1), ("C08", "销量预测与商业目标(四项基准值录入)", "MARKET_PM", "DEEP", 1),
    ("C09", "项目等级评定与差异化系数", "MARKET_PM", "DEEP", 1), ("C10", "知识产权与合规预检(含专利FTO)", "RD_PM", "DEEP", 0),
    ("C11", "Charter立项评审会", "BOTH", "DEEP", 1), ("C12", "生物特征数据合规审查", "MARKET_PM", "DEEP", 1),
    ("D01", "详细设计", "RD_PM", "LIGHT", 0), ("D02", "首版BOM冻结与采购下单", "RD_PM", "LIGHT", 0),
    ("D03", "手板/EVT样机制作", "RD_PM", "LIGHT", 0), ("D04", "软件开发与单元测试", "RD_PM", "LIGHT", 0),
    ("D05", "双周开发评审", "BOTH", "DEEP", 1), ("D06", "需求变更评估与审批", "MARKET_PM", "DEEP", 1),
    ("D07", "模具开发与T1试模", "RD_PM", "LIGHT", 0), ("D08", "开发阶段成本复核", "MARKET_PM", "LIGHT", 0),
    ("D09", "内测版本发布Alpha", "RD_PM", "LIGHT", 0), ("D10", "解决方案联调与集成测试环境搭建", "RD_PM", "LIGHT", 0),
    ("D11", "BioCV算法训练与评测", "RD_PM", "LIGHT", 0), ("K01", "销量出货量达成率归集", "GROUP_LEADER", "DEEP", 0),
    ("K02", "渠道商覆盖达成率归集", "GROUP_LEADER", "DEEP", 0), ("K03", "客户NPS调研归集", "GROUP_LEADER", "DEEP", 0),
    ("K04", "场景覆盖率归集", "GROUP_LEADER", "DEEP", 0), ("L01", "GTM上市策略", "MARKET_PM", "DEEP", 1),
    ("L02", "销售渠道与价格体系发布", "MARKET_PM", "DEEP", 1), ("L03", "销售工具包", "MARKET_PM", "DEEP", 1),
    ("L04", "销售与渠道培训", "MARKET_PM", "DEEP", 1), ("L05", "首批量产与备货", "RD_PM", "LIGHT", 0),
    ("L06", "系统上架", "MARKET_PM", "DEEP", 1), ("L07", "GTM就绪评审", "BOTH", "DEEP", 1),
    ("L08", "正式上市发布(录入上市日期)", "MARKET_PM", "DEEP", 1), ("LC01", "上市后销售与回款跟踪", "MARKET_PM", "DEEP", 1),
    ("LC02", "上市后90天复盘", "BOTH", "DEEP", 1), ("LC03", "上市后6个月终算(回款达成率+奖金池)", "MARKET_PM", "DEEP", 1),
    ("LC04", "双PM贡献度评定", "BOTH", "DEEP", 1), ("LC05", "客户反馈与质量问题处理", "MARKET_PM", "DEEP", 0),
    ("LC06", "版本迭代与维护发布", "RD_PM", "LIGHT", 0), ("LC07", "生命周期状态维护", "MARKET_PM", "DEEP", 1),
    ("LC08", "停产评估与公告", "MARKET_PM", "DEEP", 1), ("LC09", "项目归档(系统自动,业务责任产品组长)", "GROUP_LEADER", "DEEP", 1),
    ("P01", "产品需求规格定义PRD", "MARKET_PM", "DEEP", 1), ("P02", "需求优先级排序与版本规划", "MARKET_PM", "DEEP", 1),
    ("P03", "总体技术方案与系统架构设计", "RD_PM", "LIGHT", 0), ("P04", "ID/结构/硬件/固件方案设计", "RD_PM", "LIGHT", 0),
    ("P05", "软件概要设计", "RD_PM", "LIGHT", 0), ("P06", "解决方案场景设计与集成方案", "RD_PM", "LIGHT", 0),
    ("P07", "关键器件选型与供应链评估", "RD_PM", "LIGHT", 0), ("P08", "项目计划与里程碑排期", "RD_PM", "LIGHT", 0),
    ("P09", "资源与预算评估", "RD_PM", "LIGHT", 0), ("P10", "认证与法规清单确认", "RD_PM", "LIGHT", 1),
    ("P11", "风险识别与应对计划", "RD_PM", "LIGHT", 0), ("P12", "差异化卖点确认与价值定价", "MARKET_PM", "DEEP", 1),
    ("P13", "差异化确认评审会", "BOTH", "DEEP", 1), ("V01", "DVT设计验证测试", "RD_PM", "LIGHT", 0),
    ("V02", "认证测试送检", "RD_PM", "LIGHT", 1), ("V03", "Beta客户试用与反馈收集", "MARKET_PM", "DEEP", 1),
    ("V04", "软件系统测试与缺陷收敛", "RD_PM", "LIGHT", 0), ("V05", "试产PVT/小批量", "RD_PM", "LIGHT", 0),
    ("V06", "量产准入评审", "RD_PM", "DEEP", 1), ("V07", "包装说明书快速指南定稿", "MARKET_PM", "DEEP", 1),
    ("V08", "售后与维修方案准备", "MARKET_PM", "LIGHT", 0), ("V09", "解决方案试点客户交付验证", "MARKET_PM", "DEEP", 1),
    ("V10", "跨人种跨年龄适配验证", "MARKET_PM", "DEEP", 1), ("V11", "平台兼容性与SDK-API对接验证", "RD_PM", "LIGHT", 0),
    ("V12", "海外市场本地化适配验证", "MARKET_PM", "DEEP", 1),
]
# 动作 → 阶段映射（前缀决定所属阶段；K 归集类挂 LIFECYCLE）
STAGES = ["CONCEPT", "PLAN", "DEV", "VALID", "LAUNCH", "LIFECYCLE"]
STAGE_NAMES = {"CONCEPT": "概念", "PLAN": "计划", "DEV": "开发", "VALID": "验证", "LAUNCH": "发布", "LIFECYCLE": "生命周期"}


def stage_of(code: str) -> str:
    if code.startswith("LC"):
        return "LIFECYCLE"
    return {"C": "CONCEPT", "P": "PLAN", "D": "DEV", "V": "VALID", "L": "LAUNCH", "K": "LIFECYCLE"}[code[0]]


def sql_exec(stmt: str, bufsize: int = 1 << 22) -> None:
    r = subprocess.run([MYSQL, "--defaults-file=" + CNF, DB],
                       input=stmt.encode(), capture_output=True)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.decode()[:2000])


def rowcount(table: str) -> int:
    r = subprocess.run([MYSQL, "--defaults-file=" + CNF, DB, "-N", "-e",
                        f"SELECT COUNT(*) FROM {table}"], capture_output=True, text=True)
    return int(r.stdout.strip() or 0)


def batch_insert(table: str, cols: str, rows: list, batch: int = 1000) -> None:
    for i in range(0, len(rows), batch):
        chunk = rows[i:i + batch]
        stmt = f"INSERT IGNORE INTO {table} ({cols}) VALUES\n" + ",\n".join(chunk) + ";\n"
        sql_exec(stmt)


def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, (int, float)):
        return str(v)
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


def main() -> None:
    t0 = time.time()
    times = {}

    # 从主库读可复用密码哈希（按角色取已知明文账号行；明文不入库不扩散）
    r = subprocess.run([MYSQL, "--defaults-file=" + CNF, "ipd_dev", "-N", "-e",
                        "SELECT username, password_hash FROM persons "
                        "WHERE username IN ('ipd-admin','ipd-leader','ipd-rd','陈市场')"],
                       capture_output=True, text=True)
    reused = dict(line.split("\t") for line in r.stdout.strip().split("\n"))
    PH = {"MARKET_PM": reused["陈市场"], "RD_PM": reused["ipd-rd"],
          "GROUP_LEADER": reused["ipd-leader"], "SUPER_ADMIN": reused["ipd-admin"]}
    times["fetch_hash"] = round(time.time() - t0, 1)

    # ---- 500 persons：MARKET_PM 200 / RD_PM 200 / GROUP_LEADER 80 / SUPER_ADMIN 20
    persons, pmeta = [], []
    base = 7110000000000000000
    plan = [("mkt", "MARKET_PM", 200), ("rd", "RD_PM", 200), ("gl", "GROUP_LEADER", 80), ("sa", "SUPER_ADMIN", 20)]
    pid = 0
    for prefix, role, n in plan:
        for i in range(1, n + 1):
            pid += 1
            persons.append(
                f"({base+pid}, '性能{prefix}{i:03d}', 'PERF-{prefix}-{i:03d}', '{role}', "
                f"NULL, 'L3', 'API_SYNC', '2026-01-01 00:00:00', 'ACTIVE', 'ACTIVE', NULL, NULL, "
                f"'perf_{prefix}{i:03d}', '{PH[role]}', '0', NULL)")
            pmeta.append((base + pid, role, prefix, i))
    batch_insert("persons",
                 "id,name,employee_no,person_type,group_id,level,level_source,level_updated_at,"
                 "account_status,employment_status,wecom_user_id,wecom_bound_at,"
                 "username,password_hash,must_change_pwd,last_login_at", persons)
    times["persons_500"] = round(time.time() - t0, 1)

    # ---- 4 产品组
    gbase = 7100000000000000000
    groups = [f"({gbase+g}, '性能压测组{g}', {base+401+g}, NULL, 'QA-05 隔离造数')" for g in range(1, 5)]
    batch_insert("product_groups", "id,group_name,leader_person_id,parent_id,description", groups)
    times["groups_4"] = round(time.time() - t0, 1)

    # ---- 2000 projects + products（1:1）
    pbase, prjbase = 7130000000000000000, 7120000000000000000
    projects, products = [], []
    for i in range(1, 2001):
        gid = gbase + (i % 4) + 1
        mkt = next(x for x in pmeta if x[2] == "mkt" and x[3] == ((i - 1) % 200) + 1)
        lvl = "S" if i % 10 == 0 else ("A" if i % 10 in (1, 2, 3) else "B")
        tpl = "HARDWARE" if i % 2 == 0 else "SOFTWARE"
        projects.append(
            f"({prjbase+i}, 'PRJ-2026-{i:04d}', '性能压测项目{i:04d}', {pbase+i}, '{tpl}', "
            f"'[\"SA\"]', '{lvl}', 1.00, NULL, 1000000.00, 1, 50, 1, NULL, "
            f"'CONCEPT', NULL, NULL, 'NEW', NULL, '0', NULL, 'ACTIVE', {gid})")
        products.append(
            f"({pbase+i}, 'PRD-PERF-{i:04d}', '性能压测产品{i:04d}', 'MODEL-PERF-{i:04d}', "
            f"'PM_NEW', {prjbase+i}, {gid}, 'ACTIVE')")
    batch_insert("projects",
                 "id,code,name,product_id,template_type,target_markets,level,level_coefficient,"
                 "level_coefficient_reason,target_sales_amount,target_channel_count,target_nps,"
                 "target_scene_count,launch_date,current_stage,declared_stage,lifecycle_status,"
                 "source,legacy_effective_at,missing_history_ack,catchup_status,status,main_group_id",
                 projects)
    batch_insert("products",
                 "id,product_code,product_name,model_code,source,project_id,group_id,status", products)
    times["projects_products_2000"] = round(time.time() - t0, 1)

    # ---- 12000 project_stages（2000×6）
    sbase = 7140000000000000000
    stages = []
    for i in range(1, 2001):
        for s in range(1, 7):
            code = STAGES[s - 1]
            st = "IN_PROGRESS" if s == 1 else "NOT_STARTED"
            stages.append(f"({sbase+(i-1)*6+s}, {prjbase+i}, '{code}', '{STAGE_NAMES[code]}', {s}, '{st}', NULL)")
    batch_insert("project_stages", "id,project_id,stage_code,stage_name,sort_order,status,gate_id", stages)
    times["stages_12000"] = round(time.time() - t0, 1)

    # ---- 138000 stage_actions（2000×69，status 全 NOT_STARTED 供写压测）
    abase = 7150000000000000000
    actions = []
    for i in range(1, 2001):
        for a, (code, name, owner, depth, blk) in enumerate(ACTIONS):
            sid = sbase + (i - 1) * 6 + STAGES.index(stage_of(code)) + 1
            actions.append(
                f"({abase+(i-1)*69+a}, {prjbase+i}, {sid}, '{code}', '{name}', '{owner}', "
                f"'{depth}', 'NOT_STARTED', NULL, '{blk}', NULL, NULL, NULL, NULL, NULL, NULL, "
                f"'0', NULL, 0)")
    batch_insert("stage_actions",
                 "id,project_id,stage_id,action_code,action_name,owner_role,depth,status,"
                 "history_mark,is_blocking,actual_done_at,far_value,frr_value,cert_no,"
                 "cert_passed_at,algo_type,is_bio_feature,due_date,version", actions)
    times["stage_actions_138000"] = round(time.time() - t0, 1)

    # ---- 500000 audit_logs（PERF_SEED，create_time 随机近一年）
    abig = 7160000000000000000
    z64 = "0" * 64
    audit = []
    now = time.mktime(time.strptime("2026-09-04 00:00:00", "%Y-%m-%d %H:%M:%S"))
    for i in range(1, 500001):
        op = pmeta[random.randrange(500)]
        ts = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(now - random.randrange(365 * 86400)))
        audit.append(
            f"({abig+i}, NULL, {op[0]}, '性能{op[2]}{op[3]:03d}', '{op[1]}', 'PERF_SEED', 'perf', "
            f"{prjbase + random.randrange(1, 2001)}, NULL, '{{}}', NULL, '{z64}', '{z64}', "
            f"'127.0.0.1', '000000', '{ts}', 2)")
        if len(audit) >= 1000:
            batch_insert("audit_logs",
                         "id,seq,operator_id,operator_name,operator_role,action,entity_type,"
                         "entity_id,before_data,after_data,reason,prev_hash,curr_hash,"
                         "ip_address,tenant_id,create_time,hash_version", audit)
            audit = []
    if audit:
        batch_insert("audit_logs",
                     "id,seq,operator_id,operator_name,operator_role,action,entity_type,"
                     "entity_id,before_data,after_data,reason,prev_hash,curr_hash,"
                     "ip_address,tenant_id,create_time,hash_version", audit)
    times["audit_logs_500000"] = round(time.time() - t0, 1)

    # ---- 校验
    r = subprocess.run([MYSQL, "--defaults-file=" + CNF, DB, "-N", "-e",
                        "SELECT 'persons',COUNT(*) FROM persons UNION ALL "
                        "SELECT 'projects',COUNT(*) FROM projects UNION ALL "
                        "SELECT 'products',COUNT(*) FROM products UNION ALL "
                        "SELECT 'project_stages',COUNT(*) FROM project_stages UNION ALL "
                        "SELECT 'stage_actions',COUNT(*) FROM stage_actions UNION ALL "
                        "SELECT 'audit_logs',COUNT(*) FROM audit_logs"], capture_output=True, text=True)
    print(r.stdout)
    print("TIMINGS(s):", json.dumps(times))
    print("TOTAL(s):", round(time.time() - t0, 1))


if __name__ == "__main__":
    main()
