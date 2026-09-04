---
source: file:///Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-system/src/main/java/org/ruoyi/system/domain/CmsContent.java
collected: 2026-09-04
published: 2026-09-04
topic: system-source
---

# CmsContent.java

```java
package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import org.ruoyi.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 内容管理表 cms_content
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_content")
public class CmsContent extends TenantEntity {

    /**
     * 内容ID
     */
    @TableId(value = "content_id")
    private Long contentId;

    /**
     * 内容标题
     */
    private String title;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 正文内容
     */
    private String content;

    /**
     * 封面图片
     */
    private String coverImage;

    /**
     * 状态（0草稿 1已发布 2下架）
     */
    private String status;

    /**
     * 发布时间
     */
    private Date publishTime;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;

}
```
