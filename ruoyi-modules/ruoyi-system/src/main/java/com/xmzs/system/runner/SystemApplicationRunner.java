package com.xmzs.system.runner;

import com.xmzs.system.service.ISysOssConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 初始化 system 模块对应业务数据
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class SystemApplicationRunner implements ApplicationRunner {

    private final ISysOssConfigService ossConfigService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ossConfigService.init();
        log.info("初始化OSS配置成功");
    }

}
