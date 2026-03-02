package org.ruoyi.common.chat.domain.dto.request;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

import java.util.List;

/**
 * 工作流请求体信息
 */
@Data
public class WorkFlowRunner {
    private List<ObjectNode> inputs;
    private String uuid;
}
