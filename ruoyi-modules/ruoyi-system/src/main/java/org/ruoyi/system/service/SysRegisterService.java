package org.ruoyi.system.service;

import cn.dev33.satoken.secure.BCrypt;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.constant.Constants;
import org.ruoyi.common.core.constant.GlobalConstants;
import org.ruoyi.common.core.domain.model.RegisterBody;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.core.exception.user.CaptchaException;
import org.ruoyi.common.core.exception.user.CaptchaExpireException;
import org.ruoyi.common.core.exception.user.UserException;
import org.ruoyi.common.core.utils.MessageUtils;
import org.ruoyi.common.core.utils.ServletUtils;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.log.event.LogininforEvent;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.domain.SysUserRole;
import org.ruoyi.system.domain.bo.SysUserBo;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;

/**
 * 注册校验方法
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysRegisterService {

    private final ISysUserService userService;

    private final SysUserRoleMapper userRoleMapper;

    /**
     * 注册
     */
    public void register(RegisterBody registerBody) {


        String tenantId = Constants.TENANT_ID;
        if (StringUtils.isNotBlank(registerBody.getTenantId())) {
            tenantId = registerBody.getTenantId();
        }
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();

        // 检查验证码是否正确
        validateEmail(username, registerBody.getCode());
        SysUserBo sysUser = new SysUserBo();
        sysUser.setDomainName(registerBody.getDomainName());
        sysUser.setUserName(username);
        sysUser.setNickName(username);
        sysUser.setPassword(BCrypt.hashpw(password));
        if (!userService.checkUserNameUnique(sysUser)) {
            throw new UserException("添加用户失败", username);
        }
        sysUser.setUserBalance(1.0);
        SysUser user = userService.registerUser(sysUser, tenantId);
        if (user == null) {
            throw new UserException("用户注册失败!");
        }
        // 设置默认角色
        SysUserRole sysRole = new SysUserRole();
        sysRole.setUserId(user.getUserId());
        sysRole.setRoleId(1L);
        userRoleMapper.insert(sysRole);
        recordLogininfor(tenantId, username, Constants.REGISTER, MessageUtils.message("user.register.success"));
    }

    /**
     * 重置密码
     */
    public void resetPassWord(RegisterBody registerBody) {
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        SysUserVo user = userService.selectUserByUserName(username);
        if (user == null) {
            throw new UserException(String.format("用户【%s】,未注册!", username));
        }
        // 检查验证码是否正确
        validateEmail(username, registerBody.getCode());
        userService.resetUserPwd(user.getUserId(), BCrypt.hashpw(password));
    }

    /**
     * 校验邮箱验证码
     *
     * @param username 用户名
     */
    public void validateEmail(String username, String code) {
        String key = GlobalConstants.CAPTCHA_CODE_KEY + username;
        String captcha = RedisUtils.getCacheObject(key);
        if (code.equals(captcha)) {
            RedisUtils.deleteObject(captcha);
        } else {
            throw new BaseException("验证码错误,请重试！");
        }
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     */
    public void validateCaptcha(String tenantId, String username, String code, String uuid) {
        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + StringUtils.defaultString(uuid, "");
        String captcha = RedisUtils.getCacheObject(verifyKey);
        RedisUtils.deleteObject(verifyKey);
        if (captcha == null) {
            recordLogininfor(tenantId, username, Constants.REGISTER, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha)) {
            recordLogininfor(tenantId, username, Constants.REGISTER, MessageUtils.message("user.jcaptcha.error"));
            throw new CaptchaException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @param status   状态
     * @param message  消息内容
     * @return
     */
    private void recordLogininfor(String tenantId, String username, String status, String message) {
        LogininforEvent logininforEvent = new LogininforEvent();
        logininforEvent.setTenantId(tenantId);
        logininforEvent.setUsername(username);
        logininforEvent.setStatus(status);
        logininforEvent.setMessage(message);
        logininforEvent.setRequest(ServletUtils.getRequest());
        SpringUtils.context().publishEvent(logininforEvent);
    }

}
