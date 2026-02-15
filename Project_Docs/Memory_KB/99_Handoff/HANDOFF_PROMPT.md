# Handoff Prompt (Copy-Paste)

你现在接管 MapBot2NeoForge 的 Step-04（B2）任务。
请先阅读：
- Project_Docs/Memory_KB/README.md
- Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md
- Project_Docs/Memory_KB/02_Status/BLOCKERS_STEP04.md
- Project_Docs/Memory_KB/03_Plan/NEXT_ACTIONS.md

当前权威状态：Step-04 FAIL，NO-GO B3。
必须优先修复四个阻断项：
1) 角色收敛 user/admin/owner
2) 越权统一 AUTH-403
3) alpha.properties unknown-key fail-closed
4) reload 事务闭环（parse/validate/staging/atomic/audit/rollback）

修复完成后：
- 回填 Project_Docs/Re_Step/Artifacts/Step04/01..06
- 生成 Project_Docs/Re_Step/Evidence/Step04/{RUN_ID}/gate*.{log,exit} 与 final_verdict.md
- 输出 Verdict、Blocking Issues、Fix Actions、是否 GO B3。
