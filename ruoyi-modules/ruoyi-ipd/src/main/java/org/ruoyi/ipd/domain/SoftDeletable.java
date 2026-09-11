package org.ruoyi.ipd.domain;

/**
 * 软删除契约（G-02：禁物理 DELETE，仅 UPDATE del_flag）。
 * 任何允许走 DeletionRequestService 审核删除流程的目标实体必须实现本接口；
 * 执行器通过 entity_type 查找并调用，实现与审核引擎解耦。
 *
 * <p>实现规范：del_flag 取值 "0"=正常 / "1"=已删；执行器调用 {@code setDelFlag("1")} 即视为软删除。
 * 实体若使用 Lombok {@code @Accessors(chain = true)}（setter 返回 self），
 * 需要额外写一个 {@code public void setDelFlag(String flag)} 方法显式覆盖 Lombok 生成的链式 setter。
 *
 * <p>各实体 del_flag 列已存在于 v3 schema，无需新增列。
 *
 * @see org.ruoyi.ipd.service.DeleteAuditService
 * @see org.ruoyi.ipd.service.SoftDeleteExecutor
 */
public interface SoftDeletable {
    Long getId();
    String getDelFlag();
    void setDelFlag(String flag);
}