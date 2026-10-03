-- PC 管理后台和短剧工作台：无操作 12 小时后登录失效（单位：秒）。
-- 仅迁移原 30 分钟策略，保留已自定义的客户端设置。
UPDATE sys_client
SET active_timeout = 43200
WHERE device_type = 'pc' AND active_timeout = 1800;
-- 已部署环境通过 /system/client 更新相同字段以同步清理客户端缓存。
-- 已签发的令牌需重新登录后使用新策略。
