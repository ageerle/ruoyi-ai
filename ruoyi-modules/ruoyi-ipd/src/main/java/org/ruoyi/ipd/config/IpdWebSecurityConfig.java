package org.ruoyi.ipd.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermissionException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SEC 兜底：/api/v1/** 业务接口统一走 IPD 会话 + 注解鉴权。
 * <p>
 * 基线 SecurityConfig 已 exclude /api/v1/**，故此处自行：
 * <ol>
 *   <li>登录校验（StpLogic "ipd"）</li>
 *   <li>SaInterceptor 注解鉴权（@SaCheckPermission type=ipd）</li>
 * </ol>
 * 公开入口豁免：
 * <ul>
 *   <li>/api/v1/auth/login——认证入口</li>
 *   <li>/api/v1/public/**——需求门户公开端点（P4-1.1 游客 submit/products/trace）</li>
 * </ul>
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
            .excludePathPatterns("/api/v1/auth/login", "/api/v1/public/**")
            .order(Ordered.HIGHEST_PRECEDENCE);

        // 注解鉴权：依赖上一层已完成 ipd 登录；type=ipd 的 @SaCheckPermission 在此生效
        registry.addInterceptor(new SaInterceptor().isAnnotation(true))
            .addPathPatterns("/api/v1/**")
            .excludePathPatterns("/api/v1/auth/login", "/api/v1/public/**")
            .order(Ordered.HIGHEST_PRECEDENCE + 1);
    }
}
