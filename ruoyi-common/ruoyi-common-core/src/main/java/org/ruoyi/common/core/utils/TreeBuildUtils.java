package org.ruoyi.common.core.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.lang.tree.parser.NodeParser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.ruoyi.common.core.utils.reflect.ReflectUtils;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 扩展 hutool TreeUtil 封装系统树构建
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TreeBuildUtils extends TreeUtil {

    /**
     * 根据前端定制差异化字段
     */
    public static final TreeNodeConfig DEFAULT_CONFIG = TreeNodeConfig.DEFAULT_CONFIG.setNameKey("label");

    /**
     * 构建树形结构
     *
     * @param <T>        输入节点的类型
     * @param <K>        节点ID的类型
     * @param list       节点列表，其中包含了要构建树形结构的所有节点
     * @param nodeParser 解析器，用于将输入节点转换为树节点
     * @return 构建好的树形结构列表
     */
    public static <T, K> List<Tree<K>> build(List<T> list, NodeParser<T, K> nodeParser) {
        if (CollUtil.isEmpty(list)) {
            return CollUtil.newArrayList();
        }
        K k = ReflectUtils.invokeGetter(list.get(0), "parentId");
        return TreeUtil.build(list, k, DEFAULT_CONFIG, nodeParser);
    }

    /**
     * 构建树形结构
     *
     * @param <T>        输入节点的类型
     * @param <K>        节点ID的类型
     * @param parentId   顶级节点
     * @param list       节点列表，其中包含了要构建树形结构的所有节点
     * @param nodeParser 解析器，用于将输入节点转换为树节点
     * @return 构建好的树形结构列表
     */
    public static <T, K> List<Tree<K>> build(List<T> list, K parentId, NodeParser<T, K> nodeParser) {
        if (CollUtil.isEmpty(list)) {
            return CollUtil.newArrayList();
        }
        return TreeUtil.build(list, parentId, DEFAULT_CONFIG, nodeParser);
    }

    /**
     * 构建多根节点的树结构（支持多个顶级节点）
     *
     * @param list        原始数据列表
     * @param getId       获取节点 ID 的方法引用，例如：node -> node.getId()
     * @param getParentId 获取节点父级 ID 的方法引用，例如：node -> node.getParentId()
     * @param parser      树节点属性映射器，用于将原始节点 T 转为 Tree 节点
     * @param <T>         原始数据类型（如实体类、DTO 等）
     * @param <K>         节点 ID 类型（如 Long、String）
     * @return 构建完成的树形结构（可能包含多个顶级根节点）
     */
    public static <T, K> List<Tree<K>> buildMultiRoot(List<T> list, Function<T, K> getId, Function<T, K> getParentId, NodeParser<T, K> parser) {
        if (CollUtil.isEmpty(list)) {
            return CollUtil.newArrayList();
        }

        Set<K> rootParentIds = StreamUtils.toSet(list, getParentId);
        rootParentIds.removeAll(StreamUtils.toSet(list, getId));

        // 构建每一个根 parentId 下的树，并合并成最终结果列表
        return rootParentIds.stream()
            .flatMap(rootParentId -> TreeUtil.build(list, rootParentId, parser).stream())
            .collect(Collectors.toList());
    }

    /**
     * 获取节点列表中所有节点的叶子节点
     *
     * @param <K>   节点ID的类型
     * @param nodes 节点列表
     * @return 包含所有叶子节点的列表
     */
    public static <K> List<Tree<K>> getLeafNodes(List<Tree<K>> nodes) {
        if (CollUtil.isEmpty(nodes)) {
            return CollUtil.newArrayList();
        }
        return nodes.stream()
            .flatMap(TreeBuildUtils::extractLeafNodes)
            .collect(Collectors.toList());
    }

    /**
     * 获取指定节点下的所有叶子节点
     *
     * @param <K>  节点ID的类型
     * @param node 要查找叶子节点的根节点
     * @return 包含所有叶子节点的列表
     */
    private static <K> Stream<Tree<K>> extractLeafNodes(Tree<K> node) {
        if (!node.hasChild()) {
            return Stream.of(node);
        } else {
            // 递归调用，获取所有子节点的叶子节点
            return node.getChildren().stream()
                .flatMap(TreeBuildUtils::extractLeafNodes);
        }
    }

}
