package org.ruoyi.ipd.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/** P1-3.1：bootstrap 必须加入创建事务并在异常时回滚。 */
@Tag("dev")
class ProjectBootstrapServiceTest {

    @Test
    void bootstrapRequiresExistingTransactionAndRollsBackCheckedExceptions() throws NoSuchMethodException {
        Method method = ProjectBootstrapService.class.getDeclaredMethod("bootstrap", Long.class, Long.class);
        Transactional transaction = method.getAnnotation(Transactional.class);

        assertThat(transaction).isNotNull();
        assertThat(transaction.rollbackFor()).contains(Exception.class);
        assertThat(transaction.propagation()).isEqualTo(Propagation.MANDATORY);
    }
}
