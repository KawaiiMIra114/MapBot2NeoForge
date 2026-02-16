# Contributing to MapBot2NeoForge

感谢你对 MapBot2NeoForge 的贡献兴趣！

## 行为准则

本项目遵守 [Contributor Covenant 行为准则](CODE_OF_CONDUCT.md)。参与即表示你同意遵守。

## 开发环境

| 工具 | 版本 |
|---|---|
| JDK | 21+ |
| Gradle | 包含在 gradlew wrapper |
| Git | 2.x+ |
| IDE | IntelliJ IDEA (推荐) |

### 构建步骤

```bash
# 1. Clone
git clone https://github.com/<org>/MapBot2NeoForge.git
cd MapBot2NeoForge

# 2. 编译 Alpha
cd Mapbot-Alpha-V1
./gradlew compileJava

# 3. 编译 Reforged
cd ../MapBot_Reforged
./gradlew compileJava
```

### NeoForge 开发环境

本项目是 NeoForge Minecraft mod, 需要 NeoForge MDK 环境。请参考 [NeoForge 文档](https://docs.neoforged.net/) 设置开发环境。

## 提交规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式:

```
<type>(<scope>): <description>

[optional body]
```

类型: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

## Pull Request 流程

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feat/my-feature`)
3. 按提交规范提交更改
4. 确保双端编译通过
5. 提交 PR 到 `main` 分支
6. 等待维护者 review

## Issue 报告

提交 Issue 时请提供:
- 环境信息 (OS, JDK 版本, Minecraft 版本)
- 复现步骤
- 预期行为 vs 实际行为
- 相关日志

## Discord Bot Token

开发测试需要 Discord Bot Token:
1. 访问 [Discord Developer Portal](https://discord.com/developers/applications)
2. 创建 Application → Bot
3. 复制 Token 到 `config/production.yml`
