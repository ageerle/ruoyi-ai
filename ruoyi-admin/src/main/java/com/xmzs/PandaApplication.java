package com.xmzs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 启动程序
 *
 * @author Lion Li
 */

@SpringBootApplication
public class PandaApplication {


    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PandaApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  panda智能助手启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
