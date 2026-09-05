package org.ruoyi.ipd.service.executor;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.service.SoftDeleteExecutor;
import org.springframework.stereotype.Component;

/**
 * Project 软删除执行器（entityType=projects，P0-6.2 / P1-1.1）。
 * <p>幂等：已软删不重复写。软删后释放产品侧 projectId，避免 1:1 指针残留阻塞重建。
 */
@Component
@RequiredArgsConstructor
public class ProjectSoftDeleteExecutor implements SoftDeleteExecutor<Project> {

    public static final String ENTITY_TYPE = "projects";

    private final ProjectMapper projectMapper;
    private final ProductMapper productMapper;

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public Class<Project> entityClass() {
        return Project.class;
    }

    /**
     * 软删项目并清空仍指向本项目的产品 projectId。
     *
     * @param id 项目主键
     */
    @Override
    public void softDelete(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null || "1".equals(project.getDelFlag())) {
            return;
        }
        project.setDelFlag("1");
        int rows = projectMapper.updateById(project);
        if (rows != 1) {
            throw new ServiceException("项目软删除未更新唯一记录: id=" + id);
        }
        releaseProductLink(id, project.getProductId());
    }

    /**
     * 释放产品侧 1:1 指针（优先按项目上记录的 productId；避免 Lambda 缓存依赖）。
     *
     * @param projectId 已软删项目 ID
     * @param productId 项目上记录的产品 ID，可为 null
     */
    private void releaseProductLink(Long projectId, Long productId) {
        if (productId == null) {
            return;
        }
        Product product = productMapper.selectById(productId);
        if (product == null || "1".equals(product.getDelFlag())) {
            return;
        }
        if (projectId.equals(product.getProjectId())) {
            product.setProjectId(null);
            productMapper.updateById(product);
        }
    }

    /**
     * 判断项目是否已软删或不存在。
     *
     * @param id 项目主键
     * @return true 表示应记 DELETE_NOOP
     */
    @Override
    public boolean isDeleted(Long id) {
        Project project = projectMapper.selectById(id);
        return project == null || "1".equals(project.getDelFlag());
    }
}
