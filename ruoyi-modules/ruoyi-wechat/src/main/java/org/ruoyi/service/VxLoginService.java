package org.ruoyi.service;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.ruoyi.common.core.constant.Constants;
import org.ruoyi.common.core.domain.model.VisitorLoginUser;
import org.ruoyi.common.core.enums.DeviceType;
import org.ruoyi.common.core.enums.UserType;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.MessageUtils;
import org.ruoyi.common.core.utils.ServletUtils;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.log.event.LogininforEvent;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.domain.bo.SysUserBo;
import org.ruoyi.system.domain.vo.LoginVo;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.service.ISysUserService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 *  微信公众号登录
 *
 * @author ageerle@163.com
 * date 2025/4/30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VxLoginService {

    private final ISysUserService userService;

    private final ConfigService configService;

    public LoginVo mpLogin(String openid) {
        // 使用 openid 查询绑定用户 如未绑定用户 则根据业务自行处理 例如 创建默认用户
        SysUserVo user = userService.selectUserByOpenId(openid);
        VisitorLoginUser loginUser = new VisitorLoginUser();
        if (ObjectUtil.isNull(user)) {
            SysUserBo sysUser = new SysUserBo();
            String name = "用户" + UUID.randomUUID().toString().replace("-", "");
            // 设置默认用户名
            sysUser.setUserName(name);
            // 设置默认昵称
            sysUser.setNickName(name);
            // 设置默认密码
            sysUser.setPassword(BCrypt.hashpw("123456"));
            // 设置微信openId
            sysUser.setOpenId(openid);
            String configValue = configService.getConfigValue("mail", "amount");
            // 设置默认余额
            sysUser.setUserBalance(NumberUtils.toDouble(configValue, 1));
            // 注册用户,设置默认租户为0
            SysUser registerUser = userService.registerUser(sysUser, "0");

            // 构建登录用户信息
            loginUser.setTenantId("0");
            loginUser.setUserId(registerUser.getUserId());
            loginUser.setUsername(registerUser.getUserName());
            loginUser.setUserType(UserType.APP_USER.getUserType());
            loginUser.setOpenid(openid);
            loginUser.setNickName(registerUser.getNickName());
        } else {
            // 此处可根据登录用户的数据不同 自行创建 loginUser
            loginUser.setTenantId(user.getTenantId());
            loginUser.setUserId(user.getUserId());
            loginUser.setUsername(user.getUserName());
            loginUser.setUserType(user.getUserType());
            loginUser.setNickName(user.getNickName());
            loginUser.setAvatar(user.getWxAvatar());
            loginUser.setOpenid(openid);
        }
        // 生成token
        LoginHelper.loginByDevice(loginUser, DeviceType.XCX);
        recordLogininfor(loginUser.getTenantId(), loginUser.getUsername(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        LoginVo loginVo = new LoginVo();
        // 生成令牌
        loginVo.setToken(StpUtil.getTokenValue());
        loginVo.setUserInfo(loginUser);
        return loginVo;
    }

    /**
     * 记录登录信息
     *
     * @param tenantId 租户ID
     * @param username 用户名
     * @param status   状态
     * @param message  消息内容
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
