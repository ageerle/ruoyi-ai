# assets_公共规范-通用.md（骨架）

> **状态**：❌ 缺失（等待填充）
> **重要性**：⭐⭐（前端 UI 设计规范，前端仓库使用）

---

## 用途

- 前端 UI 设计的 token 骨架（按钮 / 表格 / 表单 / 输入 / 弹窗的 token 命名）
- 与 `docs/开发说明/spec/_公共规范.md` 配合使用
- 前端仓库（`ruoyi-web` / `ruoyi-admin` Vue 工程）使用

## 关键内容（推断）

按 Vben Admin / Element Plus 默认 + 开发说明书规范，应该包含：

### 1. 颜色 token

```css
:root {
  --color-primary: #006BE6;
  --color-primary-hover: #0059C2;
  --color-primary-light: #E6F1FE;
  --color-success: #57D188;
  --color-warning: #EFBD48;
  --color-danger: #FF3860;
  --color-info: #909399;

  --color-bg: #F2F3F5;
  --color-card-bg: #FFFFFF;
  --color-text-main: #303133;
  --color-text-secondary: #606266;
  --color-text-placeholder: #A8ABB2;
  --color-border: #DCDFE6;
}
```

### 2. 尺寸 token

```css
:root {
  --sidebar-width: 224px;
  --sidebar-collapsed: 60px;
  --header-height: 50px;
  --tab-height: 38px;
  --spacing-sm: 8px;
  --spacing-md: 12px;
  --spacing-lg: 16px;
  --spacing-xl: 24px;
  --radius-sm: 4px;
  --radius-md: 8px;
  --z-index-base: 200;
}
```

### 3. 字体 token

```css
:root {
  --font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Arial, sans-serif;
  --font-size-h1: 20px;
  --font-size-h2: 18px;
  --font-size-h3: 16px;
  --font-size-body: 14px;
  --font-size-table: 13px;
  --font-size-muted: 12px;
}
```

## 引用位置

- `docs/开发说明/spec/_公共规范.md` §一、§三、§四、§五（基于此文件）

## 等待填充

- [ ] 完整的 Element Plus 组件 token 表
- [ ] 业务语义色使用示例（深管 / 轻管 / 阻断性动作等）

## 临时替代

本仓库 `docs/wiki/wiki/automation/claude-code-setup.md` 与 `docs/开发说明/spec/_公共规范.md` 已含核心规范。前端仓库（独立）使用 Vben Admin 默认值即可。

---

**最后更新**：2026-09-04