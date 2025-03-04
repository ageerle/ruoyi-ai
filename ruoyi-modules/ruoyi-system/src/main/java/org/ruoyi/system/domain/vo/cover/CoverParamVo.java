package org.ruoyi.system.domain.vo.cover;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 翻唱歌曲入参
 *
 * @author NSL
 * @since  2024-12-25
 */
@Data
public class CoverParamVo implements Serializable {

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
     * 参考音频：用户首次翻唱，必填；第二次及之后翻唱：若为空则沿用最近一次参考音频训练模型，若不为空则使用该参考音频训练模型。
     * 可多传
     */
    @ExcelProperty(value = "参考音频")
    private List<String> promptAudioUrl;
    /**
     * 用户性别 FEMALE MALE
     */
    @ExcelProperty(value = "用户性别")
    private String userGender;
    /**
     * 用户id
     */
    @ExcelProperty(value = "用户id")
    private String userId;

    /**
     * 业务主键id
     */
    @ExcelProperty(value = "业务主键id")
    private String orderId;
}
