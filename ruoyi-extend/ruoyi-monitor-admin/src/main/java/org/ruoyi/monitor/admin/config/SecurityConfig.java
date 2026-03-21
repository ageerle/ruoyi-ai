package org.ruoyi.monitor.admin.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * admin 监控 安全配置
 *
 * @author Lion Li
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final String adminContextPath;

    public SecurityConfig(AdminServerProperties adminServerProperties) {
        this.adminContextPath = adminServerProperties.getContextPath();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(adminContextPath + "/");
        PathPatternRequestMatcher.Builder mvc = PathPatternRequestMatcher.withDefaults();
        return httpSecurity
            .headers((header) ->
                header.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            .authorizeHttpRequests((authorize) ->
                authorize.requestMatchers(
                        mvc.matcher(adminContextPath + "/assets/**"),
                        mvc.matcher(adminContextPath + "/login")
                    ).permitAll()
                    .anyRequest().authenticated())
            .formLogin((formLogin) ->
                formLogin.loginPage(adminContextPath + "/login").successHandler(successHandler))
            .logout((logout) ->
                logout.logoutUrl(adminContextPath + "/logout"))
            .httpBasic(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .build();
    }

}
