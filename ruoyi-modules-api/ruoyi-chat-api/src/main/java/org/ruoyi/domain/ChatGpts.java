package org.ruoyi.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 应用管理对象 chat_gpts
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_gpts")
public class ChatGpts extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * gpts应用id
     */
    private String gid;

    /**
     * gpts应用名称
     */
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
    private Long useCnt;

    /**
     * 差评
     */
    private Long bad;

    /**
     * 类型
     */
    private String type;

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
