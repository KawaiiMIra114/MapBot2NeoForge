# 04 发布产物结构规范 (Release Artifact Layout Spec)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-21 |
| Artifact | 04/05 |
| RUN_ID | 20260216T182100Z |

## 发布包目录结构
```
MapBot2NeoForge-v<X.Y.Z>/
├── Mapbot-Alpha-V1/
│   ├── build/libs/mapbot-alpha-<version>.jar
│   └── config/example.yml
├── MapBot_Reforged/
│   ├── build/libs/mapbot-reforged-<version>.jar
│   └── config/example.yml
├── docs/
│   ├── QUICK_START.md
│   ├── ARCHITECTURE.md (link)
│   └── CHANGELOG.md
├── LICENSE
├── README.md
├── CONTRIBUTING.md
├── SECURITY.md
├── CODE_OF_CONDUCT.md
├── CHECKSUMS.sha256
└── RELEASE_NOTES.md
```

## 发布工件清单
| # | 工件 | 必需 | 校验方式 |
|---|---|---|---|
| RA-01 | mapbot-alpha-<version>.jar | ✅ | SHA-256 in CHECKSUMS.sha256 |
| RA-02 | mapbot-reforged-<version>.jar | ✅ | SHA-256 in CHECKSUMS.sha256 |
| RA-03 | config/example.yml (per module) | ✅ | YAML lint |
| RA-04 | LICENSE | ✅ | 文件存在性 |
| RA-05 | README.md | ✅ | 文件存在性 + 非空 |
| RA-06 | RELEASE_NOTES.md | ✅ | 版本号匹配 |
| RA-07 | CHECKSUMS.sha256 | ✅ | 自校验 |

## 版本命名规范
- 格式: `v<Major>.<Minor>.<Patch>` (SemVer 2.0)
- 对齐: `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`
- RC 命名: `v<X.Y.Z>-rc.<N>`
- 快照: `v<X.Y.Z>-SNAPSHOT`

## 兼容声明
| 项目 | 兼容范围 |
|---|---|
| Minecraft | 1.21.x |
| NeoForge | 对应 1.21.x 最新 |
| Java | 21+ |
| Discord JDA | 5.x |

## 文档入口与断链检查
| 入口 | 路径 | 状态 |
|---|---|---|
| 项目根 README | `/README.md` | ⚠️ J1 创建 |
| 快速开始 | `docs/QUICK_START.md` | ⚠️ J1 创建 |
| 架构文档 | `Project_Docs/Architecture/SYSTEM_CONTEXT.md` | ✅ 存在 |
| 变更日志 | `CHANGELOG.md` | ⚠️ J1 创建 |
| 发布清单 | `Project_Docs/Manuals/RELEASE_CHECKLIST.md` | ✅ 存在 |

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| RAL-01 | 根 README.md 尚未创建 | Medium |
| RAL-02 | CHANGELOG.md 尚未创建 | Medium |
| RAL-03 | CHECKSUMS.sha256 自动生成脚本未编写 | Low |
