package org.ruoyi.system.domain.vo.cover;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.ruoyi.system.domain.Cover;

import java.io.Serial;
import java.io.Serializable;

/**
 * 翻唱视图对象
 *
 * @author NSL
 * @date 2024-12-25
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = Cover.class)
public class CoverVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "id")
    private Long id;
    /**
     * 歌曲名称
     */
    @ExcelProperty(value = "歌曲名称")
    private String coverMusicName;
    /**
     * 歌曲地址
     */
    @ExcelProperty(value = "歌曲地址")
    private String coverMusicUrl;
    /**
     * 歌手性别 枚举 FEMALE 女性 MALE 男性
     */
    @ExcelProperty(value = "歌手性别")
    private String coverSingerGender;
    /**
     * 歌手姓名
     */
    @ExcelProperty(value = "歌手姓名")
    private String coverSingerName;
    /**
     * 用户性别 FEMALE MALE
     */
    @ExcelProperty(value = "用户性别")
    private String userGender;
    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private Long userId;
    /**
     * 本次消费金额
     */
    @ExcelProperty(value = "本次消费金额")
    private String cost;
    /**
     * 翻唱后的URL
     */
    @ExcelProperty(value = "翻唱后的URL")
    private String coverUrl;
}
