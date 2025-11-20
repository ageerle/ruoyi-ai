package org.ruoyi.workflow.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.ruoyi.workflow.workflow.data.NodeIOData;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NodeProcessResult {

    private List<NodeIOData> content = new ArrayList<>();

    /**
     * 条件执行时使用
     */
    private String nextNodeUuid;

    /**
     * 是否发生错误
     */
    private boolean error = false;

    /**
     * 错误或提示信息
     */
    private String message;
}
