package org.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.system.domain.ChatGpts;

import java.io.Serial;
import java.io.Serializable;


/**
 * gpts管理视图对象 chat_gpts
 *
 * @author Lion Li
 * @date 2024-07-09
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = ChatGpts.class)
public class ChatGptsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @ExcelProperty(value = "id")
    private Long id;

    /**
     * gpts应用id
     */
    @ExcelProperty(value = "gpts应用id")
    private String gid;

    /**
     * gpts应用名称
     */
    @ExcelProperty(value = "gpts应用名称")
    private String name;

    /**
     * gpts图标
     */
    @ExcelProperty(value = "gpts图标")
    private String logo;

    /**
     * gpts描述
     */
    @ExcelProperty(value = "gpts描述")
    private String info;

    /**
     * 作者id
     */
    @ExcelProperty(value = "作者id")
    private String authorId;

    /**
     * 作者名称
     */
    @ExcelProperty(value = "作者名称")
    private String authorName;

    /**
     * 点赞
     */
    @ExcelProperty(value = "点赞")
    private String useCnt;

    /**
     * 差评
     */
    @ExcelProperty(value = "差评")
    private String bad;

    /**
     * 类型
     */
    @ExcelProperty(value = "类型")
    private String type;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 更新IP
     */
    @ExcelProperty(value = "更新IP")
    private String updateIp;


}
