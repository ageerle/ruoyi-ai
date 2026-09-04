---
topic: modules/aiflow
title: ruoyi-aiflow — 可视化 AI 工作流引擎
updated: 2026-09-04
raw:
  - raw/aiflow-source/workflow-controller.md
  - raw/aiflow-source/workflow-runtime-controller.md
  - raw/aiflow-source/entity-workflow.md
  - raw/aiflow-source/entity-workflow-node.md
  - raw/aiflow-source/entity-workflow-edge.md
  - raw/aiflow-source/workflow-engine.md
  - raw/aiflow-source/wf-node-factory.md
  - raw/aiflow-source/workflow-graph-builder.md
---

# ruoyi-aiflow — 可视化 AI 工作流引擎

`ruoyi-aiflow` 是 RuoYi-AI 的**可视化工作流编排模块**，跟 `ruoyi-workflow`（基于 Warm-Flow 的传统 BPMN）不同——这是**图驱动的 AI 工作流**，专为「拖拽节点 → 编排 AI 处理管道」设计。

107 个 Java 文件，结构如下：

```
org.ruoyi.workflow/
├── controller/
│   ├── WorkflowController.java           # 工作流定义 CRUD
│   ├── WorkflowRuntimeController.java    # 工作流运行实例
│   └── admin/                            # 管理后台 controller
├── workflow/                             # 引擎核心（55 文件）
│   ├── WorkflowEngine.java               # 主执行引擎
│   ├── WorkflowGraphBuilder.java         # 图构建器
│   ├── WfNodeFactory.java                # 节点工厂
│   ├── WfState.java / WfNodeState.java   # 状态机
│   ├── CompileNode.java / GraphCompileNode.java  # 编译
│   ├── WfComponentNameEnum.java          # 组件名枚举
│   ├── WfNodeInputConfig.java            # 节点输入配置
│   ├── WfNodeIODataUtil.java             # I/O 数据工具
│   └── node/, edge/, data/, def/         # 节点类型细分
├── entity/                               # 6 个实体
│   ├── Workflow.java
│   ├── WorkflowNode.java
│   ├── WorkflowEdge.java
│   ├── WorkflowComponent.java
│   ├── WorkflowRuntime.java
│   └── WorkflowRuntimeNode.java
├── service/                              # 6 个 service
├── mapper/                               # 6 个 mapper
├── dto/                                  # 12 个 dto
├── util/                                 # 10 个工具
├── enums/                                # AiModelStatus / WfIODataTypeEnum
├── config/BeanConfig.java                # Spring 配置
├── base/                                 # 2 个基类
├── helper/PrivilegeUtil.java
└── cosntant/                             # 2 个常量（注意：项目原拼写错误，保留）
```

参见：[pom-xml.md § modules](../raw/project-skeleton/pom-xml.md)、[claude-md.md § Module Layout](../raw/project-skeleton/claude-md.md)。

## 核心实体模型（图驱动）

工作流由 4 个核心实体构成一个**有向图**：

| 实体 | 作用 |
|---|---|
| `Workflow` | 工作流定义（名称、描述、版本） |
| `WorkflowNode` | 节点（一个处理单元，挂载组件 + 输入输出配置） |
| `WorkflowEdge` | 边（连接两个节点，定义执行顺序） |
| `WorkflowComponent` | 组件（节点类型的实现，如「模型调用」/「邮件发送」/「人工审核」） |

运行时新增 2 个实体：

| 实体 | 作用 |
|---|---|
| `WorkflowRuntime` | 运行实例（一次执行产生的状态） |
| `WorkflowRuntimeNode` | 运行时的节点状态 |

参见：[entity-workflow.md](../raw/aiflow-source/entity-workflow.md)、[entity-workflow-node.md](../raw/aiflow-source/entity-workflow-node.md)、[entity-workflow-edge.md](../raw/aiflow-source/entity-workflow-edge.md)。

## 三层执行模型

工作流执行分三层（参照 [workflow-engine.md](../raw/aiflow-source/workflow-engine.md)）：

```
1. WorkflowGraphBuilder
   ↓
   把数据库里的 Workflow/Node/Edge 编译为内存中的「可执行图」
   ↓
2. CompileNode / GraphCompileNode
   ↓
   验证图合法性、解析输入输出依赖、生成执行计划
   ↓
3. WorkflowEngine
   ↓
   按依赖顺序执行节点，处理状态转换与异常传播
```

**关键优势**：图与执行分离，节点可独立开发与测试。

## 节点工厂 — WfNodeFactory + WfComponentNameEnum

```java
WfNodeFactory.getNode(String componentName) → AbstractNode
```

`WfComponentNameEnum` 是节点类型枚举，常见类型：

- `MODEL`（模型调用）
- `EMAIL`（邮件发送）
- `MANUAL_REVIEW`（人工审核）
- `WEB_SEARCH`（联网搜索）
- `KNOWLEDGE`（知识库检索）
- `CODE_EXEC`（代码执行）

新增节点类型：实现 `AbstractNode` 接口 → 在 `WfComponentNameEnum` 加枚举 → 在 `WfNodeFactory` 注册。

参见：[wf-node-factory.md](../raw/aiflow-source/wf-node-factory.md)。

## I/O 数据流

每个节点有：

- **输入**（`WfNodeInputConfig`）：上游数据依赖 + 数据类型约束（`WfIODataTypeEnum`：TEXT / JSON / NUMBER / BOOLEAN / IMAGE / FILE）
- **输出**：执行结果，按 `WfIODataTypeEnum` 序列化

`WfNodeIODataUtil` 处理节点间的数据传递与类型转换。

**约束**：上游节点的输出类型必须匹配下游节点的输入类型——这是图编译期检查（`GraphCompileNode` 负责）。

## 状态机

工作流状态由 `WfState` 跟踪，节点状态由 `WfNodeState` 跟踪：

- `WorkflowRuntime`: `PENDING / RUNNING / PAUSED / COMPLETED / FAILED / CANCELED`
- `WorkflowRuntimeNode`: `WAITING / RUNNING / SUCCESS / FAILED / SKIPPED`

状态转换由 `WorkflowEngine` 在执行节点前后驱动。

参见：[workflow-engine.md](../raw/aiflow-source/workflow-engine.md)。

## Controller 入口

```java
@RestController
@RequestMapping("/workflow")
public class WorkflowController { ... }

@RestController
@RequestMapping("/workflow/runtime")
public class WorkflowRuntimeController { ... }
```

`/workflow/*` 是工作流**定义**的 CRUD（保存图、版本管理、节点配置）。
`/workflow/runtime/*` 是工作流**运行**的接口（启动 / 暂停 / 恢复 / 取消 / 查询状态）。

参见：[workflow-controller.md](../raw/aiflow-source/workflow-controller.md)、[workflow-runtime-controller.md](../raw/aiflow-source/workflow-runtime-controller.md)。

## SSE 流式执行

工作流执行期间，前端通过 SSE（参见 [application-yml.md § sse](../raw/project-skeleton/application-yml.md)）订阅节点状态变化。每节点执行完，引擎推送事件到前端 UI（前端由 `ruoyi-web` 提供可视化设计器）。

## 与 ruoyi-workflow 的边界

| 模块 | 引擎 | 适用场景 |
|---|---|---|
| `ruoyi-workflow` | Warm-Flow（BPMN 2.0） | 传统审批流（请假 / 报销 / 多级签批） |
| `ruoyi-aiflow` | 自研图驱动 | AI 处理管道（模型调用 → RAG → 邮件） |

两者**并存**：业务审批用 Warm-Flow，AI 编排用 aiflow。

参见：[claude-md.md § Module Layout](../raw/project-skeleton/claude-md.md)。

## 已知约束

- 节点 I/O 类型必须**编译期匹配**——一旦图保存，修改节点类型会破坏现有实例
- `cosntant` 包名是项目历史拼写错误（应为 `constant`），保留以避免破坏引用
- 节点超过 50 个 / 图深度 > 20 时，建议拆成子工作流（暂无强制限制）

参见：[workflow-graph-builder.md](../raw/aiflow-source/workflow-graph-builder.md)。