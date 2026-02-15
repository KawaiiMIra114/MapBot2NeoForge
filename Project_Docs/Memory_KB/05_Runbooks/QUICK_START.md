# Quick Start for New Model

## 1) 读取顺序
- `Project_Docs/Memory_KB/README.md`
- `Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md`
- `Project_Docs/Memory_KB/03_Plan/NEXT_ACTIONS.md`

## 2) 快速核对命令
```bash
cd /mnt/d/axm/mcs/MapBot2NeoForge
ls -1 Project_Docs/Re_Step/Evidence/Step04/20260215T081808Z
sed -n '1,200p' Project_Docs/Re_Step/Evidence/Step04/20260215T081808Z/final_verdict.md
rg -n "VIEWER|OPERATOR|ADMIN|AUTH-403|fail-closed|atomic|rollback|reload" Mapbot-Alpha-V1/src/main/java
```

## 3) 接手后第一动作
- 先修复 `B-01..B-04`，再回填 Step-04 文档与证据，最后重跑 gate。
