package org.ruoyi.domain.vo;


import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.domain.ChatAppStore;


import java.io.Serial;
import java.io.Serializable;



/**
 * 应用市场视图对象
 *
 * @author Lion Li
 * @date 2024-03-19
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
     * 角色名称
     */
    @ExcelProperty(value = "名称")
    private String name;

    /**
     * 角色描述
     */
    @ExcelProperty(value = "描述")
    private String description;

    /**
     * 头像
     */
    @ExcelProperty(value = "头像")
    private String avatar;

    /**
     * 音频地址
     */
    @ExcelProperty(value = "应用地址")
    private String appUrl;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
