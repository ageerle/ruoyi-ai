package org.ruoyi.workflow.service;

import org.ruoyi.workflow.domain.vo.NodeExtVo;

import java.util.Map;

/**
 * 流程节点扩展属性 服务层
 *
 * @author AprilWind
 */
public interface IFlwNodeExtService {

    /**
     * 解析扩展属性 JSON 并构建 Node 扩展属性对象
     * <p>
     * 根据传入的 JSON 字符串，将扩展属性分为三类：
     * 1. ButtonPermissionEnum：解析为按钮权限列表，标记每个按钮是否勾选
     * 2. CopySettingEnum：解析为抄送对象 ID 集合
     * 3. VariablesEnum：解析为自定义参数 Map
     *
     * <p>示例 JSON：
     * [
     * {"code": "ButtonPermissionEnum", "value": "back,termination"},
     * {"code": "CopySettingEnum", "value": "1"},
     * {"code": "VariablesEnum", "value": "key1=value1,key2=value2"}
     * ]
     *
     * @param ext      扩展属性 JSON 字符串
     * @param variable 流程变量
     * @return NodeExtVo 对象，封装按钮权限列表、抄送对象集合和自定义参数 Map
     */
    NodeExtVo parseNodeExt(String ext, Map<String, Object> variable);

}
