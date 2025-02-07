# 项目简介

---

## 平台简介

- 本仓库为前端技术栈 [Vue3](https://v3.cn.vuejs.org) + [Element Plus](https://element-plus.org/zh-CN) + [Vite](https://cn.vitejs.dev) 版本。
- 配套后端代码仓库地址
- [RuoYi-Vue-Plus 5.X(注意版本号)](https://gitee.com/dromara/RuoYi-Vue-Plus)
- [RuoYi-Cloud-Plus 2.X(注意版本号)](https://gitee.com/dromara/RuoYi-Cloud-Plus)

## 前端运行

```bash
# 克隆项目
git clone https://gitee.com/JavaLionLi/plus-ui.git

# 安装依赖
npm install --registry=https://registry.npmmirror.com

# 启动服务
npm run dev

# 推荐使用yarn或pnpm包管理工具
# 构建测试环境 yarn build:stage
# 构建生产环境 yarn build:prod
# 前端访问地址 http://localhost:80
```

## 后端改造

参考后端代码内 `ruoyi-gen/resources/vm/vue/v3/readme.txt` 说明

## 内置功能

1. 租户管理：配置系统租户，支持 SaaS 场景下的多租户功能。
2. 用户管理：用户是系统操作者，该功能主要完成系统用户配置。
3. 部门管理：配置系统组织机构（公司、部门、小组），树结构展现支持数据权限。
4. 岗位管理：配置系统用户所属担任职务。
5. 菜单管理：配置系统菜单，操作权限，按钮权限标识等。
6. 角色管理：角色菜单权限分配、设置角色按机构进行数据范围权限划分。
7. 字典管理：对系统中经常使用的一些较为固定的数据进行维护。
8. 参数管理：对系统动态配置常用参数。
9. 通知公告：系统通知公告信息发布维护。
10. 操作日志：系统正常操作日志记录和查询；系统异常信息日志记录和查询。
11. 登录日志：系统登录日志记录查询包含登录异常。
12. 在线用户：当前系统中活跃用户状态监控。
13. 定时任务：在线（添加、修改、删除)任务调度包含执行结果日志。
14. 代码生成：前后端代码的生成（java、html、xml、sql）支持 CRUD 下载 。
15. 系统接口：根据业务代码自动生成相关的 api 接口文档。
16. 服务监控：监视当前系统 CPU、内存、磁盘、堆栈等相关信息。
17. 缓存监控：对系统的缓存信息查询，命令统计等。
18. 在线构建器：拖动表单元素生成相应的 HTML 代码。(TS 版本正在开发中。)
