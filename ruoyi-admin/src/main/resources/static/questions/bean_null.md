# 实体bean为空问题
- - -
### 问题排查

检查是否存在 `链式调用` 注解 `@Accessors(chain = true)` 删除即可

### 原因
java 规范 set 返回值为 `void` 链式调用 set 返回值为 `this`<br>
故多数框架底层使用 jdk 工具导致找不到 set 方法<br>
例如: `easyexcel` `cglib` `mybatis` 等