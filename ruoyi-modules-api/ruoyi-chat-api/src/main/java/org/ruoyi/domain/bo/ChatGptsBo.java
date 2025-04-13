package org.ruoyi.domain.bo;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.domain.ChatGpts;
import org.ruoyi.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 应用管理业务对象 chat_gpts
 *
 * @author ageerle
 * @date 2025-04-08
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
    @NotBlank(message = "gpts图标不能为空", groups = { AddGroup.class, EditGroup.class })
    private String logo;

    /**
     * gpts描述
     */
    @NotBlank(message = "gpts描述不能为空", groups = { AddGroup.class, EditGroup.class })
    private String info;

    /**
     * 作者id
     */
    @NotBlank(message = "作者id不能为空", groups = { AddGroup.class, EditGroup.class })
    private String authorId;

    /**
     * 作者名称
     */
    @NotBlank(message = "作者名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String authorName;

    /**
     * 点赞
     */
    @NotNull(message = "点赞不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long useCnt;

    /**
     * 差评
     */
    @NotNull(message = "差评不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long bad;

    /**
     * 类型
     */
    @NotBlank(message = "类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String type;

    /**
     * 备注
     */
    @NotBlank(message = "备注不能为空", groups = { AddGroup.class, EditGroup.class })
    private String remark;

    /**
     * 更新IP
     */
    @NotBlank(message = "更新IP不能为空", groups = { AddGroup.class, EditGroup.class })
    private String updateIp;


}
