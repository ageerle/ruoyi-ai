package org.ruoyi.chat.domain.bo.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;

@Data
public class AgentSkillBo {

    @NotNull(message = "技能ID不能为空", groups = EditGroup.class)
    private Long id;

    @NotBlank(message = "技能标识不能为空", groups = AddGroup.class)
    @Size(max = 64, message = "技能标识最多 64 个字符", groups = {AddGroup.class, EditGroup.class})
    @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
        message = "技能标识只能用小写字母、数字和分隔用的短横线", groups = {AddGroup.class, EditGroup.class})
    private String name;

    @NotBlank(message = "技能描述不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 1024, message = "技能描述最多 1024 个字符", groups = {AddGroup.class, EditGroup.class})
    private String description;

    @NotBlank(message = "技能指令不能为空", groups = {AddGroup.class, EditGroup.class})
    private String content;

    @Pattern(regexp = "[01]", message = "技能状态无效", groups = {AddGroup.class, EditGroup.class})
    private String status;
}
