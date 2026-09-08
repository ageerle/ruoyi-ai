-- =====================================================================
-- [数据治理] 真库在任超管收敛至 1 名（owner 裁决 2026-09-07）
-- 看板卡：288e0d6b-3ad6-45a1-bac6-6ae904ca0a4c（外部直投）
-- 依据：P2-7.3 验收 §4-③ 真库存量违反单超管不变式
--       （docs/ipd-系统说明/验收/P2-7.3-超管移交-验收-20260906.md）
--       不变式源：ZK-IPD §九 / AC-HAND-07 / BR-ADM-02/03
-- 裁决：保留 900101 ipd-admin；
--       9110001（傅志谦）、2096897116407382018（系统管理员）置 DISABLED
-- 口径：在任超管 = person_type='SUPER_ADMIN' AND account_status='ACTIVE'
-- 注意：本脚本仅置 account_status、不改 person_type（HandoverService 多名
--       在任防御即按此双口径判定）；旧 token 无需清理——DISABLED 后
--       IpdAuthService.scopeOf 每请求实查库，下一个请求即 401。
-- 幂等：仅命中 ACTIVE 行，重放零副作用；隔离库人工 apply，不做直灌。
-- 防呆：若 apply 时在任超管 ID 与本脚本不一致（种子被重建过），
--       须回报 owner 重新裁决，不得自行扩大命中范围。
-- =====================================================================

-- 前置快照（apply 前人工核对：应恰好 3 行 = 900101 / 9110001 / 2096897116407382018）
select id, name, employee_no, person_type, account_status
from persons
where person_type = 'SUPER_ADMIN' and account_status = 'ACTIVE';

-- 收敛：两名非保留超管置 DISABLED（幂等）
update persons
set account_status = 'DISABLED',
    remark = concat('超管收敛处置20260907：owner裁决保留900101，本账号停用（多名在任超管违反单超管不变式）',
                    case when remark is null or remark = '' then '' else concat('；原remark：', remark) end),
    update_time = now()
where id in (9110001, 2096897116407382018)
  and person_type = 'SUPER_ADMIN'
  and account_status = 'ACTIVE';

-- 回读校验1：在任超管应恰好 1 行且 id=900101
select id, name, employee_no, account_status
from persons
where person_type = 'SUPER_ADMIN' and account_status = 'ACTIVE';

-- 回读校验2：被处置两名应均为 DISABLED 且 remark 已登记处置原因
select id, name, account_status, remark
from persons
where id in (9110001, 2096897116407382018);
