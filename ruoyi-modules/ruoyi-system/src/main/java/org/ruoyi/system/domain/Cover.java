package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 翻唱对象
 *
 * @author NSL
 * @date 2024-12-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("chat_cover")
public class Cover extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;
    /**
     * 歌曲名称
     */
    private String coverMusicName;
    /**
     * 歌曲地址
     */
    private String coverMusicUrl;
    /**
     * 歌手性别 枚举 FEMALE 女性 MALE 男性
     */
    private String coverSingerGender;
    /**
     * 歌手姓名
     */
    private String coverSingerName;
    /**
     * 用户性别 FEMALE MALE
     */
    private String userGender;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 本次消费金额
     */
    private String cost;
    /**
     * 翻唱后的URL
     */
    private String coverUrl;
}
