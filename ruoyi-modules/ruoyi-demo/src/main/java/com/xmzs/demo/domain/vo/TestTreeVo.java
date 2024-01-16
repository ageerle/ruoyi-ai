package com.xmzs.demo.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.xmzs.demo.domain.TestTree;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;


/**
 * 测试树表视图对象 test_tree
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = TestTree.class)
public class TestTreeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 父id
     */
    @ExcelProperty(value = "父id")
    private Long parentId;

    /**
     * 部门id
     */
    @ExcelProperty(value = "部门id")
    private Long deptId;

    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private Long userId;

    /**
     * 树节点名
     */
    @ExcelProperty(value = "树节点名")
    private String treeName;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;


}
