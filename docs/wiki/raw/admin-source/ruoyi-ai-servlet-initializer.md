---
source: file:///Users/mac/Documents/ruoyi-ai/ruoyi-admin/src/main/java/org/ruoyi/RuoYiAIServletInitializer.java
collected: 2026-09-04
published: 2026-09-04
topic: admin-source
---

# RuoYiAIServletInitializer.java

```java
package org.ruoyi;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * web容器中进行部署
 *
 * @author Lion Li
 */
public class RuoYiAIServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(RuoYiAIApplication.class);
    }

}

```
