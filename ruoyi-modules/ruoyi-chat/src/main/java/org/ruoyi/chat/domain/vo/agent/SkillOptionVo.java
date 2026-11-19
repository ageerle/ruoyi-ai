package org.ruoyi.chat.domain.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 启用的技能选项
 *
 * @author ruoyi team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillOptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 技能标识
     */
    private String name;

    /**
     * 技能描述
     */
    private String description;

}
