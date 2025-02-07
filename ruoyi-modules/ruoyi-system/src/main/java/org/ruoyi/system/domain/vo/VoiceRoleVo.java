package org.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.system.domain.VoiceRole;

import java.io.Serial;
import java.io.Serializable;



/**
 * 配音角色视图对象 voice_role
 *
 * @author Lion Li
 * @date 2024-03-19
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = VoiceRole.class)
public class VoiceRoleVo implements Serializable {

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
    @ExcelProperty(value = "角色名称")
    private String name;

    /**
     * 角色描述
     */
    @ExcelProperty(value = "角色描述")
    private String description;

    /**
     * 头像
     */
    @ExcelProperty(value = "头像")
    private String avatar;

    /**
     * 角色id
     */
    @ExcelProperty(value = "角色id")
    private String voiceId;

    /**
     * 音频地址
     */
    @ExcelProperty(value = "音频地址")
    private String fileUrl;

    /**
     * 音频预处理（实验性）
     */
    @ExcelProperty(value = "音频预处理")
    private String preProcess;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
