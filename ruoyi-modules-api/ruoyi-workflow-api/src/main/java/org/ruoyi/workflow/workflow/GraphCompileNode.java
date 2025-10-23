package org.ruoyi.workflow.workflow;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
public class GraphCompileNode extends CompileNode {
    private CompileNode root;

    public void appendToLeaf(CompileNode node) {
        boolean exists = false;
        CompileNode tail = root;
        while (!tail.getNextNodes().isEmpty()) {
            tail = tail.getNextNodes().get(0);
            if (tail.getId().equals(node.getId())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            tail.getNextNodes().add(node);
        }
    }

    public CompileNode getTail() {
        if (root.nextNodes.isEmpty()) {
            return root;
        }
        CompileNode tail = root.nextNodes.get(0);
        while (!tail.getNextNodes().isEmpty()) {
            tail = tail.getNextNodes().get(0);
        }
        return tail;
    }
}
