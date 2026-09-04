---
source: file:///Users/mac/Documents/ruoyi-ai/ruoyi-common/ruoyi-common-sensitive/src/main/java/org/ruoyi/common/sensitive/core/SensitiveService.java
collected: 2026-09-04
published: 2026-09-04
topic: common-source
---

# SensitiveService.java

```java
package org.ruoyi.common.sensitive.core;

/**
 * 脱敏服务
 * 默认管理员不过滤
 * 需自行根据业务重写实现
 *
 * @author Lion Li
 * @version 3.6.0
 */
public interface SensitiveService {

    /**
     * 是否脱敏
     */
    boolean isSensitive(String[] roleKey, String[] perms);

}

```
