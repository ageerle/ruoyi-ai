# MCP工具管理前端开发指南

## 前置条件

- Node.js >= 16.0
- Vue 3
- Element Plus
- ECharts (用于图表展示)
- Axios (用于HTTP请求)

## 安装依赖

```bash
npm install element-plus echarts axios
```

## 项目结构

```
ruoyi-ui/
├── src/
│   ├── api/
│   │   └── mcp/
│   │       └── tool.js          # API接口
│   ├── views/
│   │   └── mcp/
│   │       ├── tool/
│   │       │   └── index.vue    # 工具管理页面
│   │       ├── market/
│   │       │   └── index.vue    # 市场管理页面
│   │       └── log/
│   │           └── index.vue    # 调用日志页面
│   └── utils/
│       └── request.js           # Axios封装
```

## 菜单配置

在系统菜单管理中添加以下菜单：

### 1. MCP工具管理

| 字段 | 值 |
|------|-----|
| 菜单名称 | MCP工具管理 |
| 菜单类型 | 目录 |
| 显示顺序 | 1 |
| 路由地址 | mcp |
| 组件路径 | |

#### 子菜单：工具列表

| 字段 | 值 |
|------|-----|
| 菜单名称 | 工具列表 |
| 菜单类型 | 菜单 |
| 显示顺序 | 1 |
| 路由地址 | tool |
| 组件路径 | mcp/tool/index |
| 权限标识 | mcp:tool:list |

#### 子菜单：市场管理

| 字段 | 值 |
|------|-----|
| 菜单名称 | 市场管理 |
| 菜单类型 | 菜单 |
| 显示顺序 | 2 |
| 路由地址 | market |
| 组件路径 | mcp/market/index |
| 权限标识 | mcp:market:list |

#### 子菜单：调用日志

| 字段 | 值 |
|------|-----|
| 菜单名称 | 调用日志 |
| 菜单类型 | 菜单 |
| 显示顺序 | 3 |
| 路由地址 | log |
| 组件路径 | mcp/log/index |
| 权限标识 | mcp:tool:query |

## 权限配置

| 权限标识 | 权限名称 | 说明 |
|----------|----------|------|
| mcp:tool:list | 工具列表 | 查看工具列表 |
| mcp:tool:query | 工具查询 | 查看工具详情 |
| mcp:tool:add | 工具新增 | 新增工具 |
| mcp:tool:edit | 工具修改 | 修改工具 |
| mcp:tool:remove | 工具删除 | 删除工具 |
| mcp:tool:export | 工具导出 | 导出工具数据 |
| mcp:market:list | 市场列表 | 查看市场列表 |
| mcp:market:query | 市场查询 | 查看市场详情 |
| mcp:market:add | 市场新增 | 新增市场 |
| mcp:market:edit | 市场修改 | 修改市场 |
| mcp:market:remove | 市场删除 | 删除市场 |

## 路由配置

在路由配置文件中添加：

```javascript
{
  path: '/mcp',
  component: Layout,
  redirect: '/mcp/tool',
  name: 'Mcp',
  meta: { title: 'MCP工具管理', icon: 'tools' },
  children: [
    {
      path: 'tool',
      name: 'McpTool',
      component: () => import('@/views/mcp/tool/index'),
      meta: { title: '工具列表', icon: 'tool' }
    },
    {
      path: 'market',
      name: 'McpMarket',
      component: () => import('@/views/mcp/market/index'),
      meta: { title: '市场管理', icon: 'shop' }
    },
    {
      path: 'log',
      name: 'McpLog',
      component: () => import('@/views/mcp/log/index'),
      meta: { title: '调用日志', icon: 'document' }
    }
  ]
}
```

## API请求配置

确保Axios请求拦截器正确配置：

```javascript
// src/utils/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 30000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 添加token
    const token = localStorage.getItem('Admin-Token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  error => {
    ElMessage.error(error.message)
    return Promise.reject(error)
  }
)

export default service
```

## 开发步骤

1. **复制代码文件**
   - 将 `tool.js` 复制到 `src/api/mcp/` 目录
   - 将 `*.vue` 文件复制到对应的视图目录

2. **安装依赖**
   ```bash
   npm install element-plus echarts
   ```

3. **配置路由**
   - 在路由配置中添加MCP相关路由

4. **配置菜单**
   - 在系统管理中添加菜单

5. **配置权限**
   - 在系统管理中添加权限标识

6. **测试功能**
   - 启动开发服务器
   - 测试各项功能

## 注意事项

1. **工具类型说明**
   - BUILTIN: 内置工具（系统自带，不可编辑）
   - LOCAL: 本地STDIO工具（通过命令行启动）
   - REMOTE: 远程HTTP工具（通过网络连接）

2. **配置JSON格式**
   - LOCAL类型: `{"command": "npx", "args": ["-y", "@example/tool"], "env": {}}`
   - REMOTE类型: `{"baseUrl": "http://localhost:8080/mcp"}`

3. **错误处理**
   - 工具连接测试可能超时，请合理设置超时时间
   - 删除工具前请确认没有正在运行的Agent使用该工具

4. **性能优化**
   - 调用日志数据量大时，建议使用分页加载
   - 图表数据建议缓存处理，避免频繁请求

## 常见问题

### 1. 跨域问题
在 `vue.config.js` 中配置代理：
```javascript
devServer: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### 2. 图表不显示
确保ECharts容器有固定高度，并在数据加载后初始化图表。

### 3. 权限不生效
检查菜单权限配置和后端接口权限注解是否一致。
