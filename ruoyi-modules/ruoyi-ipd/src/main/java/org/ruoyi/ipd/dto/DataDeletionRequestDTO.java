package org.ruoyi.ipd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建数据删除请求入参（P2-5.1 合规卡 AC-COMP-02/03）。
 * <p>{@code requesterId} 不允许前端透传，服务端从会话 actor.id 推导。
 *
 * @param resourceType 资源类型
 * @param resourceId   资源 ID
 * @param reason       删除理由
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataDeletionRequestDTO {

    @NotBlank(message = "resourceType 必填")
    @Size(max = 64, message = "resourceType 最长 64")
    private String resourceType;

    @NotNull(message = "resourceId 必填")
    private Long resourceId;

    @NotBlank(message = "reason 必填")
    @Size(max = 1024, message = "reason 最长 1024")
    private String reason;
}
