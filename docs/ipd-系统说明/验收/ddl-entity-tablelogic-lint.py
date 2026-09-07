"""
DDL ↔ entity 一致性扫描脚本（[SEC-FIX-FOLLOWUP-2] 2026-09-06，[DDL-AUTODISC-3] 2026-09-06）

聚焦 2 类关键漏洞——专注本次踩坑点（不是完整字段对照，避免 snake/camel 误报）：
1. DDL 有 del_flag 列但 entity 缺 @TableLogic（Deliverable 类教训）
2. @TableLogic 注解多余（DDL 无 del_flag 列却加了注解）

完整字段对照走 qa04_ddl_entity_mapping.py。

reference DDL 自动发现（与 qa04 对齐）：
- glob scan docs/script/sql/update/*.sql + docs/script/sql/*.sql
- 过滤含 CREATE TABLE / ALTER TABLE / INSERT INTO 的 ipd 文件
- 输出扫到 N 个 DDL / 含 M 个 CREATE TABLE / K 个 ALTER TABLE
- 若 M+K 与实际 entity 表数严重不一致 → fail-fast（防白名单漏扫早期发现）

退出码：0=无严重；1=有严重（CI 阻断）；2=reference DDL 发现异常（fail-fast）。
"""
import os, re, glob, sys
REPO = '/Users/mac/Documents/ruoyi-ai'
DOMAIN_DIR = REPO + '/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/domain'
DDL_SCAN_DIRS = [
    REPO + '/docs/script/sql/update',
    REPO + '/docs/script/sql',
]
SQL_KEYWORDS = re.compile(r'create\s+table\b|alter\s+table\b|insert\s+into\b', re.I)
# 隐性白名单：文件名不含 ipd 但内容是 ipd 补缺表
SQL_BATCH_HIDDEN = {'batch_missing_tables.sql'}

def auto_discover_ddl():
    """自动扫描 reference DDL：含 CREATE/ALTER/INSERT 关键字的 ipd 模块文件。

    返回：扫描到的文件路径列表（相对 REPO）+ 统计 (n_create, n_alter)。
    """
    files, n_create, n_alter = [], 0, 0
    for d in DDL_SCAN_DIRS:
        if not os.path.isdir(d):
            continue
        for f in sorted(os.listdir(d)):
            if not f.endswith('.sql'):
                continue
            if not ('ipd-' in f.lower() or f in SQL_BATCH_HIDDEN):
                continue
            p = os.path.join(d, f)
            try:
                text = open(p, encoding='utf-8', errors='ignore').read()
            except Exception:
                continue
            if not SQL_KEYWORDS.search(text):
                continue
            files.append(os.path.relpath(p, REPO))
            n_create += len(re.findall(r'create\s+table\b', text, re.I))
            n_alter += len(re.findall(r'alter\s+table\b', text, re.I))
    return files, n_create, n_alter

DDL_FILES, N_CREATE_TOTAL, N_ALTER_TOTAL = auto_discover_ddl()
DDL_GLOB_PATTERNS = [d + '/*.sql' for d in DDL_SCAN_DIRS]

def parse_ddl():
    """解析 CREATE TABLE 列 + ALTER TABLE ADD COLUMN 列（含动态 SQL 包装）。

    动态 SQL 处理策略：先解开 '...ALTER/CREATE TABLE...' 字符串字面量；
    对嵌套 IF(..., '...', '...') 模式只取 CREATE/ALTER 分支。
    """
    cols = {}; cur = None
    for ddl in [os.path.join(REPO, f) for f in DDL_FILES]:
        try: text = open(ddl, encoding='utf-8', errors='ignore').read()
        except: continue
        text = _unwrap_dynamic_sql(text)
        for line in text.split('\n'):
            m = re.match(r"\s*create table\s+(?:if not exists\s+)?`?(\w+)`?", line, re.IGNORECASE)
            if m: cur = m.group(1).lower(); cols.setdefault(cur, set()); continue
            if cur is None: continue
            m = re.match(r"\s*`?(\w+)`?\s+(bigint|varchar|char|int|tinyint|datetime|decimal|text|longtext|json|date|timestamp)\b", line, re.I)
            if m:
                c = m.group(1).lower()
                if c in {"primary","key","unique","constraint","index","foreign"}: continue
                cols[cur].add(c)
            # ALTER TABLE x ADD COLUMN y type（lint 只关心 del_flag 在不在，类型无关）
            am = re.match(r"\s*alter\s+table\s+`?(\w+)`?\s+.*?add\s+column\s+(?:if\s+not\s+exists\s+)?`?(\w+)`?\s+", line, re.I)
            if am:
                tname, cname = am.group(1).lower(), am.group(2).lower()
                cols.setdefault(tname, set()).add(cname)
            if line.strip().startswith(")") and cur: cur = None
    return cols


def _unwrap_dynamic_sql(sql):
    """解开 SQL 字符串字面量中的动态 SQL：'...CREATE/ALTER TABLE...' 模式。

    兼容：
      1. SET @sql := '<SQL>'; 简单形式
      2. SET @sql := IF(cond, '<CREATE/ALTER>', '<其他>'); 嵌套 IF
      3. 字符串内 '' 转义为 '
    """
    out = sql

    def _pick_inner(inner):
        """从 IF(..., 'A', 'B') 的两个字符串中挑选 CREATE/ALTER 分支。"""
        for cand in re.split(r"'\s*,\s*'", inner):
            if re.match(r"^\s*(alter|create)\s+(table|database)\b", cand.strip(), re.I):
                return cand.strip().replace("''", "'")
        return ""

    # 处理嵌套 IF(cond, 'A', 'B') 形式
    for m in re.finditer(r"SET\s+@\w+\s*:=\s*IF\s*\(([^;]+)\)\s*;", out, re.I | re.S):
        args = m.group(1)
        # 提取 IF 中所有单引号字符串
        strs = re.findall(r"'((?:[^']|'')*)'", args)
        picked = ""
        for s in strs:
            if re.match(r"^\s*(alter|create)\s+(table|database)\b", s.replace("''", "'"), re.I):
                picked = s.replace("''", "'")
                break
        if picked:
            out = out.replace(m.group(0), picked + ";")

    # 处理简单 SET @var := '...'; 形式
    for m in re.finditer(r"SET\s+@\w+\s*:=\s*'((?:[^']|'')*)'\s*;", out, re.I):
        inner = m.group(1).replace("''", "'").strip()
        if re.match(r"^\s*(alter|create)\s+(table|database)\b", inner, re.I):
            out = out.replace(m.group(0), inner + ";")
    return out

def parse_entities():
    out = {}
    for java in sorted(glob.glob(DOMAIN_DIR + '/*.java')):
        try: content = open(java, encoding='utf-8', errors='ignore').read()
        except: continue
        for m in re.finditer(r'@TableName\s*\(\s*(?:value\s*=\s*)?["\'](\w+)["\']', content):
            t = m.group(1).lower()
            after = content[m.end():m.end()+5000]
            mc = re.search(r'public\s+(?:final\s+|abstract\s+)?class\s+(\w+)', after)
            if not mc: continue
            out[t] = {'file': os.path.basename(java), 'has_table_logic': '@TableLogic' in content}
            break
    return out

table_cols = parse_ddl()
entities = parse_entities()

# fail-fast：reference DDL 自动发现的 CREATE+ALTER 计数与 ipd 实体表数严重不一致
# 经验阈值：实体表数应远小于 DDL 计数（多个实体可能共用一张表），反之 DDL 计数应 >= 实体表数
# 容忍度：DDL 计数 >= 实体表数 × 0.5 即视为一致；若 DDL 比实体还少 50% → fail-fast
expected_min_ddl = max(3, len(entities) // 2)
if N_CREATE_TOTAL + N_ALTER_TOTAL < expected_min_ddl:
    sys.stderr.write(
        "[FAIL-FAST] reference DDL 自动发现异常：扫到 %d 个 DDL 文件，仅含 %d CREATE TABLE / %d ALTER TABLE，"
        "远低于实体表数 %d（期望 ≥ %d）。疑似 glob 漏扫或白名单漂移，请核对 docs/script/sql/ 路径与 ipd- 前缀过滤。\n"
        % (len(DDL_FILES), N_CREATE_TOTAL, N_ALTER_TOTAL, len(entities), expected_min_ddl)
    )
    sys.exit(2)

findings = {'ERROR_TABLE_LOGIC_MISSING': [], 'WARN_TABLE_LOGIC_NO_DDL': []}
for table, ent in entities.items():
    ddl = table_cols.get(table, set())
    if 'del_flag' in ddl and not ent['has_table_logic']:
        findings['ERROR_TABLE_LOGIC_MISSING'].append({'table': table, 'file': ent['file'], 'fix': f'Add @TableLogic + @TableField("del_flag")'})
    if ent['has_table_logic'] and 'del_flag' not in ddl:
        findings['WARN_TABLE_LOGIC_NO_DDL'].append({'table': table, 'file': ent['file']})

entity_files = sorted(glob.glob(DOMAIN_DIR + '/*.java'))
total_err = sum(len(v) for k, v in findings.items() if k.startswith('ERROR'))
total_warn = sum(len(v) for k, v in findings.items() if k.startswith('WARN'))
print(f"扫描：{len(entity_files)} entity / {len(entities)} 个顶级 @TableName / {len(DDL_FILES)} DDL")
print(f"扫到 {len(DDL_FILES)} 个 DDL 文件 / 含 {N_CREATE_TOTAL} 个 CREATE TABLE / {N_ALTER_TOTAL} 个 ALTER TABLE")
print(f"严重：{total_err}  /  警告：{total_warn}")
print()
for cat, items in findings.items():
    if items:
        print(f"=== {cat} ({len(items)}) ===")
        for it in items[:8]:
            line = f"  {it['table']} ({it['file']})"
            if 'fix' in it: line += f" → {it['fix']}"
            print(line)
        if len(items) > 8: print(f"  ... 还有 {len(items)-8} 项")
        print()
sys.exit(0 if total_err == 0 else 1)
