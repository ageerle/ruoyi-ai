package org.ruoyi.ipd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 数据删除请求 VO（P2-5.1 合规卡 AC-COMP-02/03）。
 *
 * <p>30 天 deadline（个保法 / GDPR Art.12.3）。
 *
 * @param id           请求 ID
 * @param resourceType 资源类型
 * @param resourceId   资源 ID
 * @param requesterId  申请人 ID（服务端 actor.id）
 * @param reason       删除理由
 * @param status       状态（PENDING / PROCESSED / REJECTED）
 * @param deadlineAt   必处理期限（now + 30 days）
 * @param createdAt    申请时间
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDeletionRequestVO {
    private Long id;
    private String resourceType;
    private Long resourceId;
    private Long requesterId;
    private String reason;
    private String status;
    private Date deadlineAt;
    private Date createdAt;
}
