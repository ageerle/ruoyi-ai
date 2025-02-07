# 更新日志
- - -

## v5.2.2 - 2024-08-26

### 重大改动

* 增加 ruoyi-common-sse 模块 支持SSE推送 比ws更轻量更稳定的推送
* 增加 springboot snailjob 等 actuator 账号密码认证 杜绝内外网信息泄漏问题
* 增加 重构代码生成器 集成anyline开源框架 支持400+种数据库适配

### 依赖升级

* update springboot 3.2.6 => 3.2.9
* update snailjob 1.0.1 => 1.1.2
* update mapstruct-plus 1.4.3 => 1.4.4
* update hutool 5.8.27 => 5.8.31 解决hutool不兼容jakarta问题
* update anyline 8.7.2-20240808
* update sms4j 3.2.1 => 3.3.2
* update redisson 3.31.0 => 3.34.1
* update mapstruct-plus 1.3.6 => 1.4.3
* update lombok 1.18.32 => 1.18.34
* update easyexcel 3.3.4 => 4.0.2
* update springdoc 2.5.0 => 2.6.0
* update flowable 7.0.0 => 7.0.1

### 功能更新

* update 优化 去除日志部署环境判断 通过日志级别控制
* update 优化 忽略租户与忽略数据权限支持嵌套使用(感谢 amadeus5201)
* update 优化 租户相关controller 增加租户开关配置控制是否注册
* update 优化 移除 alibaba ttl 与线程池搭配有问题(可传递但无法清除与更新)
* update 优化 个人中心编辑 忽略数据权限
* update 优化 兼容部分用户不想给用户分配角色与部门的场景
* update 优化 租户套餐重名校验
* update 优化 部门下存在岗位不允许删除
* update 优化 角色编辑状态未校验问题
* update 优化 用户脱敏增加编辑权限标识符
* update 优化 代码生成器 自动适配oss翻译
* update 优化 临时升级 undertow 版本 解决虚拟线程溢出问题
* update 优化 支持通过配置文件关闭工作流
* update 优化 增加mybatis-plus填充器兜底策略
* update 优化 TenantSpringCacheManager 处理逻辑
* update 优化 角色权限判断
* update 优化 增加删除标志位常量优化查询代码
* update 优化 监控使用独立web依赖
* update 优化 更多脱敏策略(感谢 hemengji)
* update 优化 设置nginx sse相关代理参数
* update 优化 调整默认推送使用SSE
* update 优化 Monitor监控服务通知分类打印(感谢 AprilWind)
* update 优化 限流注解 又写key又不是表达式的情况
* update 优化 WorkflowUtils查询用户信息发送消息未查询邮件和手机号(感谢 yanzy)
* update 优化 注释掉其他数据库 jdbc 依赖 由用户手动添加
* update 优化 oracle snailjob 兼容低版本oracle索引名称长度限制
* update 优化 数据权限支持通过菜单标识符获取数据所有权
* update 优化 数据权限支持自定义连接符
* update 优化 TestDemo 删除前校验数据权限
* update 优化 更换docker镜像底层系统 避免无字体情况

### 问题修复

* fix 修复 三方登录构建去除无用代码
* fix 修复 多线程对同一个session发送ws消息报错问题
* fix 修复 依赖漏洞 限制部分依赖版本
* fix 修复 excel 基于其他字段 合并错误问题
* fix 修复 一级缓存key未区分租户问题
* fix 修复 id字符串格式转换错误问题
* fix 修复 登出无法正确删除对应的租户数据问题
* fix 修复 登录错误锁定不区分租户问题
* fix 修复 转换模型缺少分类字段
* fix 修复 权限标识符处理未设置成功状态问题
* fix 修复 无法导入 bpmn 类型文件问题

### 前端改动

* update element-plus 2.7.5 => 2.7.8
* update vue 3.4.25 => 3.4.34
* update vite 5.2.10 => 5.2.12
* add 增加 使用 vueuse 编写 sse 推送功能
* update 优化 使用匹配模式简化预编译配置
* update 优化 时间搜索组件统一
* update 优化 oss 配置按钮 使用ossConfig权限标识符与oss权限分离
* update 优化 类型报错问题
* update 优化 切换租户后刷新首页
* update 优化 实现表格行选中切换
* update 优化 使用 vueuse 重构 websocket 实现
* update 优化 代码生成器编辑页禁用缓存 防止同步后页面不更新问题
* update 优化 调整默认推送使用SSE
* fix 修复 租户套餐导出路径错误问题
* fix 修复 登出后重新登录 sse推送报错问题


## v5.2.1 - 2024-07-09

### 功能更新

* update 优化 更改prod环境 snailjob状态 默认启用
* update 优化 替换过期方法
* update 优化 租户列表接口 避免登录之后列表被域名过滤
* update 优化 获取用户账户方法 LoginHelper#getUsername(感谢 AprilWind)
* update 优化 用户ID查询角色列表代码实现(感谢 AprilWind)
* update 优化 大数据量下join卡顿问题 使用子查询提高性能
* update 优化 修改路由name命名规则 防止重复路由覆盖问题(感谢 玲娜贝er)
* update 优化 修改 snailjob 默认端口 避免与系统内置端口冲突问题
* update 优化 isTenantAdmin 空校验
* update 优化 webscoket 配置与异常拦截
* update 优化 更新 redis 密码策略(密码必填 升级需注意)
* update 优化 更新使用 Spring 官方推荐 JDK
* update 优化 StreamUtils 抽取 findFirst findAny 方法
* update 优化 工作流相关代码方法

### 问题修复

* fix 修复 postgres flowable sql 缺失字段问题
* fix 修复 新版上传未设置acl问题
* fix 修复 get路径特殊规则 导致 actuator 泄漏问题 [issue#4f9ceb0a](https://gitee.com/dromara/RuoYi-Vue-Plus/commit/4f9ceb0a8057284a0d9d69da58df630d8bc2e84f)
* fix 修复 pg数据库 用户查询报错问题
* fix 修复 isLogin 方法抛异常无法正常返回值问题

### 前端改动

* update 优化 工作流选人改为懒加载窗口
* update 优化 路由name重复检查
* update 优化 eslint 语法
* update 优化 动态创建组件实例时, 设置路由name为组件名 解决缓存问题
* fix 修复 由于没有await 导致执行顺序不可控
* fix 修复 富文本编辑器 添加之后内容未清理问题

## v5.2.0 - 2024-06-20

### 重大改动

* 集成 flowable 增加工作流相关功能(感谢 May)
* 集成 snailjob 移除 powerjob(投诉的人太多使用成本太高)(感谢 dhb52)
* 升级 aws s3 升级到 2.X 性能大幅提升
* 优化 数据权限 数据加密 使用预扫描mapper注解提升代码性能(感谢 老马)
* 新增 caffeine 减少将近90%的redis查询提高性能

### 依赖升级

* update springboot 3.1.7 => 3.2.6 支持虚拟线程
* update springboot-admin 3.1.8 => 3.2.3
* update mybatis-plus 3.5.4 => 3.5.7 适配更改代码
* update springdoc 2.2.0 => 2.5.0
* update easyexcel 3.3.3 => 3.3.4
* update redisson 3.24.3 => 3.31.0
* update lombok 1.18.30 => 1.18.32
* update sms4j 2.2.0 => 3.2.1 支持自定义配置key 可用于多厂商多租户等
* update satoken 1.37.0 -> 1.38.0
* update hutool 5.8.22 => 5.8.26
* update mapstruct-plus 1.3.5 => 1.3.6
* update lock4j 2.2.5 => 2.2.7
* update dynamic-ds 4.2.0 => 4.3.1

### 功能更新

* update 优化 三方登录不同域名问题 采用新方案
* update 优化 获取aop代理的方式 减少与其他使用aop的功能冲突的概率
* update 优化 token无效时关闭ws连接(感谢 AprilWind)
* update 优化 移除表单构建菜单(没有可用组件 用处不大以后再考虑)
* update 优化 切换动态租户 默认线程内切换(如需全局 手动传参)
* update 优化 代码生成注释，删除无用引入(感谢 AprilWind)
* update 优化 代码生成 el-radio 标签过期属性
* update 优化 异常处理器自动配置
* update 优化 文件下载使用对流下载降低内存使用(感谢 PhoenixL)
* update 优化 去除gc日志参数(有需要自己加)
* update 优化 拆分异常处理器
* update 优化 常规web异常状态码
* update 优化 设置静态资源路径防止所有请求都可以访问静态资源
* update 优化 redis 对Long值的存储类型不同问题
* update 优化 去除加密请求类型限制
* update 优化 mp多租户插件注入逻辑
* update 优化 RedisUtils 支持忽略租户
* update 优化 更新ip地址xdb文件
* update 优化 验证码背景色改为浅灰色
* update 优化 mybatis依赖设置为可选依赖 避免出现不应该注入的情况
* update 优化 GET 方法响应体支持加密
* update 优化 excel插件合并策略 去除被合并单元格的非首行内容(感谢 司猫子)
* update 优化 下拉选接口数据权限
* update 优化 OssFactory 获取实例锁性能
* update 优化 使用翻译注解简化用户查询 调整用户查询逻辑
* update 优化 框架整体提高查询性能
* update 优化 将p6spy配置文件统一放置到 common-mybatis 插件包内

### 新增功能

* add 新增 分布式锁Lock4j异常拦截器
* add 新增 个人中心-在线设备管理
* add 新增 岗位编码与部门编码并将岗位调整到部门下(感谢 AprilWind)
* add 新增 BaseMapperPlus提供可选是否抛异常selectVoOne方法(感谢 秋辞未寒)
* add 新增 用户、部门、角色、岗位 下拉选接口与代码实现优化
* add 增加 StringUtils.isVirtual 方法
* add 增加 JustAuth 整合 TopIam 单点登录

### 问题修复

* fix 修复 websocket clientid 参数不走mvc拦截器 无法生效问题
* fix 修复 oss未使用租户 拼接租户id null问题
* fix 修复 用户昵称修改后未清除对应缓存问题(感谢 zhuweitung)
* fix 修复 图片预览问题(感谢 AprilWind)
* fix 修复 三方账号可以绑定多平台账号问题
* fix 修复 主建错别字(感谢 good)
* fix 修复 兼容redis5.0出现的问题
* fix 修复 部分浏览器无法获取加密响应头问题
* fix 修复 用户未设置部门 登录报错问题
* fix 修复 excel 表达式字典 下拉框导出格式错误
* fix 修复 提升锁的作用域 并采用双重校验锁(感谢 fanc)
* fix 修复 用户登录查询部门缓存无法获取租户id问题
* fix 修复 关闭租户功能 三方登录报错问题


### 前端改动

* update element-plus 2.7.5
* update vite 5.2.10
* update vue 3.4.25
* update vue-router 4.3.2
* update nodejs 升级到最低 18.18.0
* update 优化 跟密码相关的默认前端关闭防重功能
* update 优化 点击左边菜单时页面空白或者刷新整个页面的问题
* update 优化 el-select 与 el-input 全局样式
* update 优化 首页打开topNav不展开菜单问题
* update 优化 支持全局开启或关闭接口加密功能
* update 优化 密码校验策略增加非法字符限制
* update 优化 图片上传组件增加压缩功能支持 可自行开关(感谢 fengheguai)
* update 优化 request请求类判断请求头方式
* update 优化 更改客户端状态接口 使用clientId传参
* update 优化 ws开关改为常开(vite5修复了崩溃bug)
* fix 修复 移动端下 无法展开菜单问题
* fix 修复 面板因为min width原因收缩不全
* fix 修复 文件预览大写后缀不展示的问题(感谢 北桥)
* fix 修复 i18n无感刷新问题
* fix 修复 websocket 非index页面刷新无法重连问题

## v5.1.2 - 2023-12-22

### 依赖升级

* update springboot 3.1.5 => 3.1.7
* update mybatis-boot 3.0.2 => 3.0.3 优化依赖传递
* update powerjob 4.3.3 => 4.3.6
* update easyexcel 3.3.2 => 3.3.3
* update transmittable-thread-local 2.14.2 => 2.14.4
* update justauth 1.16.5 => 1.16.6
* update redisson 3.24.1 => 3.24.3 修复订阅重启连接超时问题

### 功能更新

* update 优化 为 admin 模块 单独增加 ratelimiter 模块
* update 优化 验证码接口 增加限流配置
* update 优化 excel合并注解会根据第一合并列的结果来决定后续的列合并 (感谢 Simple)
* update 优化 SocialUtils 代码
* update 优化 删除无用异常类
* update 优化 补全三方登录校验国际化
* update 优化 sms组件 预留自动配置类
* update 更新 关于数据库的说明
* update 优化 sms组件 预留自动配置类
* update 优化 将 OSS配置 改为全局模式 降低使用难度 保留sql便于用户自行扩展(常规项目用不上配置分多租户)
* update 优化 细化oss配置管理权限控制
* update 优化 开启 redisson 脚本缓存 减少网络传输
* update 优化 删除 hikaricp 官方不推荐使用的配置 jdbc4 协议自带校验方法
* update 优化 减少 PlusSaTokenDao 不必要的查询优化性能
* update 优化 更新用户异常提示 使用登录账号
* update 优化 使用登录用户判断是否登录 提高效率
* update 优化 重构 LoginHelper 将本地存储代码操作封装
* update 优化 getTenantId 判断是否开启多租户
* update 优化 Dockerfile 使用shell模式 支持环境变量传入jvm参数
* update 优化 WebSocketUtils 连接关闭改为警告
* update 优化 excel多sheet页导出 (感谢 May)
* update 优化 删除无用接口实现
* update 优化 jvm参数调整 全面启用zgc
* update 优化 使用动态租户重构业务对租户的逻辑
* update 优化 TenantHelper 动态租户支持函数式方法
* update 优化 支持多租户绑定相同的三方登录
* update 优化 更新用户登录信息方法忽略数据权限
* update 优化 补全三方绑定时间字段 删除无用excel注解
* update 优化 将登录记录抽取到监听器统一处理
* update 优化 租户插件 ignoreTable 方法支持动态租户

### 新增功能

* add 新增 RedisUtils.setObjectIfExists 如果存在则设置方法
* add 新增 丰富RedisUtils对List Set类型的操作
* add 新增 翻译组件 用户昵称翻译实现
* add 新增 响应加密功能 支持注解强制加密接口数据 (感谢 MichelleChung)

### 问题修复

* fix 修复 selectDictTypeByType 查询方法错误问题
* fix 修复 一些不正常类无法加载报错问题
* fix 修复 powerjob sql脚本针对其他数据库转义符问题 (感谢 branches)
* fix 修复 MybatisSystemException 空指针问题
* fix 修复 excel合并注解会根据第一合并列的结果来决定后续的列合并
* fix 修复 session 多账号共用覆盖问题 改为 tokenSession 独立存储
* fix 修复 token 失效后 登录获取用户null问题
* fix 修复 powerjob部署方案 高版本nginx不生效问题
* fix 修复 OssFactory 并发多创建实例问题
* fix 修复 延迟队列在投递消息未到达时间的时候 服务死机导致重启收不到消息

### 前端改动

* update 优化 用户头像 img 变量无确定类型问题
* update 优化 细化oss配置管理权限控制
* update 优化 明确打包命令
* update 优化 代码中存在的警告
* update 优化 前端白名单页面放行逻辑
* update 优化 页面关于权限标识符说明
* fix 修复 append-to-body 编写错误 (感谢 Ai3_刘小龙)
* fix 关闭动态路由tab页签时不清理组件缓存 (感谢 NickLuo)
* fix 删除重复环境变量ElUploadInstance (感谢 棉花)
* fix 修复 在线用户 强推按钮点击取消控制台警告问题
* fix 修复 字典使用 default 样式报警告问题

## v4.8.2 - 2023-11-27

### 依赖升级

* update springboot 2.7.17 => 2.7.18 升级到2.X最终版本(官方停更)
* update mybatis-plus 3.5.3.2 => 3.5.4
* update springboot 2.7.14 => 2.7.17
* update satoken 1.36.0 => 1.37.0
* update aws-java-sdk-s3 1.12.400 => 1.12.540
* update vue-quill 1.1.0 => 1.2.0

### 功能更新

* update 优化 页面关于权限标识符说明
* update 优化 数据权限拦截器优先判断方法是否有效 提高性能减少无用sql解析
* update 优化 部门数据权限使用默认兜底方案
* update 优化 更改默认日志等级为info 避免日志过多(按需开启debug)
* update 优化 补全代码生成 columnList 接口参数注解缺失
* update 优化 操作日志 部门信息完善 vue3页面
* update 优化 AddressUtils 兼容linux系统本地ip
* update 优化 操作日志 部门信息完善 (感谢 柏竹)
* update 优化 数据权限 减少二次校验查询
* update 优化 vue3 版本用户初始密码从字典查询
* update 优化 富文本Editor组件检验图片格式
* update 优化 操作日志列表新增IP地址查询
* update 优化 全局数据存储用户编号
* update 优化 菜单管理类型为按钮状态可选

### 问题修复

* fix 修复 OssFactory 并发多创建实例问题
* fix 修复 demo的form字段有误 (感谢 dhb52)
* fix 修复 延迟队列在投递消息未到达时间的时候 服务死机导致重启收不到消息
* fix 修复 数据权限优化后 update delete 报null问题
* fix 修复 五级路由缓存无效问题
* fix 修复 oss服务无法连接 导致业务异常问题 查询不应该影响业务
* fix 修复 内链iframe没有传递参数问题
* fix 修复 外链带端口出现的异常
* fix 修复 普通角色编辑使用内置管理员code越权问题
* fix 修复 代码生成 是否必填与数据库不匹配问题
* fix 修复 HeaderSearch组件跳转query参数丢失问题
* fix 修复 树结构代码生成新增方法赋值错误 (感谢 这夏天依然平凡)

## v5.1.1 - 2023-11-14

### 依赖升级

* update springboot 3.1.3 => 3.1.5
* update springboot 2.7.14 => 2.7.17(扩展服务)
* update springboot-admin 3.1.5 => 3.1.7
* update satoken 1.35.0.RC => 1.37.0
* update mybatis-plus 3.5.3.2 => 3.5.4 适配mp新版本改动
* update dynamic-ds 4.1.3 => 4.2.0
* update bouncycastle 1.72 => 1.76
* update poi 5.2.3 => 5.2.4
* update redisson 3.23.2 => 3.24.1
* update hutool 5.8.20 => 5.8.22
* update lombok 1.18.26 => 1.18.30(适配支持jdk21)
* update vue-quill 1.1.0 => 1.2.0

### 功能更新

* update 优化 数据权限拦截器优先判断方法是否有效 提高性能减少无用sql解析
* update 优化 适配 maxkey 新版本
* update 优化 @Sensitive脱敏增加角色和权限校验 (感谢 盘古给你一斧)
* update 优化 部门数据权限使用默认兜底方案
* update 优化 更改默认日志等级为info 避免日志过多(按需开启debug)
* update 优化 登录策略代码优化(感谢 David Wei)
* update 优化 补全代码生成 columnList 接口参数注解缺失
* update 优化 nginx 配置支持 websocket
* update 优化 notice 新增通知公告发送ws推送
* update 优化 websocket 模块减少日志输出 增加登录推送
* update 优化 重构登录策略增加扩展性降低复杂度
* update 优化 addressUtils 兼容linux系统本地ip
* update 优化 补全操作日志部门数据
* update 优化 支持数据库操作在非web环境下切换租户
* update 优化 排除powerjob无用的依赖 减少打包30M体积
* update 优化 删除 satoken yml 时间配置 此功能已迁移至客户端管理
* update 优化 redis 集群模式注释说明
* update 优化 客户端禁用限制
* update 优化 登录日志, 在线用户展示信息(增加 客户端, 设备类型)(感谢 MichelleChung)
* update 优化 限制框架中的fastjson版本
* update 优化 数据权限 减少二次校验查询
* update 优化 将部门id存入token避免过度查询redis
* update 优化 增加租户ID为Null错误日志
* update 优化 操作日志列表新增IP地址查询
* update 优化 通过参数键名获取键值接口的返回体(感谢 David Wei)
* update 优化 为 sys_grant_type 字典增加样式
* update 优化 代码生成 页面输入框样式
* update 优化 全业务分页查询增加排序规则避免因where条件导致乱序问题
* update 优化 登录接口租户id被强制校验问题
* update 优化 加密模块 支持父类统一使用加密注解(感谢 Tyler Ge)
* update 优化 将graalvm镜像更新为openjdk镜像 需要的人自行切换即可
* update 优化 部分使用者乱设权限导致无法获取用户信息 增加权限提示
* update 优化 表格列的显示与隐藏小组件(感谢 bestrevens)
* update 优化 增加表单构建不能使用说明
* update 优化 富文本Editor组件检验图片格式
* update 优化 操作日志列表新增IP地址查询
* update 优化 菜单管理类型为按钮状态可选
* update 优化 用户初始密码从参数配置查询
* update 优化 通过参数键名获取键值接口的返回体(感谢 David Wei)
* update 优化 字典标签支持数组和多标签(感谢 抓蛙师)

### 新增功能

* add 新增 websocket 群发功能
* add 新增 前端接入websocket接收消息(感谢 三个三)

### 问题修复

* fix 修复 oss服务无法连接 导致业务异常问题 查询不应该影响业务
* fix 修复 租户id为null 无法匹配字符串导致的嵌套key问题
* fix 修复 部门管理orderNum排序失效问题
* fix 修复 外链带端口出现的异常
* fix 修复 普通角色编辑使用内置管理员code越权问题
* fix 修复 代码生成 是否必填与数据库不匹配问题
* fix 修复 用户注册接口校验用户名不区分租户问题
* fix 修复 错误增加组导致的校验不生效问题
* fix 修复 新增校验主键id问题
* fix 修复 powerjob 使用 nginx 部署无法访问的问题
* fix 修复 SysUserMapper 内标签使用错误(不影响使用)
* fix 修复 新增或编辑 SysOssConfig 数据后 推送到 redis 数据不完整
* fix 修复 树表生成查询变量使用错误
* fix 修复 个人信息修改密码接口隐藏新旧密码参数明文(感谢 bleachtred)
* fix 修复 删除字段后 * update sql 未更新问题
* fix 修复 三方登录支付宝source与实际支付宝业务code不匹配问题
* fix 修复 五级路由缓存无效问题
* fix 修复 内链iframe没有传递参数问题
* fix 修复 绑定第三方帐号参数“wechar”更正为“wechat” (感谢 scmiot)
* fix 修复 用户注册缺失 clientid 问题
* fix 修复 HeaderSearch组件跳转query参数丢失问题
* fix 修复 自定义字典样式不生效的问题
* fix 修复 login 页面 loading 未关闭问题

## v4.8.1 - 2023-09-25

### 依赖升级

* update springboot 2.7.15 => 2.7.16
* update springboot-admin 2.7.10 => 2.7.11
* update satoken 1.35.0.RC => 1.36.0
* update lombok 1.18.26 =. 1.18.30
* update mybatis-plus 3.5.3.1 => 3.5.3.2
* update easyexcel 3.3.1 => 3.3.2
* update hutool 5.8.18 => 5.8.20

### 功能更新

* update 优化 重置密码注释参数中文解释错误
* update 优化 getTokenActivityTimeout => getTokenActiveTimeout
* update 优化字典标签支持传分隔符分隔的字符串和数组，优化渲染效果
* update 优化 控制台debuger位置错误问题
* update 优化 TopNav 菜单样式
* update 优化 全局异常处理器 业务异常不输出具体堆栈信息 减少无用日志存储
* update 优化 用户管理 只查询未禁用的部门角色岗位数据
* update 优化 岗位如果绑定了用户则不允许禁用
* update 优化 部门与角色如果绑定了用户则不允许禁用
* update 优化 加密实现 使用 EncryptUtils 统一处理
* update 优化 excel导出字典转下拉框 无需标记index自动处理
* update 优化 excel 导出字典默认转为下拉框
* update  优化 删除一些跟 swagger 有关的字眼 避免误解
* update 优化 角色权限支持仅本人权限查看 解决无法查看自己创建的角色问题
* update 优化 RedisCacheController 注释错误
* update 优化 xxljob 端口随着主应用端口飘逸 避免集群冲突
* update 优化 powerjob 端口随着主应用端口飘逸 避免集群冲突

### 问题修复

* fix 修复 代码生成后 vo 定义 'serialVersionUID' 字段的不可序列化类
* fix 修复 自定义字典样式不生效的问题
* fix 修复 布局配置失效问题
* fix 修复 新建用户可能会存在的越权行为
* fix 修复 字典缓存删除方法参数错误问题
* fix 修复 修复树模板父级编码变量错误
* fix 修复 有界队列与优先队列 错误使用问题
* fix 修复 升级 mp 版本导致的问题
* fix 修复 vue3 版本注册页验证码不显示问题
* fix 修复 加密模块数据转换异常问题
* fix 修复 动态设置 token 有效期不生效问题
* fix 修复 token 过期登出无法清理在线用户问题


## v5.1.0 - 2023-09-05

# 开发历程

* 2023年5月 开始 5.1.0 计划 历经1个月的设计与讨论
* 2023年6月 开始着手开发 历经2个多月的开发 特别感谢团队的小伙伴与一些热心的粉丝 参与功能开发与测试
* 2023年8月 开始公测 历经将近1个月的公测与修复工作(期间成功支持多位使用者生产使用)
* 2023年9月初 正式发布(经过多个小伙伴的生产实践 已基本可尝试生产使用)
> 关于4.X的说明 由于SpringBoot2.X与vue2.X均在11月底停止维护<br>
> 故而咱们vue版本4.X也无法再继续更新<br>
> 介于4.X的用户量特别庞大 功能也非常的稳定<br>
> 计划于11月底同Boot2.X一同停止更新但还会持续维护修复bug(修复的形式为直接提交到4.X分支停止发版)<br>

# 视频介绍

为了更好的让大家了解 5.1.0 作者录制了相关的视频 供大家快速了解上手

* 5.1.0 新功能与变更介绍: https://www.bilibili.com/video/BV1fj411y71X/
* 搭建与运行: https://www.bilibili.com/video/BV1Fg4y137JK/
* 生产环境搭建部署: https://www.bilibili.com/video/BV1mL411e7ha/

# 更新日志

### 重大更新

* [重大更新] 优化 相关代码 完成代码生成多数据源统一存储(感谢 WangBQ !pr349)
* [不兼容更新] 移除 原短信功能 集成更强大的 sms4j 短信工具包(感谢 友杰 !pr367)
* [不兼容更新] 对接 powerjob 实现分布式任务调度 删除原有 xxljob 原因为社区不更新功能太少只支持mysql(感谢 yhan219 !pr359)
* [重大更新] 新增 三方授权绑定登录功能 基于 justauth 支持市面上大部分三方登录(感谢 三个三 !pr370)
* [不兼容更新] 新增 客户端授权功能 不需要更改任何代码即可完成多端动态对接(感谢 Michelle.Chung !pr379)
* [重大更新] 新增 前后端接口请求加密传输 基于AES+RSA动态高强度加密(感谢 wdhcr !pr377)
* [重大更新] 新增 三方授权登录 对接 maxkey 单点登录
* [不兼容更新] 优化 redis序列化配置 更改为通用格式(升级需清除redis所有数据)

### 依赖升级

* update springboot 3.0.7 => 3.1.3
* update springboot-admin 3.1.3 => 3.1.5
* update springdoc 2.1.0 => 2.2.0
* update spring-mybatis 3.0.1 => 3.0.2
* update mybatis-plus 3.5.3.1 => 3.5.3.2
* update easyexcel 3.2.1 => 3.3.2
* update mapstruct-plus 1.2.3 => 1.3.5 解决修改实体类 idea不编译问题
* update satoken 1.34.0 => 1.35.0.RC 优化过期配置 支持多端token自定义有效期
* update dynamic-ds 3.6.1 => 4.1.3 支持 SpringBoot3
* update sms4j 2.2.0
* update hutool 5.8.18 => 5.8.20
* update redisson 3.20.1 => 3.23.4
* update lock4j 2.2.4 => 2.2.5
* update aws-java-sdk-s3 1.12.400 => 1.12.540
* update maven-surefire-plugin 3.0.0 => 3.1.2

### 功能更新

* update 优化 excel 导出合并 在初始化类时进行数据的处理
* update 优化 简化 flatten 插件语法写法
* update 优化 支持本地虚拟域名调试(感谢 代星登 !pr363)
* update 重构 将框架内的 swagger 命名更改为 springdoc 命名避免误解
* update 重构 将系统内置配置放置到 common 包内独立加载 不允许用户随意修改
* update 优化 切换 maven 仓库到 华为云(aliyun依赖不更新拉取不到)
* update 优化 升级 satoken 支持多端 token 自定义有效期功能
* update 优化 RepeatSubmitAspect 逻辑避免并发请求问题
* update 优化 在全局异常拦截器中增加两类异常处理
* update 优化 提供powerjob完整sql脚本 降低用户使用难度
* update 优化 StreamUtils 其他方法过滤null值(感谢 bleachtred !pr390)
* update 优化 powerjob 端口随着主应用端口飘逸 避免集群冲突
* update 优化 角色权限支持仅本人权限查看 解决无法查看自己创建的角色问题
* update 修改代码生成模版，日期范围统一采用addDateRange方法(感谢 LiuHao !pr397)
* update 优化 树表生成前端缺少 children 字段
* update 优化 CryptoFilter null判断工具
* update 优化 websocket 路径与 cloud 版本保持一致
* update 优化 更新登录策略返回值(感谢 zlyx)
* update 修改代码生成模板，调整列表打开对话框和接口请求顺序
* update 优化 SaInterceptor 拦截器判断 token 客户端id是否有效(感谢 zlyx !pr402)
* update 优化 excel 导出字典默认转为下拉框
* update 优化 兼容 clientid 通过 param 传输
* update 优化 excel导出字典转下拉框 无需标记index自动处理(感谢 一夏coco)
* update 优化 简化线程池配置
* update 优化 屏蔽 powerjob 无用的心跳日志
* update 优化 适配 mysql 8.0.34 升级连接机制
* update 优化 加密实现 使用 EncryptUtils 统一处理
* update 优化 删除字典无用状态字段(基本用不上 禁用后还会导致回显问题)
* update 优化 部门与角色如果绑定了用户则不允许禁用
* update 优化 岗位如果绑定了用户则不允许禁用
* update 优化 用户管理 只查询未禁用的部门角色岗位数据
* update 优化 登录用户增加昵称返回
* update 优化 将部门管理 负责人选项改为下拉框选择(感谢 Lionel !pr410)
* update 优化 全局异常处理器 业务异常不输出具体堆栈信息 减少无用日志存储
* update 优化 登录用户缓存 去除冗余统一存储
* update 优化 放宽菜单权限 角色关联菜单无需管理员

### 新增功能

* add 增加 RedisUtils 批量删除 hash key 方法
* add 新增 Oss 上传 File 文件方法(感谢 jenn !pr362)
* add 增加 excel 导出下拉框功能
* add 新增 RedisUtils.setObjectIfAbsent 如果不存在则设置方法

### 修复问题

* fix 修复 脱敏注解标记位置错误
* fix 修复 OssClient 实例多租户相同key缓存覆盖问题
* fix 修复 关闭多租户 脱敏判断问题
* fix 修复 OssClient 切换服务 实例不正确问题(感谢 jenn !pr360)
* fix 修复 传参类型不正确导致 postgreSql 同步套餐报错问题
* fix 修复 参数类型修改 未修改校验注解
* fix 修复 登录校验错误次数未达到上限时 错误次数缓存未设置有效时间问题(感谢 konbai !pr366)
* fix 修复 common-core 包使用aop注解 但未添加aop实现类导致单独使用报错问题
* fix 修复 Mapper 多参数未加 @Param 注解问题
* fix 修复 邮箱登录 查询值错误问题
* fix 修复 用户篡改管理员角色标识符越权问题
* fix 修复 字典缓存注解使用错误问题
* fix 修复 查询部门下拉树未过滤数据权限问题
* fix 修复 CacheName 缓存key存储错误问题
* fix 修复 代码生成 前端添加或修改表单修改列生成问题
* fix 修复 新增角色使用内置管理员标识符问题
* fix 修复 代码生成 前端添加或修改表单修改列生成问题
* fix 修复 token 过期登出无法清理在线用户问题
* fix 修复 加密模块数据转换异常问题
* fix 修复 可能导致异常类无法反序列化问题
* fix 修复 代码生成 编辑按钮刷新列表问题
* fix 修复 客户端编辑时授权类型变更未保存的问题(感谢 David Wei !pr400)
* fix 修复 有界队列与优先队列 错误使用问题
* fix 修复 修复树模板父级编码变量错误
* fix 修复 部署部分系统出现乱码问题
* fix 修复 一级菜单无法显示问题
* fix 修复 可能会存在的越权行为(感谢 丶Stone !pr416)
* fix 修复 代码生成页面参数缺少逗号问题

### 移除功能

* remove 移除原有短信功能(建议使用sms4j)
* remove 移除xxljob功能(建议使用powerjob)


## v4.8.0 - 2023-07-10

### 重大更新

* [重大更新] 新增 sms4j 短信融合框架整合(支持数十种短信厂商接入、发送限制、负载均衡等功能)
* [不兼容更新] 移除 原短信功能(建议使用新 sms4j 功能)
* [重要迁移] 迁移 vue3 前端到主仓库统一维护

### 依赖升级

* update springboot 2.7.11 => 2.7.13
* update satoken 1.34.0 => 1.35.0.RC
* update easyexcel 3.2.1 => 3.3.1
* update sms4j 2.2.0

### 功能更新

* update 优化 StreamUtils 方法过滤null值
* update 优化 页签在Firefox浏览器被遮挡
* update 优化 在全局异常拦截器中增加两类异常处理
* update 优化 下载zip方法增加遮罩层(感谢@梁剑锋)
* update 优化 用户昵称非空校验
* update 优化 修改角色如果未绑定用户则无需清理
* update 优化 RepeatSubmitAspect 逻辑避免并发请求问题
* update 优化 satoken 过期配置 支持多端token自定义有效期
* update 优化 加密注解注释错误
* update 优化 切换 maven 仓库到华为云(aliyun 不可用)
* update 优化 excel 导出存在合并项时在初始化类时进行数据的处理避免多次调用(感谢@yueye)
* update 优化 重构 CellMergeStrategy 支持多级表头修复一些小问题 整理代码结构

### 新增功能

* add 新增 RedisUtils.setObjectIfAbsent 不存在则设置方法
* add 新增 Excel 导出附带有下拉框(字典自动导出为下拉框) 可自定义多级下拉框(感谢@Emil.Zhang)
* add 新增 OssClient File 文件上传方法
* add 增加 RedisUtils 批量删除 hash key 方法

### 问题修复

* fix 修复 新增角色使用内置管理员标识符问题
* fix 修复 缓存监控图表 支持跟随屏幕大小自适应调整(感谢@抓蛙师)
* fix 修复 防重组件 错删注解问题
* fix 修复 CacheName 缓存key存储错误问题
* fix 修复 字典缓存注解使用错误问题
* fix 修复 用户篡改管理员角色标识符越权问题
* fix 修复 登录校验错误次数未达到上限时 错误次数缓存未设置有效时间问题
* fix 修复 OssClient 切换服务 实例不正确问题
* fix 修复 element ui 因版本而未被工具识别问题(感谢@梁剑锋)
* fix 修复 admin监控 切换tab页需要重复登录问题

## v5.0.0 - 2023-05-19

# 开发历程

* 2022年11月 开始5.X计划 历经2个月的设计与讨论
* 2023年1月 开始着手开发 历经3个月的开发 特别感谢团队的小伙伴与一些热心的粉丝 参与功能开发与测试
* 2023年4月 开始公测 历经将近2个月的公测与修复工作(期间成功支持多位使用者生产使用)
* 2023年5月底 正式发布 虽然已经有生产实践 但是springboot3.0与jdk17使用者还处于少数 另外5.X后续还有一些不兼容更新 求稳者建议在等一等
* 关于4.X的说明 由于springboot2.X 与 vue2.X 匀在年底停止维护 故此4.X也将于年底同boot2一同停止维护

# 视频介绍

为了更好的让大家了解 5.X 作者录制了相关的视频 供大家快速了解上手

* 搭建与运行: https://www.bilibili.com/video/BV1Fg4y137JK/
* 新功能与变更介绍: https://www.bilibili.com/video/BV1Us4y1m7ky/
* 生产环境搭建部署: https://www.bilibili.com/video/BV1mL411e7ha/

# 更新日志

### 重大更新

* [不兼容升级] java 版本从 jdk 8 升级到 jdk 17 且需要使用 graalvm 运行(暂时未解决原生jdk存在的问题)
* [不兼容升级] springboot 升级 3.0 版本
* [不兼容升级] 重构 项目模块结构 采用插件化结构 易扩展易解耦
* [不兼容升级] com.sun.mail 更改为 jakarta.mail 修改最新写法
* [不兼容升级] javax.servlet 替换为 jakarta.servlet 更新所有代码
* [简化性升级] 默认开启复杂结构 resultMap 自动映射 简化xml编码(多结构实体需带上主键id) 
* [数据库改动] 更新 create_by update_by 字段类型 (保存用户id)
* [数据库改动] 新增 create_dept 字段 (保存创建部门id)
* [不兼容更新] system 模块 所有实体类均使用 bo|vo 规范化
* [重大更新] 新增 多租户功能设计 整体框架代码结构与数据库更改
* [重大更新] 新增 mapstruct-plus 替换 BeanUtil 与 BeanCopyUtils 工具
* [不兼容更新] 重构 登录注解接口与cloud版本统一接口路径
* [不兼容更新] 重构 BaseMapperPlus接口 去除 `@param <M> Mapper` 泛型
* [不兼容更新] 移除 vue2 前端工程 全面启用 vue3
* [重大更新] 新增 vue3 + TS 版本前端(独立仓库后续与Cloud版本共用)
* [重大更新] 增加 websocket 模块 支持token鉴权 支持分布式集群消息同步
* [重大更新] 框架文档全面翻新 https://plus-doc.dromara.org

### 依赖升级

* update java 1.8 => 17
* update springboot 2.7.7 => 3.0.7
* update springboot-admin 2.7.10 => 3.0.4
* update springdoc 1.6.14 => 2.1.0
* update lock4j 2.2.3 => 2.2.4
* update dynamic-ds 3.5.2 => 3.6.1
* update easyexcel 3.1.5 => 3.2.1
* update hutool 5.8.11 => 5.8.18
* update redisson 3.19.2 => 3.20.1
* update lombok 1.18.24 => 1.18.26
* update spring-boot.mybatis 2.2.2 => 3.0.1
* update mapstruct-plus 1.2.3
* update maven-compiler-plugin 3.10.1 => 3.11.0
* update maven-surefire-plugin 3.0.0-M7 => 3.0.0
* update docker mysql 8.0.31 => 8.0.33
* update docker nginx 1.22.1 => 1.32.4
* update docker redis 6.2.7 => 6.2.12
* update docker minio RELEASE.2023-04-13T03-08-07Z

### 功能更新

* update 适配 AsyncConfig 替换过期继承类改为实现 AsyncConfigurer 接口
* update 适配 redis 新版本配置文件写法
* update 适配 获取redis 监控参数接口 替换过期语法
* update 适配 sa-token 替换新依赖 sa-token-spring-boot3-starter
* update 适配 springboot-admin 改为最新 spring-security 写法
* update 适配 springdoc 新版本配置方式
* update 适配 ServletUtils 更换继承 JakartaServletUtil
* update 适配 新序列化注解
* update 优化 利用 resultMap 自动映射配置 简化 xml (非嵌套)
* update 优化 调整 system entity 实体与 controller 包结构
* update 优化 实体类中校验注解的提示信息
* update 优化 使用 jdk17 语法优化代码
* update 优化 所有 properties 文件改为注解启用
* update 更新 docker 基础镜像 graalvm java17
* update 优化 用户头像 改为存储 ossId 使用转换模块转为 url 展示
* update 优化 重构 CellMergeStrategy 支持多级表头修复一些小问题 整理代码结构
* update 优化 登录流程代码注释

### 新增功能

* add 新增 flatten-maven-plugin 插件统一版本号管理
* add 新增 ip2region 实现离线IP地址定位库

### 移除功能

* remove 移除 BeanCopyUtils 工具类 与 JDK17 不兼容
* remove 移除 devtools 依赖 并不好用(建议直接用idea自带的热更)
* remove 移除 vue2 前端工程 统一使用 vue3 工程

## v4.7.0 - 2023-05-08

### 依赖升级

* update springboot 2.7.9 => 2.7.11 修复 DoS 漏洞
* update xxljob 2.3.1 => 2.4.0
* update minio 升级至最新版 避免低版本信息泄漏问题
* update hutool 5.8.15 => 5.8.18
* update redisson 3.20.0 => 3.20.1

### 功能更新

* update 优化 更改 sys_oss_config 表注释 避免误解
* update 项目正式入驻 dromara 开源社区 更改项目地址
* update 全新 logo 全新背景图(设计师打造)
* update 优化代码生成 同步操作使用批量处理
* update 重写项目 readme 说明
* update 修改controller中校验直接返回R.fail
* update 更换默认用户头像
* update 优化 限流注解 key 支持简单 spel 表达式
* update 优化弹窗后导航栏偏移的问题
* update 优化$tab.closePage后存在非首页页签时不应该跳转首页
* update delete build style
* update 优化选择图标组件
* update 移除vue-multiselect样式
* update 优化固定头部页签滚动条被隐藏的问题
* update 按代码规范补全重写注解
* update 优化 极端情况获取LoginUser可能为null问题
* update 优化 更改系统所有服务日志配置文件命名为 logback-plus.xml 避免与其他框架默认配置冲突
* update 优化 加解密模块 将null判断下推防止任何可能的null出现
* update 优化 调整配置文件错误注释
* update 优化 在线用户token获取方式
* update 优化 用户更改角色 踢掉角色相关所有在线用户
* update 优化 下拉图标选择组件优化：1.已选择图标高亮回显 2.滚动条采用el-scrollbar
* update 优化 Vue的DictTag组件 当value没有匹配的值时 展示空value
* update 优化 恢复翻页/切换路由滚动功能

### 新增功能

* add 新增 ip2region 实现离线IP地址定位库
* add 增加 邮箱验证码发送接口
* add 增加 邮箱登陆接口
* add 增加 EncryptUtils 加解密安全工具类 可以处理base64,aes,sm4,sm2,rsa,md5,sha256加解密
* add 增加 EncryptUtils 类中增加国密sm3的不可逆加密算法
* add 新增 忽略数据权限写法 防止异常不执行关闭问题

### 问题修复

* fix 修复 代码生成 点选按钮不生效问题
* fix 修复 用户密码更新无效问题
* fix 修复 findInSet 在mysql下方法搜索非数字字段时 无引号报错问题
* fix 修复 oracle postgres 数据库日志表索引创建错误
* fix 角色列表关联多表sort值都一样 导致排序不稳定、临时表没有原来的主键顺序
* fix 修复 DefaultExcelResult 单词拼写错误
* fix 修复页面切换时布局错乱的问题
* fix 修复tab栏“关闭其他”异常的问题
* fix 修复 加解密拦截器 对象属性为null问题
* fix 修复 取消oss预览状态修改 图标变化不正常问题
* fix 修复 开启TopNav后一级菜单路由参数设置无效问题
* fix 修复 路由跳转被阻止时vue-router内部产生报错信息问题
* fix 修复 缓存列表：多次清除操作，提示不变的问题

## v4.6.0 - 2023-03-13

### 重大更新

[重大更新] add 新增 基于 Mybatis 实现数据库字段加解密功能
[重大更新] add 新增 通用翻译注解及实现(部门名、字典、oss、用户名)

### 依赖升级

* update springboot 2.7.7 => 2.7.9
* update easyexcel 3.1.5 => 3.2.1
* update redisson 3.19.1 => 3.20.0
* update hutool 5.8.11 => 5.8.15 (13与14有问题勿使用)
* update springdoc 1.6.14 => 1.6.15
* update aws-java-sdk-s3 1.12.373 => 1.12.400
* update element-ui 2.15.10 => 2.15.12
* update lombok 1.18.24 => 1.18.26

### 功能更新

* update 优化 实体类中校验注解的提示信息
* update 优化 修改 oss 配置页面开关说明 避免造成误解
* update 优化 框架代码书写格式
* update 优化 调整连接池默认参数
* update 优化 `DictDataMapper` 注解标注过期 推荐使用 `@Translation` 注解
* update 优化 部门更新接口 清理缓存
* update 优化 获取菜单数据权限接口 删除无用角色属性与逻辑
* update 优化 调整连接池最长生命周期 防止出现警告
* update 优化 连接池增加 `keepaliveTime` 探活参数
* update 优化 `DataPermissionHelper` 增加 `开启/关闭` 忽略数据权限功能
* update 重构 `OssFactory` 加载方式 改为每次比对配置做实例更新
* update 优化 `SaToken` 自定义扩展类 改为配置类注入 便于扩展
* update 优化 启用 `sqlserver` 高版本语法 简化sql脚本语法
* update 优化 更新角色后踢掉所有相关的登录用户 用户量过大会导致redis阻塞卡顿(应粉丝要求)
* update 优化 翻译组件 支持返回值泛型 支持多种类型数据翻译(例如: 根据主键翻译成对象)
* update 优化 限流注解使用 `SpringEl` 表达式动态定义 Key 与 message 国际化支持
* update 优化 限流功能 `redis key` 生成规则 以 `功能头+url+ip+key` 格式
* update 优化 只拦截系统内存在的路径 减少不必要的拦截造成的性能消耗
* update 优化 `tagsView` 右选框，首页不应该存在关闭左侧选项
* update 优化 `copyright 2023`
* update 优化 监控页面图标显示
* update 优化 日志注解支持排除指定的请求参数
* update 优化 业务校验优化代码
* update 优化 日志管理使用索引提升查询性能
* update 优化 框架时间检索使用时间默认值 `00:00:00 - 23:59:59`
* update 优化 oss 预览使用 `ImagePreview` 组件


### 新增功能

* add 新增 `BeanCopyUtils#mapToMap` 方法
* add 新增 `StringUtils` `splitTo` 与 `splitList` 方法 优化业务代码
* add 新增 `EasyExcel` `@ExcelEnumFormat` 枚举类数据翻译注解


### 问题修复

* fix 修复 新版本 `Redisson` 存在与 `springboot 2.X` 的兼容性问题
* fix 修复 vue3 模板点击删除按钮后弹框显示`[object Object]`或控制台报错的问题
* fix 修复 接口问题开关不生效问题
* fix 修复 前端优化文件下载出现的异常
* fix 修复 修改密码日志存储明文问题
* fix 修复 用户密码注解误删暴露问题
* fix 修复 代码生成 使用 `postgreSQL` 数据库查出已删除的字段


## v4.5.0 - 2023-01-12

### 重大更新

* [重大更新] 使用 spring 事件发布机制 重构登录日志与操作日志 支持多事件监听无入侵扩展
* 例如: 可以增加一个监听者将日志上传至ES等存储 对原有逻辑无影响

### 依赖升级

* update springboot 2.7.6 => 2.7.7
* update springboot-admin 2.7.7 => 2.7.10
* update mybatis-plus 3.5.2 => 3.5.3.1
* update redisson 3.18.0 => 3.19.1
* update sa-token 1.33.0 => 1.34.0
* update easyexcel 3.1.3 => 3.1.5
* update springdoc 1.6.13 => 1.6.14
* update snakeyaml 1.32 => 1.33
* update hutool 5.8.10 => 5.8.11
* update aws-s3 1.12.349 => 1.12.373
* update aliyun-sms 2.0.22 => 2.0.23
* update tencent-sms 3.1.635 => 3.1.660
* update echarts 4.9.0 => 5.4.0
* update vue3 element-plus 2.2.21 => 2.2.27

### 功能更新

* update 优化 BaseMapperPlus 使用 MP V3.5.3 新工具类 Db 简化批处理操作实现
* update 优化 将环境配置放到 pom 文件上方 便于查看使用
* update 优化 代码生成与框架主体使用相同的主键生成器 全局统一避免问题
* update 优化 系统登录 使用单表查询校验用户 避免多次 join 查询
* update 优化 删除 vue3 模板无用参数
* update 优化 xss 包装器 变量命名错误
* update 优化 重构 ExcelUtil 全导出方法支持 OutputStream 流导出 不局限于 response
* update 优化 maven 地址切换回 aliyun 仓库
* update 优化 去除无用 guava 依赖管理 项目中已无此依赖
* update 优化 springdoc 配置鉴权头写死问题 增加持久化鉴权头配置
* update 优化 验证码结果使用 spel 引擎自动计算
* update 优化 弹窗内容过多展示不全问题
* update 优化 删除 fuse 无效选项 maxPatternLength
* update 优化 minio 安装警告 使用新版本参数
* update 优化 使用 spring 事件机制 重构 OssConfig 缓存更新
* update 优化 抽取 SysLoginService recordLogininfor 记录登录信息方法 简化日志记录
* update 优化 使用 spring 事件发布机制 重构登录日志与操作日志
* update 优化 单元格合并判断 cellValue 是否相等方法调整
* update 优化 去除 RedisConfig 无用继承

### 新增功能

* add 增加 GET 请求提交日期参数 默认格式化配置
* add 增加 RedisUtils 检查缓存对象是否存在方法

### 问题修复

* fix 修复 根据 key 更新参数配置报null问题
* fix 修复 树形下拉不能默认选中
* fix 修复 读取 generator.yml 中文乱码问题
* fix 修复 代码生成图片/文件/单选时选择必填无法校验问题
* fix 修复 修改参数键名时 未移除过期缓存配置
* fix 修复 用户注册 用户类型字段书写错误
* fix 修复 文件名包含特殊字符（+、-、*...）的文件无法下载问题
* fix 修复 短信校验模板参数传参错误
* fix 修复 vue3 closeSidebar 这个方法定义的参数没有解构问题

## v4.4.0 - 2022-11-28

### 重大更新
* [重大更新] 优化支持 oss 私有库功能(数据库字段改动) #cd9c3c3f
* [重大更新] 连接池由 druid 修改为 hikari 更新相关配置(原因可看文档) #1f42bd3d
* [重大更新] 移除 tlog(不支持UI界面 使用的人太少) 建议使用 skywalking
* [重大更新] 增加 skywalking 集成 默认注释不开启(使用看文档)

### 依赖升级
* update springboot 2.7.5 => 2.7.6
* update springboot-admin 2.7.6 => 2.7.7
* update satoken 1.31.0 => 1.33.0
* update spring-doc 1.6.12 => 1.6.13
* update easyexcel 3.1.1 => 3.1.3
* update hutool 5.8.8 => 5.8.10
* update redisson 3.17.7 => 3.18.0
* update lock4j 2.2.2 => 2.2.3
* update s3-adk 1.12.324 => 1.12.349
* update mysql-docker 8.0.29 => 8.0.31

### 功能更新
* update 优化 oss 云厂商增加 华为obs关键字
* update 优化 冗余的三元表达式
* update 优化 重置时取消部门选中
* update 优化 新增返回警告消息提示
* update 优化 hikari 参数顺序 最常用的放上面 删除无用 druid 监控页面
* update 优化 p6spy 排除健康检查 sql 执行记录
* update 优化 Dockerfile 创建目录命令
* update 优化 将空‘catch’块形参重命名为‘ignored’
* update 优化 使用本地缓存优化 excel 导出 数据量大字典转换慢问题
* update 优化 字典转换实现 去除字符串查找拼接优化效率
* update 优化 减小腾讯短信引入jar包的体积
* update 消除Vue3控制台出现的警告信息
* update 忽略不必要的属性数据返回
* update 替换 mysql-jdbc 最新坐标

### 新增功能
* add 新增 junit5 单元测试案例 #6e8ef308
* add 增加 sys_oss_config access_policy 桶权限类型字段
* add 增加 4.3-4.4 更新 sql 文件
* add 新增 字典数据映射注解 #da94e898
* add 增加 RedisUtils 获取缓存Map的key列表

### 问题修复
* fix 修复 上传png透明图片 生成头像透明部分变成黑色
* fix 修复 sqlserver sql文件 重复主键数据问题
* fix 修复 sqlserver 特定情况下报 ssl 证书问题 默认关闭 ssl 认证
* fix 修复 table中更多按钮切换主题色未生效修复问题
* fix 修复 菜单激活无法修改其填充颜色 去除某些svg图标的fill="#bfbfbf"属性
* fix 修复 使用缓冲流 导致上传异常问题
* fix 修复 过滤器链使用IoUtil.read方法导致request流关闭
* fix 修复 Log注解GET请求记录不到参数问题
* fix 修复 某些特性的环境生成代码变乱码TXT文件问题
* fix 修复 开启TopNav没有子菜单隐藏侧边栏
* fix 修复 回显数据字典数组异常问题

### 移除功能
* remove 移除过期 Anonymous 注解与其实现代码
* remove 移除 tlog(不支持UI界面 使用的人太少) 建议使用 skywalking

## v4.3.1 - 2022-10-24

### 依赖升级
* update springboot 2.7.3 => 2.7.5
* update springboot-admin 2.7.4 => 2.7.6
* update sa-token 1.30.0 => 1.31.0
* update springdoc 1.6.11 => 1.6.12
* update poi 5.2.2 => 5.2.3
* update hutool 5.8.6 => 5.8.8
* update aws-s3 1.12.300 => 1.12.324
* update aliyun-sms 2.0.18 => 2.0.22
* update tencent-sms 3.1.591 => 3.1.611
* update tlog 1.4.3 => 1.5.0 安全性升级
* update snakeyaml 1.30 => 1.32 存在漏洞
* update redisson 3.17.6 => 3.17.7
* update nginx 1.21.6 => 1.22.1 存在漏洞
* update element-ui 2.15.8 => 2.15.10
* update core-js 3.19.1 => 3.25.3

### 功能更新
* update 修改 差异命名与镜像名同步
* update 优化 通用下载方法新增config配置选项
* update 优化 日志操作中重置按钮时重复查询的问题
* update 优化 `@Anonymous` 注解标注过期 使用 `@SaIgnore` 替换
* update 优化 前端可以配置多排序参数支持依次排序
* update 优化 oss管理 支持时间排序
* update 优化 替换 sa-token 过期配置
* update 优化 sa-token 拦截器注册 `SaTokenConfig#addInterceptors` 排除拦截路径配置
* update 优化 vue3说明文件 编码问题
* update 优化 导入更新用户数据前校验数据权限
* update 优化 `R` 类 `isError` 和 `isSuccess` 改为静态方法
* update 优化 获取用户信息getInfo接口 使用缓存数据获取
* update 优化 选择按钮宽度

### 问题修复
* fix 修复 用户导入存在则更新不生效
* fix 修复 日志转换非json数据导致报错
* fix 修复 控制台SQL日志打印时间格式化问题
* fix 修复 不同网段因reset请求头导致下载导出跨域问题
* fix 修复 在线用户设置永不过期 被过滤问题
* fix 修复 在线用户设置永不过期 超时时间-1推送redis无效问题
* fix 修复 snakeyaml 漏洞 强制升级依赖版本(临时处理等boot升级)
* fix 修复 开启账号同端互斥登录 被顶掉后登出报null异常问题
* fix 修复 Redisson 设置 `NameMapper` 导致队列功能异常问题
* fix 修复 文件上传组件格式验证问题
* fix 修复 内部调用缓存不生效问题
* fix 修复 主题颜色在Drawer组件不会加载问题
* fix 修复 小屏幕上修改头像界面布局错位的问题
* fix 修复 内链域名特殊字符替换 合并错误导致问题
* fix 修复 nginx 漏洞 https://www.oschina.net/news/214309

## v4.3.0 - 2022-09-14

### 重大更新
* [重大更新] 整合 springdoc 基于 javadoc 实现无注解零入侵生成接口文档
* [重大更新] 重写 spring-cache 实现 更人性化的操作 支持注解指定ttl等一些参数
* [不兼容更新] 移除 swagger 所属所有功能 建议使用 springdoc
* [重大更新] 移除maven docker插件 过于老旧功能缺陷大 使用idea自带的docker插件替代

### 依赖升级
* update springboot 2.6.9 => 2.7.3
* update springboot-admin 2.7.2 => 2.7.4
* update redisson 3.17.4 => 3.17.6
* update hutool 5.8.3 => 5.8.6
* update okhttp 4.9.1 => 4.10.0
* update lock4j 2.2.1 => 2.2.2
* update aws-java-sdk-s3 1.12.248 => 1.12.300 修复依赖安全漏洞
* update aliyun.sms 2.0.9 => 2.0.18
* update tencent.sms 3.1.537 => 3.1.591
* update guava 30.0-jre => 31.1-jre
* update springdoc 1.6.9 => 1.6.11
* update druid 1.2.11 => 1.2.12
* update dynamic-ds 3.5.1 => 3.5.2

### 功能更新
* update 优化 短信接口实现类 `@Override` 注解
* update 优化 登出方法代码逻辑
* update 优化 代码中的一些魔法值
* update 优化 使用 StreamUtils 简化业务流操纵
* update 修改 oss 客户端自定义域名 统一使用https开关控制协议头
* update 更新 监控过时配置 WebSecurityConfigurerAdapter 改为 bean 注入
* update 修改 生成错误注释
* update 优化 docker 部署方式 使用 host 模式简化部署流程 降低使用成本
* update 修改 验证码开关变量名
* update 优化 DateColumn 支持单模板多key场景
* update 优化 redission 处理增加前缀
* update 优化 缓存监控 相关代码
* update 优化 部署脚本 防止出现权限问题
* update 优化 多个相同角色数据导致权限SQL重复问题
* update 优化 字典数据使用store存取
* update 优化 布局设置使用el-drawer抽屉显示
* update 更新框架文档 专栏与视频 链接地址
* update 优化 OSS文件上传 主动设置文件公共读 适配天翼云OSS
* update 优化 表格上右侧工具条（搜索按钮显隐&右侧样式凸出）
* update 优化 前后端多环境部署保持一致 删除无用环境文件
* update 优化 错误登录锁定与新增解锁功能
* update 优化 getLoginId 增加必要参数空校验
* update 使用 SpringCache注解 优化参数管理、字典管理、在线用户等业务缓存
* update 优化 多角色数据权限匹配规则
* update 优化 页面内嵌iframe切换tab不刷新数据
* update 优化 调整 oss表key 与 ossconfig的service 字段长度不匹配
* update 优化 操作日志密码脱敏
* update 重构 QueueUtils 抽取通用方法 统一使用 适配优先队列新用法

### 新功能
* add 增加 StreamUtils 流工具 简化 stream 流操纵
* add 新增 缓存列表菜单功能
* add 新增 获取oss对象元数据方法
* add 增加 QueueUtils 操作普通队列的方法

### 问题修复
* fix 修复 mysql sys_notice 与 sys_config 表主键类型长度不够问题
* fix 修复 获取 SensitiveService 空问题 增加空兼容
* fix 修复 代码生成首字母大写问题
* fix 修复 minio 上传自定义域名回显路径错误问题
* fix 修复 短信功能返回实体 SysSms 序列化问题
* fix 修复 sqlserver 更新sql错误提交
* fix 修复 RedisUtils 并发 set ttl 错误问题
* fix 修复 防止主键字段名与'row'或'ids'一致导致报错的问题
* fix 修复 幂等组件 逻辑问题导致线程变量未清除
* fix 修复 脱敏没有实现类导致返回数据异常问题
* fix 修复 用户导出字典使用错误
* fix 修复 用户登录与短信登录 国际化格式不一致
* fix 修复 BaseMapperPlus 方法命令不一致问题
* fix 修复 短信功能是否启用判断不生效BUG
* fix 修复 xxljob prod 环境配置文件 数据库ip漏改
* fix 修复 部署脚本 cp 命令缺少参数问题
* fix 修复 菜单管理的一些操作问题
* fix 修复 国际化文件提交为特殊编码问题
* fix 修复 minio配置https遇到的问题
* fix 修复 点击删除后点击取消控制台报错问题
* fix 修复 文件/图片上传组件 第一次上传报错导致后续上传无限loading问题
* fix 修复 postgresql 时间查询类型转换报错问题
* fix 修复 部门与角色 状态导出字典使用错误
* fix 修复 openapi结构体 因springdoc缓存导致多次拼接接口路径问题
* fix 修复 没有权限的用户编辑部门缺少数据
* fix 修复 oss配置删除内部数据id匹配类型问题
* fix 修复 用户导入存在则更新不生效
* fix 修复 日志转换非json数据导致报错

## v4.2.0 - 2022-06-28
### 重大更新
* [重大更新] 增加 `ruoyi-sms` 短信模块 整合 阿里云、腾讯云 短信功能
* [重大改动] 基于 `AWS S3` 协议重新实现 OSS模块 支持自定义域名
* [安全性] 优化 nginx 限制外网访问内网 actuator 相关路径(建议升级)
* [不兼容] 优化 文件与图片上传组件 使用id存储回显(升级的用户需要注意 上传组件返回值变成了 ossid  便于关联)
* [不兼容] 升级 mybatis-plus 3.5.2 解决新版本兼容性问题 关键字冲突修改(新增了很多关键字 升级的需要注意 冲突的关键字建议换一个命名)

### 依赖升级
* update springboot-admin 2.6.6 => 2.6.9
* update springboot-mybatis 2.2.0 => 2.2.2
* update sa-token 1.29.0 => 1.30.0
* update hutool 5.7.22 => 5.8.3
* update druid 1.2.8 => 1.2.11
* update tlog 1.3.6 => 1.4.3
* update easyexcel 3.0.5 => 3.1.1 去除cglib 支持jdk17
* update xxl-job 2.3.0 => 2.3.1
* update redisson 3.17.0 => 3.17.4
* update mybatis-plus 3.5.1 => 3.5.2
* update poi 4.1.2 => 5.2.2 性能大幅提升
* update docker mysql 8.0.27 => 8.0.29
* update docker nginx 1.21.3 => 1.21.6
* update docker redis 6.2.6 => 6.2.7
* update docker minio 2021-10-27 => 2022-05-26

### 功能更新
* update 优化 redis 序列化 使用系统自带json工具 全局统一
* update 优化 RedisUtils 重构过期方法
* update 完善短信验证码发送接口
* update 优化 弹窗点击遮罩层 默认不关闭 可在 main.js 修改
* update 调整 CacheManager 使用系统 系统序列化器
* update 调整 图片预览组件 去除无用根目录拼接
* update 用户管理左侧树型组件增加选中高亮保持
* update 优化 DataPermissionHelper 上下文存储 使用 SaToken 的请求存储器
* update 优化 用户头像上传限制只能为图片格式
* update 优化 redis 与 jackson 使用自动装配定制器简化配置
* update 优化 getLoginUser 获取 使用一级缓存
* update 增加 redis 无密码使用说明
* update 手动配置 Undertow 缓冲池 消除运行警告
* update 优化 表单构建按钮不显示正则校验
* update 优化 oss 回显查询 使用 redis 缓存
* update 优化 用户列表查询 剔除密码字段
* update 优化 验证码 登录 登出 注册 等接口 使用匿名注解放行
* update 修改 代码生成 controller 去除查询校验 由用户自行选择是否校验
* update 优化 ExcelUtil 工具支持合并处理器
* update 使用 SaStorage 优化 LoginHelper 一级缓存 避免 ThreadLocal 清理不干净问题
* update 优化 新增用户与角色信息、用户与岗位信息逻辑
* update 优化 代码生成 业务接口 增加事务回滚
* update 优化 logback 删除无用配置

### 新功能
* add 增加 MailUtils 邮件工具
* add 增加 RedisUtils 操作原子值方法
* add 增加 demo 短信演示案例
* add 增加 获取短信验证码接口
* add 新增 SpringUtils 获取配置文件中的属性值方法
* add 新增 Anonymous 匿名访问不鉴权注解
* add 新增 easyexcel 单元格合并注解与处理器
* add 增加 ExcelUtil 模板导出方法 支持 单列表/多列表
* add 增加 Excel 模板导出 测试类

### 问题修复
* fix 修复 ExcelUtil 表达式解析 参数添反导致无法解析问题
* fix 修复 全局线程池配置 核心线程与最大线程 参数填反问题
* fix 修复 查询未分配用户角色列表 角色无绑定用户情况下 空列表问题
* fix 修复 sqlserver 新增数据 id 错误
* fix 修复 token 超时时间设置 -1 导致的单位转换问题
* fix 修复 编辑 OssConfig 在 postgres 字段重复报错 补全 remark 字段
* fix 修复 postgres 数据库 菜单部分字段类型无法转换问题
* fix 修复 脱敏实现逻辑问题
* fix 修复 登录未选部门报空问题
* fix 修复 用户注销时记录注销日志异常问题
* fix 修复 代码生成表字段类型不匹配 导致查询不准确问题

## v4.1.0 - 2022-04-24
### 重大更新
* [重大更新] 增加应用适配 oracle
* [重大更新] 增加应用适配 SQL Server
* [重大更新] 增加应用适配 postgresql
* [重大更新] 确保更好的适配 多数据库 主键策略统一改为 雪花ID

### 依赖升级
* update springboot 2.6.4 => 2.6.7 修复 CVE-2022-22965 漏洞
* update springboot-admin 2.6.2 => 2.6.6
* update hutool 5.7.21 => 5.7.22
* update dynamic-datasource 3.5.0 => 3.5.1
* update redisson 3.16.8 => 3.17.0
* update qiniu 7.9.3 => 7.9.5
* update qcloud 5.6.68 => 5.6.72
* update minio 8.3.7 => 8.3.8
* update okhttp 4.9.2 => 4.9.3

### 功能更新
* update 简化查询 部门、菜单、角色、用户、代码生成列表 功能
* update 优化 部门修改子元素关系 使用批量更新
* update 优化去除sql差异化 时间范围统一使用 between 处理
* update 优化 RepeatSubmit 注解 支持业务处理失败 与 异常快速放行
* update 优化 防重 与 限流 功能支持国际化消息返回
* update 开启TopNav没有子菜单情况隐藏侧边栏
* update 更新minio压缩配置
* update 重命名 菜单字段 query -> query_param 解决系统关键字问题
* update 使用 in 优化 or 提升索引命中率
* update 优化 TreeEntity 树实体 去除未知泛型
* update 优化菜单名称过长悬停显示标题
* update 优化固定Header后顶部导航栏样式问题
* update 优化 logback 日志 异步输出
* update 全局异常处理器引入DuplicateKeyException主键冲突异常拦截
* update topNav自定义隐藏侧边栏路由
* update 更名 SaInterfaceImpl 为 SaPermissionImpl 完善相关注释
* update 优化 sa-token 路由拦截器语法 增加注释 避免误操作
* update 优化文件上传、图片上传组件 文件列表展示文件原名便于后续处理, 完善组件删除功能
* update 优化登录失败相关部分代码结构
* update 使用 spring cglib 替换 停止维护的 cglib
* update 简化 全局线程池配置 使用cpu核心数自动处理
* update 移除 重复提交 配置文件全局配置 使用注解默认值替代

### 新功能
* add 增加 4.0 升级 4.1 的 sql 脚本(升级需执行此sql)
* add 增加 DataBaseHelper 数据库助手 用于屏蔽多类型数据库sql语句差异
* add 增加 短信登录 与 小程序登录 示例
* add 增加 Mybatis 全局异常处理 开启多数据源切换 严格模式 找不到数据源报错

### 问题修复
* fix 修复 数据权限 从 aop 切换到 拦截器 导致获取代理失败问题
* fix 修复表单清除元素位置未垂直居中问题
* fix 修复 poi 组件漏洞 与 mysql jdbc 漏洞
* fix 修复单独访问 接口文档 请求 favicon.ico 报错问题
* fix 修复 minio 上传, 因 socket 导致 available 获取数值不精确问题
* fix 修复 cos_api bcprov-jdk15on 漏洞
* fix 修复 guava 漏洞 统一依赖版本
* fix 修复 tlog 依赖漏洞

## v4.0.1 - 2022-03-01
### 依赖升级
* update springboot 2.6.3 => 2.6.4
* update hutool 5.7.20 => 5.7.21
* update qiniu 7.9.2 => 7.9.3
* update minio 8.3.5 => 8.3.7

### 功能更新
* update 图片上传 文件上传 支持并发上传
* update 组件ImageUpload支持多图同时选择上传
* udpate 组件fileUpload支持多文件同时选择上传
* update 优化 R 默认返回 msg
* update 增加 用户注册 用户类型默认值
* update 增加用户登出日志
* update 更新 多用户多设备的注释说明
* update 优化 是否为管理员的判断
* update 优化 页面若未匹配到字典标签则返回原字典值
* update 调整用户登录 将日志调整到最后 防止获取不到用户警告
* update 优化随机数生成方式 避免容易生成两个相同随机数的问题

### 问题修复
* fix 修复代码生成 基于路径生成 路径为空问题
* fix 恢复误删 `@Async` 注解线程池配置类
* fix 修复 minio 适配 https 导致的问题
* fix 修复分页组件请求两次问题

## v4.0.0 - 2022-02-18
### 重大更新
* [重大更新] 重写项目整体结构 数据处理下沉至Mapper符合MVC规范 减少循环依赖
* [重磅更新] 主分支与satoken分支合并 权限统一使用 sa-token
* [重磅更新] 适配升级 SpringBoot 2.6
* [重磅更新] EasyExcel大版本升级3.X
* [重磅更新] 移除链式调用注解 因链式调用不符合java规范 导致很多问题
* [重磅更新] 增加 轻量级 分布式队列 支持
* [重磅更新] 增加 数据脱敏注解 使用序列化控制脱敏 支持多种表达式
* [重磅更新] 重构 使用 Spring 简化 oss 模块代码
* [重磅更新] 重构 调整返回类型为 R 精简 Controller 代码

### 依赖升级
* update springboot 2.5.8 => 2.6.3
* update mybatis-plus 3.4.3.4 => 3.5.1
* update maven-jar-plugin 3.2.0 => 3.2.2
* update maven-war-plugin 3.2.0 => 3.2.2
* update maven-compiler-plugin 3.1 => 3.9.0
* update hutool 5.7.18 => 5.7.20
* update springboot-admin 2.6.0 => 2.6.2
* update redisson 3.16.7 => 3.16.8
* update qiniu 7.9.0 => 7.9.2
* update aliyun 3.13.1 => 3.14.0
* update qcloud 5.6.58 => 5.6.68
* update minio 8.3.4 => 8.3.5

### 功能更新
* update 用户管理部门查询选择节点后分页参数初始
* update 防重复提交标识组合（key + url + header）
* update 接口文档增加 basic 账号密码验证
* update 用户修改减少一次角色列表关联查询
* update 优化部门修改缩放后出现的错位问题
* update 指定 maven 资源过滤为具体文件 防止错误过滤
* update hutool 引入改为 bom 依赖项引入
* update 降低开发环境 redis连接池数量
* update 升级 springboot 2.6.X 解决 springfox 兼容性问题
* update 优化多用户体系处理 更名 LoginUtils 为 LoginHelper 支持 LoginUser 多级缓存
* update 优化加载字典缓存数据
* update 数据库更改 对接多用户体系
* update 移除掉 StringUtils 语义不明确的api方法 使用特定工具替换
* update 优化登录、注册在接口通过`@Validated`注解进行数据基础校验
* update 优化 查询登录用户数据 统一走缓存
* update 优化 redisson 配置 去除掉不常用的配置 使用默认配置
* update 用户访问控制时校验数据权限，防止越权
* update 修改用户注册报未登录警告
* update 调整oss预览开关 使用前端直接调用更改配置参数
* update 使用 satoken 自带的 BCrypt 工具 替换 Security 加密工具 减少依赖
* update 优化 TreeBuildUtils 工具 使用反射自动获取顶级父id
* update 使用 hutool Dict 优化 JsonUtils 防止类型解析异常
* update 优化代码生成 使用新 JsonUtils.parseMap 方法
* update 更新 所有 oss 均支持 https 配置

### 新功能
* add 增加 RedisUtils 工具 hasKey 检查key存在方法
* add 增加 监控中心 自定义事件通知
* add 增加 3.X update 4.0 更新sql

### 问题修复
* fix 修复登录失效后多次请求提示多次弹窗问题
* fix 修复 StringUtils 通配符匹配无效
* fix 修复选项卡点击右键刷新丢失参数问题
* fix 修复 数据权限 缓存方法名错误问题
* fix 修复自定义组件`file-upload`无法显示第一个文件，列表显示的文件比实际文件少一个的问题
* fix 修复因升级 sa-token 导致 doLogin 无法获取 token 问题
* fix 修复分页组件请求两次问题

### 移除功能
* remove 移除过期代码 分页工具相关
* remove 移除过期代码 多数据源切换
* remove 移除过期代码 数据权限

### 其他
* 3.X 版本进入维护阶段 不进行更新 只修复bug 持续维护到2022年10月
* 4.X 版本公测将近一个月 大部分bug已修复 官网主分支更改为 4.X 版本 推荐使用


## v3.5.0 - 2021-12-28
### 重大更新
* [重大更新] 重写数据权限实现
* [重磅更新] 重构分页 简化使用
* [重磅更新] 用户登录 支持校验错误次数锁定登录
* [重磅更新] 增加 jdbc 批处理参数 大幅提升批量操作性能 对原生语句与 MP 均有效

### 依赖升级
* update springboot 2.5.7 => 2.5.8 升级预防 log4j2 问题
* update springboot-admin 2.5.4 => 2.5.5
* update hutool 5.7.16 => 5.7.18
* update redisson 3.16.4 => 3.16.7
* update dynamic-ds 3.4.1 => 3.5.0
* update qiniu 7.8.0 => 7.9.0
* update minio 8.3.3 => 8.3.4
* update tlog 1.3.4 => 1.3.6 启用 tlog 自动配置
* update clipboard 2.0.6 => 2.0.8

### 功能更新
* update 多数据源切换标注过期 3.6.0 移除 推荐使用原生注解
* update 通用权限服务 迁移回 ruoyi-framework 模块
* update 使用 hutool-jwt 替换老旧 jjwt 依赖
* update 调整 OSS 表字段内容长度
* update LoginUser 增加角色缓存 优化角色权限代码
* update 使用 Cglib 重构 BeanCopyUtils 性能优异
* update 禁止所有工具类实例化 优化代码书写规范
* update 优化查询用户的角色组、岗位组代码
* update 更新 RedisUtils 返回客户端实例
* update 修改 健康检查权限 改为用户放行 提高安全性
* update hutool 工具 改为单包引入 减少无用依赖
* update ServicePlusImpl 功能 下沉到 BaseMapperPlus
* update 去除 jdk17 标签 由于很多组件还未适配 导致一些问题
* udpate 代码生成预览支持复制内容
* update 用户导入提示溢出则显示滚动条
* update 路由支持单独配置菜单或角色权限
* update 优化web拦截器 使用原生接口处理 默认非生产环境开启
* update 调整监控依赖 从 common 迁移到 framework

### 新功能
* add 新增 Vue3 分支 与 代码生成模板(由于组件还未完善 仅供学习)
* add 增加 RedisUtils 注册监听器方法
* add 增加 自定义 Xss 校验注解 用户导入增加 Bean 校验
* add oss下载增加 loading 层
* add 新增图片预览组件
* add 集成compression-webpack-plugin插件实现打包Gzip压缩
* add 新增 SqlUtils 检查关键字方法

### 问题修复
* fix 修复 集群雪花id重复问题 使用网卡信息绑定生成
* fix 修复 count 语法异常
* fix 修复更改密码问题
* fix 修复sql关键字处理 防止解析器报错
* fix 修复 TreeBuildUtils 顶节点不为 0 问题
* fix 修复 SysOssConfig 主键类型错误
* fix 修复代码生成 导出注解错误
* fix 修复 redisson 集群模式 路径未匹配协议头问题
* fix 修复打包后字体图标偶现的乱码问题
* fix 修复版本差异导致的懒加载报错问题
* fix 修复代码生成字典组重复问题

### 移除功能
* remove 删除 jjwt 无用依赖
* remove 移除过期 用户导入
* remove 移除过期工具 DictUtils

## v3.4.0 - 2021-11-29

### 重磅更新
* update [重磅更新] 重构 Excel 导入 支持 Validator 校验 支持自定义监听器
* update [重磅更新] Validator 校验框架支持国际化

### 依赖升级
* update springboot 2.5.6 => 2.5.7
* update hutool 5.7.15 => 5.7.16
* update okhttp 4.9.1 => 4.9.2
* update spring-boot-admin 2.5.2 => 2.5.4
* update redisson 3.16.3 => 3.16.4
* update tlog 1.3.3 => 1.3.4
* update axios 0.21.0 => 0.24.0
* update core-js 3.8.1 => 3.19.1
* update js-cookie 2.2.1 => 3.0.1
* update velocity 1.7 => 2.3
* update 升级 docker 基础镜像

### 功能更新
* update 基于 hutool 封装树构建工具 重构部门与菜单树结构返回
* update 减少使用特定数据库函数
* update 配置应用前缀路径 改为配置文件统一配置
* update 升级 swagger 配置 使用 knife4j 增强模式
* update 监控中心 集成监控客户端 实现自监控
* update 调度中心 集成监控客户端 注册到监控中心
* update 优化 tab 对象简化页签操作
* update 解耦 LoginUser 与 SysUser 强关联
* update 更新 RepeatSubmit 注解 aop 处理 针对特殊参数进行过滤
* update DictUtils 字典工具类 标记过期 3.5.0 版本移除 使用 DictService 代替
* update 抽象 DictService 通用 字典服务
* update 抽象 ConfigService 通用 参数配置服务
* update 基于 DictService 重构 Excel 内字典查询功能
* update OSS 模块 整体重命名 消除歧义
* update 更新 redis.conf 存储策略 aof 与 rdb 配置参数
* update 初始化数据转移到 ApplicationRunner 统一处理
* update 优化时间查询语句

### 新功能
* add 增加 框架缓存懒加载 开关
* add 新增 监控中心 Bean 初始化 startup trace 监控插件
* add 增加 ValidatorUtils 校验框架工具 用于在非 Controller 的地方校验对象

### 漏洞修复
* fix 修复 SysOss、SysOssConfig 未继承 BaseEntity 基础实体问题
* fix 修复 xxl-job-admin 部署问题
* fix 修复 回显数据字典键值修正
* fix 修复 Linux 清除临时目录 导致上传找不到目录报错问题
* fix 修复通用实体 传参无法接收问题
* fix 修复 SysLoginController 接口文档书写错误问题
* fix 修复 用户逻辑删除 差异问题
* fix 修复 OSS 七牛云 token 过期未刷新问题
* fix 修复 分页工具 排序字段 null 处理
* fix 修复 用户导入字典使用错误
* fix 修复 关闭 xss 功能导致可重复读 RepeatableFilter 失效
* fix 修复 使用 this.$options.data 报错问题
* fix 修复 代码生成复选框字典遗漏问题
* fix 修复 重复提交不生效问题 由于概念不同 使用 RedisUtils 重构
* fix 修复 OSS 工厂 未实例化服务更新加载问题

### 功能移除
* remove 移除 quartz 相关代码与依赖
* remove 移除 feign 相关代码与依赖
* remove 移除 MybatisPlusRedisCache 二级缓存

## v3.3.0 - 2021-10-29

### 重磅更新
* add [重磅更新] 增加分布式日志框架 TLog
* add [重磅更新] 增加分布式任务调度系统 Xxl-Job
* add [重大更新] 增加 ruoyi-job 任务调度模块(基于xxl-job)
* update [重大更新]全业务 增加 接口文档注解 格式化代码

### 依赖更新
* update springboot 2.5.5 => 2.5.6
* update springboot-admin 2.5.1 => 2.5.2
* update element-ui 2.15.5 => 2.15.6
* update hutool 5.7.13 => 5.7.15
* update qcloud.cos 5.6.55 => 5.6.58
* update minio 8.3.0 => 8.3.3

### 功能更新
* update 更新 element 2.15.6 表格样式
* update 优化 代码生成常量 关于 BO VO 注释
* update 优化代码生成 导入表 列表返回 主键默认选中
* update MybatisPlusRedisCache 标记过期 推荐使用 spring-cache
* update Quartz 标记过期 推荐迁移至新框架 xxl-job
* update Feign 标记过期
* update 前端增加默认国际化参数
* update 更新 Admin 监控 注释 避免错误使用
* update Admin 监控增加日志文件输出
* update 优化 xxl-job-admin 增加格式化日志输出与 docker 镜像
* update 更新 xxl-job 执行器开关功能
* update 代码生成 改为生成抽象实体
* update 代码生成 搜索框 更新文本域生成 用于模糊查询
* update 通用数据注入改为适配通用实体类
* update 使用路由懒加载提升页面响应速度
* update 迁移所有脚本文件至 script 目录
* update swagger 组顺序配置
* update sql 文件更新 xxljob 控制台菜单
* update 前端增加 任务调度中心页面与环境及 nginx 配置
* update 合并 oss.sql 至主 sql
* update 补全国际化文件(英文)
* update 更新关于全局路径设置与文档链接
* update 删除无用 setUsername 使用自动注入
* update RedisUtils 更新删除 hash 数据方法

### 漏洞修复
* fix 修复 多数据源 aop 语法错误
* fix 修复 子菜单无 query 参数问题
* fix 修复 oss 配置删除时删除缓存 bug
* fix 修复无权限获取请求头 download-filename 导致文件名为空问题

## v3.2.0 - 2021-9-28

### 重大更新
* update [重大改动]接口文档 支持分组配置
* update [重大改动]security 路径配置抽取到配置文件
* update [重大改动] 将 framework 与 system 模块 解耦 调整依赖结构 解决依赖冲突
* update [重大改动]重写 防重提交实现 使用分布式锁 解决并发问题 压测通过

### 依赖更新
* update springboot 2.5.4 => 2.5.5 bugfix版本
* update mybatis-plus 3.4.3.3 => 3.4.3.4 bugfix版本
* update redisson 3.16.2 => 3.16.3 bugfix版本
* update easyexcel 2.2.10 => 2.2.11
* update hutool 5.7.11 => 5.7.13
* update file-saver 2.0.4 => 2.0.5
* update dart-sass 1.32.0 => 1.32.13
* update sass-loader 10.1.0 => 10.1.1

### 功能更新
* update 优化代码生成 根据MP生成特性 调整导入表结构默认值合理化
* update 将所有 云存储字样 改为 对象存储 避免误解
* update 更新 @Cacheable 错误用法 注意事项
* update 优化 AddressUtils 空校验处理
* update 菜单管理支持配置路由参数
* update 优化aop语法 使用spring自动注入注解
* update 使用 Redisson 限流工具 重写限流实现
* update 使用 vue-data-dict 简化数据字典使用
* update 增加日志注解新增是否保存响应参数开关
* update 用户未登录日志改为 warn 级别
* update OSS模块 关于下载403报错信息优化
* update 更新 Actuator prod 默认暴漏端点 增加暴漏 logfile 日志端点
* update 默认适配jdk11 测试 jdk17 无异常
* update 封装通用下载方法简化下载使用

### 新功能
* add 新增通用方法简化模态/缓存使用
* add 增加 限流演示案例
* add 增加 redis redisson 集群配置

### 漏洞修复
* fix Cron表达式生成器关闭时销毁，避免再次打开时存在上一次修改的数据
* fix 全局限流key会多出一个"-" 将其移动到IP后面 去除多余的空格
* fix 修复多主键代码生成bug
* fix 修复 @Cacheable 与 @DataScope 冲突问题
* fix 修复代码生成页面数据编辑保存之后总是跳转第一页的问题

### 功能移除
* remove 移除过期工具 RedisCache
* remove 移除无用配置类 ServerConfig
* remove 移除 SysUser 无用字段 salt

## v3.1.0 - 2021-9-7

### 重大更新
* add [重大改动] 过期 RedisCache 新增 RedisUtils 工具类 新增 发布订阅功能 更灵巧便于使用
* add [重大改动] 新增 saveOrUpdateAll 方法 可完美替代 saveOrUpdateBatch 高性能
* update [重大改动] 重写 InsertAll 方法实现 可完美替代 saveBatch 秒级插入上万数据
* update [重大改动] 更改OSS上传通用路径生成 按照年月日分三级目录
* update [重大改动] MP字段验证策略更改为 NOT_NULL 个别特殊字段使用注解单独处理
* update [重大改动] 所有业务适配 RedisUtils 新工具

### 依赖升级
* update springboot 2.5.3 => 2.5.4
* update spring-boot-admin 2.5.0 => 2.5.1
* update mybatis-plus 3.4.3 => 3.4.3.3 适配升级 (包含不兼容升级)
* update aliyun.oss 3.13.0 => 3.13.1
* update qcloud.cos 5.6.47 => 5.6.51
* update hutool 5.7.9 => 5.7.11
* update maven-jar-plugin 3.1.1 => 3.2.0
* update feign-okhttp 11.2 => 11.6
* update redisson 3.16.1 => 3.16.2

### 新功能
* add 优化 docker 增加 redis 配置文件
* add 新增暗色菜单风格主题
* add 菜单&部门新增展开/折叠功能
* add 页签右键按钮添加图标 页签新增关闭左侧

### 功能更新
* update 优化 OSS 模块与上传组件 异常处理
* update 更新 jackson 配置 支持 LocalDateTime 全局格式化
* update 优化 使用权限工具 获取用户信息
* update 自定义可拖动弹窗宽度指令
* update 重构 将下载excel工具提取到全局
* update 定时任务对检查异常进行事务回滚
* update 优化spy配置文件为 UTF8编码 解决中文注释乱码问题
* update 修改时检查用户数据权限范围
* update 解决 logout 写死 无法扩展路径问题
* update 优化代码生成 导入与同步 批处理效率
* update 修改时检查用户数据权限范围
* update 修改代码生成字典回显样式
* update 修改数据字典回显
* update 优化验证码配置 使用泛型 防止错误输入
* update 优化全局线程池配置 使用泛型 防止错误输入
* update 使用 MP 全局配置分页溢出
* update 代码生成器 导入表时查询 新创建表的优先排序在前面
* update 定时任务支持在线生成cron表达式
* update 自定义弹层溢出滚动样式
* update 优化分页工具排序处理
* update 优化 oss配置 使用发布订阅工具 刷新配置
* update 代码生成 查询数据库列表 按照时间倒序
* update 使用MP自行判断数据库类型

### 漏洞修复
* fix 修复保存配置主题颜色失效问题
* fix 修复 导出雪花id excel失真问题
* fix 修复 druid 监控 集群模式下 无法路由到同一台服务器问题
* fix 解决搜索校验不通过问题
* fix 修复定时器工具编写错误问题
* fix 修复 minio 无 perfix 问题
* fix 修复 富文本图片路径错误问题
* fix 修复 OSS配置清空被过滤问题
* fix 修复 excel 导入与 class 未对应问题
* fix 修复字典组件值为整形不显示问题

## v3.0.0 - 2021-8-18

### 重大更新
* add [重大更新]重写 OSS 模块相关实现 支持动态配置(页面配置)
* add [重大更新]增加 jackson 超出 JS 最大数值自动转字符串(雪花id序列化)处理
* add [重大更新]重写 防重提交拦截器 支持全局与注解自定义 拦截时间配置配置 优化逻辑
* add [重大更新]新增是否开启用户注册功能
* add [重大更新]增加 easyexcel 工具类
* add [重大更新]集成 性能分析插件 p6spy 更强劲的 SQL 分析
* add [重大更新]增加 完整国际化解决方案
* add [重大更新]支持自定义注解实现接口限流

### 依赖升级
* update feign-okhttp 11.0 => 11.2
* update okhttp 3.19.4 => 4.9.1
* update minio 8.2.0 => 8.3.0
* update hutool 5.7.6 => 5.7.7
* update element-ui 2.15.2 => 2.15.5
* update springboot admin 2.4.3 => 2.5.0 (新增 Quartz 专属监控页)

### 新功能
* add 增加 admin 监控客户端开关
* add 增加 国际化演示demo

### 依赖更新
* update 更新软件架构图
* update 优化XSS跨站脚本过滤
* update 优化BLOB下载时清除URL对象引用
* update 更新 防重提交拦截器 demo演示案例
* update 日常字符串校验 统一重构到 StringUtils 便于维护扩展
* update 修改 自动注入器 用户未登录异常拦截抛出警告 返回Null
* update 重构 统一使用 流工具下载
* update 重写 所有业务导出 适配easyexcel工具
* update 移动文件存储业务到 system 模块
* update 代码生成模板 适配新excel导出
* update 将 Actuator 配置 移动到全局配置
* update 统一镜像时区配置 移除主机时间映射
* update 更改多数据源框架更清晰的依赖名
* update 更新 阿里云 maven源 新地址
* update 补全基础实体 文档注解
* update 代码生成文档注解 增加必填判断配置
* update 注入器 insert 增加 update 字段处理
* update 默认首页使用keep-alive缓存

### 漏洞修复
* fix 生产minio回显问题
* fix 修复角色分配用户页面接收参数与传递参数类型不一致导致的错误
* fix 修复代码生成 删除按钮报错 loading 不取消问题
* fix 解决登录后浏览器后台Breadcrumb组件报错
* fix 修复DictUtils方法报错
* fix 头像上传 未走OSS存储问题
* fix oss列表 jpeg 不回显问题
* fix 修复操作日志根据状态查询异常问题

### 功能移除
* remove 移除原生excel工具
* remove 移除通用上传下载接口与配置

## v2.6.0 - 2021-7-28

### 重大更新
* add [重大新增] 增加 OSS 对象存储模块
* remove [重大改动] 删除 自带通用上传 接口 使用OSS模块替换
* update [重大改动] 重写VO转换 支持深拷贝 将VO类抽象到 ServicePlus 泛型处理
* update [重大改动] 多BO合并 使用分组校验 生成BO代码
* update [重大改动] 重构 IServicePlus 功能 增加 BeanCopyUtils 深拷贝工具

### 依赖升级
* update springboot 2.4.9 => 2.5.3
* update hutool 5.7.4 => 5.7.6
* update minio 8.2.2 => 8.3.0
* update docker plugin 1.2.0 => 1.2.2
* update redisson 3.16.0 => 3.16.1
* update datasource 3.4.0 => 3.4.1
* update element-ui 2.15.2 => 2.15.3

### 新功能
* add 演示Demo增加自定义分页接口案例
* add 角色&菜单新增字段属性提示信息

### 功能更新
* update 更新druid配置 独立配置更明显
* update 顶部菜单排除隐藏的默认路由
* update 富文本新增上传文件大小限制
* update 导入用户样式调整
* update 顶部菜单样式调整
* update 密码框新增显示切换密码图标
* update 内链设置meta信息
* update 跳转路由高亮相对应的菜单栏

### 漏洞修复
* fix 修复多数据源druid全局配置缩进错误 引起无效配置问题
* fix 修复定时任务日志执行状态显示
* fix 修复 授权角色空数据问题
* fix 修复 DictData 删除逻辑问题
* fix 修复任意账户越权漏洞

## v2.5.2 - 2021-7-19

### 功能更新
* update 优化代码生成器注释格式

### 漏洞修复
* fix 回滚代码生成 批处理优化
* fix 代码生成 queryType 重复勾选数据库无默认值问题
* fix 修复接口单参数校验无效问题
* fix 代码生成 queryType >= <= 标识符错误问题
* fix 修复代码生成字典问题
* fix 修复 thread-pool: enabled 配置不生效问题

### 功能移除
* remove 删除无用文档与脚本

## v2.5.1 - 2021-7-13
* update 验证码开关 转移到表 参数管理 内
* update 使用hutool重构 判断是否url

### 漏洞修复

### 功能更新
* fix 修复 docker业务集群部署与文件上传的问题
* fix 修复代码生成同步表结构id冲突问题
* fix 修复代码生成选择字典 无法取消问题
* fix 修复代码生成字典为null问题
* fix 图片上传 多图时无法删除相应图片修复

### 功能移除
* remove 删除富文本video事件

## v2.5.0 - 2021-7-12

### 依赖升级
* update springboot 2.4.7 => 2.4.8
* update knife4j 3.0.2 => 3.0.3
* update hutool 5.7.2 => 5.7.4
* update spring-boot-admin 2.4.1 => 2.4.3
* update redisson 3.15.2 => 3.16.0

### 新功能
* add 增加 docker 编排 与 shell 脚本
* add 增加 feign 熔断 自定义结构体解析方法 与 demo 注释
* add 用户管理新增分配角色功能
* add 角色管理新增分配用户功能
* add 增加spring-cache演示案例

### 功能更新
* update 独立 springboot-admin 监控到扩展模块项目
* update springboot-admin 监控 增加用户登录权限管理
* update 优化代码生成器 批量导入
* update 优化 增加MP注入异常拦截
* update 关闭默认二级缓存 推荐使用 spring-cache 注解手动缓存
* update FileUpload ImageUpload组件 支持多图片上传
* update 优化中英文语言配置
* update 规范maven写法

### 漏洞修复
* fix redis获取map属性bug修复。
* fix 修复 按钮loading 后端500卡死问题
* fix 相对路径下载问题
* fix 修复 hutool 工具返回结果不一致问题

## v2.4.0 - 2021-6-24

### 依赖升级
* update springboot 2.3.11 => 2.4.7
* update springboot-admin 2.3.1 => 2.4.1
* update feign 2.2.6 => 3.0.3
* update hutool 5.6.7 => 5.7.2

### 功能更新
* update 多数据源替换成dynamic-datasource
* update 适配 jdk11
* update 集成 Lock4j 分布式锁
* update 移除 fastjson 增加 jackson 工具类 重写相关业务
* update 优化 异步工厂重写 使用 spring 异步处理
* update 全局挂载字典标签组件
* update 日志列表支持排序操作
* update 更新 feign demo 更清晰的用法
* update 更新多数据源演示案例

### 新功能
* add 增加 ServicePlusImpl 自动以实现类 重写移除事务注解方法 防止多数据源失效
* add 增加 自定义 批量insert方法
* add 增加 Swagger3 用法示例

### 漏洞修复
* fix 修复地址ip地址特殊回环问题

## v2.3.2 - 2021-6-11

### 新功能
* add redis锁工具类编写

### 功能更新
* update spring-cache 整合 redisson
* update MybatisPlus整合Redis二级缓存
* update swagger 升级为 3.0.0 使用 OAS_30 协议
* update 优化 代码生成器 增加表单防重注解
* update 优化 锁切面代码 key到常量类

### 漏洞修复
* fix 修复相对路径上传异常问题

## v2.3.1 - 2021-6-4

### 新功能
* add 增加 redisson 分布式锁 注解与demo案例
* add 增加 Oracle 分支

### 功能更新
* update 优化 redis 空密码兼容性
* update 优化前端代码生成按钮增加 loading

### 漏洞修复
* fix 修复 redisson 不能批量删除的bug
* fix 修复表单构建选择下拉选择控制台报错问题
* fix 修复 vo 代码生成 主键列表显示 重复生成bug
* fix 修复上传路径 win 打包编译为 win 路径, linux 报错bug

## v2.3.0 - 2021-6-1

### 新功能
* add 升级 luttuce 为 redisson 性能更强 工具更全
* add 增加测试数据sql文件
* add 增加demo模块 单表演示案例(包含数据权限)

### 功能更新
* update 完美修复 数据权限功能(支持单表多表过滤)
* update 优化代码生成模板
* update 优化 system 模块 批量操作性能

## v2.2.1 - 2021-5-29

### 新功能
* add 增加 security 权限框架 `@Async` 异步注解配置

### 功能更新
* update 优化dataScope参数防止注入
* update 优化参数&字典缓存操作
* update 增加修改包名文档
* update 文档增加演示图例

### 漏洞修复
* fix 修复部门类sql符号错误

## v2.2.0 - 2021-5-25

* 同步升级 RuoYi-Vue 3.5.0

### 新功能
* add 增加验证码开关
* add 新增IE浏览器版本过低提示页面

### 功能更新
* update 升级druid到最新版本v1.2.6
* update 升级fastjson到最新版1.2.76
* update 修改bo加入判断是否设置必填再加载必填注解
* update 生成vue模板导出按钮点击后添加遮罩
* update Redis设置HashKey序列化
* update 优化Redis序列化配置

### 漏洞修复
* fix 修复代码生成器中表字段取消必填无法更新问题

## v2.1.2 - 2021-5-21

### 功能更新
* update springboot 升级 2.3.11
* update mybatis-plus 升级 3.4.3 分页Plus对象适配更新
* update 验证码生成更新为无符号整数计算
* update 请求响应对象 与 分页对象 结构修改 适配接口文档配置
* update swagger增加请求前缀

## v2.1.1 - 2021-5-19

### 功能更新
* update 配置统一提取为 properties 配置类
* update 分页工具 删除过期方法
* update admin 实时监控日志 改为保留一天

### 漏洞修复
* fix 修复swagger开关无法控制关闭问题
* fix maven install 异常

## v2.1.0 - 2021-5-17

### 功能更新
* update knife4j升级3.0.2
* update 增强分页工具兼容性
* update 通用Service接口 增加自定义vo转换函数

### 移除功能
* remove 移除ruoyi自带服务监控(Admin已全部包含)

## v2.0.0 - 2021-5-15

### 依赖升级
* springboot 升级 2.3.10 依赖全面升级适配

### 新功能
* add 增加分页工具
* add 增加 增强Mapper 与 增强Service 重写业务适配
* add 代码生成器 增加校验注解

### 功能更新
* update 代码生成器修改为MP分页
* update 使用 MP 分页工具 重构业务
* update 重写文档介绍

### 移除功能
* remove 移除 pagehelper 分页工具

### 漏洞修复
* fix 修复代码生成 数据权限问题

## v1.0.2 - 2021-5-13

### 功能更新
* update 更新整合打包文档 重新排版

### 漏洞修复
* fix vue与boot整合打包与admin页面路由冲突

## v1.0.1 - 2021-5-11

### 依赖更新
* update 更新banner
* update 配置转移到 yml 文件 统一管理
* update 上传媒体类型添加视频格式
* update 树级结构更新子节点使用replaceFirst
* update 删除操作日志记录日志

### 漏洞修复
* fix 修正导入表权限标识
* fix 文件上传时报错

## v1.0.0 - 2021-5-10
* 基于 ruoyi-vue 3.4.0 发布 v1.0.0 稳定版