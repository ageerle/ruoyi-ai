package org.ruoyi.domain.vo.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 磁盘技能可选项
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
     * 技能名称（对应 SKILL.md front-matter name）
     */
    private String name;

    /**
     * 技能描述
     */
    private String description;

}
