package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * P1-7.1：项目级认证清单项（模板快照 + 手工补充；status=DONE 不被 re-sync 重置）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "project_cert_items", autoResultMap = true)
public class ProjectCertItem extends BaseEntity implements SoftDeletable {

    @TableId
    private Long id;

    private Long projectId;
    private Long templateId;
    private String countryCode;
    private String countryName;
    private String certName;
    private String certAuthority;
    private String requirementDesc;
    private String isMandatory;
    /** AUTO|MANUAL */
    private String source;
    /** PENDING|IN_PROGRESS|DONE|NA */
    private String status;
    private String catalogVersion;
    private String tenantId;
    /**
     * Round 8 / R8-P0-8：加 @TableLogic + @TableField("del_flag") 让 MyBatis-Plus 自动过滤 delFlag=1 的行
     * 配套 SQL：{@code docs/script/sql/update/2026-09-05-ipd-project-cert-items.sql} 已加 version 列
     * 配套乐观锁：changeStatus 用 @Version 防并发写覆盖
     */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;
    /** Round 8 / R8-P0-9：changeStatus 并发保护 */
    @Version
    @TableField("version")
    private Integer version;

    /** 显式覆盖链式 setter，满足 SoftDeletable。 */
    public void setDelFlag(String flag) {
        this.delFlag = flag;
    }
}
