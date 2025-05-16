package org.ruoyi.common.security.handler;

import cn.hutool.core.util.ReUtil;
import lombok.Data;
import org.ruoyi.common.core.utils.SpringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.*;
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
                .flatMap(info -> info.getPatternsCondition().getPatterns().stream())
                .collect(Collectors.toSet());

        // 获取注解上边的 path 替代 path variable 为 *
        handlerSet.stream().map(path -> ReUtil.replaceAll(path, pattern, "*"))
                .forEach(item -> urls.add(item));
    }

}
