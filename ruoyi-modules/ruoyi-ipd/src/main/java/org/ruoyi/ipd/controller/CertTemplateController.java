package org.ruoyi.ipd.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.CertTemplate;
import org.ruoyi.ipd.service.CertTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 国别认证模板接口 /api/v1/cert-templates（M1：选目标市场自动带出）
 */
@RestController
@RequestMapping("/api/v1/cert-templates")
@RequiredArgsConstructor
public class CertTemplateController {

    private final CertTemplateService certTemplateService;

    /** 项目选定目标市场后自动带出认证清单（如 ?markets=SA,AE） */
    @GetMapping("/resolve")
    public ApiV1Response<List<CertTemplate>> resolve(@RequestParam String markets) {
        return ApiV1Response.ok(certTemplateService.resolve(markets.split(",")));
    }

    @GetMapping
    public ApiV1Response<List<CertTemplate>> list() {
        return ApiV1Response.ok(certTemplateService.listAll());
    }

    @GetMapping("/country-counts")
    public ApiV1Response<Map<String, Long>> countryCounts() {
        return ApiV1Response.ok(certTemplateService.countByCountry());
    }

    @PostMapping
    public ApiV1Response<CertTemplate> create(@RequestBody CertTemplate template, @RequestParam Long operatorId) {
        return ApiV1Response.ok(certTemplateService.create(template, operatorId));
    }

    @PostMapping("/{id}/remove")
    public ApiV1Response<Void> remove(@PathVariable Long id, @RequestParam Long operatorId) {
        certTemplateService.remove(id, operatorId);
        return ApiV1Response.ok(null);
    }
}