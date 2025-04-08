package org.ruoyi.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import org.ruoyi.domain.ChatGpts;

/**
 * gpts管理业务对象 chat_gpts
 *
 * @author Lion Li
 * @date 2024-07-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ChatGpts.class, reverseConvertGenerate = false)
public class ChatGptsBo extends BaseEntity {

    /**
     * id
     */
    @NotNull(message = "id不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * gpts应用id
     */
    @NotBlank(message = "gpts应用id不能为空", groups = { AddGroup.class, EditGroup.class })
    private String gid;

    /**
     * gpts应用名称
     */
    @NotBlank(message = "gpts应用名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String name;

    /**
     * gpts图标
     */
    private String logo;

    /**
     * gpts描述
     */
    private String info;

    /**
     * 作者id
     */
    private String authorId;

    /**
     * 作者名称
     */
    private String authorName;

    /**
     * 点赞
     */
    private String useCnt;

    /**
     * 差评
     */
    private String bad;

    /**
     * 类型
     */
    private String type;

    /**
     * 备注
     */
    private String remark;

    /**
     * 更新IP
     */
    private String updateIp;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型system
     */
    private String systemPrompt;

}
