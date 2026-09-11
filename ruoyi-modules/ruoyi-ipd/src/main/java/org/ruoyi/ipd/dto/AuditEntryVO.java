package org.ruoyi.ipd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 审计条目 VO（P2-5.1 合规卡 AC-COMP-04）。
 * <p>从 {@code AuditLog} 投影出前端可读字段；{@code before}/{@code after} 为 JSON 字符串。
 *
 * @param seq        序列号（链序号）
 * @param actorId    操作人 ID
 * @param actorName  操作人姓名
 * @param action     动作
 * @param before     前置 JSON
 * @param after      后置 JSON
 * @param createTime 时间戳
 * @param entityType 实体类型
 * @param entityId   实体 ID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntryVO {
    private Long seq;
    private Long actorId;
    private String actorName;
    private String action;
    private String before;
    private String after;
    private Date createTime;
    private String entityType;
    private Long entityId;
}
