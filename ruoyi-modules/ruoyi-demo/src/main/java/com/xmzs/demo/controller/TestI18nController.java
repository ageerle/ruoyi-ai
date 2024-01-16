package com.xmzs.demo.controller;

import com.xmzs.common.core.domain.R;
import com.xmzs.common.core.utils.MessageUtils;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


/**
 * 测试国际化
 *
 * @author Lion Li
 */
@Validated
@RestController
@RequestMapping("/demo/i18n")
public class TestI18nController {

    /**
     * 通过code获取国际化内容
     * code为 messages.properties 中的 key
     * <p>
     * 测试使用 user.register.success
     *
     * @param code 国际化code
     */
    @GetMapping()
    public R<Void> get(String code) {
        return R.ok(MessageUtils.message(code));
    }

    /**
     * Validator 校验国际化
     * 不传值 分别查看异常返回
     * <p>
     * 测试使用 not.null
     */
    @GetMapping("/test1")
    public R<Void> test1(@NotBlank(message = "{not.null}") String str) {
        return R.ok(str);
    }

    /**
     * Bean 校验国际化
     * 不传值 分别查看异常返回
     * <p>
     * 测试使用 not.null
     */
    @GetMapping("/test2")
    public R<TestI18nBo> test2(@Validated TestI18nBo bo) {
        return R.ok(bo);
    }

    @Data
    public static class TestI18nBo {

        @NotBlank(message = "{not.null}")
        private String name;

        @NotNull(message = "{not.null}")
        @Range(min = 0, max = 100, message = "{length.not.valid}")
        private Integer age;
    }
}
