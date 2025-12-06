package org.ruoyi.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.core.collection.CollUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.Constants;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.domain.model.*;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StreamUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.tenant.helper.TenantHelper;
import org.ruoyi.system.domain.bo.SysTenantBo;
import org.ruoyi.system.domain.vo.LoginTenantVo;
import org.ruoyi.system.domain.vo.LoginVo;
import org.ruoyi.system.domain.vo.SysTenantVo;
import org.ruoyi.system.domain.vo.TenantListVo;
import org.ruoyi.system.service.ISysTenantService;
import org.ruoyi.system.service.SysLoginService;
import org.ruoyi.system.service.SysRegisterService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.util.List;

/**
 * 认证
 *
 * @author Lion Li
 */
@SaIgnore
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SysLoginService loginService;
    private final SysRegisterService registerService;
    private final ISysTenantService tenantService;

    /**
     * 登录方法
     *
     * @param body 登录信息
     * @return 结果
     */
    @PostMapping("/login")
    public R<LoginVo> login(@Validated @RequestBody LoginBody body) {
        body.setTenantId(Constants.TENANT_ID);
        LoginVo loginVo = new LoginVo();
        // 生成令牌
        String token = loginService.login(
                body.getTenantId(),
                body.getUsername(), body.getPassword(),
                body.getCode(), body.getUuid());
        loginVo.setToken(token);
        // 兼容后台管理登录
        loginVo.setAccess_token(token);
        loginVo.setUserInfo(LoginHelper.getLoginUser());
        return R.ok(loginVo);
    }

    /**
     * 短信登录
     *
     * @param body 登录信息
     * @return 结果
     */
    @PostMapping("/smsLogin")
    public R<LoginVo> smsLogin(@Validated @RequestBody SmsLoginBody body) {
        LoginVo loginVo = new LoginVo();
        // 生成令牌
        String token = loginService.smsLogin(body.getTenantId(), body.getPhonenumber(), body.getSmsCode());
        loginVo.setToken(token);
        return R.ok(loginVo);
    }

    /**
     * 访客登录
     *
     * @param loginBody 登录信息
     * @return token信息
     */
    @PostMapping("/visitorLogin")
    public R<LoginVo> visitorLogin(@RequestBody VisitorLoginBody loginBody) {
        LoginVo loginVo = new LoginVo();
        return R.ok(loginVo);
    }

    /**
     * 邮件登录
     *
     * @param body 登录信息
     * @return 结果
     */
    @PostMapping("/emailLogin")
    public R<LoginVo> emailLogin(@Validated @RequestBody EmailLoginBody body) {
        LoginVo loginVo = new LoginVo();
        // 生成令牌
        String token = loginService.emailLogin(body.getTenantId(), body.getEmail(), body.getEmailCode());
        loginVo.setToken(token);
        return R.ok(loginVo);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public R<Void> logout() {
        loginService.logout();
        return R.ok("退出成功");
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public R<Void> register(@Validated @RequestBody RegisterBody user, HttpServletRequest request) {
        String domainName = request.getServerName();
        user.setDomainName(domainName);
        registerService.register(user);
        return R.ok();
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset/password")
    @SaIgnore
    public R<Void> resetPassWord(@Validated @RequestBody RegisterBody user) {
        registerService.resetPassWord(user);
        return R.ok();
    }

    /**
     * 登录页面租户下拉框
     *
     * @return 租户列表
     */
    @GetMapping("/tenant/list")
    public R<LoginTenantVo> tenantList(HttpServletRequest request) throws Exception {
        List<SysTenantVo> tenantList = tenantService.queryList(new SysTenantBo());
        List<TenantListVo> voList = MapstructUtils.convert(tenantList, TenantListVo.class);
        // 获取域名
        String host = new URL(request.getRequestURL().toString()).getHost();
        // 根据域名进行筛选
        List<TenantListVo> list = StreamUtils.filter(voList, vo -> StringUtils.equals(vo.getDomain(), host));
        // 返回对象
        LoginTenantVo vo = new LoginTenantVo();
        vo.setVoList(CollUtil.isNotEmpty(list) ? list : voList);
        vo.setTenantEnabled(TenantHelper.isEnable());
        return R.ok(vo);
    }

}
