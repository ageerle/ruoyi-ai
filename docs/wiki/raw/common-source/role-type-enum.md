---
source: file:///Users/mac/Documents/ruoyi-ai/ruoyi-common/ruoyi-common-chat/src/main/java/org/ruoyi/common/chat/enums/RoleType.java
collected: 2026-09-04
published: 2026-09-04
topic: common-source
---

# RoleType.java

```java
package org.ruoyi.common.chat.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 角色枚举
 *
 * @author ageerle@163.com
 * @date 2025-12-17
 */
@Getter
@AllArgsConstructor
public enum RoleType {

    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    FUNCTION("function"),
    TOOL("tool"),
    WORKFLOW("workFlow")
    ;

    private final String name;

}

```
