package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * 员工分配对象 store_employee
 *
 * @author ageerle
 * @date Mon Aug 18 21:33:27 CST 2025
 */
@Data
@TableName("store_employee")
public class StoreEmployee implements Serializable {


    /**
     * 主键ID
     */
        @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 门店ID
     */
    private Long storeId;

    /**
     * 员工ID
     */
    private Long userId;

    /**
     * 职位
     */
    private String roleInStore;

    /**
     * 分配时间
     */
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
