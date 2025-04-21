package org.ruoyi.domain.bo;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author ageer
 */
@Data
public class KnowledgeInfoUploadBo {

    private String kid;

    private MultipartFile file;

}
