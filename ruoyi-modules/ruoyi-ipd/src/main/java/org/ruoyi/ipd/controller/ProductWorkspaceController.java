package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.Requirement;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.RequirementMapper;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 产品空间聚合（P0 域#25；对齐 ZK-IPD 原型 GET /api/products/:id/workspace）。
 * 页：产品空间（产品档案 + 关联项目 + 需求池 + 指标卡）唯一数据源。
 * demand-themes 归并域 P1 落地后补 themes 列表；当前 metrics.themes 恒 0。
 */
@RestController
@RequestMapping("/api/v1/products/{id}/workspace")
@RequiredArgsConstructor
public class ProductWorkspaceController {

    private static final String ST_ACTIVE = "ACTIVE";
    private static final String ST_ARCHIVED = "ARCHIVED";

    private final ProductMapper productMapper;
    private final ProjectMapper projectMapper;
    private final RequirementMapper requirementMapper;

    /** 产品工作区视图：product + projects + demands + metrics。 */
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_PRODUCT_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    @GetMapping
    public ApiV1Response<Map<String, Object>> workspace(@PathVariable Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "产品不存在");
        }

        List<Project> projects = projectMapper.selectList(new LambdaQueryWrapper<Project>()
            .eq(Project::getProductId, id)
            .orderByDesc(Project::getId));
        List<Requirement> demands = requirementMapper.selectList(new LambdaQueryWrapper<Requirement>()
            .eq(Requirement::getProductId, id)
            .orderByDesc(Requirement::getId)
            .last("LIMIT 100"));

        Map<String, Object> productView = new LinkedHashMap<>();
        productView.put("id", product.getId());
        productView.put("productCode", product.getProductCode());
        productView.put("productName", product.getProductName());
        productView.put("modelCode", product.getModelCode());
        productView.put("source", product.getSource());
        productView.put("groupId", product.getGroupId());
        productView.put("status", product.getStatus());
        productView.put("projectId", product.getProjectId());

        List<Map<String, Object>> projectViews = new ArrayList<>();
        long activeProjects = 0;
        long closedProjects = 0;
        for (Project p : projects) {
            if (ST_ACTIVE.equals(p.getStatus())) {
                activeProjects += 1;
            }
            if (ST_ARCHIVED.equals(p.getStatus())) {
                closedProjects += 1;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("code", p.getCode());
            m.put("status", p.getStatus());
            m.put("currentStage", p.getCurrentStage());
            m.put("launchDate", p.getLaunchDate());
            m.put("updatedAt", p.getUpdateTime());
            projectViews.add(m);
        }

        List<Map<String, Object>> demandViews = new ArrayList<>();
        for (Requirement r : demands) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("title", r.getTitle());
            m.put("customerName", r.getCustomerName());
            m.put("submitterName", r.getSubmitterName());
            m.put("source", r.getSource());
            m.put("status", r.getStatus());
            m.put("projectId", r.getProjectId());
            m.put("createdAt", r.getCreateTime());
            demandViews.add(m);
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("feedback", demands.size());
        metrics.put("themes", 0);
        // int 装箱保持 JSON 数字（全局 Long→String 序列化规避，与 WorkbenchService 一致）
        metrics.put("activeProjects", (int) activeProjects);
        metrics.put("closedProjects", (int) closedProjects);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("product", productView);
        result.put("projects", projectViews);
        result.put("demands", demandViews);
        result.put("metrics", metrics);
        return ApiV1Response.ok(result);
    }
}
