package org.ruoyi.system.domain.vo.cover;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.system.domain.Cover;

import java.io.Serial;
import java.io.Serializable;

/**
 * 翻唱用户参考音频视图对象
 *
 * @author NSL
 * @since  2024-12-25
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = Cover.class)
public class CoverPromptAudioVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "id")
    private Long id;
    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private Long userId;
    /**
     * 翻唱主表id
     */
    @ExcelProperty(value = "翻唱主表id")
    private Long coverId;
    /**
     * 翻唱后的URL
     */
    @ExcelProperty(value = "参考音频")
    private String promptAudioUrl;

}
