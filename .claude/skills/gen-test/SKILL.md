---
name: gen-test
description: 为 ruoyi-ai 项目按现有 `@Tag("dev")` Surefire 过滤规范，生成 Service / Controller 层单元测试。使用 Mockito + AssertJ，覆盖正常路径、参数校验、租户隔离、权限注解。
disable-model-invocation: true
---

# gen-test

为 RuoYi-AI 项目生成符合项目约定的单元测试。

## 何时使用

- 新写一个 Service / Controller，需要补单测。
- 现有 Service 加了新方法，需要补分支覆盖。
- 重构了工具类（Util），需要锁定行为。
- CI 报告显示 `surefire` 覆盖率告警。

## 项目测试规范（必须遵守）

### 关键约束

- Maven Surefire 配 `<groups>${profiles.active}</groups>`（见 `pom.xml:464-476`），默认跑 `@Tag("dev")`。
- **每个测试类必须显式 `@Tag("dev")`**，否则 `mvn test` 不会执行。
- 集成测试（启动 Spring 上下文）用 `@Tag("integration")` + `@Tag("dev")` 双注解，避免误跑。
- `@Tag("exclude")` 显式排除（性能 / 手动测试）。

### 依赖（已在 `ruoyi-common-bom` 统一）

```xml
spring-boot-starter-test     // JUnit 5 + Mockito + AssertJ + Spring Test
```

不要新增额外测试依赖，直接 `import static` 即可。

## 测试类模板

### Service 单测（Mockito 风格）

```java
package org.ruoyi.system.service.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
class SysConfigServiceImplTest {

    @Mock private SysConfigMapper baseMapper;
    @InjectMocks private SysConfigServiceImpl service;

    @Test
    @DisplayName("根据 configId 查询 - 命中缓存")
    void getById_cacheHit() {
        SysConfig cached = new SysConfig();
        cached.setConfigId(1L);
        cached.setConfigValue("hit");
        when(baseMapper.selectById(1L)).thenReturn(cached);

        SysConfig actual = service.getById(1L);

        assertThat(actual.getConfigValue()).isEqualTo("hit");
        verify(baseMapper, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("根据 configId 查询 - 未命中抛 ServiceException")
    void getById_notFound() {
        when(baseMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.getById(99L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("配置不存在");
    }
}
```

### Controller 单测（MockMvc 风格）

```java
@Tag("dev")
@WebMvcTest(SysConfigController.class)
class SysConfigControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private ISysConfigService service;

    @Test
    void list_returnsTableDataInfo() throws Exception {
        when(service.list(any(), any())).thenReturn(TableDataInfo.build(Collections.emptyList()));

        mockMvc.perform(get("/system/config/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
```

### 集成测试（启动 Spring 上下文）

```java
@Tag("integration")
@Tag("dev")
@SpringBootTest
@TestPropertySource(properties = "demo.enabled=false")
class ChatAgentIntegrationTest {
    // 需要真实 MySQL / Redis / 向量库 — 跑前确认 docker compose 已起
}
```

## 必须覆盖的维度

每个 Service 至少覆盖：

1. **正常路径**：所有 public 方法的主路径。
2. **参数校验**：null / 空 / 非法值 → 抛 `ServiceException`。
3. **多租户过滤**：构造含 `tenantId` 的上下文，确认 mapper 调用带 tenant 条件（参考 `MybatisTenantInterceptor`）。
4. **权限边界**：需要 `StpUtil.checkPermission(...)` 的方法，缺权限时抛 `NotPermissionException`。
5. **幂等 / 重复操作**：update / delete 重复调用不报错。
6. **异常转换**：`RuntimeException` 是否被捕获并转 `ServiceException`（参考 `GlobalExceptionHandler`）。

## Mock 合法性规约（2026-09-08 立，防假绿）

> 触发：WB-17-1 Gate 仲裁真活验证发现 mock 造了真库不可能的数据组合，单测全绿业务死路。规约本体与存量违法清单（A/B 类）见 `docs/ipd-系统说明/mock合法性与已知死路登记-20260908.md`。

为聚合器 / 待办类 Service 生成测试时，三条硬规则：

1. **写测试前先读写入路径**：mock 的投递锚字段（confirmerId / leaderId / reviewerId / decision / arbitratorId 等）取值，必须能由真实写入路径产生——先 grep 对应 Service 的 insert/update 链，确认「什么状态下哪些字段是什么值」。反例：mock 给 PENDING_SECOND 态配了 confirm 时才回填的 confirmerId。
2. **状态组合必须满足状态机**：外层状态与子行状态的组合必须真实可达（仲裁行只挂 REJECTED gate；「确认人 ID 回填」与离开待办态是同一事务的两面）。复制粘贴相邻聚合器测试时，强制 diff 检查状态过滤条件。
3. **NOT NULL 列必须显式赋值**：mock 依赖表的 NOT NULL 列在 builder 中必须给值——与被测路径无关也要补，防「真库不可能行」潜伏。

配套：「未办态」用例优先走真实写入路径构造数据（如 `service.sign()` 真实触发而非 builder 直造）；纯 mock 用例须在 `@DisplayName` 标注「未覆盖 DDL 合法性」。

## 禁止清单

- ❌ 测试类不加 `@Tag("dev")`（会被静默跳过）。
- ❌ mock 真库不可能的数据组合（违反上节三条硬规则——单测全绿业务死路）。
- ❌ 用 `@SpringBootTest` 测纯 Service（速度慢且没必要）。
- ❌ 用真实数据库 / Redis（mock 即可；集成测试另起 `@Tag("integration")`）。
- ❌ 复制 `RuoYiAIApplication` 启动做集成测试（已有 `ruoyi-admin` 即可）。
- ❌ 在测试里 `Thread.sleep` 等待异步结果（用 Awaitility 或回调通知）。

## 输出交付物

调用完成后必须给出：

1. 新建的所有测试文件清单（含相对路径）。
2. 跑测命令：`mvn test -pl <module> -Dtest=<ClassName>`。
3. 覆盖率缺口清单（哪些 public 方法还没覆盖，需要 follow-up）。
4. 集成测试待办（如有需要 `@Tag("integration")` 的场景）。