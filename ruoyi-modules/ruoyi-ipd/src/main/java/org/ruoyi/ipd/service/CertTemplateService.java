package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.CertTemplate;
import org.ruoyi.ipd.mapper.CertTemplateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 国别认证模板库服务（M1：项目选定目标市场自动带出认证清单，允许手工补充其他项）
 * resolve 语义：多目标市场合并去重（同国多认证并列），强制项排前。
 */
@Service
@RequiredArgsConstructor
public class CertTemplateService {

    private final CertTemplateMapper certTemplateMapper;
    private final AuditLogService auditLogService;

    /** 按目标市场国家代码数组解析认证清单（如 ["SA","AE"]）；未匹配国别返回空表由前端提示手工补充 */
    public List<CertTemplate> resolve(String[] targetMarkets) {
        if (targetMarkets == null || targetMarkets.length == 0) {
            throw new ServiceException("目标市场为空，无法解析认证清单");
        }
        List<String> codes = Arrays.stream(targetMarkets).map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (codes.isEmpty()) {
            throw new ServiceException("目标市场为空，无法解析认证清单");
        }
        return certTemplateMapper.selectList(new LambdaQueryWrapper<CertTemplate>()
            .in(CertTemplate::getCountryCode, codes)
            .eq(CertTemplate::getDelFlag, "0")
            .orderByDesc(CertTemplate::getIsMandatory)
            .orderByAsc(CertTemplate::getCountryCode)
            .orderByAsc(CertTemplate::getId));
    }

    /** 按国别统计（管理视图：国家 → 认证项数） */
    public Map<String, Long> countByCountry() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (CertTemplate t : listAll()) {
            result.merge(t.getCountryCode() + " " + t.getCountryName(), 1L, Long::sum);
        }
        return result;
    }

    public List<CertTemplate> listAll() {
        return certTemplateMapper.selectList(new LambdaQueryWrapper<CertTemplate>()
            .eq(CertTemplate::getDelFlag, "0").orderByAsc(CertTemplate::getCountryCode, CertTemplate::getId));
    }

    @Transactional(rollbackFor = Exception.class)
    public CertTemplate create(CertTemplate template, Long operatorId) {
        if (isBlank(template.getCountryCode()) || isBlank(template.getCountryName()) || isBlank(template.getCertName())) {
            throw new ServiceException("countryCode/countryName/certName 必填");
        }
        Long dup = certTemplateMapper.selectCount(new LambdaQueryWrapper<CertTemplate>()
            .eq(CertTemplate::getCountryCode, template.getCountryCode())
            .eq(CertTemplate::getCertName, template.getCertName())
            .eq(CertTemplate::getDelFlag, "0"));
        if (dup != null && dup > 0) {
            throw new ServiceException("该国家已存在同名认证项: " + template.getCertName());
        }
        // SEC-API-01：服务端权威字段强制覆写，防客户端注入 id/tenantId/delFlag/createTime/updateTime
        template.setId(null);
        template.setCreateTime(new Date());
        template.setUpdateTime(null);
        template.setDelFlag("0");
        if (isBlank(template.getTenantId())) {
            template.setTenantId("000000");
        }
        if (isBlank(template.getIsMandatory())) {
            template.setIsMandatory("1");
        }
        certTemplateMapper.insert(template);
        audit(template.getId(), template.getCountryName() + "/" + template.getCertName(), operatorId, "CERT_TPL_CREATE");
        return template;
    }

    /**
     * 禁止直删旁路（P0-6.2 / G-02）：认证模板须走删除审核引擎。
     *
     * @param id         模板 ID（仅用于错误上下文）
     * @param operatorId 操作人（保留签名兼容，不执行删除）
     * @throws ServiceException 始终拒绝，提示走 DeletionRequest
     */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id, Long operatorId) {
        throw new ServiceException("认证模板禁止直删，请提交删除审核（entityType=cert_templates, id=" + id + "）");
    }

    private void audit(Long id, String name, Long operatorId, String action) {
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId).action(action).entityType("cert_templates").entityId(id).reason(name)
            .createTime(new Date()).build());
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}