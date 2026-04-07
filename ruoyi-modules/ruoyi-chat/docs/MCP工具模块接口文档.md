# MCP工具管理模块 - API接口文档

## 概述

本文档描述了MCP工具管理模块的REST API接口，供前端开发人员参考。

## 基础信息

- **Base URL**: `/api/mcp`
- **认证方式**: Bearer Token (SaToken)
- **响应格式**: JSON

---

## 1. MCP工具管理

### 1.1 查询工具列表（分页）

**接口**: `GET /tool/list`

**权限**: `mcp:tool:list`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 否 | 工具名称（模糊查询） |
| description | String | 否 | 工具描述（模糊查询） |
| type | String | 否 | 工具类型：LOCAL/REMOTE/BUILTIN |
| status | String | 否 | 状态：0-启用, 1-禁用 |
| pageNum | Integer | 是 | 页码，默认1 |
| pageSize | Integer | 是 | 每页数量，默认10 |

**响应示例**:
```json
{
  "rows": [
    {
      "id": 1,
      "name": "ReadFileTool",
      "description": "读取文件内容工具",
      "type": "BUILTIN",
      "status": "0",
      "configJson": null,
      "createTime": "2026-03-08 10:00:00",
      "updateTime": "2026-03-08 10:00:00"
    }
  ],
  "total": 1
}
```

### 1.2 查询工具列表（不分页）

**接口**: `GET /tool/all`

**权限**: `mcp:tool:list`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| keyword | String | 否 | 关键词 |
| type | String | 否 | 工具类型 |
| status | String | 否 | 状态 |

**响应示例**:
```json
{
  "tools": [
    {
      "id": 1,
      "name": "ReadFileTool",
      "description": "读取文件内容工具",
      "type": "BUILTIN",
      "status": "0"
    }
  ],
  "total": 1
}
```

### 1.3 获取工具详情

**接口**: `GET /tool/{id}`

**权限**: `mcp:tool:query`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 工具ID |

### 1.4 新增工具

**接口**: `POST /tool`

**权限**: `mcp:tool:add`

**请求体**:
```json
{
  "name": "MyMcpTool",
  "description": "我的MCP工具",
  "type": "REMOTE",
  "status": "0",
  "configJson": "{\"baseUrl\": \"http://localhost:8080/mcp\"}"
}
```

### 1.5 修改工具

**接口**: `PUT /tool`

**权限**: `mcp:tool:edit`

**请求体**: 同新增工具

### 1.6 删除工具

**接口**: `DELETE /tool/{ids}`

**权限**: `mcp:tool:remove`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ids | String | 是 | 工具ID，多个用逗号分隔 |

### 1.7 更新工具状态

**接口**: `PUT /tool/{id}/status`

**权限**: `mcp:tool:edit`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 工具ID |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | String | 是 | 状态：0-启用, 1-禁用 |

### 1.8 测试工具连接

**接口**: `POST /tool/{id}/test`

**权限**: `mcp:tool:query`

**响应示例**:
```json
{
  "success": true,
  "message": "连接测试成功",
  "toolCount": 5,
  "tools": ["tool1", "tool2", "tool3", "tool4", "tool5"]
}
```

---

## 2. MCP市场管理

### 2.1 查询市场列表

**接口**: `GET /market/list`

**权限**: `mcp:market:list`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 否 | 市场名称 |
| description | String | 否 | 市场描述 |
| status | String | 否 | 状态 |
| pageNum | Integer | 是 | 页码 |
| pageSize | Integer | 是 | 每页数量 |

### 2.2 获取市场工具列表

**接口**: `GET /market/{marketId}/tools`

**权限**: `mcp:market:query`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| marketId | Long | 是 | 市场ID |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认10 |

### 2.3 刷新市场工具

**接口**: `POST /market/{marketId}/refresh`

**权限**: `mcp:market:edit`

**响应示例**:
```json
{
  "success": true,
  "message": "刷新成功",
  "addedCount": 3,
  "updatedCount": 5
}
```

### 2.4 加载工具到本地

**接口**: `POST /market/tool/{toolId}/load`

**权限**: `mcp:market:edit`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| toolId | Long | 是 | 市场工具ID |

### 2.5 批量加载工具

**接口**: `POST /market/tools/batchLoad`

**权限**: `mcp:market:edit`

**请求体**:
```json
{
  "toolIds": [1, 2, 3]
}
```

---

## 3. 工具调用日志

### 3.1 查询调用日志

**接口**: `GET /tool/callLog`

**权限**: `mcp:tool:query`

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| toolId | Long | 否 | 工具ID |
| sessionId | Long | 否 | 会话ID |
| startDate | Date | 否 | 开始日期 |
| endDate | Date | 否 | 结束日期 |
| pageNum | Integer | 是 | 页码 |
| pageSize | Integer | 是 | 每页数量 |

### 3.2 获取工具统计

**接口**: `GET /tool/{toolId}/metrics`

**权限**: `mcp:tool:query`

**响应示例**:
```json
{
  "toolId": 1,
  "toolName": "ReadFileTool",
  "today": {
    "callCount": 100,
    "successCount": 95,
    "failureCount": 5,
    "avgDurationMs": 150,
    "successRate": 95.0
  },
  "week": {
    "callCount": 500,
    "successCount": 475,
    "failureCount": 25,
    "avgDurationMs": 160,
    "successRate": 95.0
  }
}
```

---

## 4. 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

---

## 5. 前端页面需求

### 5.1 MCP工具管理页面 (`/mcp/tool`)

**功能**:
- 工具列表展示（分页）
- 工具搜索和筛选
- 新增/编辑/删除工具
- 工具状态切换
- 工具连接测试

**表格列**:
- 工具名称
- 工具描述
- 工具类型（标签显示）
- 状态（开关）
- 创建时间
- 操作（编辑、删除、测试）

### 5.2 MCP市场管理页面 (`/mcp/market`)

**功能**:
- 市场列表展示
- 市场工具浏览
- 刷新市场工具
- 加载工具到本地

### 5.3 工具调用日志页面 (`/mcp/log`)

**功能**:
- 调用日志列表
- 按工具/日期筛选
- 成功率统计
- 响应时间统计

**图表**:
- 每日调用次数趋势图
- 工具调用成功率饼图
- 平均响应时间柱状图
