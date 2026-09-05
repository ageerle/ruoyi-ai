package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * IPD 产品组服务（P2-1）。
 *
 * <p>组织架构主数据：产品组=组长（一人一组；A3 决策：组长随 API 同步，不支持代理组长）。
 * <ul>
 *   <li>create/update：组名必填 + 同名去重 + 审计写入</li>
 *   <li>updateLeader：HR 同步组长；leaderPersonId 必填</li>
 *   <li>remove：禁止直删旁路（P0-6.2），须走 DeletionRequest</li>
 * </ul>
 *
 * <p>对应验收：AC-INC-15c（产品组长确认系数时的组长来源）、BR-ORG-01（主组=市场 PM 所在产品组）。
 */
@Service
@RequiredArgsConstructor
public class ProductGroupService {

    private final ProductGroupMapper productGroupMapper;
    private final AuditLogService auditLogService;

    /** 列出所有非删除产品组（按名称升序） */
    public List<ProductGroup> listAll() {
        return productGroupMapper.selectList(new LambdaQueryWrapper<ProductGroup>()
            .eq(ProductGroup::getDelFlag, "0")
            .orderByAsc(ProductGroup::getGroupName, ProductGroup::getId));
    }

    /** 按 id 取详情（含删除态） */
    public ProductGroup getById(Long id) {
        ProductGroup g = productGroupMapper.selectById(id);
        if (g == null) {
            throw new ServiceException("产品组不存在: id=" + id);
        }
        return g;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductGroup create(ProductGroup group, Long operatorId) {
        validateGroupName(group.getGroupName());
        Long dup = productGroupMapper.selectCount(new LambdaQueryWrapper<ProductGroup>()
            .eq(ProductGroup::getGroupName, group.getGroupName().trim())
            .eq(ProductGroup::getDelFlag, "0"));
        if (dup != null && dup > 0) {
            throw new ServiceException("已存在同名产品组: " + group.getGroupName().trim());
        }
        // SEC-API-01：服务端权威字段覆写
        group.setId(null);
        group.setGroupName(group.getGroupName().trim());
        group.setCreateTime(new Date());
        group.setUpdateTime(null);
        group.setDelFlag("0");
        if (isBlank(group.getTenantId())) {
            group.setTenantId("000000");
        }
        productGroupMapper.insert(group);
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId)
            .action("PRODUCT_GROUP_CREATE")
            .entityType("product_groups")
            .entityId(group.getId())
            .reason(group.getGroupName())
            .createTime(new Date())
            .build());
        return group;
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductGroup updateLeader(Long groupId, Long newLeaderPersonId, Long operatorId) {
        if (newLeaderPersonId == null) {
            throw new ServiceException("组长必填（leaderPersonId 不能为空）");
        }
        ProductGroup existing = getById(groupId);
        Long oldLeader = existing.getLeaderPersonId();
        existing.setLeaderPersonId(newLeaderPersonId);
        existing.setUpdateTime(new Date());
        productGroupMapper.updateById(existing);
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId)
            .action("PRODUCT_GROUP_LEADER_CHANGE")
            .entityType("product_groups")
            .entityId(groupId)
            .reason("old=" + oldLeader + ",new=" + newLeaderPersonId)
            .createTime(new Date())
            .build());
        return existing;
    }

    /**
     * 禁止直删旁路（P0-6.2 / G-02）：产品组须走 DeletionRequest 审核。
     */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id, Long operatorId) {
        throw new ServiceException("产品组禁止直删，请提交删除审核（entityType=product_groups, id=" + id + "）");
    }

    private void validateGroupName(String name) {
        if (isBlank(name)) {
            throw new ServiceException("产品组组名必填");
        }
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}
