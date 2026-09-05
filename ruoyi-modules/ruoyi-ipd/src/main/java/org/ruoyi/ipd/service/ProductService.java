package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 产品服务（BR-PROD-01 三路来源；产品:项目 = 1:1 Q5）
 * 来源规则：
 *  ADMIN_IMPORT 超管导入在售型号 → modelCode 必填
 *  PM_NEW       PM 新增         → 常规
 *  GUEST_OTHER  游客「其他」占位 → 仅占位，不可关联项目
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final Set<String> SOURCES = Set.of(Product.SRC_ADMIN_IMPORT, Product.SRC_PM_NEW, Product.SRC_GUEST_OTHER);

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
            product.setStatus("ACTIVE");
        }
        if (product.getId() == null) {
            product.setCreateTime(new Date());
        }
        productMapper.insert(product);
        audit(product.getId(), product.getProductName(), operatorId, "PRODUCT_CREATE");
        return product;
    }

    /** 关联项目到已有产品（1:1：项目与产品互斥占用） */
    @Transactional(rollbackFor = Exception.class)
    public void bindProject(Long productId, Long projectId, Long operatorId) {
        Product product = require(productId);
        checkProjectNotTaken(projectId);
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在: " + projectId);
        }
        product.setProjectId(projectId);
        productMapper.updateById(product);
        audit(productId, product.getProductName(), operatorId, "PRODUCT_BIND_PROJECT");
    }

    /** 状态切换 ACTIVE|INACTIVE（删除走两级审核引擎，此处只做启停） */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long productId, String status, Long operatorId) {
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new ServiceException("产品状态非法: " + status);
        }
        Product product = require(productId);
        product.setStatus(status);
        productMapper.updateById(product);
        audit(productId, product.getProductName(), operatorId, "PRODUCT_STATUS_" + status);
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

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}