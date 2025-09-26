package org.ruoyi.common.security.handler;

import cn.hutool.core.util.ReUtil;
import lombok.Data;
import org.ruoyi.common.core.utils.SpringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 获取所有Url配置
 *
 * @author Lion Li
 */
@Data
public class AllUrlHandler implements InitializingBean {

    private List<String> urls = new ArrayList<>(256);

    @Override
    public void afterPropertiesSet() {
        String name = "requestMappingHandlerMapping";
        RequestMappingHandlerMapping mapping = SpringUtils.getBean(name, RequestMappingHandlerMapping.class);

        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();

        Pattern pattern = Pattern.compile("\\{(.*?)\\}");

        Set<String> handlerSet = handlerMethods.keySet().stream()
                .flatMap(info -> {
                    // Spring 5 (AntPath) 风格
                    if (info.getPatternsCondition() != null && info.getPatternsCondition().getPatterns() != null) {
                        return info.getPatternsCondition().getPatterns().stream();
                    }
                    // Spring 6 (PathPattern) 风格
                    if (info.getPathPatternsCondition() != null && info.getPathPatternsCondition().getPatterns() != null) {
                        return info.getPathPatternsCondition().getPatterns().stream().map(PathPattern::getPatternString);
                    }
                    return java.util.stream.Stream.<String>empty();
                })
                .collect(Collectors.toSet());

        // 获取注解上边的 path 替代 path variable 为 *
        handlerSet.stream().map(path -> ReUtil.replaceAll(path, pattern, "*"))
                .forEach(item -> urls.add(item));
    }

}
