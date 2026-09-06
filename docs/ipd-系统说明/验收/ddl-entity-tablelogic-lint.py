"""
DDL ↔ entity 一致性扫描脚本（[SEC-FIX-FOLLOWUP-2] 2026-09-06）

聚焦 2 类关键漏洞——专注本次踩坑点（不是完整字段对照，避免 snake/camel 误报）：
1. DDL 有 del_flag 列但 entity 缺 @TableLogic（Deliverable 类教训）
2. @TableLogic 注解多余（DDL 无 del_flag 列却加了注解）

完整字段对照走 qa04_ddl_entity_mapping.py。

退出码：0=无严重；1=有严重（CI 阻断）。
"""
import os, re, glob, sys
REPO = '/Users/mac/Documents/ruoyi-ai'
DOMAIN_DIR = REPO + '/ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/domain'
DDL_GLOB = REPO + '/docs/script/sql/update/*.sql'

def parse_ddl():
    cols = {}; cur = None
    for ddl in sorted(glob.glob(DDL_GLOB)):
        try: text = open(ddl, encoding='utf-8', errors='ignore').read()
        except: continue
        for line in text.split('\n'):
            m = re.match(r"\s*create table\s+(?:if not exists\s+)?(\w+)", line, re.IGNORECASE)
            if m: cur = m.group(1).lower(); cols.setdefault(cur, set()); continue
            if cur is None: continue
            m = re.match(r"\s*(\w+)\s+(bigint|varchar|char|int|tinyint|datetime|decimal|text|longtext|json|date|timestamp)\b", line)
            if m:
                c = m.group(1).lower()
                if c in {"primary","key","unique","constraint","index","foreign"}: continue
                cols[cur].add(c)
            if line.strip().startswith(")") and cur: cur = None
    return cols

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

findings = {'ERROR_TABLE_LOGIC_MISSING': [], 'WARN_TABLE_LOGIC_NO_DDL': []}
for table, ent in entities.items():
    ddl = table_cols.get(table, set())
    if 'del_flag' in ddl and not ent['has_table_logic']:
        findings['ERROR_TABLE_LOGIC_MISSING'].append({'table': table, 'file': ent['file'], 'fix': f'Add @TableLogic + @TableField("del_flag")'})
    if ent['has_table_logic'] and 'del_flag' not in ddl:
        findings['WARN_TABLE_LOGIC_NO_DDL'].append({'table': table, 'file': ent['file']})

entity_files = sorted(glob.glob(DOMAIN_DIR + '/*.java'))
ddl_files = sorted(glob.glob(DDL_GLOB))
total_err = sum(len(v) for k, v in findings.items() if k.startswith('ERROR'))
total_warn = sum(len(v) for k, v in findings.items() if k.startswith('WARN'))
print(f"扫描：{len(entity_files)} entity / {len(entities)} 个顶级 @TableName / {len(ddl_files)} DDL")
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
