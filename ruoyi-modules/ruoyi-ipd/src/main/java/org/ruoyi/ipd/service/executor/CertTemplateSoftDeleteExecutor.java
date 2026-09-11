package org.ruoyi.ipd.service.executor;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.CertTemplate;
import org.ruoyi.ipd.mapper.CertTemplateMapper;
import org.ruoyi.ipd.service.SoftDeleteExecutor;
import org.springframework.stereotype.Component;

/**
 * CertTemplate 软删除执行器（entityType=cert_templates，P0-6.2）。
 * 幂等：已软删不重复写。
 */
@Component
@RequiredArgsConstructor
public class CertTemplateSoftDeleteExecutor implements SoftDeleteExecutor<CertTemplate> {

    public static final String ENTITY_TYPE = "cert_templates";

    private final CertTemplateMapper certTemplateMapper;

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public Class<CertTemplate> entityClass() {
        return CertTemplate.class;
    }

    @Override
    public void softDelete(Long id) {
        CertTemplate cert = certTemplateMapper.selectById(id);
        if (cert == null || "1".equals(cert.getDelFlag())) {
            return;
        }
        cert.setDelFlag("1");
        int rows = certTemplateMapper.updateById(cert);
        if (rows != 1) {
            throw new ServiceException("认证模板软删除未更新唯一记录: id=" + id);
        }
    }

    /**
     * 判断认证模板是否已软删或不存在。
     *
     * @param id 模板主键
     * @return true 表示应记 DELETE_NOOP
     */
    @Override
    public boolean isDeleted(Long id) {
        CertTemplate cert = certTemplateMapper.selectById(id);
        return cert == null || "1".equals(cert.getDelFlag());
    }
}