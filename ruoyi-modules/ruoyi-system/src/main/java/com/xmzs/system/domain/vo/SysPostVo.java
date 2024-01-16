package com.xmzs.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.xmzs.common.excel.annotation.ExcelDictFormat;
import com.xmzs.common.excel.convert.ExcelDictConvert;
import com.xmzs.system.domain.SysPost;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;



/**
 * 岗位信息视图对象 sys_post
 *
 * @author Michelle.Chung
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysPost.class)
public class SysPostVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 岗位ID
     */
    @ExcelProperty(value = "岗位序号")
    private Long postId;

    /**
     * 岗位编码
     */
    @ExcelProperty(value = "岗位编码")
    private String postCode;

    /**
     * 岗位名称
     */
    @ExcelProperty(value = "岗位名称")
    private String postName;

    /**
     * 显示顺序
     */
    @ExcelProperty(value = "岗位排序")
    private Integer postSort;

    /**
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

}
