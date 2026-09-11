---
source: file:///Users/mac/Documents/ruoyi-ai/ruoyi-extend/ruoyi-monitor-admin/src/main/java/org/ruoyi/monitor/admin/MonitorAdminApplication.java
collected: 2026-09-04
published: 2026-09-04
topic: extend-source
---

# MonitorAdminApplication.java

```java
package org.ruoyi.monitor.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Admin 监控启动程序
 *
 * @author Lion Li
 */
@EnableAdminServer
@SpringBootApplication
public class MonitorAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorAdminApplication.class, args);
        System.out.println("Admin 监控启动成功");
    }

}

```
