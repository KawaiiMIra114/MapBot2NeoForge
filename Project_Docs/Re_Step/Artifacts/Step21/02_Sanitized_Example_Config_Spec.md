# 02 去敏示例配置规范 (Sanitized Example Config Spec)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-21 |
| Artifact | 02/05 |
| RUN_ID | 20260216T182100Z |

## 敏感项分类与去敏策略
| # | 类型 | 示例 | 去敏方式 | .gitignore |
|---|---|---|---|---|
| S-01 | Bot Token | `discord_bot_token` | 替换为 `YOUR_BOT_TOKEN_HERE` | ✅ 排除 .env |
| S-02 | API 密钥 | `api_key`, `secret` | 替换为 `YOUR_API_KEY_HERE` | ✅ 排除 config/*.secret |
| S-03 | 数据库凭据 | `redis_password` | 替换为 `YOUR_REDIS_PASSWORD` | ✅ 排除 |
| S-04 | 服务器地址 | `server_ip`, `port` | 替换为 `127.0.0.1:25565` | 否 (示例值) |
| S-05 | 文件路径 | `data_dir`, `log_dir` | 替换为 `./data`, `./logs` | 否 (相对路径) |
| S-06 | 用户ID | `owner_uuid`, `admin_id` | 替换为 `00000000-0000-0000-0000-000000000000` | ✅ 排除 |

## .gitignore 扫描规则
```gitignore
# Sensitive files
*.env
*.secret
config/secrets/
config/production.yml
*.pem
*.key
```

## 最小示例配置 (可运行)
```yaml
# config/example.yml - MapBot2NeoForge 示例配置
# 复制为 config/production.yml 并填入实际值
bot:
  token: "YOUR_BOT_TOKEN_HERE"
  prefix: "!"
  
server:
  host: "127.0.0.1"
  port: 25565

redis:
  host: "127.0.0.1"
  port: 6379
  password: "YOUR_REDIS_PASSWORD"

bridge:
  enabled: true
  heartbeat_interval_ms: 30000
  reconnect_max_retries: 5
```

## 验证方法
| 检查项 | 命令 | 预期 |
|---|---|---|
| 无明文 token | `rg -i "token.*=[^Y]" --glob "!*.md"` | 0 命中 |
| 无明文密码 | `rg -i "password.*=[^Y]" --glob "!*.md"` | 0 命中 |
| .gitignore 存在 | `test -f .gitignore` | exit=0 |
| 示例配置可解析 | YAML lint | PASS |

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| SAN-01 | 自动扫描 pre-commit hook 未安装 (设计完成) | Medium |
