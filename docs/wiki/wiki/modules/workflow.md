---
topic: modules/workflow
title: ruoyi-workflow — Warm-Flow BPMN 引擎
updated: 2026-09-04
raw:
  - raw/workflow-source/warm-flow-controller.md
  - raw/workflow-source/workflow-service.md
---

# ruoyi-workflow — Warm-Flow BPMN 引擎

`ruoyi-workflow` 是基于 **Warm-Flow**（国产 BPMN 引擎）的传统审批工作流模块。**78 个 Java 文件**，跟 `ruoyi-aiflow` 的图驱动 AI 工作流不同——这是面向审批场景的 BPMN 2.0 流程引擎。

参见：[claude-md.md § Module Layout — workflow / aiflow](../raw/project-skeleton/claude-md.md)、[claude-md.md § workflow / generator](../raw/project-skeleton/claude-md.md)。

## 与 aiflow 的边界

| 维度 | ruoyi-workflow | ruoyi-aiflow |
|---|---|---|
| 引擎 | Warm-Flow（第三方） | 自研图驱动 |
| 适用 | 传统审批（请假 / 报销 / 多级签批） | AI 处理管道（模型调用 → RAG → 邮件） |
| 节点 | BPMN 元素（userTask / serviceTask / gateway） | 自定义节点（model / email / web_search） |
| 状态 | BPMN 标准状态机 | 自定义 WfState / WfNodeState |

**两者并存不冲突**：业务审批用 Warm-Flow，AI 编排用 aiflow。

## 关键 Controller

```
FlwDefinitionController       流程定义 CRUD（部署 BPMN XML）
FlwInstanceController         流程实例（启动、暂停、终止）
FlwTaskController             用户任务（签收、完成、转办、委派）
FlwCategoryController         流程分类
FlwSpelController             SPEL 表达式（流程条件）
TestLeaveController           示例：请假流程
```

参见：[warm-flow-controller](../raw/workflow-source/warm-flow-controller)。

## 关键 Service

| Service | 作用 |
|---|---|
| `FlwDefinitionService` | 流程定义（部署 BPMN XML 到数据库） |
| `FlwInstanceService` | 流程实例（启动、暂停、终止） |
| `FlwTaskService` | 用户任务（审批相关） |
| `FlwSpelService` | SPEL 表达式求值（用于流程条件） |

参见：[workflow-service](../raw/workflow-source/workflow-service)。

## Warm-Flow 集成

Warm-Flow 依赖（参见 [pom-xml.md § warm-flow](../raw/project-skeleton/pom-xml.md)）：

```xml
<dependency>
    <groupId>org.dromara.warm</groupId>
    <artifactId>warm-flow-mybatis-plus-sb3-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.dromara.warm</groupId>
    <artifactId>warm-flow-plugin-ui-sb-web</artifactId>
</dependency>
```

配置（参见 [application-yml.md § warm-flow](../raw/project-skeleton/application-yml.md)）：

```yaml
warm-flow:
  enabled: true            # 总开关
  ui: true                 # 设计器 UI
  top-text-show: true      # 流程图顶部文字
  node-tooltip: true       # 节点悬浮提示
  token-name: Authorization,clientid  # token 名称（Sa-Token + 客户端）
```

参见：[application-yml.md § warm-flow](../raw/project-skeleton/application-yml.md)。

## 流程设计器

`warm-flow-plugin-ui-sb-web` 提供 Web 端流程设计器 UI（拖拽节点 → 画 BPMN → 保存 XML）。

## 包结构

```
org.ruoyi.workflow/
├── controller/             # 6 个（Flw* + TestLeave）
├── service/                # 业务 service
├── domain/                 # 实体（BPMN 相关）
├── mapper/                 # MyBatis-Plus mapper
├── handler/                # 自定义 BPMN 节点处理器
├── listener/               # 流程事件监听（启动 / 完成 / 异常）
├── rule/                   # 流程规则（条件、网关判断）
├── config/                 # Warm-Flow 配置
└── common/                 # 公共
```

## 接入业务系统

传统审批流接入示例：

1. 业务模块（`ruoyi-system` 的请假 service）注入 `FlwInstanceService`
2. 调用 `startProcess(businessKey, variables)` 启动流程
3. 监听 `FlwProcessEvent`（listener 模块）处理业务状态变更
4. 在 BPMN XML 里画 userTask / serviceTask 节点

**注意**：BPMN 流程定义（XML）存在 `flw_definition` 表，由 Warm-Flow 管理；不要自己改表结构。