package org.ruoyi.ipd.config;

import cn.dev33.satoken.exception.NotLoginException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermissionException;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SEC 兜底：/api/v1/** 业务接口统一走 IPD 会话校验（StpLogic "ipd"），
 * 防止基线 security.excludes 放行 /api/v1/** 后出现匿名访问。
 * 认证入口 /api/v1/auth/login 保持匿名；auth 域其余接口内部自行 checkLogin。
 * 拒绝统一抛 IpdPermissionException(401, UNAUTHORIZED)，由
 * IpdPermissionExceptionHandler 转为 ApiV1Response 包络。
 */
@Configuration
public class IpdWebSecurityConfig implements WebMvcConfigurer {

    private final IpdAuthSession session;

    public IpdWebSecurityConfig(IpdAuthSession session) {
        this.session = session;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                try {
                    session.currentPerson();
                } catch (NotLoginException e) {
                    throw new IpdPermissionException(401, ApiV1ErrorCode.UNAUTHORIZED);
                }
                return true;
            }
        }).addPathPatterns("/api/v1/**")
            .excludePathPatterns("/api/v1/auth/login");
    }
}
