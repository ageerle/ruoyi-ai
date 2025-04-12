package org.ruoyi.domain.vo;


import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.ChatAppStore;

import java.io.Serial;
import java.io.Serializable;




/**
 * 应用商店视图对象 chat_app_store
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatAppStore.class)
public class ChatAppStoreVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * 名称
     */
    @ExcelProperty(value = "名称")
    private String name;

    /**
     * 描述
     */
    @ExcelProperty(value = "描述")
    private String description;

    /**
     * logo
     */
    @ExcelProperty(value = "logo")
    private String avatar;

    /**
     * 地址
     */
    @ExcelProperty(value = "地址")
    private String appUrl;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
