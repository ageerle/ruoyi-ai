package org.ruoyi.domain.entity.chat;

import org.ruoyi.common.tenant.core.TenantEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 厂商管理对象 chat_provider
 *
 * @author ageerle
 * @date 2025-12-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_provider")
public class ChatProvider extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 厂商名称
     */
    private String providerName;

    /**
     * 厂商编码
     */
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
     * 版本
     */
    @Version
    private Long version;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 更新IP
     */
    private String updateIp;


}
