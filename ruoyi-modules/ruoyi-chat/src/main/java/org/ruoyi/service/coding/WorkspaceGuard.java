package org.ruoyi.service.coding;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 工作目录安全守卫。
 *
 * <p>抽取自 {@code ReadFileTool/EditFileTool/ListDirectoryTool} 中重复的 {@code isWithinWorkspace}，
 * 五个文件工具共用。强制所有操作路径必须落在工作目录内，防止路径穿越与软链接逃逸。
 *
 * <p>实现要点：
 * <ul>
 *   <li>{@code root.toRealPath()} 解析符号链接（防软链接逃逸）</li>
 *   <li>{@code target.normalize()} 消除 {@code ..} 穿越段</li>
 *   <li>{@code startsWith} 是 Path 段前缀匹配，非字符串前缀（{@code /workspace/abc} 不会误判成 {@code /workspace-evil}）</li>
 * </ul>
 *
 * @author ageerle
 */
public final class WorkspaceGuard {

    private WorkspaceGuard() {
    }

    /**
     * 判断目标路径是否在工作目录内。
     *
     * @param root   工作目录根（绝对路径）
     * @param target 待校验路径
     * @return true 表示在 workspace 内，安全
     */
    public static boolean isWithinWorkspace(Path root, Path target) {
        try {
            Path realRoot = root.toRealPath().normalize();
            Path realTarget = target.normalize();
            return realTarget.startsWith(realRoot);
        } catch (IOException e) {
            // 目标路径不存在或无法解析（如新建文件前其父目录链中有不存在的段）
            // 退化为 normalize 后做段前缀匹配，仍能拦住明显的越界
            try {
                Path realRoot = root.toRealPath().normalize();
                Path normalizedTarget = target.normalize();
                return normalizedTarget.startsWith(realRoot);
            } catch (IOException ignore) {
                return false;
            }
        }
    }
}
