package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CODE-01 Part A：StageActionService 所有写方法的事务注解必须带 rollbackFor = Exception.class。
 *
 * <p>模块约定（CertTemplate/GateElement/Project/IpdAuth/AuditLog 全部如此）：裸 @Transactional
 * 默认仅回滚 RuntimeException；checked 异常（SQLException 包装链等）下深管动作与审计链会出现
 * 部分提交、hash 链断裂。本测试防回归：新增写方法漏写 rollbackFor 直接失败。
 */
@Tag("dev")
class StageActionServiceTransactionAnnotationTest {

    @Test
    @DisplayName("所有 @Transactional 方法必须 rollbackFor 含 Exception.class")
    void allTransactionalMethodsRollbackForException() {
        List<String> violations = new ArrayList<>();
        for (Method m : StageActionService.class.getDeclaredMethods()) {
            Transactional tx = m.getAnnotation(Transactional.class);
            if (tx == null) continue;
            boolean coversException = false;
            for (Class<? extends Throwable> rf : tx.rollbackFor()) {
                if (rf.isAssignableFrom(Exception.class)) { coversException = true; break; }
            }
            if (!coversException) violations.add(m.getName());
        }
        assertThat(violations)
            .as("这些方法缺 rollbackFor = Exception.class: %s", violations)
            .isEmpty();
    }
}
