# Triage Labels

The skills speak in terms of five canonical triage roles. This file maps those roles to the actual label strings used in this repo's issue tracker.

| Label in mattpocock/skills | Label in our tracker | Meaning                                  |
| -------------------------- | -------------------- | ---------------------------------------- |
| `needs-triage`             | `needs-triage`       | Maintainer needs to evaluate this issue  |
| `needs-info`               | `needs-info`         | Waiting on reporter for more information |
| `ready-for-agent`          | `ready-for-agent`    | Fully specified, ready for an AFK agent  |
| `ready-for-human`          | `ready-for-human`    | Requires human implementation            |
| `wontfix`                  | `wontfix`            | Will not be actioned                     |

When a skill mentions a role (e.g. "apply the AFK-ready triage label"), use the corresponding label string from this table.

Edit the right-hand column to match whatever vocabulary you actually use.

## 本仓库额外补充标签

除上述 5 个核心标签外，本仓库额外使用以下业务标签（由 setup-matt-pocock-skills 默认配置 + Gavin 决策）：

| 标签 | 含义 | 何时打 |
|---|---|---|
| `bug` | 代码缺陷 | 任何代码 / 配置错误 |
| `enhancement` | 功能增强 | 新功能或改进 |
| `documentation` | 文档变更 | 仅改文档（不动代码） |
| `p0` / `p1` / `p2` / `p3` / `p4` | P0–P4 阶段（IPD 二开） | 任何 IPD 二开任务 |
| `chat` / `aiflow` / `system` / `common` / `infra` | 模块标签（对应 `ruoyi-modules/*`） | 模块相关任务 |
| `ipd` | IPD 二开相关 | 任何改造任务 |
| `urgent` | 紧急 | 阻塞性问题 |
| `low-priority` | 低优先 | 不急的事 |

**复合标签规则**：一个 issue 可以同时打多个标签（如 `["needs-triage", "p0", "ipd"]` 表示「P0 阶段的 IPD 二开任务，待 triage」）。

## 自动化建议

GitHub Actions 可在 `.github/workflows/triage.yml` 自动打标签（基于标题关键词）：

```yaml
name: auto-triage
on: [issues]
jobs:
  label:
    runs-on: ubuntu-latest
    steps:
      - name: Auto-label by title
        uses: actions/github-script@v7
        with:
          script: |
            const title = context.payload.issue.title;
            const labels = [];
            if (/^p0/i.test(title) || title.includes('[p0]')) labels.push('p0');
            if (/^p1/i.test(title) || title.includes('[p1]')) labels.push('p1');
            if (/^p2/i.test(title) || title.includes('[p2]')) labels.push('p2');
            if (/^p3/i.test(title) || title.includes('[p3]')) labels.push('p3');
            if (/^p4/i.test(title) || title.includes('[p4]')) labels.push('p4');
            if (/chat|agent|langchain4j|mcp/i.test(title)) labels.push('chat');
            if (/aiflow|workflow/i.test(title)) labels.push('aiflow');
            if (/system|rbac|sys_user|sys_role/i.test(title)) labels.push('system');
            if (/common|common-/i.test(title)) labels.push('common');
            if (/ipd/i.test(title)) labels.push('ipd');
            if (labels.length > 0) github.rest.issues.addLabels({...});
```

---

**最后更新**：2026-09-04（setup-matt-pocock-skills 自动生成）