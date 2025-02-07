package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 【请填写功能名称】对象 wx_rob_keyword
 *
 * @author Lion Li
 * @date 2024-05-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wx_rob_keyword")
public class WxRobKeyword extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 机器唯一码
     */
    private String uniqueKey;

    /**
     * 关键词
     */
    private String keyData;

    /**
     * 回复内容
     */
    private String valueData;

    /**
     * 回复类型
     */
    private String typeData;

    /**
     * 目标昵称
     */
    private String nickName;

    /**
     * 群1好友0
     */
    private Integer toGroup;

    /**
     * 启用1禁用0
     */
    private Integer enable;


}
