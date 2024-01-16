package com.xmzs.test;

import com.xmzs.common.core.config.RuoYiConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

/**
 * 单元测试案例
 *
 * @author Lion Li
 */
@SpringBootTest // 此注解只能在 springboot 主包下使用 需包含 main 方法与 yml 配置文件
@DisplayName("单元测试案例")
public class DemoUnitTest {

    @Autowired
    private RuoYiConfig ruoYiConfig;

    @DisplayName("测试 @SpringBootTest @Test @DisplayName 注解")
    @Test
    public void testTest() {
        System.out.println(ruoYiConfig);
    }

    @Disabled
    @DisplayName("测试 @Disabled 注解")
    @Test
    public void testDisabled() {
        System.out.println(ruoYiConfig);
    }

    @Timeout(value = 2L, unit = TimeUnit.SECONDS)
    @DisplayName("测试 @Timeout 注解")
    @Test
    public void testTimeout() throws InterruptedException {
        Thread.sleep(3000);
        System.out.println(ruoYiConfig);
    }


    @DisplayName("测试 @RepeatedTest 注解")
    @RepeatedTest(3)
    public void testRepeatedTest() {
        System.out.println(666);
    }

    @BeforeAll
    public static void testBeforeAll() {
        System.out.println("@BeforeAll ==================");
    }

    @BeforeEach
    public void testBeforeEach() {
        System.out.println("@BeforeEach ==================");
    }

    @AfterEach
    public void testAfterEach() {
        System.out.println("@AfterEach ==================");
    }

    @AfterAll
    public static void testAfterAll() {
        System.out.println("@AfterAll ==================");
    }

}
