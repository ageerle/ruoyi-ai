package org.ruoyi.ipd.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * R/W 权限分离判定 VO（P2-5.1 合规卡 AC-COMP-05）。
 *
 * <p>{@code conflict=true} 表示「同用户同时持 {@code ipd:compliance:read} 与 {@code ipd:compliance:write}」，
 * 越权风险。
 *
 * @param userId       用户 ID
 * @param hasReadRole  是否持合规读权限
 * @param hasWriteRole 是否持合规写权限
 * @param conflict     是否 R/W 同源冲突
 * @param roleList     当前用户角色清单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionSeparationVO {
    private Long userId;
    private boolean hasReadRole;
    private boolean hasWriteRole;
    private boolean conflict;
    private List<String> roleList;
}
