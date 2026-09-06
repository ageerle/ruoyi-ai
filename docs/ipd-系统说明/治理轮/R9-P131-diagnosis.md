# P131 回归诊断 · R9 根因补遗

## 现状
- P131AcceptanceTest: 46 tests, 15F + 7E
- 根因：ProjectBootstrapService insert()→insertBatch() 重构，测试断言未同步
- verify(times(6).insert) / verify(times(69).insert) → mock路径不命中

## 生产代码（b25930e6后）
- line 92: projectStageMapper.insertBatch(stageList, STAGE_BATCH_SIZE)
- line 121: stageActionMapper.insertBatch(actionList, ACTION_BATCH_SIZE)

## 关联失败
- P132AcceptanceTest: 3 Errors（同根因）
- P191AcceptanceTest.markHistoricalMissing: 1F（updateById→updateBatchById改造）

## 修复方案
方案A：测试契约化重构（推荐）- 改为数据状态断言
方案B：生产回滚（不推荐）- 80 SQL→3 SQL性能回归

--- R9 2026-09-06