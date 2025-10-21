package org.ruoyi.workflow.base;

import cn.dev33.satoken.stp.StpUtil;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.workflow.entity.User;
import org.ruoyi.workflow.enums.UserStatusEnum;

import static org.ruoyi.workflow.enums.ErrorEnum.A_USER_NOT_FOUND;

/**
 * 线程上下文适配器，统一接入 Sa-Token 登录态。
 */
public class ThreadContext {

    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_TOKEN = new ThreadLocal<>();

    private ThreadContext() {
    }

    /**
     * 获取当前登录的工作流用户。
     */
    public static User getCurrentUser() {
        User cached = CURRENT_USER.get();
        if (cached != null) {
            return cached;
        }
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            throw new BaseException(A_USER_NOT_FOUND.getInfo());
        }
        User mapped = mapToWorkflowUser(loginUser);
        CURRENT_USER.set(mapped);
        return mapped;
    }

    /**
     * 允许在测试或特殊场景下显式设置当前用户。
     */
    public static void setCurrentUser(User user) {
        if (user == null) {
            CURRENT_USER.remove();
        } else {
            CURRENT_USER.set(user);
        }
    }

    /**
     * 获取当前登录用户 ID。
     */
    public static Long getCurrentUserId() {
        Long userId = LoginHelper.getUserId();
        if (userId != null) {
            return userId;
        }
        return getCurrentUser().getId();
    }

    /**
     * 获取当前访问 token。
     */
    public static String getToken() {
        String token = CURRENT_TOKEN.get();
        if (StringUtils.isNotBlank(token)) {
            return token;
        }
        try {
            token = StpUtil.getTokenValue();
        } catch (Exception ignore) {
            token = null;
        }
        if (StringUtils.isNotBlank(token)) {
            CURRENT_TOKEN.set(token);
        }
        return token;
    }

    public static void setToken(String token) {
        if (StringUtils.isBlank(token)) {
            CURRENT_TOKEN.remove();
        } else {
            CURRENT_TOKEN.set(token);
        }
    }

    public static boolean isLogin() {
        return LoginHelper.isLogin();
    }

    public static User getExistCurrentUser() {
        return getCurrentUser();
    }

    public static void unload() {
        CURRENT_USER.remove();
        CURRENT_TOKEN.remove();
    }

    private static User mapToWorkflowUser(LoginUser loginUser) {
        User user = new User();
        user.setId(loginUser.getUserId());
        String nickname = loginUser.getNickName();
        user.setName(StringUtils.defaultIfBlank(nickname, loginUser.getUsername()));
        user.setEmail(loginUser.getUsername());
        user.setUuid(String.valueOf(loginUser.getUserId()));
        user.setUserStatus(UserStatusEnum.NORMAL);
        user.setIsAdmin(LoginHelper.isSuperAdmin(loginUser.getUserId()));
        user.setUnderstandContextMsgPairNum(0);
        user.setQuotaByTokenDaily(0);
        user.setQuotaByTokenMonthly(0);
        user.setQuotaByRequestDaily(0);
        user.setQuotaByRequestMonthly(0);
        user.setQuotaByImageDaily(0);
        user.setQuotaByImageMonthly(0);
        user.setIsDeleted(false);
        return user;
    }
}
