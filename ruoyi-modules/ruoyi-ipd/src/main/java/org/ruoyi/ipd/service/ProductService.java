package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 产品服务（BR-PROD-01 三路来源；产品:项目 = 1:1 Q5）
 * 来源规则：
 *  ADMIN_IMPORT 超管导入在售型号 → modelCode 必填
 *  PM_NEW       PM 新增         → 常规
 *  GUEST_OTHER  游客「其他」占位 → 仅占位，不可关联项目
 *
 * Round 8 / R8-P0-7：batchImportOnSale 预取优化（一次性 selectList in (...) → Map.get）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> SOURCES = Set.of(Product.SRC_ADMIN_IMPORT, Product.SRC_PM_NEW, Product.SRC_GUEST_OTHER);

    /** Round 8 / R8-P0-10：批量导入单次最大行数 */
    public static final int MAX_BATCH_SIZE = 500;

    private final ProductMapper productMapper;
    private final ProjectMapper projectMapper;
    private final AuditLogService auditLogService;

    @Transactional(rollbackFor = Exception.class)
    public Product create(Product product, Long operatorId) {
        if (product.getSource() == null || !SOURCES.contains(product.getSource())) {
            throw new ServiceException("产品来源非法: " + product.getSource() + "（允许 ADMIN_IMPORT|PM_NEW|GUEST_OTHER）");
        }
        if (Product.SRC_ADMIN_IMPORT.equals(product.getSource()) && isBlank(product.getModelCode())) {
            throw new ServiceException("超管导入在售型号必须填写 modelCode");
        }
        if (product.getProjectId() != null) {
            if (Product.SRC_GUEST_OTHER.equals(product.getSource())) {
                throw new ServiceException("游客「其他」占位产品不可关联项目");
            }
            checkProjectNotTaken(product.getProjectId());
        }
        if (isBlank(product.getStatus())) {
            // AC-PROD-06/07：超管导入→在售；PM 新增→在研
            if (Product.SRC_ADMIN_IMPORT.equals(product.getSource())) {
                product.setStatus(Product.ST_ON_SALE);
            } else if (Product.SRC_PM_NEW.equals(product.getSource())) {
                product.setStatus(Product.ST_IN_RD);
            } else {
                product.setStatus(Product.ST_IN_RD);
            }
        }
        if (product.getId() == null) {
            product.setCreateTime(new Date());
        }
        productMapper.insert(product);
        audit(product.getId(), product.getProductName(), operatorId, "PRODUCT_CREATE");
        return product;
    }

    /**
     * P1-1.2：编辑产品基础字段（名称/编码/型号/组）；已绑项目时禁止改来源。
     * R8X-CONT-1 P0-2：加 actor.groupId == product.groupId 横向越权防护（SUPER_ADMIN 豁免）。
     *
     * @param productId    产品 ID
     * @param patch        白名单变更
     * @param operatorId   操作人 ID
     * @param actorGroupId 操作人所属产品组
     * @param actorRole    操作人角色
     * @return 更新后实体
     */
    @Transactional(rollbackFor = Exception.class)
    public Product update(Long productId, Product patch, Long operatorId,
                          Long actorGroupId, String actorRole) {
        Product product = require(productId);
        assertSameGroup(actorRole, actorGroupId, product.getGroupId(), "操作人");
        if (patch.getProductName() != null && !patch.getProductName().isBlank()) {
            product.setProductName(patch.getProductName().trim());
        }
        if (patch.getProductCode() != null) {
            product.setProductCode(patch.getProductCode().isBlank() ? null : patch.getProductCode().trim());
        }
        if (patch.getModelCode() != null) {
            if (Product.SRC_ADMIN_IMPORT.equals(product.getSource()) && patch.getModelCode().isBlank()) {
                throw new ServiceException("超管导入在售型号必须填写 modelCode");
            }
            product.setModelCode(patch.getModelCode().isBlank() ? null : patch.getModelCode().trim());
        }
        if (patch.getGroupId() != null) {
            product.setGroupId(patch.getGroupId());
        }
        if (patch.getSource() != null && !patch.getSource().equals(product.getSource())) {
            throw new ServiceException("产品来源创建后不可变更");
        }
        productMapper.updateById(product);
        audit(productId, product.getProductName(), operatorId, "PRODUCT_UPDATE");
        return product;
    }

    /**
     * P1-1.2 / AC-PROD-06：超管批量导入在售型号；按 modelCode 幂等（已存在则更新名称）。
     * Round 8 / R8-P0-7：从「循环每行 selectOne」改造为「一次性 selectList in(...) → Map.get」。
     * 100 行导入 IO 从 100 SELECT 降到 1 SELECT。
     *
     * @param items      导入行（受 Controller @Size(max=500) 约束）
     * @param operatorId 超管
     * @return 逐行结果（ok/error）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> batchImportOnSale(List<Product> items, Long operatorId) {
        if (items == null || items.isEmpty()) {
            throw new ServiceException("导入列表不能为空");
        }
        if (items.size() > MAX_BATCH_SIZE) {
            throw new ServiceException("单次导入最多 " + MAX_BATCH_SIZE + " 行（实际 " + items.size() + " 行）");
        }
        // R8-P0-7：一次性预取所有已存在的 modelCode → Map<modelCode, Product>
        Set<String> models = new HashSet<>();
        for (Product item : items) {
            if (item != null && !isBlank(item.getModelCode())) {
                models.add(item.getModelCode().trim());
            }
        }
        Map<String, Product> existingMap = models.isEmpty()
            ? Map.of()
            : productMapper.selectList(new LambdaQueryWrapper<Product>()
                .in(Product::getModelCode, models)
                .eq(Product::getSource, Product.SRC_ADMIN_IMPORT))
                .stream()
                .collect(Collectors.toMap(Product::getModelCode, p -> p, (a, b) -> a));

        List<Map<String, Object>> report = new ArrayList<>();
        int row = 0;
        for (Product item : items) {
            row++;
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("row", row);
            try {
                if (item == null || isBlank(item.getProductName())) {
                    throw new ServiceException("产品名称必填");
                }
                if (isBlank(item.getModelCode())) {
                    throw new ServiceException("超管导入在售型号必须填写 modelCode");
                }
                String model = item.getModelCode().trim();
                Product existing = existingMap.get(model);
                if (existing != null) {
                    existing.setProductName(item.getProductName().trim());
                    if (item.getProductCode() != null && !item.getProductCode().isBlank()) {
                        existing.setProductCode(item.getProductCode().trim());
                    }
                    if (item.getGroupId() != null) {
                        existing.setGroupId(item.getGroupId());
                    }
                    existing.setStatus(Product.ST_ON_SALE);
                    productMapper.updateById(existing);
                    audit(existing.getId(), existing.getProductName(), operatorId, "PRODUCT_IMPORT_UPSERT");
                    line.put("status", "UPSERT");
                    line.put("id", existing.getId());
                } else {
                    Product created = Product.builder()
                        .productName(item.getProductName().trim())
                        .productCode(item.getProductCode())
                        .modelCode(model)
                        .source(Product.SRC_ADMIN_IMPORT)
                        .groupId(item.getGroupId())
                        .status(Product.ST_ON_SALE)
                        .tenantId("000000")
                        .delFlag("0")
                        .build();
                    create(created, operatorId);
                    line.put("status", "CREATED");
                    line.put("id", created.getId());
                }
                line.put("ok", true);
            } catch (ServiceException ex) {
                line.put("ok", false);
                line.put("error", ex.getMessage());
            }
            report.add(line);
        }
        return report;
    }

    /**
     * 状态切换 ON_SALE|IN_RD|INACTIVE|ACTIVE（删除走两级审核引擎）。
     * R8X-CONT-1 P0-2：加 actor.groupId == product.groupId 横向越权防护（SUPER_ADMIN 豁免）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long productId, String status, Long operatorId,
                             Long actorGroupId, String actorRole) {
        if (!Product.STATUSES.contains(status)) {
            throw new ServiceException("产品状态非法: " + status + "（允许 ON_SALE|IN_RD|INACTIVE|ACTIVE）");
        }
        Product product = require(productId);
        assertSameGroup(actorRole, actorGroupId, product.getGroupId(), "操作人");
        product.setStatus(status);
        productMapper.updateById(product);
        audit(productId, product.getProductName(), operatorId, "PRODUCT_STATUS_" + status);
    }

    /**
     * P1-1.1：关联项目到已有产品（产品:项目 = 1:1，两端同事务维护）。
     * <p>AC-PROD-01：已挂项目的产品再关联第二个项目 ⇒ 拒绝「一个产品仅对应一个项目」（409 STATE_CONFLICT）。
     * <p>GUEST_OTHER 占位不可绑定；软删项目/产品拒绝；同 id 重绑幂等成功不写二次审计。
     * <p>R8X-CONT-1 P0-2：加 actor.groupId == product.groupId 横向越权防护（SUPER_ADMIN 豁免，403 FORBIDDEN）。
     * <p>P1-1.1 并发防御：product 端用条件 UPDATE {@code id=? AND project_id IS NULL}（行锁原子）；
     * project 端用条件 UPDATE {@code id=? AND product_id IS NULL}；任一端 affected=0 即拒绝。
     * DB 兜底：{@code uk_products_project(project_id)} + {@code uk_projects_product(product_id)} UNIQUE。
     *
     * @param productId    产品 ID
     * @param projectId    目标项目 ID
     * @param operatorId   操作人 ID
     * @param actorGroupId 操作人所属产品组
     * @param actorRole    操作人角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindProject(Long productId, Long projectId, Long operatorId,
                            Long actorGroupId, String actorRole) {
        Product product = require(productId);
        assertSameGroupIpd(actorRole, actorGroupId, product.getGroupId());
        if (Product.SRC_GUEST_OTHER.equals(product.getSource())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "游客「其他」占位产品不可关联项目");
        }
        // 同 id 重绑幂等：产品已绑该 project 且 project 已绑该 product 视为成功
        if (projectId.equals(product.getProjectId())) {
            Project cur = projectMapper.selectById(projectId);
            if (cur != null && productId.equals(cur.getProductId())) {
                return;
            }
        }
        if (product.getProjectId() != null) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "一个产品仅对应一个项目（当前已绑 projectId=" + product.getProjectId() + "）");
        }
        Project project = requireProject(projectId);
        if (project.getProductId() != null && !project.getProductId().equals(productId)) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "该项目已关联其他产品（产品:项目 = 1:1，当前 productId=" + project.getProductId() + "）");
        }

        try {
            // product 端条件 UPDATE：行锁 + 1:1 自洽守卫
            int rows1 = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .isNull(Product::getProjectId)
                .set(Product::getProjectId, projectId));
            if (rows1 == 0) {
                throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                    "一个产品仅对应一个项目（条件更新冲突，请先解绑）");
            }
            // project 端条件 UPDATE：行锁 + 1:1 自洽守卫
            int rows2 = projectMapper.update(null, new LambdaUpdateWrapper<Project>()
                .eq(Project::getId, projectId)
                .isNull(Project::getProductId)
                .set(Project::getProductId, productId));
            if (rows2 == 0) {
                // product 端已写入但 project 端失败 → 事务回滚（@Transactional）
                throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                    "该项目已被其他产品绑定（产品:项目 = 1:1）");
            }
        } catch (DuplicateKeyException dup) {
            // DB UNIQUE 兜底：uk_products_project / uk_projects_product
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "绑定冲突：产品或项目已被占用（产品:项目 = 1:1）");
        }
        // 自洽终态校验：重读两端确认一致（DB UNIQUE 兜底后的最后一道）
        Product reread = productMapper.selectById(productId);
        Project rereadP = projectMapper.selectById(projectId);
        if (reread == null || rereadP == null
            || !projectId.equals(reread.getProjectId())
            || !productId.equals(rereadP.getProductId())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "绑定终态自洽校验失败（产品:项目 = 1:1）");
        }
        audit(productId, product.getProductName(), operatorId, "PRODUCT_BIND_PROJECT");
    }

    /**
     * P1-1.1：解绑项目（产品 ↔ 项目双向 1:1）。
     * <p>产品未绑该项目 / 项目未绑该产品 ⇒ 拒绝 409（终态自洽，避免「解绑非绑定对端」静默成功）。
     * <p>条件 UPDATE：product 端 {@code id=? AND project_id=?} 与 project 端 {@code id=? AND product_id=?};
     * 任一端 affected=0 即 STATE_CONFLICT。审计 PRODUCT_UNBIND_PROJECT（operator_id/actor 一致）。
     * <p>R8X-CONT-1 P0-2：actor.groupId == product.groupId 横向越权防护（SUPER_ADMIN 豁免）。
     *
     * @param productId    产品 ID
     * @param projectId    目标项目 ID
     * @param operatorId   操作人 ID
     * @param actorGroupId 操作人所属产品组
     * @param actorRole    操作人角色
     */
    @Transactional(rollbackFor = Exception.class)
    public void unbindProject(Long productId, Long projectId, Long operatorId,
                              Long actorGroupId, String actorRole) {
        Product product = require(productId);
        assertSameGroupIpd(actorRole, actorGroupId, product.getGroupId());
        if (!projectId.equals(product.getProjectId())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "产品未绑定该项目（产品:项目 = 1:1 自洽）");
        }
        Project project = requireProject(projectId);
        if (!productId.equals(project.getProductId())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "项目未绑定该产品（产品:项目 = 1:1 自洽）");
        }
        // product 端条件 UPDATE
        int rows1 = productMapper.update(null, new LambdaUpdateWrapper<Product>()
            .eq(Product::getId, productId)
            .eq(Product::getProjectId, projectId)
            .set(Product::getProjectId, null));
        if (rows1 == 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "解绑冲突：产品端条件更新受影响行 0");
        }
        // project 端条件 UPDATE
        int rows2 = projectMapper.update(null, new LambdaUpdateWrapper<Project>()
            .eq(Project::getId, projectId)
            .eq(Project::getProductId, productId)
            .set(Project::getProductId, null));
        if (rows2 == 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "解绑冲突：项目端条件更新受影响行 0");
        }
        // 自洽终态校验：两端均已解绑
        Product reread = productMapper.selectById(productId);
        Project rereadP = projectMapper.selectById(projectId);
        if (reread == null || rereadP == null
            || reread.getProjectId() != null
            || rereadP.getProductId() != null) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "解绑终态自洽校验失败（产品:项目 = 1:1）");
        }
        audit(productId, product.getProductName(), operatorId, "PRODUCT_UNBIND_PROJECT");
    }

    public Product getById(Long id) {
        return productMapper.selectById(id);
    }

    public List<Product> list(String keyword) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<Product>().eq(Product::getDelFlag, "0");
        if (keyword != null && !keyword.isBlank()) {
            qw.like(Product::getProductName, keyword);
        }
        return productMapper.selectList(qw.orderByDesc(Product::getId));
    }

    private Product require(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || "1".equals(product.getDelFlag())) {
            throw new ServiceException("产品不存在: " + id);
        }
        return product;
    }

    /**
     * 加载未软删项目，用于 1:1 绑定。
     *
     * @param id 项目主键
     * @return 存活项目
     */
    private Project requireProject(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null || "1".equals(project.getDelFlag())) {
            throw new ServiceException("项目不存在: " + id);
        }
        return project;
    }

    private void checkProjectNotTaken(Long projectId) {
        Long taken = productMapper.selectCount(new LambdaQueryWrapper<Product>()
            .eq(Product::getProjectId, projectId).eq(Product::getDelFlag, "0"));
        if (taken != null && taken > 0) {
            throw new ServiceException("该项目已关联其他产品（产品:项目 = 1:1）");
        }
    }

    private void audit(Long id, String name, Long operatorId, String action) {
        auditLogService.append(AuditLog.builder()
            .operatorId(operatorId).action(action).entityType("products").entityId(id).reason(name)
            .createTime(new Date()).build());
    }

    /**
     * R8X-CONT-1 P0-2：横向越权防护——SUPER_ADMIN 一律通过；其他角色必须 actor.groupId == product.groupId。
     * 复用 ProjectService.assertSameGroup 语义，本类独享以避免 service 间循环依赖。
     * P1-1.1 / SEC-02：改用 IpdBusinessException(FORBIDDEN=30001)，HTTP 403 而非旧 ServiceException 50000。
     */
    private void assertSameGroupIpd(String actorRole, Long actorGroupId, Long objectGroupId) {
        if ("SUPER_ADMIN".equals(actorRole)) {
            return;
        }
        if (actorGroupId == null || !actorGroupId.equals(objectGroupId)) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN,
                "操作人必须归属产品组（横向越权防护 SEC-02）");
        }
    }

    /** 兼容旧调用点（create/update/changeStatus），仍走 ServiceException 路径以维持既有契约。 */
    private void assertSameGroup(String actorRole, Long actorGroupId, Long objectGroupId, String roleLabel) {
        if ("SUPER_ADMIN".equals(actorRole)) {
            return;
        }
        if (actorGroupId == null || !actorGroupId.equals(objectGroupId)) {
            throw new ServiceException(roleLabel + "必须归属产品组（横向越权防护）");
        }
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}