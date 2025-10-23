package org.ruoyi.workflow.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.workflow.util.LocalDateTimeUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class BeanConfig {

    @Bean
    public RestTemplate restTemplate() {
        log.info("Configuration:create restTemplate");
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 设置建立连接超时时间  毫秒
        requestFactory.setConnectTimeout(60000);
        // 设置读取数据超时时间  毫秒
        requestFactory.setReadTimeout(60000);
        RestTemplate restTemplate = new RestTemplate();
        // 注册LOG拦截器
//        restTemplate.setInterceptors(Lists.newArrayList(new LogClientHttpRequestInterceptor()));
        restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(requestFactory));

        return restTemplate;
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        log.info("Configuration:create objectMapper");
        ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder().createXmlMapper(false).build();
        objectMapper.registerModules(LocalDateTimeUtil.getSimpleModule(), new JavaTimeModule(), new Jdk8Module());
        //设置null值不参与序列化(字段不被显示)
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    @Bean(name = "mainExecutor")
    @Primary
    public AsyncTaskExecutor mainExecutor() {
        int processorsNum = Runtime.getRuntime().availableProcessors();
        log.info("mainExecutor,processorsNum:{}", processorsNum);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(processorsNum * 2);
        executor.setMaxPoolSize(100);
        return executor;
    }

    @Bean(name = "imagesExecutor")
    public AsyncTaskExecutor imagesExecutor() {
        int processorsNum = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        log.info("imagesExecutor corePoolSize:{},maxPoolSize:{}", processorsNum, processorsNum * 2);
        executor.setCorePoolSize(processorsNum);
        executor.setMaxPoolSize(processorsNum * 2);
        return executor;
    }

    @Bean(name = "beanValidator")
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }
}
