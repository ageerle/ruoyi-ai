package org.ruoyi.ipd.service.executor;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.service.SoftDeleteExecutor;
import org.springframework.stereotype.Component;

/**
 * Product 软删除执行器（entityType=products，P0-6.2）。
 * 幂等：已软删（del_flag="1"）不重复写。
 */
@Component
@RequiredArgsConstructor
public class ProductSoftDeleteExecutor implements SoftDeleteExecutor<Product> {

    public static final String ENTITY_TYPE = "products";

    private final ProductMapper productMapper;

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public Class<Product> entityClass() {
        return Product.class;
    }

    @Override
    public void softDelete(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null || "1".equals(product.getDelFlag())) {
            return;
        }
        product.setDelFlag("1");
        int rows = productMapper.updateById(product);
        if (rows != 1) {
            throw new ServiceException("产品软删除未更新唯一记录: id=" + id);
        }
    }

    /**
     * 判断产品是否已软删或不存在。
     *
     * @param id 产品主键
     * @return true 表示应记 DELETE_NOOP
     */
    @Override
    public boolean isDeleted(Long id) {
        Product product = productMapper.selectById(id);
        return product == null || "1".equals(product.getDelFlag());
    }
}