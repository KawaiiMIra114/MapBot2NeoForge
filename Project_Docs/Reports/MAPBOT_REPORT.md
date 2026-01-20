# MapBot Reforged v5.0.0 Technical Report

## Executive Summary

MapBot Reforged is a NeoForge mod that bridges Minecraft servers with QQ groups via OneBot protocol. Version 5.0.0 introduces enhanced server management, performance monitoring integration, and improved user experience features.

## Architecture Overview

```
┌─────────────────┐    WebSocket     ┌─────────────────┐
│   QQ Client     │◄───────────────►│    OneBot API   │
│  (Phone/PC)     │                  │  (go-cqhttp)    │
└─────────────────┘                  └────────┬────────┘
                                              │
                                              ▼
┌─────────────────┐    Events        ┌─────────────────┐
│  Minecraft      │◄───────────────►│    MapBot       │
│   Server        │                  │   Reforged      │
└─────────────────┘                  └─────────────────┘
```

## New Features (v5.0.0)

### 1. Duolingo Super Integration
| Command | Description |
|---------|-------------|
| `#report` | Reads `duolingo_super_report.json` and sends formatted performance summary to QQ group |

**Report includes:**
- Total compression statistics
- LZ4/Zstd packet counts
- Bytes saved calculation
- Filter statistics

### 2. Server Shutdown Management
| Command | Description |
|---------|-------------|
| `#stopserver [seconds]` | Initiates countdown shutdown (default: 60s) |
| `#cancelstop` | Cancels ongoing shutdown (admin only) |

**Features:**
- Countdown announcements to both QQ and in-game
- Graceful cancellation support
- Minimum 10-second countdown enforced

### 3. Command Cooldown System
- 5-second cooldown between commands per user
- Administrators exempt from cooldown
- Prevents command spam

### 4. Server Lifecycle Messages
| Event | Message |
|-------|---------|
| Server Started | "早安! 服务器已启动" (after 5s delay) |
| Server Stopping | "晚安! 服务器即将关闭" |

## Commands Reference

### Player Commands
| Command | Aliases | Description |
|---------|---------|-------------|
| `#id <name>` | `#bind` | Bind QQ to Minecraft account |
| `#unbind` | - | Unbind account |
| `#list` | `#在线` | Show online players |
| `#status` | `#tps`, `#状态` | Server status |
| `#playtime` | `#在线时长` | Query play time |
| `#report` | `#报告` | Performance report |
| `#help` | `#菜单` | Show help |

### Admin Commands (Admin Group Only)
| Command | Description |
|---------|-------------|
| `#inv <name> [-e]` | View inventory/ender chest |
| `#location <name>` | Query player location |
| `#stopserver [sec]` | Shutdown server |
| `#cancelstop` | Cancel shutdown |
| `#addadmin <QQ>` | Add administrator |
| `#removeadmin <QQ>` | Remove administrator |
| `#adminunbind <name>` | Force unbind player |
| `#reload` | Reload configuration |

## Configuration

**File:** `config/mapbot-common.toml`

```toml
[bot]
websocketUrl = "ws://127.0.0.1:8080"
playerGroupId = 123456789
adminGroupId = 987654321
reconnectDelay = 5000

[features]
enableCooldown = true
cooldownSeconds = 5
```

## Technical Implementation

### WebSocket Handler
- Auto-reconnection with configurable delay
- Message queue for offline buffering
- Heartbeat monitoring

### Dual Group Support
- Player group: General commands, message relay
- Admin group: Management commands, alerts

### CQ Code Parser
- Extracts @mentions for targeted notifications
- Reply message handling with context preservation

## Changelog (v5.0.0)

- ✅ Added `#report` command for duolingo_super integration
- ✅ Added `#cancelstop` command with admin protection
- ✅ Implemented 5-second command cooldown
- ✅ Added server start/stop QQ notifications
- ✅ Updated #help menu with new commands
- ✅ Fixed command permission separation

## Dependencies

- NeoForge 21.1+
- Minecraft 1.21.1
- OneBot v11 compatible bot (go-cqhttp recommended)

## Known Issues

1. WebSocket reconnection may take up to 30 seconds
2. Large inventory queries may timeout on slow connections

---

*Document generated: 2026-01-20*  
*Version: 5.0.0*
