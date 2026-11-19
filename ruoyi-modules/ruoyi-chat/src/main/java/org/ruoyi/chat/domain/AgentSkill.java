package org.ruoyi.chat.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.tenant.core.TenantEntity;

/** 智能体可关联的技能定义。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_skill")
public class AgentSkill extends TenantEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String content;

    /** 0 启用，1 停用。 */
    private String status;
}
