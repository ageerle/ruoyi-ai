package org.ruoyi.workflow.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompileNode {
    protected String id;
    protected Boolean conditional = false;

    /**
     * 以下两种情况会导致多个nextNode出现：
     * 1. 下游节点为并行节点，所有的下游节点同时运行
     * 2. 当前节点为条件分支节点，下游节点为多个节点，实际执行时只会执行一条
     * 两种节点根据是否GraphCompileNode来区分
     */
    protected List<CompileNode> nextNodes = new ArrayList<>();
}
