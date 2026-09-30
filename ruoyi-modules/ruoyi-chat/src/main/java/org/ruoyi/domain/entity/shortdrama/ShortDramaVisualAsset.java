package org.ruoyi.domain.entity.shortdrama;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("short_drama_visual_asset")
public class ShortDramaVisualAsset {
    @TableId private Long id;
    private Long projectId;
    private Long storyboardId;
    private String assetKey;
    private String kind;
    private String title;
    private String prompt;
    private String referenceImages;
    private String sourceHash;
    private String status;
    private String imageUrl;
    private String model;
    private String predictionId;
    private String errorMessage;
    private Date createTime;
    private Date updateTime;
}
