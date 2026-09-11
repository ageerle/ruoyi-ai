---
topic: modules/common-core-utilities
title: ruoyi-common 核心工具层（core / json / doc / excel）
updated: 2026-09-04
raw:
  - raw/common-source/ExcelDictFormat.md
  - raw/common-source/global-exception-handler.md
  - raw/common-source/security-config.md
---

# ruoyi-common 核心工具层

覆盖 4 个基础工具模块，是所有功能模块的依赖基石。共 127 个 Java 文件。

参见：[modules/common.md § 27 子模块清单](../modules/common.md)、[security-config.md](../raw/common-source/security-config.md)。

## ruoyi-common-core（98 文件，最重）

核心工具集，几乎所有模块都引用。

**关键类**：

| 类 | 作用 |
|---|---|
| `R<T>` | 统一响应包装 `{code, msg, data}`，code=200 成功，500 业务异常 |
| `PageQuery / TableDataInfo<T>` | 分页查询参数 + 分页响应 |
| `SpringUtils` | Spring 上下文静态访问（`getBean` / `getApplicationName`） |
| `StringUtils` | 字符串工具（`isBlank` / `format` 等） |
| `DateUtils` | 日期工具 |
| `DictUtils` | 字典查询（缓存） |
| `Convert` | 对象转换（基于 Hutool） |
| `PageUtils` | 分页参数处理 |
| `BaseEntity` | 所有 entity 基类（含 id / 时间字段 / 租户字段 / delFlag） |

**自动配置**：`spring.factories` 或 `AutoConfiguration.imports` 注册。

## ruoyi-common-json（7 文件）

Jackson 序列化配置：

- `JsonUtils` —— 静态 JSON 工具（`toJson` / `parse`）
- `BigNumberSerializer` / `BigNumberDeserializer` —— BigDecimal / BigInteger 防止精度丢失
- `LongSerializer` / `LongDeserializer` —— Long 转 String 防前端精度问题（**常见坑**）
- `NumberSerializer` —— 数字统一转字符串
- `JacksonConfig` —— Spring Boot 自动配置

**关键决策**：所有 Long 在 JSON 里**默认转字符串**（避免 JavaScript 数字精度丢失）。前端需要用 `parseInt(str)` 转回。

**配合 application.yml**：

```yaml
spring:
  jackson:
    serialization:
      fail-on-empty-beans: false
    deserialization:
      fail-on-unknown-properties: false
```

## ruoyi-common-doc（3 文件）

springdoc / Swagger UI 集成：

- `OpenApiConfig` —— 全局 OpenAPI 配置
- `SpringDocUtils` —— Springdoc 工具
- `Customizer` —— 自定义分组

**6 个 OpenAPI 分组**（参见 [application-yml.md](../raw/project-skeleton/application-yml.md)）：

```yaml
springdoc:
  group-configs:
    - group: 1.演示模块       # org.ruoyi.demo
    - group: 2.通用模块       # org.ruoyi.web
    - group: 3.系统模块       # org.ruoyi.system
    - group: 4.代码生成模块   # org.ruoyi.generator
    - group: 5.工作流模块     # org.ruoyi.workflow
    - group: 6.MCP模块       # org.ruoyi.mcp
```

新增分组时同时改 application.yml 和扫描包。

## ruoyi-common-excel（19 文件）

基于 **FastExcel**（阿里 EasyExcel 替代）的导入导出。

参见 [ExcelDictFormat.md](../raw/common-source/ExcelDictFormat.md)。

**关键注解**：

| 注解 | 作用 |
|---|---|
| `@ExcelProperty("列名")` | 字段映射 |
| `@ExcelDictFormat(dictType = "sys_user_sex")` | 字典转换（sex:0 → 翻译为"男"） |
| `@ExcelDictFormat(readConverterExp = "0=男,1=女")` | 简单字典映射 |
| `@CellMerge` | 合并单元格 |
| `@HeadRowHeight` / `@ColumnWidth` | 样式 |

**导出**：

```java
response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
response.setHeader("Content-Disposition", "attachment;filename=data.xlsx");
EasyExcel.write(response.getOutputStream(), SysUserExportVo.class)
    .sheet("用户列表")
    .doWrite(dataList);
```

**导入**：

```java
EasyExcel.read(inputStream, SysUserImportVo.class, new SysUserImportListener(userMapper))
    .sheet().doRead();
```

参见 [modules/system-listener-runner.md § SysUserImportListener](../modules/system-listener-runner.md)。

## ruoyi-common-global-exception-handler

参见 [global-exception-handler.md](../raw/common-source/global-exception-handler.md)。

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ServiceException.class)
    public R<Void> handleService(ServiceException e) { ... }

    @ExceptionHandler(NotPermissionException.class)
    public R<Void> handleNotPermission(NotPermissionException e) { ... }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValid(MethodArgumentNotValidException e) { ... }

    @ExceptionHandler(Exception.class)
    public R<Void> handleAny(Exception e) { ... }
}
```

**约定**：
- ServiceException → code 500 + msg
- NotLoginException → code 401
- NotPermissionException → code 403
- 校验异常 → code 400 + 字段错误列表
- 其他 Exception → code 500 + 默认 msg

**前端约定**：根据 code 判断成功 / 失败 / 登录态 / 权限。