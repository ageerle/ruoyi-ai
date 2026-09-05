package org.ruoyi.ipd.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * P2-1.4 真库 HTTP 验收发现：{@code @RequestBody Long} 既违反 sibling 风格也不友好。
 * 这里封一个最小 DTO。约束：newLeaderPersonId 必填。
 *
 * 引用：本会话 2026-09-05 13:47 P2-1.4 验收集落。
 */
@Data
public class ProductGroupLeaderReq {

    @NotNull(message = "新组长 personId 不能为空")
    private Long newLeaderPersonId;
}
