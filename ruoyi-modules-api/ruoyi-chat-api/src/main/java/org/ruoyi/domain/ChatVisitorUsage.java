package org.ruoyi.domain;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 访客管理对象 chat_visitor_usage
 *
 * @author Lion Li
 * @date 2024-07-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_visitor_usage")
public class ChatVisitorUsage extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 浏览器指纹
     */
    private String fingerprint;

    /**
     * 使用次数
     */
    private String usageCount;

    /**
     * ip地址
     */
    private String ipAddress;

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
