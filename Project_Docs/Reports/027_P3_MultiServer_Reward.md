# Task 027 - P3 多服发放离线自动生成 CDK

## 任务目标
- 当玩家全服离线时，自动生成签到奖励 CDK，避免需要手动再执行 `#cdk`。
- 保持在线时将奖励发放到玩家当前在线的所有服务器。

## 变更文件
- `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/impl/AcceptCommand.java`
- `Project_Docs/Reports/027_P3_MultiServer_Reward.md`

## 关键实现
- 在 `#accept` 领取逻辑中，当多服查询结果为全服离线时，直接生成 CDK 并返回兑换码与使用提示。
- 若 Redis 未启用导致 CDK 生成失败，则提示稍后使用 `#cdk` 处理。

## 测试验证
- 未执行（逻辑分支调整，无自动化测试）。

## Git 提交记录
- 9553e03 feat: generate cdk when offline accept
