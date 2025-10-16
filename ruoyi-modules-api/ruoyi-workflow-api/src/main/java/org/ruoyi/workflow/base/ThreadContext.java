package org.ruoyi.workflow.base;

import io.micrometer.common.util.StringUtils;
import org.ruoyi.workflow.entity.User;
import org.ruoyi.workflow.enums.UserStatusEnum;
import org.ruoyi.workflow.exception.WorkflowBaseException;

import static org.ruoyi.workflow.enums.ErrorEnum.A_USER_NOT_FOUND;

public class ThreadContext {
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();
    private static final ThreadLocal<String> currentToken = new ThreadLocal<>();

    private ThreadContext() {
    }

    public static User getCurrentUser() {
        User user = new User();
        user.setName("admin");
        user.setEmail("12345@qq.com");
        user.setUuid("123456789");
        user.setUnderstandContextMsgPairNum(1);
        user.setQuotaByTokenDaily(1);
        user.setQuotaByTokenMonthly(1);
        user.setQuotaByRequestDaily(1);
        user.setQuotaByRequestMonthly(1);
        user.setQuotaByImageDaily(1);
        user.setQuotaByImageMonthly(1);
        user.setUserStatus(UserStatusEnum.NORMAL);
        user.setIsAdmin(true);
        user.setId(1L);
        return user;
    }

    public static void setCurrentUser(User user) {
        currentUser.set(user);
    }

    public static Long getCurrentUserId() {
        return 1L;
    }

    public static String getToken() {
        return currentToken.get();
    }

    public static void setToken(String token) {
        currentToken.set(token);
    }

    public static boolean isLogin() {
        return StringUtils.isNotBlank(currentToken.get());
    }

    public static User getExistCurrentUser() {
        User user = ThreadContext.getCurrentUser();
        if (null == user) {
            throw new WorkflowBaseException(A_USER_NOT_FOUND);
        }
        return user;
    }

    public void unload() {
        currentUser.remove();
        currentToken.remove();
    }
}
