# 03 外部贡献者上手验证 (External Contributor Onboarding Test)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-21 |
| Artifact | 03/05 |
| RUN_ID | 20260216T182100Z |

## 前置条件
| 项目 | 要求 |
|---|---|
| JDK | 21+ |
| Gradle | 包含在 gradlew wrapper |
| Git | 2.x+ |
| OS | Windows / Linux / macOS |

## 外部贡献者路径验证 (clone→build→test→run)

### Step 1: Clone
```bash
git clone https://github.com/<org>/MapBot2NeoForge.git
cd MapBot2NeoForge
```
**结果**: ✅ 仓库可 clone, 无 LFS 依赖

### Step 2: Build Alpha
```bash
cd Mapbot-Alpha-V1
./gradlew compileJava
```
**结果**: ✅ BUILD SUCCESSFUL (5s, 0 errors)
**阻塞点**: 无

### Step 3: Build Reforged
```bash
cd ../MapBot_Reforged
./gradlew compileJava
```
**结果**: ✅ BUILD SUCCESSFUL (6s, 0 errors)
**阻塞点**: 无

### Step 4: 配置
```bash
cp config/example.yml config/production.yml
# 编辑 production.yml 填入实际 token/密码
```
**结果**: ✅ 示例配置已提供
**阻塞点**: 需要 Discord Bot Token (文档已说明获取方式)

### Step 5: Run
```bash
cd Mapbot-Alpha-V1
./gradlew runServer
```
**结果**: ⚠️ 需要 Minecraft 服务端环境 (NeoForge), 非纯 Java 独立运行
**阻塞点**: 贡献者需了解 NeoForge mod 开发环境

## 阻塞点汇总与修复
| # | 阻塞点 | 修复 | 状态 |
|---|---|---|---|
| B-01 | NeoForge 开发环境设置 | CONTRIBUTING.md 增加 NeoForge 开发环境指引 | ✅ 已覆盖 |
| B-02 | Discord Bot Token 获取 | CONTRIBUTING.md 增加 Token 申请链接 | ✅ 已覆盖 |
| B-03 | 配置文件位置 | 示例配置 + 文档说明 | ✅ 已覆盖 |

## 最小上手时间
| 经验水平 | 预估时间 |
|---|---|
| 熟悉 NeoForge mod 开发 | ~15 分钟 |
| 熟悉 Java 但不熟悉 NeoForge | ~45 分钟 |
| 纯新手 | ~2 小时 (含环境搭建) |

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| ONB-01 | 缺少自动化 CI/CD 构建验证 (GitHub Actions) | Medium |
