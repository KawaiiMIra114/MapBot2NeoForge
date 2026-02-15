# Step-04 Blockers (Detailed)

## B-01 Role Convergence Missing
- 现状：VIEWER/OPERATOR/ADMIN
- 目标：user/admin/owner
- 风险：权限语义与合同不一致，存在越权/误拒绝风险

## B-02 AUTH-403 Not Unified
- 现状：存在 HTTP 200 + 错误文本拒绝路径
- 目标：统一 HTTP 403 + `errorCode=AUTH-403`
- 风险：调用端误判成功，副作用控制不可审计

## B-03 Config Fail-Closed Missing
- 现状：未知键/非法值走 fallback
- 目标：unknown-key fail-closed + 类型/范围严格校验
- 风险：非法配置潜入生产路径

## B-04 Reload Transaction Missing
- 现状：reload 非事务式，缺回滚闭环证据
- 目标：staging + atomic swap + audit + rollback
- 风险：半提交状态与不可恢复配置漂移
