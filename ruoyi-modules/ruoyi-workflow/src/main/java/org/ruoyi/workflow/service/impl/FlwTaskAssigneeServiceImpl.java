package org.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.dto.DeptDTO;
import org.ruoyi.common.core.domain.dto.TaskAssigneeDTO;
import org.ruoyi.common.core.domain.dto.UserDTO;
import org.ruoyi.common.core.domain.model.TaskAssigneeBody;
import org.ruoyi.common.core.enums.FormatsType;
import org.ruoyi.common.core.service.*;
import org.ruoyi.common.core.utils.DateUtils;
import org.ruoyi.common.core.utils.StreamUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.dromara.warm.flow.ui.dto.HandlerFunDto;
import org.dromara.warm.flow.ui.dto.HandlerQuery;
import org.dromara.warm.flow.ui.dto.TreeFunDto;
import org.dromara.warm.flow.ui.service.HandlerSelectService;
import org.dromara.warm.flow.ui.vo.HandlerFeedBackVo;
import org.dromara.warm.flow.ui.vo.HandlerSelectVo;
import org.ruoyi.workflow.common.ConditionalOnEnable;
import org.ruoyi.workflow.common.enums.TaskAssigneeEnum;
import org.ruoyi.workflow.service.IFlwSpelService;
import org.ruoyi.workflow.service.IFlwTaskAssigneeService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程设计器-获取办理人权限设置列表
 *
 * @author AprilWind
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwTaskAssigneeServiceImpl implements IFlwTaskAssigneeService, HandlerSelectService {

    private static final String DEFAULT_GROUP_NAME = "默认分组";
    private final TaskAssigneeService taskAssigneeService;
    private final UserService userService;
    private final DeptService deptService;
    private final RoleService roleService;
    private final PostService postService;
    private final IFlwSpelService spelService;

    /**
     * 获取办理人权限设置列表tabs页签
     *
     * @return tabs页签
     */
    @Override
    public List<String> getHandlerType() {
        return TaskAssigneeEnum.getAssigneeTypeList();
    }

    /**
     * 获取办理列表, 同时构建左侧部门树状结构
     *
     * @param query 查询条件
     * @return HandlerSelectVo
     */
    @Override
    public HandlerSelectVo getHandlerSelect(HandlerQuery query) {
        // 获取任务办理类型
        TaskAssigneeEnum type = TaskAssigneeEnum.fromDesc(query.getHandlerType());
        // 转换查询条件为 TaskAssigneeBody
        TaskAssigneeBody taskQuery = BeanUtil.toBean(query, TaskAssigneeBody.class);

        // 统一查询并构建业务数据
        TaskAssigneeDTO dto = fetchTaskAssigneeData(type, taskQuery);
        List<DeptDTO> depts = fetchDeptData(type);

        return getHandlerSelectVo(buildHandlerData(dto, type), buildDeptTree(depts));
    }

    /**
     * 办理人权限名称回显
     *
     * @param storageIds 入库主键集合
     * @return 结果
     */
    @Override
    public List<HandlerFeedBackVo> handlerFeedback(List<String> storageIds) {
        if (CollUtil.isEmpty(storageIds)) {
            return Collections.emptyList();
        }
        // 解析并归类 ID，同时记录原始顺序和对应解析结果
        Map<TaskAssigneeEnum, List<String>> typeIdMap = new EnumMap<>(TaskAssigneeEnum.class);
        Map<String, Pair<TaskAssigneeEnum, String>> parsedMap = new LinkedHashMap<>();
        for (String storageId : storageIds) {
            Pair<TaskAssigneeEnum, String> parsed = this.parseStorageId(storageId);
            parsedMap.put(storageId, parsed);
            if (parsed != null) {
                typeIdMap.computeIfAbsent(parsed.getKey(), k -> new ArrayList<>()).add(parsed.getValue());
            }
        }

        // 查询所有类型对应的 ID 名称映射
        Map<TaskAssigneeEnum, Map<String, String>> nameMap = new EnumMap<>(TaskAssigneeEnum.class);
        typeIdMap.forEach((type, ids) -> nameMap.put(type, this.getNamesByType(type, ids)));
        // 组装返回结果，保持原始顺序
        return parsedMap.entrySet().stream()
            .map(entry -> {
                String storageId = entry.getKey();
                Pair<TaskAssigneeEnum, String> parsed = entry.getValue();
                String handlerName = (parsed == null) ? null
                    : nameMap.getOrDefault(parsed.getKey(), Collections.emptyMap())
                    .get(parsed.getValue());
                return new HandlerFeedBackVo(storageId, handlerName);
            }).toList();
    }

    /**
     * 根据任务办理类型查询对应的数据
     */
    private TaskAssigneeDTO fetchTaskAssigneeData(TaskAssigneeEnum type, TaskAssigneeBody taskQuery) {
        return switch (type) {
            case USER -> taskAssigneeService.selectUsersByTaskAssigneeList(taskQuery);
            case ROLE -> taskAssigneeService.selectRolesByTaskAssigneeList(taskQuery);
            case DEPT -> taskAssigneeService.selectDeptsByTaskAssigneeList(taskQuery);
            case POST -> taskAssigneeService.selectPostsByTaskAssigneeList(taskQuery);
            case SPEL -> spelService.selectSpelByTaskAssigneeList(taskQuery);
        };
    }

    /**
     * 根据任务办理类型获取部门数据
     */
    private List<DeptDTO> fetchDeptData(TaskAssigneeEnum type) {
        if (type.needsDeptService()) {
            return deptService.selectDeptsByList();
        }
        return new ArrayList<>();
    }

    /**
     * 获取权限分组名称
     *
     * @param type      任务分配人枚举
     * @param groupName 权限分组
     * @return 权限分组名称
     */
    private String getGroupName(TaskAssigneeEnum type, String groupName) {
        if (StringUtils.isEmpty(groupName)) {
            return DEFAULT_GROUP_NAME;
        }
        if (type.needsDeptService()) {
            return deptService.selectDeptNameByIds(groupName);
        }
        return DEFAULT_GROUP_NAME;
    }

    /**
     * 构建部门树状结构
     */
    private TreeFunDto<DeptDTO> buildDeptTree(List<DeptDTO> depts) {
        return new TreeFunDto<>(depts)
            .setId(dept -> Convert.toStr(dept.getDeptId()))
            .setName(DeptDTO::getDeptName)
            .setParentId(dept -> Convert.toStr(dept.getParentId()));
    }

    /**
     * 构建任务办理人数据
     */
    private HandlerFunDto<TaskAssigneeDTO.TaskHandler> buildHandlerData(TaskAssigneeDTO dto, TaskAssigneeEnum type) {
        return new HandlerFunDto<>(dto.getList(), dto.getTotal())
            .setStorageId(assignee -> type.getCode() + assignee.getStorageId())
            .setHandlerCode(assignee -> StringUtils.blankToDefault(assignee.getHandlerCode(), "无"))
            .setHandlerName(assignee -> StringUtils.blankToDefault(assignee.getHandlerName(), "无"))
            .setGroupName(assignee -> this.getGroupName(type, assignee.getGroupName()))
            .setCreateTime(assignee -> DateUtils.parseDateToStr(FormatsType.YYYY_MM_DD_HH_MM_SS, assignee.getCreateTime()));
    }

    /**
     * 批量解析多个存储标识符（storageIds），按类型分类并合并查询用户列表
     * 输入格式支持多个以逗号分隔的标识（如 "user:123,role:456,789"）
     * 会自动去重返回结果，非法格式的标识将被忽略
     *
     * @param storageIds 多个存储标识符字符串（逗号分隔）
     * @return 合并后的用户列表，去重后返回，非法格式的标识将被跳过
     */
    @Override
    public List<UserDTO> fetchUsersByStorageIds(String storageIds) {
        if (StringUtils.isEmpty(storageIds)) {
            return List.of();
        }
        Map<TaskAssigneeEnum, List<String>> typeIdMap = new EnumMap<>(TaskAssigneeEnum.class);
        for (String storageId : storageIds.split(StringUtils.SEPARATOR)) {
            Pair<TaskAssigneeEnum, String> parsed = this.parseStorageId(storageId);
            if (parsed != null) {
                typeIdMap.computeIfAbsent(parsed.getKey(), k -> new ArrayList<>()).add(parsed.getValue());
            }
        }
        return typeIdMap.entrySet().stream()
            .flatMap(entry -> this.getUsersByType(entry.getKey(), entry.getValue()).stream())
            .distinct()
            .toList();
    }

    /**
     * 根据指定的任务分配类型（TaskAssigneeEnum）和 ID 列表，获取对应的用户信息列表
     *
     * @param type 任务分配类型，表示用户、角色、部门或其他（TaskAssigneeEnum 枚举值）
     * @param ids  与指定分配类型关联的 ID 列表（例如用户ID、角色ID、部门ID等）
     * @return 返回包含用户信息的列表。如果类型为用户（USER），则通过用户ID列表查询；
     * 如果类型为角色（ROLE），则通过角色ID列表查询；
     * 如果类型为部门（DEPT），则通过部门ID列表查询；
     * 如果类型为岗位（POST）或无法识别的类型，则返回空列表
     */
    private List<UserDTO> getUsersByType(TaskAssigneeEnum type, List<String> ids) {
        if (type == TaskAssigneeEnum.SPEL) {
            return new ArrayList<>();
        }
        List<Long> longIds = StreamUtils.toList(ids, Convert::toLong);
        return switch (type) {
            case USER -> userService.selectListByIds(longIds);
            case ROLE -> userService.selectUsersByRoleIds(longIds);
            case DEPT -> userService.selectUsersByDeptIds(longIds);
            case POST -> userService.selectUsersByPostIds(longIds);
            default -> new ArrayList<>();
        };
    }

    /**
     * 根据任务分配类型和对应 ID 列表，批量查询名称映射关系
     *
     * @param type 分配类型（用户、角色、部门、岗位）
     * @param ids  ID 列表（如用户ID、角色ID等）
     * @return 返回 Map，其中 key 为 ID，value 为对应的名称
     */
    private Map<String, String> getNamesByType(TaskAssigneeEnum type, List<String> ids) {
        if (type == TaskAssigneeEnum.SPEL) {
            return spelService.selectRemarksBySpels(ids);
        }

        List<Long> longIds = StreamUtils.toList(ids, Convert::toLong);
        Map<Long, String> rawMap = switch (type) {
            case USER -> userService.selectUserNamesByIds(longIds);
            case ROLE -> roleService.selectRoleNamesByIds(longIds);
            case DEPT -> deptService.selectDeptNamesByIds(longIds);
            case POST -> postService.selectPostNamesByIds(longIds);
            default -> Collections.emptyMap();
        };
        if (MapUtil.isEmpty(rawMap)) {
            return Collections.emptyMap();
        }
        return rawMap.entrySet()
            .stream()
            .collect(Collectors.toMap(
                e -> Convert.toStr(e.getKey()),
                Map.Entry::getValue
            ));
    }

    /**
     * 解析 storageId 字符串，返回类型和ID的组合
     *
     * @param storageId 例如 "user:123" 或 "456"
     * @return Pair(TaskAssigneeEnum, Long)，如果格式非法返回 null
     */
    private Pair<TaskAssigneeEnum, String> parseStorageId(String storageId) {
        if (StringUtils.isBlank(storageId)) {
            return null;
        }
        if (TaskAssigneeEnum.isSpelExpression(storageId)) {
            return Pair.of(TaskAssigneeEnum.SPEL, storageId);
        }
        try {
            String[] parts = storageId.split(StrUtil.COLON, 2);
            if (parts.length < 2) {
                return Pair.of(TaskAssigneeEnum.USER, parts[0]);
            } else {
                TaskAssigneeEnum type = TaskAssigneeEnum.fromCode(parts[0] + StrUtil.COLON);
                return Pair.of(type, parts[1]);
            }
        } catch (Exception e) {
            log.warn("解析 storageId 失败，格式非法：{}，错误信息：{}", storageId, e.getMessage());
            return null;
        }
    }

}
