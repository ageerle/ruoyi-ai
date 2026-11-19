package org.ruoyi.chat.service.coding;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * 工作目录安全守卫。
 *
 * <p>抽取自 {@code ReadFileTool/EditFileTool/ListDirectoryTool} 中重复的 {@code isWithinWorkspace}，
 * 五个文件工具共用。强制所有操作路径必须落在工作目录内，防止路径穿越与软链接逃逸。
 *
 * <p>实现要点：
 * <ul>
 *   <li>词法范围检查拦截外部绝对路径和 {@code ..} 穿越</li>
 *   <li>现存目标直接校验其真实路径</li>
 *   <li>待创建目标校验最近的现存父链，拦截软链接、junction 或 reparse point 逃逸</li>
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
        if (root == null || target == null) {
            return false;
        }

        try {
            Path normalizedRoot = root.toAbsolutePath().normalize();
            Path normalizedTarget = target.isAbsolute()
                ? target.toAbsolutePath().normalize()
                : normalizedRoot.resolve(target).normalize();

            // 先用调用方看到的 workspace 路径做词法边界检查。
            if (!normalizedTarget.startsWith(normalizedRoot)) {
                return false;
            }

            Path realRoot = normalizedRoot.toRealPath().normalize();
            Path nearestExisting = nearestExistingAncestor(normalizedTarget);
            Path realExisting = nearestExisting.toRealPath().normalize();
            return realExisting.startsWith(realRoot);
        } catch (IOException | SecurityException e) {
            // 无法确定真实路径时必须 fail closed，不再退化为纯字符串判断。
            return false;
        }
    }

    /**
     * 查找目标或其父链中最近的现存路径。
     *
     * <p>{@link LinkOption#NOFOLLOW_LINKS} 可识别悬空软链接；随后的
     * {@code toRealPath()} 会解析软链接以及 Windows junction/reparse point。
     */
    private static Path nearestExistingAncestor(Path target) throws IOException {
        Path current = target;
        while (current != null) {
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                return current;
            }
            current = current.getParent();
        }
        throw new IOException("Target has no existing ancestor: " + target);
    }
}
