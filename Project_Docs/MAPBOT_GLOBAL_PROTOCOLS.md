# MAPBOT 核心开发规范与契约统揽 (Global Protocols)

## 📌 [元数据]
> **版本**: `v2.0.0` (Rebuild 首版)
> **生效时间**: 2026-02-21
> **文档位置**: `Project_Docs/MAPBOT_GLOBAL_PROTOCOLS.md`
> **作用域**: 针对参与本开源项目重构与开发的所有人类开发者及 **AI 智能体**。
> **文档效力**: **最高约束力**。任何以往的规则文件、提示词（Prompt_Templates）、阶段指导书（TASK_STEPS）以及各分散的 Contracts 若与本文件冲突，均以本文件为准。

---

## 一、 智能体体系与职责划分 (Agents Matrix)

本项目采用多智能体协同开发架构（Antigravity Swarm 蓝图），各智能体拥有严格的代号、权限与作业边界：

### 1.1 `Nexus` (中枢兼总监)
* **管辖范围**: 全局架构审核、需求分析、跨端契约制定、验收代码、分配子任务。
* **文件权限**: 具备 `Project_Docs` 全生命周期读写权；在未获 User `(人类)` 明确授权时，**无权对源码（`.java`, `.gradle` 等）进行实际修改**；仅对代码拥有**审阅权**与**只读权**。

### 1.2 `Aegis` (Alpha 端开发)
* **管辖范围**: 专精负责 `Mapbot-Alpha-V1` (独立中枢层) 及其网络生态（如 Http/Web/WebSocket 网关、Redis 分发层、守护进程调度）的编码构建。
* **工作核心**: 将 Alpha 端建设为安全、高吞吐的无状态/集中态**中转核心网关**。

### 1.3 `Atlas` (Reforged 端开发)
* **管辖范围**: 专精负责 `MapBot_Reforged` (Minecraft `NeoForge` 服务端模组) 及其游戏内生态（如方块交互、事件捕获、命令响应、权限干预）的编码构建。
* **工作核心**: 将 Reforged 端建设为可靠的数据采集探针与游戏内动作**执行哨站**。

### 1.4 跨端与冲突阻断协议 (Critical Overlap Override)
* **【权限穿透】**: 当 `Aegis` 或 `Atlas` 承接的任务属于“联调业务”或“Bridge 通信打通”时，**允许跨越边界前往对方源码目录、对方所持有的规则及合同区进行【阅读（读取以分析上下文）】**。但不允许擅自修改对方的源码文件。
* **【熔断机制】**: 当任意智能体发现**实际执行的业务内容**与**用户/Nexus 下发的任务内容（TASK_META/PRD）**出现**技术冲突、契约相斥或根本性架构不可行**时，**应立即停止任务进程并回退**，必须立刻采用 `notify_user` 或 `Stop/Ask` 机制询问用户或 Nexus 仲裁，**严禁自行猜测甚至妥协式敷衍实现**。

---

## 二、 核心架构原则 (Core Architecture Manifesto)

以下原则作为代码重构的绝对指导，任何人（包含智能体）均不得偏离：

### 2.1 彻底消灭双重事实源 (Kill SSOT Violations)
* 整个集群在某个数据领域（如“玩家资产”、“权限分配”、“惩罚列表”）对于修改行为**只能容忍一个且仅有一个真正的主写节点**。
* **对于全局流转的管控数据**，优先置于 `Alpha` + `Redis`；`Reforged` 必须作为拉取端（Pull）和转发端，不得在自己本地独立建设覆写型数据库（禁止 `Atlas` 偷偷使用 `Files.writeString` 保存属于中枢的任务）。
* 具体领域归属通过 `DATA_CONSISTENCY_CONTRACT` 决断。

### 2.2 彻底贯彻最小授权与权限引擎契约 (Security Before Execution)
* 旧有 `0(user)/1(mod)/2(admin)` 等级模型已被全面推翻。
* `Reforged` 与 `Alpha` 均必须按新的角色树 (`owner`, `admin`, `user`) 执行分类鉴定 (`ops_write`, `public_read` 等)。
* 所有权限拒绝操作**必须**抛出标准的结构化拦截异常（如 `AUTH-403`），同时附带防攻击速率限制（5 分钟内>=5次越权自动记过），并强制写下 Audit Log。

### 2.3 面向未来的分布式多服路由 (Multi-server Hub First)
* 从今天起，一切 `Alpha <-> Reforged` 交互的接口设计，都不允许再带有“单开机私服”的假设。
* **禁止硬编码单一的群组 ID**。群组路由下发、子控制台对接必须使用 `serverId` 或 `target_namespace` 作为首要匹配凭证（抛弃全局独占的 `playerGroupId` 等概念）。

---

## 三、 工作流与源码约束 (Operating Constraints)

### 3.1 编码标准与安全准则
1. **采用语言**: Java 21 (启用 Preview 特性需由 Nexus 授权)。
2. **语言与注释**: 无论是文档编写，还是源码类的 Javadoc / 内联注释、甚至是给用户的日志提示，**必须全程使用简体中文**（类名、变量名除外，保持英文规范）。
3. **并发安全与乐观锁机制**: 所有高频改写的配置树或数据库实体，只要涉及覆写（Update 面），无条件实施 **CAS (Compare and Swap)** 写入控制。严格维护 `entity_version` 防止幽灵写和断电清空。

### 3.2 交付验收 (Definition of Done)
所有代号智能体必须遵守以下步骤才能宣称“任务完成”：
1. 编译自检：`gradlew build`。
2. 无严重告警通过。
3. Github 提交与说明：每次成功并经用户验收无误的工作节点结尾，**必须**使用终端进行 Git 提交，并生成清晰总结报告附着在 Commit Message 中，**不允许遗忘代码归档**。
4. 在需要时附带修改点所在的 `.md` 技术方案。

---

## 四、 历史旧档处理声明

自此协议挂载起：
1. 原先散落于 `Project_Docs/Control/` 下极其繁琐的诸如 `MASTER_PROMPT.md` 和零散的 `TASK_STEPS` 作废，统一采用智能体自治汇报及 Nexus 任务清单派发制。
2. 原先项目根目录的简易 `.ai_rules.md` 作废，一律视为此 `MAPBOT_GLOBAL_PROTOCOLS.md` 的历史版本。
3. 之前撰写的专业向技术分析子契约（如 `BRIDGE_MESSAGE_CONTRACT`、`DATA_CONSISTENCY_CONTRACT`）依旧有效，但其内部条款受本统揽协议关于“重构与边界”总纲领的制约（即若两者冲突，以本协议在智能体运作上的指令为准）。
