package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * 国别认证清单模板库（v3 BR-IPD-05b / 补漏表 M1）
 * 项目选定目标市场后按 country_code 自动带出认证项；超管可维护。
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName(value = "cert_templates", autoResultMap = true)
public class CertTemplate extends BaseEntity implements SoftDeletable {

    @TableId
    private Long id;

    /** 国家/地区代码（如 SA） */
    private String countryCode;

    /** 国家名（如 沙特阿拉伯） */
    private String countryName;

    /** 认证名（如 SABER/SASO） */
    private String certName;

    /** 认证机构 */
    private String certAuthority;

    /** 要求说明 */
    private String requirementDesc;

    /** 是否强制（1是） */
    private String isMandatory;

    /** 租户ID */
    private String tenantId;

    /** 删除标志 */
    private String delFlag;

    /** 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter，以满足 SoftDeletable.setDelFlag(void) 接口签名。 */
    public void setDelFlag(String flag) { this.delFlag = flag; }
}