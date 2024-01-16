package com.xmzs.demo.controller;

import com.xmzs.common.core.domain.R;
import com.xmzs.common.web.core.BaseController;
import com.xmzs.common.sensitive.annotation.Sensitive;
import com.xmzs.common.sensitive.core.SensitiveStrategy;
import lombok.Data;
import com.xmzs.common.sensitive.core.SensitiveService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试数据脱敏控制器
 * <p>
 * 默认管理员不过滤
 * 需自行根据业务重写实现
 *
 * @author Lion Li
 * @version 3.6.0
 * @see SensitiveService
 */
@RestController
@RequestMapping("/demo/sensitive")
public class TestSensitiveController extends BaseController {

    /**
     * 测试数据脱敏
     */
    @GetMapping("/test")
    public R<TestSensitive> test() {
        TestSensitive testSensitive = new TestSensitive();
        testSensitive.setIdCard("210397198608215431");
        testSensitive.setPhone("17640125371");
        testSensitive.setAddress("北京市朝阳区某某四合院1203室");
        testSensitive.setEmail("17640125371@163.com");
        testSensitive.setBankCard("6226456952351452853");
        return R.ok(testSensitive);
    }

    @Data
    static class TestSensitive {

        /**
         * 身份证
         */
        @Sensitive(strategy = SensitiveStrategy.ID_CARD)
        private String idCard;

        /**
         * 电话
         */
        @Sensitive(strategy = SensitiveStrategy.PHONE)
        private String phone;

        /**
         * 地址
         */
        @Sensitive(strategy = SensitiveStrategy.ADDRESS)
        private String address;

        /**
         * 邮箱
         */
        @Sensitive(strategy = SensitiveStrategy.EMAIL)
        private String email;

        /**
         * 银行卡
         */
        @Sensitive(strategy = SensitiveStrategy.BANK_CARD)
        private String bankCard;

    }

}
