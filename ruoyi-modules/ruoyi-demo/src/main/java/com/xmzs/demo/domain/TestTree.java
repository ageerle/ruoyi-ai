package com.xmzs.demo.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.xmzs.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 测试树表对象 test_tree
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_tree")
public class TestTree extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 父ID
     */
    private Long parentId;

    /**
     * 部门id
     */
    private Long deptId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 树节点名
     */
    private String treeName;

    /**
     * 版本
     */
    @Version
    private Long version;

    /**
     * 删除标志
     */
    @TableLogic
    private Long delFlag;

}
