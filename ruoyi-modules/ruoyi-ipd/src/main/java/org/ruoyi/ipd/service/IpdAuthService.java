package org.ruoyi.ipd.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * IPD 认证状态机（D.0.6 /api/auth/*；B8 先移交后禁用；FROZEN_PENDING_HANDOVER 仅移交权限）
 *
 * 登录判定顺序：账号存在性/密码（合并报错防枚举）→ 在职状态 → 账号状态 → 授权范围。
 * - ACTIVE            → FULL（全量权限；must_change_pwd=1 时前端强制改密流程）
 * - FROZEN_PENDING_HANDOVER → HANDOVER_ONLY（仅移交相关权限，BR-HAND/B8）
 * - DISABLED / RESIGNED → 拒绝
 * 会话令牌签发（Sa-Token LoginHelper 绑定）随 P1 人员同步的控制器层落地，本引擎只产判定与状态迁移。
 */
@Service
@RequiredArgsConstructor
public class IpdAuthService {

    public enum Scope { FULL, PASSWORD_CHANGE_REQUIRED, HANDOVER_ONLY, NONE }

    public record LoginResult(Person person, Scope scope, boolean mustChangePwd) {
    }

    private final PersonMapper personMapper;
    private final AuditLogService auditLogService;

    /** 授权范围判定（IpdPermission 拦截器 / IpdAuthController me+refresh 复用）。
     * <p>映射：DISABLED/RESIGNED/状态异常→NONE；ACTIVE+mustChangePwd=1→PASSWORD_CHANGE_REQUIRED；
     * FROZEN_PENDING_HANDOVER→HANDOVER_ONLY；其余 ACTIVE→FULL。与 login() 状态机同源。 */
    public Scope scopeOf(Person person) {
        if (person == null || person.getId() == null) return Scope.NONE;
        if ("RESIGNED".equals(person.getEmploymentStatus())) return Scope.NONE;
        switch (nvl(person.getAccountStatus())) {
            case "ACTIVE", "" -> {
                if ("1".equals(nvl(person.getMustChangePwd()))) return Scope.PASSWORD_CHANGE_REQUIRED;
                return Scope.FULL;
            }
            case "FROZEN_PENDING_HANDOVER" -> { return Scope.HANDOVER_ONLY; }
            default -> { return Scope.NONE; }
        }
    }

    /** 登录：合并用户名/密码错误信息防枚举；状态机映射授权范围。
     * <p>SEC-AUD-01：失败分支也写审计 (action=LOGIN_FAIL)，auditLogService.append 走 REQUIRES_NEW
     * 不受本事务回滚影响，爆破/撞库/离职尝试均有运营/法务可见证据。
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResult login(String username, String rawPassword) {
        Person person = personMapper.selectOne(new LambdaQueryWrapper<Person>()
            .eq(Person::getUsername, username).last("limit 1"));
        if (person == null || !BCrypt.checkpw(rawPassword, nvl(person.getPasswordHash()))) {
            auditFail(person == null ? null : person.getId(), username, "BAD_CREDENTIALS");
            throw new ServiceException("用户名或密码错误");
        }
        if ("RESIGNED".equals(person.getEmploymentStatus())) {
            auditFail(person.getId(), username, "RESIGNED");
            throw new ServiceException("离职账号禁止登录");
        }
        Scope scope;
        switch (nvl(person.getAccountStatus())) {
            case "ACTIVE", "" -> scope = Scope.FULL;
            case "FROZEN_PENDING_HANDOVER" -> scope = Scope.HANDOVER_ONLY;
            case "DISABLED" -> {
                auditFail(person.getId(), username, "DISABLED");
                throw new ServiceException("账号已禁用，请联系超管");
            }
            default -> {
                auditFail(person.getId(), username, "STATUS_ABNORMAL:" + person.getAccountStatus());
                throw new ServiceException("账号状态异常: " + person.getAccountStatus());
            }
        }
        person.setLastLoginAt(new Date());
        personMapper.updateById(person);
        audit(person.getId(), person.getName(), "LOGIN");
        return new LoginResult(person, scope, "1".equals(nvl(person.getMustChangePwd())));
    }

    /** 授权范围判定（网关/拦截器复用）：冻结移交态仅放行移交域 */
    public boolean canAccessFull(Person person) {
        return "ACTIVE".equals(person.getAccountStatus())
            && !"RESIGNED".equals(person.getEmploymentStatus());
    }

    public List<String> permittedPaths(Person person) {
        if (canAccessFull(person)) {
            return List.of("*");
        }
        if ("FROZEN_PENDING_HANDOVER".equals(person.getAccountStatus())) {
            return List.of("/api/handovers/**", "/api/auth/password", "/api/auth/refresh");
        }
        return List.of();
    }

    /**
     * 改密：验旧密码；成功后清除首登强制改密标记。
     *
     * <p>W5-E-2.1 IDOR 修复（最高危 P0）：原签名仅接受 personId，攻击者可用任意登录
     * 会话调用此方法修改其他用户的密码——灾难性安全漏洞。新签名要求 caller 显式传入
     * 当前会话 actor，服务层校验：
     * <ol>
     *   <li>{@code actor == null || actor.id() == null} → UNAUTHORIZED（防御性兜底，控制器已 {@code requireInternal} 守门）</li>
     *   <li>{@code personId == null} → PARAM_INVALID（参数校验）</li>
     *   <li>非 SUPER_ADMIN 且 {@code actor.id() != personId} → FORBIDDEN（核心：仅本人改本人密码，超管运维豁免）</li>
     *   <li>其余业务逻辑（BCrypt 验旧密码、长度校验、新密码唯一性、must_change_pwd 清零、事务边界）保持不变</li>
     * </ol>
     *
     * @param actor 当前会话身份（必填，由 Controller {@code permission.requireInternal()} 传入）
     * @param personId 待修改密码的人员主键（必须 == actor.id()，除非 actor 为 SUPER_ADMIN）
     * @param oldRaw 当前明文密码（用于验旧）
     * @param newRaw 新明文密码（≥8 位 UTF-8 字节 ≤72）
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(IpdActor actor, Long personId, String oldRaw, String newRaw) {
        // 件 1.2：actor 缺失 → UNAUTHORIZED（防御性兜底，service 层不信任 controller 必传）
        if (actor == null || actor.id() == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.UNAUTHORIZED, "未认证或凭证失效");
        }
        // 件 1.3：personId 缺失 → PARAM_INVALID
        if (personId == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "personId 不能为空");
        }
        // 件 1.3 核心：仅本人改本人密码；SUPER_ADMIN 运维豁免（W5-E IDOR 修复）
        if (!"SUPER_ADMIN".equals(actor.role()) && !actor.id().equals(personId)) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "无权修改他人密码");
        }
        if (newRaw == null || newRaw.length() < 8) {
            throw new IpdAuthInputException(IpdAuthInputException.Reason.PASSWORD_LENGTH);
        }
        Person person = personMapper.selectById(personId);
        if (person == null || !BCrypt.checkpw(oldRaw, nvl(person.getPasswordHash()))) {
            throw new IpdAuthInputException(IpdAuthInputException.Reason.CURRENT_PASSWORD_INCORRECT);
        }
        if (BCrypt.checkpw(newRaw, nvl(person.getPasswordHash()))) {
            throw new IpdAuthInputException(IpdAuthInputException.Reason.PASSWORD_UNCHANGED);
        }
        person.setPasswordHash(BCrypt.hashpw(newRaw));
        person.setMustChangePwd("0");
        personMapper.updateById(person);
        audit(person.getId(), person.getName(), "PASSWORD_CHANGE");
    }

    /** 禁用前冻结（BR-HAND/B8：先进入待移交冻结态） */
    @Transactional(rollbackFor = Exception.class)
    public void freezeForHandover(Long personId, Long operatorId) {
        Person person = requirePerson(personId);
        if ("FROZEN_PENDING_HANDOVER".equals(person.getAccountStatus())) {
            return; // 幂等
        }
        if (!"ACTIVE".equals(person.getAccountStatus())) {
            throw new ServiceException("仅 ACTIVE 账号可进入冻结移交态");
        }
        person.setAccountStatus("FROZEN_PENDING_HANDOVER");
        personMapper.updateById(person);
        auditFor(person.getId(), person.getName(), operatorId, "ACCOUNT_FREEZE_PENDING_HANDOVER");
    }

    /** 移交完成后禁用：仅允许从 FROZEN_PENDING_HANDOVER 迁入（B8 顺序不可跳） */
    @Transactional(rollbackFor = Exception.class)
    public void disableAfterHandover(Long personId, Long operatorId) {
        Person person = requirePerson(personId);
        if (!"FROZEN_PENDING_HANDOVER".equals(person.getAccountStatus())) {
            throw new ServiceException("须先进入冻结移交态（先移交后禁用 B8）");
        }
        person.setAccountStatus("DISABLED");
        personMapper.updateById(person);
        auditFor(person.getId(), person.getName(), operatorId, "ACCOUNT_DISABLED_AFTER_HANDOVER");
    }

    private Person requirePerson(Long personId) {
        Person person = personMapper.selectById(personId);
        if (person == null) {
            throw new ServiceException("人员不存在: " + personId);
        }
        return person;
    }

    private void audit(Long personId, String name, String action) {
        auditFor(personId, name, personId, action);
    }

    private void auditFor(Long personId, String name, Long operatorId, String action) {
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId)
            .operatorName(name)
            .action(action)
            .entityType("persons")
            .entityId(personId)
            .createTime(new Date())
            .build());
    }

    /**
     * SEC-AUD-01：登录失败审计。
     * <p>auditLogService.append 走 REQUIRES_NEW，独立提交，不受 login() REQUIRED 事务回滚影响；
     * operatorId 在失败分支无可靠身份时记为 null（无法伪造业务身份）。
     */
    private void auditFail(Long personId, String username, String reason) {
        auditLogService.append(AuditLog.builder()
            .action("LOGIN_FAIL")
            .entityType("persons")
            .entityId(personId)
            .operatorName(username)
            .reason(reason)
            .createTime(new Date())
            .build());
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }
}