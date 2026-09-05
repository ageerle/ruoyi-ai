package org.ruoyi.ipd.service.executor;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.service.SoftDeleteExecutor;
import org.springframework.stereotype.Component;

/**
 * Project 软删除执行器（entityType=projects，P0-6.2）。
 * 幂等：已软删（del_flag="1"）不重复写；不存在抛 ServiceException 拒绝执行。
 */
@Component
@RequiredArgsConstructor
public class ProjectSoftDeleteExecutor implements SoftDeleteExecutor<Project> {

    public static final String ENTITY_TYPE = "projects";

    private final ProjectMapper projectMapper;

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public Class<Project> entityClass() {
        return Project.class;
    }

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