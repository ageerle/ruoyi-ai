package com.xmzs.demo.controller;

import com.xmzs.common.core.domain.R;
import com.xmzs.demo.domain.TestDemoEncrypt;
import com.xmzs.demo.mapper.TestDemoEncryptMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


/**
 * 测试数据库加解密功能
 *
 * @author Lion Li
 */
@Validated
@RestController
@RequestMapping("/demo/encrypt")
public class TestEncryptController {

    @Autowired
    private TestDemoEncryptMapper mapper;
    @Value("${mybatis-encryptor.enable}")
    private Boolean encryptEnable;

    /**
     * 测试数据库加解密
     *
     * @param key   测试key
     * @param value 测试value
     */
    @GetMapping()
    public R<Map<String, TestDemoEncrypt>> test(String key, String value) {
        if (!encryptEnable) {
            throw new RuntimeException("加密功能未开启!");
        }
        Map<String, TestDemoEncrypt> map = new HashMap<>(2);
        TestDemoEncrypt demo = new TestDemoEncrypt();
        demo.setTestKey(key);
        demo.setValue(value);
        mapper.insert(demo);
        map.put("加密", demo);
        TestDemoEncrypt testDemo = mapper.selectById(demo.getId());
        map.put("解密", testDemo);
        return R.ok(map);
    }


}
