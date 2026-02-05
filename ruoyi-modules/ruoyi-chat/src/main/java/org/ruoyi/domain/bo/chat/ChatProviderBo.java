package org.ruoyi.domain.bo.chat;

import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import org.ruoyi.domain.entity.chat.ChatProvider;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 厂商管理业务对象 chat_provider
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = ChatProvider.class, reverseConvertGenerate = false)
public class ChatProviderBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 厂商名称
     */
    @NotBlank(message = "厂商名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String providerName;

    /**
     * 厂商编码
     */
    @NotBlank(message = "厂商编码不能为空", groups = { AddGroup.class, EditGroup.class })
    private String providerCode;

    /**
     * 厂商图标
     */
    private String providerIcon;

    /**
     * 厂商描述
     */
    private String providerDesc;

    /**
     * API地址
     */
    private String apiHost;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 排序
     */
    private Long sortOrder;

    /**
     * 备注
     */
    private String remark;

    /**
     * 更新IP
     */
    private String updateIp;


}
