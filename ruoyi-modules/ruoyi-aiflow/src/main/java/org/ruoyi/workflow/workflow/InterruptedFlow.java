package org.ruoyi.workflow.workflow;

import org.apache.commons.collections4.map.PassiveExpiringMap;

/**
 * 已中断正在等待用户输入的流程 <br/>
 * TODO 需要考虑项目多节点部署的情况
 */
public class InterruptedFlow {

    /**
     * 10分钟超时
     */
    private static final PassiveExpiringMap.ExpirationPolicy<String, WorkflowEngine> ep = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(60 * 1000 * 10);
    public static PassiveExpiringMap<String, WorkflowEngine> RUNTIME_TO_GRAPH = new PassiveExpiringMap<>(ep);

}
