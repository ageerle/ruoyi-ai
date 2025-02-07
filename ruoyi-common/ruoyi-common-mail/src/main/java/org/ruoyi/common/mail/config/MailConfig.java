package org.ruoyi.common.mail.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.mail.utils.MailAccount;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * JavaMail 配置
 *
 * @author Michelle.Chung
 */

@RequiredArgsConstructor
@Configuration
@Slf4j
public class MailConfig {

    private final ConfigService configService;
    private MailAccount account;  // 缓存MailAccount实例

    @Bean
    @Scope("singleton")
    public MailAccount mailAccount() {
        if (account == null) {
            account = new MailAccount();
            updateMailAccount();
        }
        return account;
    }

    @Scheduled(fixedDelay = 10000)  // 每10秒检查一次
    public void updateMailAccount() {
        account.setHost(getKey("host"));
        account.setPort(NumberUtils.toInt(getKey("port"), 465));
        account.setAuth(true);
        account.setFrom(getKey("from"));
        account.setUser(getKey("user"));
        account.setPass(getKey("pass"));
        account.setSocketFactoryPort(NumberUtils.toInt(getKey("port"), 465));
        account.setStarttlsEnable(true);
        account.setSslEnable(true);
        account.setTimeout(0);
        account.setConnectionTimeout(0);
    }

    public String getKey(String key){
        return configService.getConfigValue("mail", key);
    }
}
