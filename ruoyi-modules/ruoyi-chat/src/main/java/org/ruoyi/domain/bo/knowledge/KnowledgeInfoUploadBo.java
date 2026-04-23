package org.ruoyi.domain.bo.knowledge;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
 * 附件上传请求
 */
@Data
public class KnowledgeInfoUploadBo {

    private Long knowledgeId;

    private MultipartFile file;

    /**
     * 是否自动解析 (true: 立即解析, false: 仅上传)
     */
    private Boolean autoParse;

    /**
     * 生效时间, 为空则立即生效
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date effectiveTime;

}
