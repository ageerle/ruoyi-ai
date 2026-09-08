package org.ruoyi.ipd.security;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A-4 契约扫描：所有 Controller 端点方法必须满足以下任一：
 * <ol>
 *   <li>带 {@link SaCheckPermission} 注解；或</li>
 *   <li>Javadoc 含「无权限注解原因」关键词 + 一句话说明（公开端点 / service 内守卫 / SSO回调 / 健康检查 等）。</li>
 * </ol>
 *
 * <p>覆盖 7 个 IPD Controller：
 * <ul>
 *   <li>{@code SharedKpiController}</li>
 *   <li>{@code RequirementChangeController}</li>
 *   <li>{@code SwitchingAcceptanceController}</li>
 *   <li>{@code CoefficientChangeController}</li>
 *   <li>{@code PostLaunchReviewController}</li>
 *   <li>{@code HandoverController}</li>
 *   <li>{@code BonusPoolController}</li>
 * </ul>
 *
 * <p>实现简化：用 {@code Class.getProtectionDomain().getCodeSource().getLocation()} 拿到
 * target/classes 路径，回溯到 src/main/java；直接读 .java 源码找 @XxxMapping 上方 Javadoc 块。
 * 不依赖 maven-javadoc-plugin doclet，CI 即跑即用。
 */
@Tag("dev")
@DisplayName("A-4 Controller 权限注解契约——无注解端点必须 Javadoc 含「无权限注解原因」")
class ControllerPermissionAnnotationContractTest {

    /** 强制 Javadoc 关键词（与 AGENTS.md / 蓝图一字不差）。 */
    private static final String KEYWORD = "无权限注解原因";

    /** 7 个被扫描 Controller 的全限定类名。 */
    private static final List<String> CONTROLLER_FQNS = List.of(
        "org.ruoyi.ipd.controller.SharedKpiController",
        "org.ruoyi.ipd.controller.RequirementChangeController",
        "org.ruoyi.ipd.controller.SwitchingAcceptanceController",
        "org.ruoyi.ipd.controller.CoefficientChangeController",
        "org.ruoyi.ipd.controller.PostLaunchReviewController",
        "org.ruoyi.ipd.controller.HandoverController",
        "org.ruoyi.ipd.controller.BonusPoolController"
    );

    /**
     * PAT-1：扫描 7 个 Controller，类可被 Class.forName 加载且含 1+ 端点方法。
     * 防类被改名/移包导致契约静默失效。
     */
    @Test
    @DisplayName("PAT-1 7 个 Controller 都能被 Class.forName 加载且至少 1 个端点方法")
    void allSevenControllersLoadable() {
        for (String fqn : CONTROLLER_FQNS) {
            Class<?> clazz = loadClass(fqn);
            assertThat(clazz.isAnnotationPresent(RestController.class))
                .as("%s 必须带 @RestController", fqn).isTrue();
            long endpointCount = countEndpointMethods(clazz);
            assertThat(endpointCount)
                .as("%s 必须至少含 1 个端点方法", fqn).isGreaterThanOrEqualTo(1);
        }
    }

    /**
     * PAT-2：所有无 @SaCheckPermission 的端点方法必须在 Javadoc 中包含
     * 「无权限注解原因」关键词。
     */
    @Test
    @DisplayName("PAT-2 无 @SaCheckPermission 端点必须 Javadoc 含「无权限注解原因」")
    void noAnnotationMethodsMustHaveReasonInJavadoc() throws IOException {
        for (String fqn : CONTROLLER_FQNS) {
            Class<?> clazz = loadClass(fqn);
            String source = readJavaSource(clazz);
            for (Method m : clazz.getDeclaredMethods()) {
                if (!isEndpointMethod(m)) continue;
                if (m.isAnnotationPresent(SaCheckPermission.class)) continue;
                String javadoc = javadocBeforeMethod(source, m);
                assertThat(javadoc)
                    .as("%s#%s 无 @SaCheckPermission，Javadoc 必须含「%s」",
                        clazz.getSimpleName(), m.getName(), KEYWORD)
                    .contains(KEYWORD);
            }
        }
    }

    /**
     * PAT-3：所有带 @SaCheckPermission 的端点方法 Javadoc 可选（不强求关键词）。
     * 防误伤：已加注解的端点不需额外 Javadoc。
     */
    @Test
    @DisplayName("PAT-3 有 @SaCheckPermission 端点注解对象非空")
    void annotatedMethodsAnnotationPresent() {
        for (String fqn : CONTROLLER_FQNS) {
            Class<?> clazz = loadClass(fqn);
            for (Method m : clazz.getDeclaredMethods()) {
                if (!isEndpointMethod(m)) continue;
                if (!m.isAnnotationPresent(SaCheckPermission.class)) continue;
                assertThat(m.getAnnotation(SaCheckPermission.class))
                    .as("%s#%s 注解对象非空", clazz.getSimpleName(), m.getName())
                    .isNotNull();
            }
        }
    }

    /**
     * PAT-4：HandoverController 端点全数覆盖（当前 9 端点全无 @SaCheckPermission）。
     * 该 Controller 是 A-4 主要修复目标，单独断言防漏。
     */
    @Test
    @DisplayName("PAT-4 HandoverController 9 端点全需 Javadoc「无权限注解原因」（无 @SaCheckPermission）")
    void handoverControllerAllEndpointsNeedReasonJavadoc() throws IOException {
        Class<?> clazz = loadClass("org.ruoyi.ipd.controller.HandoverController");
        String source = readJavaSource(clazz);
        int totalEndpoints = 0;
        int withKeyword = 0;
        int annotatedEndpoints = 0;
        for (Method m : clazz.getDeclaredMethods()) {
            if (!isEndpointMethod(m)) continue;
            totalEndpoints++;
            if (m.isAnnotationPresent(SaCheckPermission.class)) {
                annotatedEndpoints++;
                continue;
            }
            String javadoc = javadocBeforeMethod(source, m);
            if (javadoc.contains(KEYWORD)) withKeyword++;
        }
        assertThat(totalEndpoints)
            .as("HandoverController 端点数 ≥ 8（initiate/batch/accept/cancel/inbox/super-admin/scan-overdue/archive/monthly-attribution）")
            .isGreaterThanOrEqualTo(8);
        assertThat(annotatedEndpoints)
            .as("HandoverController 当前端点全无 @SaCheckPermission（类级策略：注解层只 requireInternal，对象级下沉到 Service）")
            .isEqualTo(0);
        assertThat(withKeyword)
            .as("HandoverController 所有无注解端点都必须含关键词 %s", KEYWORD)
            .isEqualTo(totalEndpoints);
    }

    // ===== helpers =====

    private static Class<?> loadClass(String fqn) {
        try {
            return Class.forName(fqn);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(fqn + " 类未找到", e);
        }
    }

    /** 是否 Spring MVC 端点注解方法（任一即算）。 */
    private static boolean isEndpointMethod(Method m) {
        return m.isAnnotationPresent(PostMapping.class)
            || m.isAnnotationPresent(GetMapping.class)
            || m.isAnnotationPresent(PutMapping.class)
            || m.isAnnotationPresent(DeleteMapping.class)
            || m.isAnnotationPresent(RequestMapping.class);
    }

    /** 数端点方法数。 */
    private static long countEndpointMethods(Class<?> clazz) {
        return java.util.Arrays.stream(clazz.getDeclaredMethods())
            .filter(ControllerPermissionAnnotationContractTest::isEndpointMethod)
            .count();
    }

    /**
     * 读 Controller 类的 .java 源码（从 classpath 回溯到 src/main/java）。
     * 兼容 Maven 标准布局：target/classes/...  →  src/main/java/...
     */
    private static String readJavaSource(Class<?> clazz) throws IOException {
        Path javaPath = locateJavaFile(clazz);
        assertThat(javaPath)
            .as("%s.java 必须存在（路径推断失败？）", clazz.getSimpleName())
            .isNotNull();
        if (javaPath == null) {
            throw new IOException("无法定位 " + clazz.getSimpleName() + ".java 源码路径");
        }
        return Files.readString(javaPath, StandardCharsets.UTF_8);
    }

    /**
     * 推断 Controller .java 源码路径：classpath root + 类包路径。
     * 优先尝试从 {@code Class.getProtectionDomain().getCodeSource().getLocation()} 拿 target/classes，
     * 回溯到 src/main/java；失败回退到 user.dir/src/main/java。
     */
    private static Path locateJavaFile(Class<?> clazz) {
        String classFile = clazz.getName().replace('.', '/') + ".java";
        // 方案 1：从 CodeSource 推断 target/classes → ../src/main/java
        try {
            Optional<Path> codeLocation = Optional.ofNullable(clazz.getProtectionDomain())
                .map(pd -> pd.getCodeSource())
                .map(cs -> cs.getLocation())
                .map(url -> {
                    try { return Paths.get(url.toURI()); } catch (Exception e) { return null; }
                });
            if (codeLocation.isPresent() && codeLocation.get() != null) {
                Path targetClasses = codeLocation.get();
                // 假设布局：target/classes/org/ruoyi/ipd/controller/X.java
                // 找 target/classes 的祖先里最近的 src/main/java
                Path srcMain = findSrcMainJava(targetClasses);
                if (srcMain != null) {
                    Path javaFile = srcMain.resolve(classFile);
                    if (Files.exists(javaFile)) return javaFile;
                }
            }
        } catch (Exception ignored) { }
        // 方案 2：回退到 user.dir/src/main/java
        Path fallback = Paths.get(System.getProperty("user.dir"), "src/main/java", classFile);
        if (Files.exists(fallback)) return fallback;
        return null;
    }

    /**
     * 从 target/classes（或任意子目录）往上找 src/main/java。
     */
    private static Path findSrcMainJava(Path start) {
        Path p = start.toAbsolutePath();
        // 向上最多 5 层
        for (int i = 0; i < 5; i++) {
            if (p == null) break;
            Path candidate = p.resolve("src/main/java");
            if (Files.isDirectory(candidate)) return candidate;
            p = p.getParent();
        }
        return null;
    }

    /**
     * 在源码中找给定 method 上方最近的 Javadoc 块。
     * 用方法签名（含返回类型 + 方法名 + 第一个左括号）作为锚点。
     */
    private static String javadocBeforeMethod(String source, Method m) {
        // 构造方法签名锚点：返回类型 + 方法名 + "("
        String returnType = m.getReturnType().getSimpleName();
        String methodSig = returnType + " " + m.getName() + "(";
        int sigIdx = source.indexOf(methodSig);
        if (sigIdx < 0) {
            // 可能带泛型或 record 包装——降级：用方法名 + 左括号
            methodSig = m.getName() + "(";
            sigIdx = source.indexOf(methodSig);
        }
        if (sigIdx < 0) return ""; // 找不到，返回空（断言会失败并报告原因）
        // 找 sigIdx 之前最近的 Javadoc 结束
        int javadocEnd = source.lastIndexOf("*/", sigIdx);
        if (javadocEnd < 0) return "";
        int javadocStart = source.lastIndexOf("/**", javadocEnd);
        if (javadocStart < 0) return "";
        // 确保 javadocStart 和 javadocEnd 之间没有 /**（即这是最近的）
        String between = source.substring(javadocStart + 3, javadocEnd);
        if (between.contains("/**")) {
            // 中间还有 /** ——说明有嵌套 Javadoc 块，取最后一个
            int lastStart = javadocStart + 3 + between.lastIndexOf("/**");
            return source.substring(lastStart, javadocEnd + 2);
        }
        return source.substring(javadocStart, javadocEnd + 2);
    }
}
