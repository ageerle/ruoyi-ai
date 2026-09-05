package org.ruoyi.ipd.service;

/**
 * 单实体类型的软删除执行器（每个 entity_type 一个 Spring bean）。
 * DeleteAuditService 通过 entityType() 查找并调用 softDelete(id)，
 * 在审核通过后事务内完成 UPDATE del_flag='1'。
 *
 * <p>实现必须是 idempotent：del_flag 已为 "1" 时直接视为已删，
 * 不得因重复触发造成异常或写入被审计误报。
 *
 * @param <T> 目标实体类型（必须实现 {@link org.ruoyi.ipd.domain.SoftDeletable}）
 */
public interface SoftDeleteExecutor<T> {

    /** DeletionRequest.entityType 的取值（小写、snake_case 形式） */
    String entityType();

    /** 目标实体类型，供 DeleteAuditService 做类型校验 */
    Class<T> entityClass();

    /**
     * 软删除目标行：UPDATE del_flag='1'。
     * 实体不存在或 del_flag 已为 "1" 时不得抛业务异常（幂等）。
     */
    void softDelete(Long id);
}