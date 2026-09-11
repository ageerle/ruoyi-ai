package org.ruoyi.ipd.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC-HIGH-3（CWE-693）：权限 advice 覆盖面防漂移守卫。
 * <p>背景：IpdPermissionExceptionHandler 曾用 {@code assignableTypes = {...7 个 Controller}} 白名单限定，
 * 漏列 AuditLog / Bid / CoefficientChange / LaunchDateChange / ProductGroup / SystemConfig 6 个 Controller——
 * 这些 Controller 的 @SaCheckPermission 拒绝时 NotPermissionException 落到基线 SaTokenExceptionHandler，
 * 返回 R&lt;&gt; 包络而非 IPD ApiV1Response，前端解析失败白屏。
 * <p>修复：注解改为 {@code basePackages = "org.ruoyi.ipd.controller"}，新增 Controller 自动覆盖。
 * 本测试静态固化两件事，防止未来回漂：
 * <ol>
 *   <li>handler 注解必须含 basePackages="org.ruoyi.ipd.controller" 且不得回退为 assignableTypes</li>
 *   <li>模块内所有 @RestController 类必须落在 org.ruoyi.ipd.controller 包（防有人把 Controller 建到别的包而脱离 advice 覆盖）</li>
 * </ol>
 * <p>设计要点：纯静态源码扫描（findGitRoot + Files.readString），不启动 Spring 上下文；
 * 模式参考同模块 config/ActuatorNarrowTest；@RestController 用词边界正则匹配，
 * 避免误伤 @RestControllerAdvice（IpdPermissionExceptionHandler / IpdServiceExceptionAdvice 本身含该子串）。
 * 全部断言在 stripComments 去注释后的纯代码上进行——javadoc / 行注释里的描述性字面量
 * （如「assignableTypes 改为 basePackages」的历史说明、设计文档里提到的 @RestController）
 * 不得触发守卫（Batch-3 ActuatorNarrowTest 注释字面量误报同类教训）。
 */
@Tag("dev")
@DisplayName("SEC-HIGH-3: 权限 advice basePackages 覆盖防漂移守卫")
class PermissionAdviceCoverageTest {

    /** 自适应定位 git root（向上递归找 .git 目录），兼容 surefire 默认 working dir 与 IDE 单跑 working dir */
    private static final Path REPO_ROOT = findGitRoot(Paths.get(System.getProperty("user.dir")));
    private static final Path HANDLER_SOURCE = REPO_ROOT.resolve(
        "ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/security/IpdPermissionExceptionHandler.java");
    private static final Path MODULE_SOURCE_ROOT = REPO_ROOT.resolve(
        "ruoyi-modules/ruoyi-ipd/src/main/java");
    private static final Path CONTROLLER_PACKAGE_ROOT = REPO_ROOT.resolve(
        "ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/controller");

    /** advice 必须限定的 basePackages */
    private static final String ADVICE_BASE_PACKAGE = "org.ruoyi.ipd.controller";

    /** 词边界匹配 @RestController 注解——不误伤 @RestControllerAdvice（Controller 后跟 Advice 无词边界） */
    private static final Pattern REST_CONTROLLER_ANNOTATION = Pattern.compile("@RestController\\b");

    /** 从源码提取 package 声明 */
    private static final Pattern PACKAGE_DECLARATION =
        Pattern.compile("(?m)^\\s*package\\s+([A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*)\\s*;");

    /** 本卡根因中漏列的 6 个 Controller——固化其必须存在于 controller 包内 */
    private static final List<String> FORMERLY_MISSED_CONTROLLERS = List.of(
        "AuditLogController",
        "BidController",
        "CoefficientChangeController",
        "LaunchDateChangeController",
        "ProductGroupController",
        "SystemConfigController"
    );

    /** 自适应向上找 .git 目录；找不到则返回 cwd */
    private static Path findGitRoot(Path start) {
        Path current = start.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve(".git"))) return current;
            current = current.getParent();
        }
        return start.toAbsolutePath().normalize();
    }

    /** 提取源文件的 package 声明；无声明返回 null */
    private static String extractPackage(String source) {
        Matcher matcher = PACKAGE_DECLARATION.matcher(source);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 去除块注释（含 javadoc）与行注释，返回纯代码文本。
     * 描述性注释里提到被禁/被匹配字面量不应触发守卫——
     * 例如 handler javadoc 记载「assignableTypes 改为 basePackages」的历史、
     * 设计文档里写到「@RestController 均须落在 controller 包」，都不代表代码里真的用了它们。
     */
    private static String stripComments(String source) {
        String noBlockComments = source.replaceAll("(?s)/\\*.*?\\*/", "");
        return noBlockComments.replaceAll("//[^\n]*", "");
    }

    @Test
    @DisplayName("1) advice 注解含 basePackages=org.ruoyi.ipd.controller 且未回退 assignableTypes")
    void adviceAnnotationScopedByBasePackages() throws IOException {
        assertThat(Files.exists(HANDLER_SOURCE))
            .as("handler 源文件存在: " + HANDLER_SOURCE.toAbsolutePath()).isTrue();
        String code = stripComments(Files.readString(HANDLER_SOURCE));

        // 必须用 basePackages 限定到 IPD controller 包（新增 Controller 自动覆盖）
        assertThat(code).contains("@RestControllerAdvice(basePackages = \"" + ADVICE_BASE_PACKAGE + "\")");
        // 禁止回退为 assignableTypes 白名单——白名单正是本卡根因（6 Controller 漏列）。
        // 在去注释后的纯代码上断言：javadoc 里「assignableTypes 改为 basePackages」的历史说明不构成回退
        assertThat(code).doesNotContain("assignableTypes");
        // 优先级必须保持 HIGHEST_PRECEDENCE，先于基线 SaTokenExceptionHandler（返回 R<> 包络）命中
        assertThat(code).contains("@Order(Ordered.HIGHEST_PRECEDENCE)");
        // Sa-Token 注解拒绝的两类异常承接不得被删
        assertThat(code).contains("@ExceptionHandler(NotPermissionException.class)");
        assertThat(code).contains("@ExceptionHandler(NotRoleException.class)");
    }

    @Test
    @DisplayName("2) 模块内所有 @RestController 均落在 advice basePackages 覆盖范围内")
    void everyRestControllerCoveredByAdviceBasePackage() throws IOException {
        assertThat(Files.isDirectory(MODULE_SOURCE_ROOT))
            .as("IPD main 源码目录存在: " + MODULE_SOURCE_ROOT.toAbsolutePath()).isTrue();

        List<Path> controllers;
        try (Stream<Path> paths = Files.walk(MODULE_SOURCE_ROOT)) {
            controllers = paths
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> {
                    try {
                        // 逐行判定（注释行豁免）：javadoc 提到 @RestController 的非 Controller 类（如 advice 自身）不算数
                        return declaresRestController(Files.readString(p));
                    } catch (IOException e) {
                        throw new IllegalStateException("读取失败: " + p, e);
                    }
                })
                .toList();
        }

        assertThat(controllers)
            .as("模块内应至少存在一个 @RestController（当前基线 13 个）").isNotEmpty();

        for (Path controller : controllers) {
            String packageName = extractPackage(Files.readString(controller));
            assertThat(packageName)
                .as("Controller %s 的包名必须以 %s 开头（否则脱离 IpdPermissionExceptionHandler 的 basePackages 覆盖，"
                    + "权限拒绝将 fall through 到基线 SaTokenExceptionHandler 返回 R<> 包络）",
                    controller.getFileName(), ADVICE_BASE_PACKAGE)
                .isNotNull()
                .startsWith(ADVICE_BASE_PACKAGE);
        }
    }

    /**
     * 逐行判定是否真声明了 @RestController：Javadoc（* 开头）与行注释（// 开头）行豁免。
     * <p>自指陷阱防护（R9 根因 #6）：IpdPermissionExceptionHandler 的 Javadoc 描述本守卫时含
     * "所有 @RestController 均落在……"叙述文字，全文正则会把它误判为包外 Controller；
     * 实测 13 个真 Controller 的声明行均为行首裸注解形态，剥离注释行零误伤零漏报。
     */
    private static boolean declaresRestController(String source) {
        for (String line : source.split("\\R")) {
            String stripped = line.strip();
            if (stripped.startsWith("*") || stripped.startsWith("//")) {
                continue;
            }
            if (REST_CONTROLLER_ANNOTATION.matcher(stripped).find()) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("3) 本卡根因漏列的 6 个 Controller 存在于 controller 包内")
    void formerlyMissedControllersLiveInControllerPackage() throws IOException {
        for (String controllerName : FORMERLY_MISSED_CONTROLLERS) {
            Path controllerFile = CONTROLLER_PACKAGE_ROOT.resolve(controllerName + ".java");
            assertThat(Files.exists(controllerFile))
                .as("漏列根因 Controller 存在: " + controllerFile.toAbsolutePath()).isTrue();
            String source = Files.readString(controllerFile);
            assertThat(extractPackage(source))
                .as("%s 必须直接位于 %s 包（须被 basePackages 精确覆盖）", controllerName, ADVICE_BASE_PACKAGE)
                .isEqualTo(ADVICE_BASE_PACKAGE);
            assertThat(source)
                .as("%s 必须使用 @SaCheckPermission 注解鉴权", controllerName)
                .contains("@SaCheckPermission");
        }
    }
}
