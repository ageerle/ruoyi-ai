package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * IPD 项目（核心实体）——TS-05 projects
 * 阶段 CONCEPT→PLAN→DEV→VALID→LAUNCH→LIFECYCLE（Gate 门禁在阶段推进时校验，P1-5）
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "projects", autoResultMap = true)
public class Project extends BaseEntity implements SoftDeletable {

    @TableId
    private Long id;

    /** 项目编码 PRJ-YYYY-NNN 自动生成（uk_projects_code） */
    private String code;

    /** 项目名称 */
    private String name;

    /** 归属产品（1:1 唯一 Q5，uk_projects_product）；软删对端时需可写 null */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long productId;

    /** 模板类型 HARDWARE|SOFTWARE|SOLUTION */
    private String templateType;

    /** 目标市场国家/地区代码数组（JSON，M1 驱动认证清单） */
    private String targetMarkets;

    /** 项目级别 S|A|B */
    private String level;

    /** 差异化系数（S 1.5–2.0 / B 0.6–0.8 / A 固定不录，v3 G1 特殊规则） */
    private BigDecimal levelCoefficient;

    /** 系数定值理由（S/B 必填，写审计） */
    private String levelCoefficientReason;

    /** 立项目标销售额（奖金池基数 BR-INC-04，四基准值之一） */
    private BigDecimal targetSalesAmount;

    /** 立项目标渠道商数（四基准值之一） */
    private Integer targetChannelCount;

    /** 立项 NPS 目标（四基准值之一） */
    private Integer targetNps;

    /** 立项目标场景数（四基准值之一） */
    private Integer targetSceneCount;

    /** 上市日期（后置指标起算原点 BR-IPD-08，G4 后录入不可随意改） */
    private Date launchDate;

    /** 当前阶段 CONCEPT|PLAN|DEV|VALID|LAUNCH|LIFECYCLE */
    private String currentStage;

    /** P1-9.1：存量申报阶段（补齐目标展示）；NEW 项目为空 */
    private String declaredStage;

    /** 生命周期 ON_SALE|LIMITED|EOL|ARCHIVED */
    private String lifecycleStatus;

    /** 来源 NEW|LEGACY（存量导入） */
    private String source;

    /** P1-9.1：存量导入生效日 */
    private Date legacyEffectiveAt;

    /**
     * P1-9.2：最近活动日（max of stage_action.update_time / kpi/gate update_time）——
     * 存量 14 天场景复核与分段起算的「分段」基准点。
     */
    private Date lastActivityAt;

    /** P1-9.1：历史缺失声明 1=已确认（BR-PROD-03） */
    private String missingHistoryAck;

    /** P1-9.1：补齐状态 IN_PROGRESS|COMPLETE */
    private String catchupStatus;

    /** 状态 DRAFT|TEAMING|ACTIVE|SUSPENDED|ARCHIVED */
    private String status;

    /** 主组=市场PM 所在产品组（BR-ORG-01） */
    private Long mainGroupId;

    /** 租户ID */
    private String tenantId;

    /** 删除标志（删除走两级审核引擎） */
    /** [SEC-FIX-6ENTITY-LOGIC] 软删除标志（0正常 1已删；@TableLogic 守卫）。 */

    @TableLogic

    @TableField("del_flag")

    private String delFlag;

    /** 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter，以满足 SoftDeletable.setDelFlag(void) 接口签名。 */
    public void setDelFlag(String flag) { this.delFlag = flag; }
}