package org.ruoyi.knowledge.domain.req;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class KnowledgeInfoUploadRequest {

    private String kid;

    private MultipartFile file;

}
