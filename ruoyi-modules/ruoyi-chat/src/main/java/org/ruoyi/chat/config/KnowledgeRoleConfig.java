package org.ruoyi.chat.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Description:
 * @Author: violateer
 * @Date: 2025/7/20
 */
@Data
@Component
public class KnowledgeRoleConfig {
    @Value("${knowledge-role.enable}")
    private Boolean enable;
}
