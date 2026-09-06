# API-03 handoff 规格 — `Api03AcceptanceTest`（MockMvc 真实 HTTP 反例）

> 交付方：第二方独立复核会话（root-94ae 之外的第二方，只读边界）
> 接收方：后端认证 owner 泳道（兄弟会话，`IpdServiceExceptionAdvice.java` / `IpdAuthController.java` 在途编辑者）
> 日期：2026-09-05　基线 HEAD：`c85f4009`
> 边界：本会话**不代写** `Api03AcceptanceTest`（遵 SSOT L311 用户本轮边界 + r2 报告 §三/§七 + OPS-09 归属纪律）。本文仅交付可直接落地的测试契约与骨架。

## 一、为何 API-03 尚未闭环（假绿防护）

- **修复方向已落地**（脏工作树，兄弟在途）：`IpdServiceExceptionAdvice.java` L64-70 新增
  `@ExceptionHandler(IpdAuthInputException.class)` → `ApiV1ErrorCode.PARAM_INVALID`（`10001` → HTTP `400`）。
- **端点链已就位**：`IpdAuthController` L78-94 `POST /api/v1/auth/change-password`
  → `authService.changePassword(...)`；捕获 `IpdAuthInputException`（`CURRENT_PASSWORD_INCORRECT` 时写 `PASSWORD_CHANGE_REJECTED` 审计，REQUIRES_NEW 独立事务）后 **rethrow** → 交由 advice。
- **缺口**：`find src/test -iname 'Api03*'` → 不存在。当前仅服务层 `IpdAuthChangePasswordExceptionTest`（6 case）+ `Api01AcceptanceTest`（直调 handler）为绿。**服务层绿 + handler 就位 ≠ HTTP 层闭环**——卡片验收明确要求「补一条 MockMvc 真实 HTTP 反例证明状态码与 code，不得仅靠服务层断言关闭」。
- **判定**：API-03 保持未收口（不翻 done），唯缺 `Api03AcceptanceTest`。

## 二、映射契约（断言基线，禁猜）

| 场景 | 触发异常 | advice handler | HTTP | `code` |
|---|---|---|---|---|
| 原密码错误 | `IpdAuthInputException(CURRENT_PASSWORD_INCORRECT)` | `handleIpdAuthInput` | **400** | **10001** PARAM_INVALID |
| 新密码与当前相同 | `IpdAuthInputException(PASSWORD_UNCHANGED)` | `handleIpdAuthInput` | **400** | **10001** |
| 服务层密码长度/字节校验 | `IpdAuthInputException(PASSWORD_LENGTH)` | `handleIpdAuthInput` | **400** | **10001** |
| `newPassword` < 8 位（Bean 校验，未进 service） | `MethodArgumentNotValidException` | `handleValidation` | **400** | **10001** |
| 合法改密成功 | 无 | — | **200** | **0** OK（+ `session.revokeAll`） |

- 响应体一律为 `ApiV1Response` **五字段包络**：`{ code, message, data, timestamp, traceId }`（`ApiV1Response.java` L24-28，`@JsonInclude(ALWAYS)`）。
- **严禁**以 HTTP `500` 或 `code=90001`（INTERNAL_ERROR）作为任何改密可纠正输入反例的期望——那正是本卡要修复前的兜底缺陷形态。
- `PasswordRequest` 定义（`IpdAuthController` L29-30）：`@NotBlank @Size(max=72) currentPassword`、`@NotBlank @Size(min=8,max=72) newPassword`。

## 三、`Api03AcceptanceTest` 骨架（owner 泳道可直接落地）

- 位置：`ruoyi-modules/ruoyi-ipd/src/test/java/org/ruoyi/ipd/advice/Api03AcceptanceTest.java`
- 必须 `@Tag("dev")`（否则 Surefire `<groups>${profiles.active}` 静默跳过 = 假绿）。
- 采用 `MockMvcBuilders.standaloneSetup(...)` + `.setControllerAdvice(new IpdServiceExceptionAdvice())`，Mockito 打桩 `IpdAuthSession` / `IpdAuthService` / `AuditAttemptService`（无需 `@SpringBootTest`，规避并发窗口下的重装配噪声）。

```java
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class Api03AcceptanceTest {

    @Mock IpdAuthService authService;
    @Mock IpdAuthSession session;
    @Mock AuditAttemptService auditAttempt;
    @InjectMocks IpdAuthController controller;

    private MockMvc mvc;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new IpdServiceExceptionAdvice())
            .build();
    }

    private Person person() { /* 构造 id/name/personType/groupId 最小 Person */ }

    // 反例①：原密码错误 ⇒ 400 + code 10001（service 抛 IpdAuthInputException）
    @Test @DisplayName("改密-原密码错误 ⇒ HTTP400 code10001 五字段包络")
    void wrongCurrentPassword_400_10001() throws Exception {
        when(session.currentPerson()).thenReturn(person());
        doThrow(new IpdAuthInputException(IpdAuthInputException.Reason.CURRENT_PASSWORD_INCORRECT))
            .when(authService).changePassword(any(), eq("wrongOld"), eq("ValidNew123"));

        String body = om.writeValueAsString(
            new IpdAuthController.PasswordRequest("wrongOld", "ValidNew123"));

        mvc.perform(post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())                       // 400，禁 500
            .andExpect(jsonPath("$.code").value(10001))              // PARAM_INVALID，禁 90001
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.traceId").doesNotExist())         // 五字段包络（traceId 可空但字段在）
            .andExpect(jsonPath("$.data").doesNotExist());
        // 审计：CURRENT_PASSWORD_INCORRECT 触发 PASSWORD_CHANGE_REJECTED
        verify(auditAttempt).record(any(), eq(AuditAttemptService.Outcome.FAILURE),
            eq("PASSWORD_CHANGE_REJECTED"), eq("persons"), any(), any());
    }

    // 反例②：newPassword < 8 位 ⇒ Bean 校验 400 + code 10001（未进 service，不需 auth）
    @Test @DisplayName("改密-新密码过短 ⇒ HTTP400 code10001（MethodArgumentNotValid）")
    void shortNewPassword_400_10001() throws Exception {
        String body = om.writeValueAsString(
            new IpdAuthController.PasswordRequest("anyOld123", "short"));
        mvc.perform(post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(10001));
        verifyNoInteractions(authService); // 校验失败不得触达改密逻辑
    }

    // 正例：合法改密 ⇒ 200 + code 0 + revokeAll
    @Test @DisplayName("改密-合法 ⇒ HTTP200 code0 且 revokeAll")
    void validChange_200_ok() throws Exception {
        when(session.currentPerson()).thenReturn(person());
        String body = om.writeValueAsString(
            new IpdAuthController.PasswordRequest("OldPass123", "NewPass456"));
        mvc.perform(post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
        verify(session).revokeAll(any());
    }
}
```

> 注：`jsonPath("$.traceId").doesNotExist()` 与 `.value(...)` 的取舍以实际序列化为准（`@JsonInclude(ALWAYS)` 下字段恒在、值可能为 null）；核心不可让步断言是 **status=400 且 code=10001**，正例 **status=200 且 code=0**。

## 四、验收命令与收口条件（owner 泳道执行）

```bash
JAVA_HOME=/Users/mac/tools/jdk-17/Contents/Home \
/Users/mac/tools/maven/bin/mvn -o -pl ruoyi-modules/ruoyi-ipd \
  -Dtest=Api03AcceptanceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- 复核方纪律（若由第二方复验）：**错峰 + 单模块 + 不带 `-am` 不带 `clean`**，复用已编译产物；结论前复查同一命令是否被兄弟会话改写；证据带 HH:MM:SS。
- 收口条件：surefire XML `tests > 0`（确认未被 `<groups>` 静默跳过）+ 正反例证据（400/10001 与 200/0）齐备后，方可将 API-03 看板状态 `set done`。**在此之前禁凭服务层 16/16 绿关闭。**

## 五、本轮第二方已完成的只读佐证

- 12:32:46 `mvn -o -pl ruoyi-modules/ruoyi-ipd test-compile` → **BUILD SUCCESS**（green gate 当前 OK，P064 构造器冲突已消解）。
- 静态确认 advice handler（L64-70）+ 端点 rethrow（L78-94）+ 错误码映射（`ApiV1ErrorCode` PARAM_INVALID→400 / INTERNAL_ERROR→500）链路完整。
- 真实 HTTP 层反例证据见 `验收/inreview-8卡只读回归-20260905.md`（P0-7.2 改密反例共用同一端点）。
