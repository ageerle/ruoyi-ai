package org.ruoyi.common.core.constant;

/**
 * 全局的key常量 (业务无关的key)
 *
 * @author Lion Li
 */
public interface GlobalConstants {

    /**
     * 全局 redis key (业务无关的key)
     */
    String GLOBAL_REDIS_KEY = "global:";

    /**
     * 验证码 redis key
     */
    String CAPTCHA_CODE_KEY = GLOBAL_REDIS_KEY + "captcha_codes:";

    /**
     * 防重提交 redis key
     */
    String REPEAT_SUBMIT_KEY = GLOBAL_REDIS_KEY + "repeat_submit:";

    /**
     * 限流 redis key
     */
    String RATE_LIMIT_KEY = GLOBAL_REDIS_KEY + "rate_limit:";

    /**
     * 登录账户密码错误次数 redis key
     */
    String PWD_ERR_CNT_KEY = GLOBAL_REDIS_KEY + "pwd_err_cnt:";
}
