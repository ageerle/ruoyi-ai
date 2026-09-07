package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 存量导入批次台账（P1-9.1）。
 *
 * <p>DDL：batch_missing_tables.sql L156-178。
 * <p>字段映射：
 * <ul>
 *   <li>id (bigint, PK)</li>
 *   <li>batchNo (batch_no varchar(64)) — 批次号</li>
 *   <li>sourceSystem (source_system varchar(64)) — 来源系统</li>
 *   <li>importStatus (import_status varchar(16)) — PENDING|PROCESSING|COMPLETE|FAILED</li>
 *   <li>totalCount (total_count int) — 应导入总数</li>
 *   <li>successCount (success_count int) — 成功数</li>
 *   <li>errorCount (error_count int) — 失败数</li>
 *   <li>errorDetails (error_details json) — 逐行错误报告</li>
 *   <li>importedBy (imported_by bigint) — 操作人</li>
 *   <li>startedAt (started_at datetime)</li>
 *   <li>completedAt (completed_at datetime)</li>
 * </ul>
 *
 * <p>删除走软删除（del_flag='0'/'1'）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "legacy_imports", autoResultMap = true)
public class LegacyImport extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 批次号 */
    private String batchNo;

    /** 来源系统 */
    private String sourceSystem;

    /** PENDING|PROCESSING|COMPLETE|FAILED */
    private String importStatus;

    /** 应导入总数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer errorCount;

    /** 逐行错误报告（JSON） */
    private String errorDetails;

    /** 操作人 */
    private Long importedBy;

    /** 开始时间 */
    private Date startedAt;

    /** 完成时间 */
    private Date completedAt;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    @Override
    public void setDelFlag(String flag) {
        this.delFlag = flag;
    }
}
