package org.ruoyi.workflow.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.dto.DictTypeDTO;
import org.ruoyi.common.core.service.DictService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.utils.CollUtil;
import org.dromara.warm.flow.core.utils.ExpressionUtil;
import org.dromara.warm.flow.ui.service.NodeExtService;
import org.dromara.warm.flow.ui.vo.NodeExt;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.common.enums.ButtonPermissionEnum;
import org.ruoyi.workflow.common.enums.CopySettingEnum;
import org.ruoyi.workflow.common.enums.NodeExtEnum;
import org.ruoyi.workflow.common.enums.VariablesEnum;
import org.ruoyi.workflow.domain.vo.ButtonPermissionVo;
import org.ruoyi.workflow.domain.vo.NodeExtVo;
import org.ruoyi.workflow.service.IFlwNodeExtService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程设计器-节点扩展属性
 *
 * @author AprilWind
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwNodeExtServiceImpl implements NodeExtService, IFlwNodeExtService {

    /**
     * 存储不同 dictType 对应的配置信息
     */
    private static final Map<String, Map<String, Object>> CHILD_NODE_MAP;

    static {
        CHILD_NODE_MAP = Map.of(
            CopySettingEnum.class.getSimpleName(),
            Map.of(
                "label", "抄送对象",
                "type", 5,
                "must", false,
                "multiple", false,
                "desc", "设置该节点的抄送办理人"
            ),
            VariablesEnum.class.getSimpleName(),
            Map.of(
                "label", "自定义参数",
                "type", 2,
                "must", false,
                "multiple", false,
                "desc", "节点执行时可设置自定义参数，多个参数以逗号分隔，如：key1=value1,key2=value2"
            ),
            ButtonPermissionEnum.class.getSimpleName(),
            Map.of(
                "label", "权限按钮",
                "type", 4,
                "must", false,
                "multiple", true,
                "desc", "控制该节点的按钮权限"
            )
        );
    }

    private final DictService dictService;

    /**
     * 获取节点扩展属性
     *
     * @return 节点扩展属性列表
     */
    @Override
    public List<NodeExt> getNodeExt() {
        List<NodeExt> nodeExtList = new ArrayList<>();
        // 构建基础设置页面
        nodeExtList.add(buildNodeExt("wf_basic_tab", "基础设置", 1,
            List.of(CopySettingEnum.class, VariablesEnum.class)));
        // 构建按钮权限页面
        nodeExtList.add(buildNodeExt("wf_button_tab", "权限", 2,
            List.of(ButtonPermissionEnum.class)));
        // 自定义构建 规则参考 NodeExt 与 warm-flow文档说明
        // nodeExtList.add(buildNodeExt("xxx_xxx", "xxx", 1, List);
        return nodeExtList;
    }

    /**
     * 构建一个 `NodeExt` 对象
     *
     * @param code    唯一编码
     * @param name    名称（新页签时，作为页签名称）
     * @param type    节点类型（1: 基础设置，2: 新页签）
     * @param sources 数据来源（枚举类或字典类型）
     * @return 构建的 `NodeExt` 对象
     */
    @SuppressWarnings("unchecked cast")
    private NodeExt buildNodeExt(String code, String name, int type, List<Object> sources) {
        NodeExt nodeExt = new NodeExt();
        nodeExt.setCode(code);
        nodeExt.setType(type);
        nodeExt.setName(name);
        nodeExt.setChilds(sources.stream()
            .map(source -> {
                if (source instanceof Class<?> clazz && NodeExtEnum.class.isAssignableFrom(clazz)) {
                    return buildChildNode((Class<? extends NodeExtEnum>) clazz);
                } else if (source instanceof String dictType) {
                    return buildChildNode(dictType);
                }
                return null;
            })
            .filter(ObjectUtil::isNotNull)
            .toList()
        );
        return nodeExt;
    }

    /**
     * 根据枚举类型构建一个 `ChildNode` 对象
     *
     * @param enumClass 枚举类，必须实现 `NodeExtEnum` 接口
     * @return 构建的 `ChildNode` 对象
     */
    private NodeExt.ChildNode buildChildNode(Class<? extends NodeExtEnum> enumClass) {
        if (!enumClass.isEnum()) {
            return null;
        }
        String simpleName = enumClass.getSimpleName();
        NodeExt.ChildNode childNode = new NodeExt.ChildNode();
        Map<String, Object> map = CHILD_NODE_MAP.get(simpleName);
        // 编码，此json中唯
        childNode.setCode(simpleName);
        // label名称
        childNode.setLabel(Convert.toStr(map.get("label")));
        // 1：输入框 2：文本域 3：下拉框 4：选择框 5：用户选择器
        childNode.setType(Convert.toInt(map.get("type"), 1));
        // 是否必填
        childNode.setMust(Convert.toBool(map.get("must"), false));
        // 是否多选
        childNode.setMultiple(Convert.toBool(map.get("multiple"), true));
        // 描述
        childNode.setDesc(Convert.toStr(map.get("desc"), null));
        // 字典，下拉框和复选框时用到
        childNode.setDict(Arrays.stream(enumClass.getEnumConstants())
            .map(NodeExtEnum.class::cast)
            .map(x ->
                new NodeExt.DictItem(x.getLabel(), x.getValue(), x.isSelected())
            ).toList());
        return childNode;
    }

    /**
     * 根据字典类型构建 `ChildNode` 对象
     *
     * @param dictType 字典类型
     * @return 构建的 `ChildNode` 对象
     */
    private NodeExt.ChildNode buildChildNode(String dictType) {
        DictTypeDTO dictTypeDTO = dictService.getDictType(dictType);
        if (ObjectUtil.isNull(dictTypeDTO)) {
            return null;
        }
        NodeExt.ChildNode childNode = new NodeExt.ChildNode();
        // 编码，此json中唯一
        childNode.setCode(dictType);
        // label名称
        childNode.setLabel(dictTypeDTO.getDictName());
        // 1：输入框 2：文本域 3：下拉框 4：选择框 5：用户选择器
        childNode.setType(3);
        // 是否必填
        childNode.setMust(false);
        // 是否多选
        childNode.setMultiple(true);
        // 描述 (可根据描述参数解析更多配置，如type，must，multiple等)
        childNode.setDesc(dictTypeDTO.getRemark());
        // 字典，下拉框和复选框时用到
        childNode.setDict(dictService.getDictData(dictType)
            .stream().map(x ->
                new NodeExt.DictItem(x.getDictLabel(), x.getDictValue(), Convert.toBool(x.getIsDefault(), false))
            ).toList());
        return childNode;
    }

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
     * {"code": "CopySettingEnum", "value": "1,3,4,#{@spelRuleComponent.selectDeptLeaderById(#deptId", "#roleId)}"},
     * {"code": "VariablesEnum", "value": "key1=value1,key2=value2"}
     * ]
     *
     * @param ext      扩展属性 JSON 字符串
     * @param variable 流程变量
     * @return NodeExtVo 对象，封装按钮权限列表、抄送对象集合和自定义参数 Map
     */
    @Override
    public NodeExtVo parseNodeExt(String ext, Map<String, Object> variable) {
        NodeExtVo nodeExtVo = new NodeExtVo();

        // 解析 JSON 为 Dict 列表
        List<Dict> nodeExtMap = JsonUtils.parseArrayMap(ext);
        if (ObjectUtil.isEmpty(nodeExtMap)) {
            return nodeExtVo;
        }

        for (Dict nodeExt : nodeExtMap) {
            String code = nodeExt.getStr("code");
            String value = nodeExt.getStr("value");

            if (ButtonPermissionEnum.class.getSimpleName().equals(code)) {
                // 解析按钮权限
                // 将 value 拆分为 Set<String>，便于精确匹配
                Set<String> buttonSet = StringUtils.str2Set(value, StringUtils.SEPARATOR);

                // 获取按钮字典配置
                NodeExt.ChildNode childNode = buildChildNode(ButtonPermissionEnum.class);

                // 构建 ButtonPermissionVo 列表
                List<ButtonPermissionVo> buttonList = Optional.ofNullable(childNode)
                    .map(NodeExt.ChildNode::getDict)
                    .orElse(List.of())
                    .stream()
                    .map(dict -> new ButtonPermissionVo(dict.getValue(), buttonSet.contains(dict.getValue())))
                    .toList();

                nodeExtVo.setButtonPermissions(buttonList);

            } else if (CopySettingEnum.class.getSimpleName().equals(code)) {
                List<String> permissions = spelSmartSplit(value).stream()
                    .map(s -> {
                        List<String> result = ExpressionUtil.evalVariable(s, variable);
                        if (CollUtil.isNotEmpty(result)) {
                            return result;
                        }
                        return Collections.singletonList(s);
                    }).filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .distinct()
                    .collect(Collectors.toList());
                List<String> copySettings = FlowEngine.permissionHandler().convertPermissions(permissions);
                // 解析抄送对象 ID 集合
                nodeExtVo.setCopySettings(new HashSet<>(copySettings));

            } else if (VariablesEnum.class.getSimpleName().equals(code)) {
                // 解析自定义参数
                // 将 key=value 字符串拆分为 Map
                Map<String, String> variables = Arrays.stream(StringUtils.split(value, StringUtils.SEPARATOR))
                    .map(s -> StringUtils.split(s, "="))
                    .filter(arr -> arr.length == 2)
                    .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));

                nodeExtVo.setVariables(variables);
            } else {
                // 未知扩展类型，记录日志
                log.warn("未知扩展类型：code={}, value={}", code, value);
            }
        }
        return nodeExtVo;
    }

    /**
     * 按逗号分割字符串，但保留 #{...} 表达式和字符串常量中的逗号
     */
    private static List<String> spelSmartSplit(String str) {
        List<String> result = new ArrayList<>();
        if (str == null || str.trim().isEmpty()) {
            return result;
        }

        StringBuilder token = new StringBuilder();
        // #{...} 的嵌套深度
        int depth = 0;
        // 是否在字符串常量中（" 或 '）
        boolean inString = false;
        // 当前字符串引号类型
        char stringQuote = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            // 检测进入 SpEL 表达式 #{...}
            if (!inString && c == '#' && depth == 0 && checkNext(str, i, '{')) {
                depth++;
                token.append("#{");
                // 跳过 {
                i++;
                continue;
            }

            // 在表达式中遇到 { 或 } 改变嵌套深度
            if (!inString && depth > 0) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                }
                token.append(c);
                continue;
            }

            // 检测字符串开始/结束
            if (depth > 0 && (c == '"' || c == '\'')) {
                if (!inString) {
                    inString = true;
                    stringQuote = c;
                } else if (stringQuote == c) {
                    inString = false;
                }
                token.append(c);
                continue;
            }

            // 外层逗号才分割
            if (c == ',' && depth == 0 && !inString) {
                String part = token.toString().trim();
                if (!part.isEmpty()) {
                    result.add(part);
                }
                token.setLength(0);
                continue;
            }

            token.append(c);
        }

        // 添加最后一个
        String part = token.toString().trim();
        if (!part.isEmpty()) {
            result.add(part);
        }

        return result;
    }

    private static boolean checkNext(String str, int index, char expected) {
        return index + 1 < str.length() && str.charAt(index + 1) == expected;
    }

}
