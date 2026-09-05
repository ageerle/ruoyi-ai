package org.ruoyi.ipd.security;

/** 服务端会话身份的最小审计投影，不携带凭据，不从请求绑定。SEC-02 范围过滤依赖 groupId。 */
public record IpdActor(Long id, String name, String role, Long groupId) { }
