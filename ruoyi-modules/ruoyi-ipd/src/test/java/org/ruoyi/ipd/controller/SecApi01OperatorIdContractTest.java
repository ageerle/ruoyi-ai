package org.ruoyi.ipd.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC-API-01 契约：写接口不得再接受客户端 operatorId 参数。
 */
@Tag("dev")
class SecApi01OperatorIdContractTest {

    private static final List<Class<?>> CONTROLLERS = List.of(
        ProjectController.class,
        ProductController.class,
        StageActionController.class,
        GateElementController.class,
        CertTemplateController.class
    );

    /**
     * 扫描 5 个业务 Controller：任意 @RequestParam 名含 operatorId 即失败。
     */
    @Test
    @DisplayName("SEC-API-01：写接口无 @RequestParam operatorId")
    void noClientOperatorIdParam() {
        List<String> offenders = new ArrayList<>();
        for (Class<?> type : CONTROLLERS) {
            for (Method method : type.getDeclaredMethods()) {
                for (Parameter parameter : method.getParameters()) {
                    RequestParam ann = parameter.getAnnotation(RequestParam.class);
                    if (ann == null) {
                        continue;
                    }
                    String name = ann.name().isEmpty() ? ann.value() : ann.name();
                    if (name.isEmpty()) {
                        name = parameter.getName();
                    }
                    if ("operatorId".equals(name) || name.toLowerCase().contains("operatorid")) {
                        offenders.add(type.getSimpleName() + "#" + method.getName());
                    }
                }
            }
        }
        assertThat(offenders)
            .as("仍接收客户端 operatorId 的方法")
            .isEmpty();
    }
}
