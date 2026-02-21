# 阶段一·任务02：Reforged 端 DataManager 引擎换血

**接收智能体**: `Atlas` (Reforged 端开发负责人)

> **任务背景**：目前的 `MapBot_Reforged` 侧使用的 `DataManager` 在执行 `save()` 时存在严重的数据覆写风险（直接使用危险的 `Files.writeString`，缺失原子性和乐观锁保护）。如果不立刻换血升级写入引擎，将会爆发配置清空与并发写入时的“幽灵写”灾难。

---

## 📖 一、需要读取的上下文 (Read)
在进行任何改动之前，你**必须**首先读取并理解以下文件：
1. **全局规约**：`Project_Docs/MAPBOT_GLOBAL_PROTOCOLS.md`
   - 特别关注 `3.1 编码标准与安全准则` 中关于 CAS 写入与并发的强硬要求。
2. **数据一致性合同**：`Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
   - 关注里头关于乐观锁 (`expected_version`) 拦截策略设计的概念指引。
3. **目标业务代码**：
   - 研读 `MapBot_Reforged/src/main/java/com/mapbot/data/DataManager.java` 的内部 `DataModel` 原型与原 `save()` 行为。

## 🔨 二、需要输出的工程结构 (Output)
你需要**彻底重构重写** `DataManager.java` 的持久化管线：
1. **内部模型改组**：在 `DataModel` 或其管理类中引进一个 `entity_version` 字段用于追踪每次加载的版本号。
2. **CAS 检查拦截**：提供安全的并发写接口，确保在内存发生预期外变化时能够抛出类似一致性冲突（如 `CONSISTENCY-409` 警告）而不是强行覆写。
3. **原子替换特性**：替换掉原本赤裸裸的 `Files.writeString`。强制规定所有 JSON 的写盘必须先落入与最终目标同目录的 `.tmp` (临时)文件，一旦写盘成功，再利用 `StandardCopyOption.ATOMIC_MOVE` 原子的 `Files.move` 去覆盖主文件。

## ⚠️ 三、必须遵守的纪律 (Constraints)
1. **无感重组**：你可以大改 `save()` ，但不能去破坏项目中已经在使用 `DataManager.INSTANCE.getPermissionLevel` 等现有核心外探 API 签名，否则会导致 Reforged 侧大面积报错。
2. **防腐层纯粹**：严禁在此次变更中连着把 Alpha 端没用的配置 (`wsUrl` 等等) 也一并清了，那是接下来后续任务的事情！坚守单一修改原则（SRP）。
3. **编译红线**：修改完毕后，必须在 `MapBot_Reforged` 内通过 `gradlew build` 编译（允许抛弃尚未修好的 Common 包不报错前提下的 Reforged 单测），不可带任何红线提交。

## 📝 四、最终回答与汇报模板 (Reporting Template)
当你完成该任务后，请**必须**按照以下模板向用户与 Nexus 汇报（生成对应的新汇报文件并在回答里总结）：

```markdown
### 任务完成汇报：[Atlas] 重构 DataManager 引擎

**1. 实施摘要**
- 实装了基于 `.tmp` 原子替换的安全文件流覆写机制。
- 引入了 `entity_version` 作为应对幽灵写的 CAS 乐观锁基建。
- Reforged 侧 Gradle 编译验证：[说明验证结果]。
- Git Commit：[注明生成的 Commit 号]。

**2. 遭遇的兼容性调整 (选填)**
- [如果在保证向下兼容原本的查询层接口时做了包装策略，在此简要说明]。

**3. 上下文追溯更新**
- 我已在 `_AI_CONTEXT_MAP.md` 底部追加了格式化的历史锚点。
- 本次单例报告已落盘至 `Project_Docs/Reports/rebuild/report/Atlas_Task02_Done.md`。

**4. 移交与接管建议**
- 数据层基建加固完毕，防腐引擎切换完成。等待 Nexus 验收后分发下一环节。
```
