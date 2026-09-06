"""
DDL entity @TableLogic lint hook（[SEC-FIX-FOLLOWUP-2] 2026-09-06）
PreToolUse 触发：Edit/Write 命中 ruoyi-ipd domain/*.java 时，跑 ddl-entity-tablelogic-lint.py
若发现 del_flag 缺注解（ERROR 级）→ 阻断提交（exit 2）。
WARN 级（注解多余）→ 仅 stderr 警告不阻断。
"""
import json, os, subprocess, sys, re

HOOK_NAME = 'ipd-entity-tablelogic-lint'
LINT_SCRIPT = '/Users/mac/Documents/ruoyi-ai/docs/ipd-系统说明/验收/ddl-entity-tablelogic-lint.py'

def main():
    try:
        payload = json.loads(sys.stdin.read())
    except Exception:
        sys.exit(0)
    tool = payload.get('tool_name') or ''
    if tool not in ('Edit', 'Write', 'MultiEdit'): sys.exit(0)
    file_path = (payload.get('tool_input') or {}).get('file_path') or ''
    # 仅对 ruoyi-ipd domain 类生效
    if '/ruoyi-ipd/src/main/java/org/ruoyi/ipd/domain/' not in file_path or not file_path.endswith('.java'):
        sys.exit(0)
    # 仅 entity 顶层类
    if not re.search(r'/(?!.*ValueObject|.*View|.*Bo|.*Req|.*Resp|.*Dto)[A-Z]\w+\.java$', file_path):
        sys.exit(0)
    if not os.path.exists(LINT_SCRIPT): sys.exit(0)
    # 跑 lint
    r = subprocess.run(['python3', LINT_SCRIPT], capture_output=True, text=True, cwd='/Users/mac/Documents/ruoyi-ai')
    if r.returncode == 0: sys.exit(0)
    # 提取严重项
    err = r.stdout + r.stderr
    err_lines = [l for l in err.split('\n') if 'ERROR_TABLE_LOGIC_MISSING' in l or l.strip().startswith(('  products', '  projects', '  project_', '  gate_', '  cert_'))]
    if not err_lines: sys.exit(0)
    print(f"[{HOOK_NAME}] DDL entity @TableLogic lint 严重项：", file=sys.stderr)
    for l in err_lines[-15:]:
        print(l, file=sys.stderr)
    print(f"\n→ 修复：在 delFlag 字段加 @TableLogic + @TableField(\"del_flag\")", file=sys.stderr)
    # Edit 阻断
    if tool in ('Edit', 'MultiEdit'):
        sys.exit(2)
    # Write 阻断（新建 entity 缺注解是高风险）
    sys.exit(2)

if __name__ == '__main__':
    main()
