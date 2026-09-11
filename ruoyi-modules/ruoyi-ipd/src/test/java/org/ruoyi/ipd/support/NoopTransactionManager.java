package org.ruoyi.ipd.support;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * 单测用空事务管理器：让 TransactionTemplate 同步执行回调，不连真实 DataSource。
 */
public final class NoopTransactionManager implements PlatformTransactionManager {

    /** 共享实例，无状态可复用。 */
    public static final NoopTransactionManager INSTANCE = new NoopTransactionManager();

    private NoopTransactionManager() {
    }

    /**
     * 开启空事务状态。
     *
     * @param definition 忽略
     * @return 简单事务状态
     */
    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
        return new SimpleTransactionStatus();
    }

    /**
     * 空提交。
     *
     * @param status 忽略
     */
    @Override
    public void commit(TransactionStatus status) {
    }

    /**
     * 空回滚。
     *
     * @param status 忽略
     */
    @Override
    public void rollback(TransactionStatus status) {
    }
}
