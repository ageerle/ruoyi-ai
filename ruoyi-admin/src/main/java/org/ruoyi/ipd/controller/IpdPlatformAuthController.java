package org.ruoyi.ipd.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.SystemConstants;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.tenant.helper.TenantHelper;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.service.IpdAuthService;
import org.ruoyi.system.domain.SysClient;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.domain.vo.SysClientVo;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.mapper.SysClientMapper;
import org.ruoyi.system.mapper.SysUserMapper;
import org.ruoyi.system.service.SysLoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 平台会话桥（2026-09-06 owner 指令：后端原有 AI/知识库功能须在前端完整可用）。
 *
 * <p>IPD 会话（loginType=ipd，独立 StpLogic）与基线平台会话（loginType 默认）互相独立；
 * 前端持 IPD 票调用 /chat/**、/system/menu 等原平台接口会 401。本端点在验明 IPD 会话后，
 * 于服务端内部按同名 sys_user（缺失时按 personType 兜底映射 ipd-* QA 账号）签发基线平台票，
 * 前端凭此票访问原平台功能。平台侧权限 = 被映射 sys_user 自身 RBAC，不放大；
 * 基线会话写入的是 sys_user.userId，绝不写 Person.id（IpdAuthSession 红线不破）。
 *
 * <p>放行说明：本控制器位于 /api/v1/auth/**，由 IpdWebSecurityConfig 的登录校验拦截器强制
 * 要求有效 IPD 会话（仅 /api/v1/auth/login 与 /api/v1/public/** 免登录），无需另行配置。
 * 包路径说明：物理上位于 ruoyi-admin 模块，但包名归入 org.ruoyi.ipd.controller——
 * 与模块内其它 IPD 接口共享 IpdPermissionExceptionHandler / IpdServiceExceptionAdvice
 * 的 ApiV1 包络与 HTTP 语义（basePackages 按包名匹配、与物理模块无关，fat jar classpath 合并合法；
 * PermissionAdviceCoverageTest 的扫描范围仅 ruoyi-ipd 模块内，不受影响）。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class IpdPlatformAuthController {

    /** 基线 sys_client.pc 客户端（clientKey），与基线登录页一致。 */
    private static final String PLATFORM_CLIENT_KEY = "pc";

    /** person.username 在 sys_user 无同名账号时（如中文名真实人员）按角色兜底映射。 */
    private static final java.util.Map<String, String> FALLBACK_BY_PERSON_TYPE = java.util.Map.of(
        "SUPER_ADMIN", "ipd-admin",
        "GROUP_LEADER", "ipd-leader",
        "MARKET_PM", "ipd-market",
        "RD_PM", "ipd-rd"
    );

    private final IpdAuthSession session;
    private final IpdAuthService authService;
    private final SysUserMapper userMapper;
    private final SysClientMapper clientMapper;
    private final SysLoginService loginService;

    /** 平台票视图。形状对齐 IPD 登录响应习惯（token/tokenType/expiresIn）。 */
    public record PlatformTokenView(String token, String tokenType, long expiresIn, String platformUser) { }

    @PostMapping("/platform-token")
    public ResponseEntity<ApiV1Response<PlatformTokenView>> platformToken() {
        Person person = session.currentPerson();
        if (authService.scopeOf(person) != IpdAuthService.Scope.FULL) {
            return ResponseEntity.status(ApiV1ErrorCode.FORBIDDEN.getHttpStatus())
                .body(ApiV1Response.fail(ApiV1ErrorCode.FORBIDDEN, "当前账号状态不可使用平台功能"));
        }
        // sys_user 查询须脱离当前线程租户上下文（IPD 请求不携带基线租户）；建会话按目标账号租户执行。
        SysUserVo user = TenantHelper.ignore(() -> resolvePlatformUser(person));
        if (user == null) {
            return ResponseEntity.status(ApiV1ErrorCode.NOT_FOUND.getHttpStatus())
                .body(ApiV1Response.fail(ApiV1ErrorCode.NOT_FOUND,
                    "未找到可映射的平台账号，请联系管理员登记 sys_user 同名账号"));
        }
        LoginUser loginUser = TenantHelper.dynamic(user.getTenantId(), () -> loginService.buildLoginUser(user));
        SaLoginParameter model = new SaLoginParameter();
        SysClientVo client = clientMapper.selectVoOne(
            new LambdaQueryWrapper<SysClient>().eq(SysClient::getClientKey, PLATFORM_CLIENT_KEY));
        if (client != null) {
            model.setDeviceType(client.getDeviceType());
            model.setTimeout(client.getTimeout());
            model.setActiveTimeout(client.getActiveTimeout());
            model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
        } else {
            model.setDeviceType(PLATFORM_CLIENT_KEY);
        }
        LoginHelper.login(loginUser, model);
        return ResponseEntity.ok(ApiV1Response.ok(
            new PlatformTokenView(StpUtil.getTokenValue(), "Bearer", StpUtil.getTokenTimeout(), user.getUserName())));
    }

    /** 同名优先，其次按 personType 兜底；停用账号视同不存在（不放大停用身份）。 */
    private SysUserVo resolvePlatformUser(Person person) {
        SysUserVo direct = loadEnabledUser(person.getUsername());
        return direct != null ? direct : loadEnabledUser(FALLBACK_BY_PERSON_TYPE.get(person.getPersonType()));
    }

    private SysUserVo loadEnabledUser(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        SysUserVo user = userMapper.selectVoOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserName, username));
        return user == null || SystemConstants.DISABLE.equals(user.getStatus()) ? null : user;
    }
}
