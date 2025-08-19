package org.ruoyi.system.domain.vo;

    import java.time.LocalDateTime;
    import java.io.Serializable;
    import org.ruoyi.common.excel.annotation.ExcelDictFormat;
    import org.ruoyi.common.excel.convert.ExcelDictConvert;
    import org.ruoyi.system.domain.StoreEmployee;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.util.Date;


/**
 * 员工分配视图对象 store_employee
 *
 * @author ageerle
 * @date Mon Aug 18 21:33:27 CST 2025
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = StoreEmployee.class)
public class StoreEmployeeVo implements Serializable {

    private Long id;
    /**
     * 门店ID
     */
    @ExcelProperty(value = "门店ID")
    private Long storeId;
    /**
     * 员工ID
     */
    @ExcelProperty(value = "员工ID")
    private Long userId;
    /**
     * 职位
     */
    @ExcelProperty(value = "职位", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "title_name")
    private String roleInStore;
    /**
     * 分配时间
     */
    @ExcelProperty(value = "分配时间")
    private LocalDateTime assignTime;
    /**
     * 门店类型
     */
    @ExcelProperty(value = "门店类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "store_type")
    private String isPrimary;
    /**
     * 分配到期时间
     */
    @ExcelProperty(value = "分配到期时间")
    private LocalDateTime expireTime;

}
