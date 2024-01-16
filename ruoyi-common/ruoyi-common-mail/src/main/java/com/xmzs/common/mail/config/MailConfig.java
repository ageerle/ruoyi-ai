package com.xmzs.common.mail.config;

import com.xmzs.common.mail.config.properties.MailProperties;
import com.xmzs.common.mail.utils.MailAccount;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * JavaMail 配置
 *
 * @author Michelle.Chung
 */
@AutoConfiguration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    @Bean
    @ConditionalOnProperty(value = "mail.enabled", havingValue = "true")
    public MailAccount mailAccount(MailProperties mailProperties) {
        MailAccount account = new MailAccount();
        account.setHost(mailProperties.getHost());
        account.setPort(mailProperties.getPort());
        account.setAuth(mailProperties.getAuth());
        account.setFrom(mailProperties.getFrom());
        account.setUser(mailProperties.getUser());
        account.setPass(mailProperties.getPass());
        account.setSocketFactoryPort(mailProperties.getPort());
        account.setStarttlsEnable(mailProperties.getStarttlsEnable());
        account.setSslEnable(mailProperties.getSslEnable());
        account.setTimeout(mailProperties.getTimeout());
        account.setConnectionTimeout(mailProperties.getConnectionTimeout());
        return account;
    }

}
