package org.ruoyi.ipd.qa;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态守卫测试公共工具（2026-09-08 AM-GUARD 收口）。
 *
 * <p>设计要点（对齐 {@link org.ruoyi.ipd.security.PermissionAdviceCoverageTest} 金标准）：
 * 全部 IPD 静态守卫测试在裸文本匹配前必须先做注释剔除——javadoc/行注释里出现的形态相近
 * 字面量不应触发守卫。本类把金标准抽出来供 6 处 A 类裸文本守卫（最危险的
 * 下一个被误伤候选）统一调用，避免每处重写且避免规则漂移。
 *
 * <p>使用约束（写入根除建议文档 §四 层 2）：
 * <ol>
 *   <li>禁止对整文件裸 contains/doesNotContain 直接断言——必须先 stripComments 再断言；</li>
 *   <li>写/读语义模糊时（如 record accessor vs builder），用 {@link #stripComments} 后再用
 *       带参正则 `\.xxx\([^)]` 区分"带参调用=写"与"空参 accessor=读"；</li>
 *   <li>yml 守卫优先用 snakeyaml 解析（对齐 {@code ProdHikariConfigTest}），否则用
 *       {@link #stripYamlComments} 剔除 # 注释行后再断言。</li>
 * </ol>
 *
 * @author Qoder 主协调会话（2026-09-08 根除落地轮）
 * @see docs/ipd-系统说明/治理/测试债务根除与守卫加固建议-20260908.md §3.2/§四 层 2
 */
public final class GuardSourceUtils {

    private GuardSourceUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 自适应向上找 .git 目录，找不到则返回 start 的绝对路径。
     * <p>注：金标准的 findGitRoot 是 private static，被 PermissionAdviceCoverageTest
     * 单测独占；本公共版本同样语义，供守卫测试复用。测试从 cwd 起步即可覆盖大多数场景。
     */
    public static Path findGitRoot(Path start) {
        Path current = start.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve(".git"))) {
                return current;
            }
            current = current.getParent();
        }
        return start.toAbsolutePath().normalize();
    }

    /**
     * 去除 Java 源文件的块注释（含 javadoc）与行注释，返回纯代码文本。
     * <p>实现：金标准「块注释先剥，再行注释」两步走：先替换块注释为同长度空白，
     * 再替换行注释为同长度空白，保留换行结构，让后续行号 grep 仍能对齐。
     * 块注释用 lazy 匹配（最小跨度），避免嵌套注释被吞超范围。
     */
    public static String stripComments(String javaSource) {
        if (javaSource == null) {
            return "";
        }
        String noBlockComments = javaSource.replaceAll("(?s)/\\*.*?\\*/", "");
        return noBlockComments.replaceAll("//[^\n]*", "");
    }

    /**
     * 去除 yml 文件的 # 注释行（保留换行结构），返回纯配置文本。
     * <p>对齐 ProdConfigDeltaGuard#5 prodDatasourceCredentialsMustBeEnvPlaceholders 的实现。
     * 注意：仅过滤行首 # 注释，不处理行内 #（如 `key: value  # comment`——这种情况
     * 守卫语义更接近"行尾注释即非配置"，按各守卫实际语义决定是否保留）。
     */
    public static String stripYamlComments(String yamlSource) {
        if (yamlSource == null) {
            return "";
        }
        // trim + 过滤以 # 开头的行（含纯空白+井号）；保留其他行原样
        StringBuilder sb = new StringBuilder();
        for (String line : yamlSource.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#")) {
                sb.append('\n'); // 保留换行
            } else {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * 便捷：从 cwd（默认测试工作目录）找 .git 根。
     */
    public static Path findGitRootFromCwd() {
        return findGitRoot(Paths.get(System.getProperty("user.dir")));
    }
}