package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * IPD AI 文档（BR-AI，版本链）
 * 生成+人工审核+版本链+token 统计，未审核不可归档
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_documents", autoResultMap = true)
public class AiDocument extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 文档类型
     */
    private String docType;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 文档内容（mediumtext）
     */
    private String content;

    /**
     * 生成模型
     */
    private String model;

    /**
     * Token 消耗-提示词
     */
    private Integer tokenPrompt;

    /**
     * Token 消耗-补全
     */
    private Integer tokenCompletion;

    /**
     * 内容摘要 sha256 hex（P1-10.1：版本不可变锚点，写入时计算，历史行永不更新）
     */
    private String contentSha256;

    /**
     * 状态 GENERATED|REVIEWED|ARCHIVED（未审核不可归档）
     */
    private String status;

    /**
     * 版本链父文档ID
     */
    private Long parentVersionId;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 审核人ID
     */
    private Long reviewedBy;

    /**
     * 审核时间
     */
    private Date reviewedAt;

    /**
     * 审核备注（P1-10.2：reject 原因 / review 批注，BR-AI-03 审计完整性）
     */
    private String reviewComment;

    /**
     * 归档时间（P1-10.2：archivedAt 落库时点，仅 ARCHIVED 行非空）
     */
    private Date archivedAt;

    /**
     * 归档操作者（P1-10.2：审计身份可信——actor.id 写入，归档行可回溯责任人）
     */
    private Long archivedBy;

    /**
     * 软删除标志（0正常 1已删）
     */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /**
     * 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter
     */
    public void setDelFlag(String flag) { this.delFlag = flag; }
}
