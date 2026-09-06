package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.LaunchDateChangeRequest;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.LaunchDateChangeService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * 上市日期双签 API（AC-INC-33 / P1-2.2）。
 * Round 8 / R8-P0-11：proposedLaunchDate 用 LocalDate + @JsonFormat 避免 Date 反序列化踩坑，
 *                  Controller 层转 java.util.Date 传给 Service（Service 签名保留 Date，最小化改动面）。
 */
@RestController
@RequestMapping("/api/v1/launch-date-change-requests")
@RequiredArgsConstructor
public class LaunchDateChangeController {

    private final LaunchDateChangeService launchDateChangeService;
    private final IpdPermission ipdPermission;

    /**
     * 第一签提议。
     *
     * @param body 项目/日期/理由
     * @return 待第二签申请
     */
    @SaCheckPermission(value = "ipd:project:edit", type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping
    public ApiV1Response<LaunchDateChangeRequest> propose(@Valid @RequestBody ProposeReq body) {
        IpdActor actor = ipdPermission.requireInternal();
        // R8-P0-11：LocalDate 转 Date（系统时区零时），避免时区漂移
        Date date = Date.from(body.proposedLaunchDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        // R8X-2 P0-1：传 actor.groupId 给 Service 做横向越权防护
        return ApiV1Response.ok(launchDateChangeService.propose(
            body.projectId(), date, body.reason(),
            actor.id(), actor.role(), actor.groupId()));
    }

    /**
     * 第二签确认或驳回。
     *
     * @param id      申请 ID
     * @param approve true=写入项目
     * @param opinion 意见
     * @return 终态
     */
    @SaCheckPermission(value = "ipd:project:edit", type = IpdAuthSession.LOGIN_TYPE)
    @PostMapping("/{id}/second-decision")
    public ApiV1Response<LaunchDateChangeRequest> secondDecision(@PathVariable Long id,
                                                                 @RequestParam boolean approve,
                                                                 @RequestParam(required = false) String opinion) {
        IpdActor actor = ipdPermission.requireInternal();
        // R8X-2 P0-1：传 actor.groupId 给 Service 做横向越权防护
        return ApiV1Response.ok(launchDateChangeService.secondDecision(
            id, actor.id(), actor.role(), actor.groupId(), approve, opinion));
    }

    /** 提议入参。Round 8：proposedLaunchDate 改 LocalDate + @JsonFormat("yyyy-MM-dd") 解决 Date 反序列化不一致。 */
    public record ProposeReq(
        @NotNull Long projectId,
        @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate proposedLaunchDate,
        @NotBlank @Size(max = 500) String reason) {
    }
}
