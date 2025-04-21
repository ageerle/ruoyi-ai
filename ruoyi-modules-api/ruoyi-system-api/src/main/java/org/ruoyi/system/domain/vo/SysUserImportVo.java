package org.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户对象导入VO
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
public class SysUserImportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户名称
     */
    @ExcelProperty(value = "用户名称")
    private String userName;

    /**
     * 用户余额
     */
    @ExcelProperty(value = "用户余额")
    private Double userBalance;

    /**
     * 用户等级
     */
    @ExcelProperty(value = "用户等级(0免费用户 1付费用户)")
    private String userGrade;

}
