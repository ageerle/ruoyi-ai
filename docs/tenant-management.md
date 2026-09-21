# 新增租户接口

开启 `tenant.enable: true` 后，超级管理员可通过 `POST /system/tenant` 创建租户。请求需要登录凭证和 `system:tenant:add` 权限；使用反向代理时，请按部署配置添加接口前缀。

## 请求参数

请求使用 `Content-Type: application/json`。

| 参数 | 说明 |
| --- | --- |
| `companyName` | 企业名称，必填且不能与已有租户重复。 |
| `contactUserName` | 联系人，必填。 |
| `contactPhone` | 联系电话，必填。 |
| `username` | 租户初始管理员用户名，必填。 |
| `password` | 租户初始管理员密码，必填。 |
| `packageId` | 已存在的租户套餐 ID，必填。建议使用字符串传输，避免浏览器对大整数的精度损失。 |
| `expireTime` | 过期时间，使用 `yyyy-MM-dd HH:mm:ss`，例如 `2027-09-11 00:00:00`；不传或传 `null` 表示不限制有效期。未带时区的时间按后端运行时区解释。 |
| `accountCount` | 可创建的用户数量，`-1` 表示不限制。 |

## 请求示例

请将套餐 ID 替换为当前系统中的有效套餐，并设置实际使用的管理员账号和密码。

```json
{
  "companyName": "示例企业",
  "contactUserName": "联系人",
  "contactPhone": "13800000000",
  "username": "tenant_admin",
  "password": "ChangeMe_123456!",
  "packageId": "2018611998196109314",
  "expireTime": "2027-09-11 00:00:00",
  "accountCount": -1
}
```

## 日期配置

后端默认日期格式配置位于 `ruoyi-admin/src/main/resources/application.yml`：

```yaml
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
```

扩展应用的 `ObjectMapper` 时，使用 Spring 注入的 `Jackson2ObjectMapperBuilder`，以保留全局日期格式、自定义 Jackson 模块和构建器配置。
