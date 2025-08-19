package org.ruoyi.system.domain.bo;

import org.ruoyi.system.domain.StoreEmployee;
import org.ruoyi.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.io.Serializable;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;
import java.io.Serializable;
import java.io.Serializable;
import org.ruoyi.common.core.validate.AddGroup;
import org.ruoyi.common.core.validate.EditGroup;

/**
 * 员工分配业务对象 store_employee
 *
 * @author ageerle
 * @date Mon Aug 18 21:33:27 CST 2025
 */
@Data

@AutoMapper(target = StoreEmployee.class, reverseConvertGenerate = false)
public class StoreEmployeeBo implements Serializable {

    private Long id;

    /**
     * 门店ID
     */
    @NotNull(message = "门店ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long storeId;
    /**
     * 员工ID
     */
    @NotNull(message = "员工ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long userId;
    /**
     * 职位
     */
    private String roleInStore;
    /**
     * 分配时间
     */
    @NotNull(message = "分配时间不能为空", groups = { AddGroup.class, EditGroup.class })
    private LocalDateTime assignTime;
    /**
     * 门店类型
     */
    private String isPrimary;
    /**
     * 分配到期时间
     */
    private LocalDateTime expireTime;

}
