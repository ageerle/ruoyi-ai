package org.ruoyi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;

/**
 * BeanDefinitionRegistry后置处理器
 * 解决 MapStruct Plus 生成的 mapper 冲突问题
 *
 * @author ruoyi team
 */
@Configuration
public class MapperConflictResolver implements BeanDefinitionRegistryPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(MapperConflictResolver.class);

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String[] beanNames = registry.getBeanDefinitionNames();

        // 查找冲突的 mapper bean
        for (String beanName : beanNames) {
            if (beanName.equals("chatMessageBoToChatMessageMapperImpl")) {
                BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
                String beanClassName = beanDefinition.getBeanClassName();

                log.info("Found mapper bean: {} -> {}", beanName, beanClassName);

                // 如果是 org.ruoyi.domain.bo.chat 包下的（冲突的），移除它
                if (beanClassName != null && beanClassName.startsWith("org.ruoyi.domain.bo.chat")) {
                    log.warn("Removing conflicting bean definition: {} ({})", beanName, beanClassName);
                    registry.removeBeanDefinition(beanName);
                }
            }
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 不需要实现
    }
}
