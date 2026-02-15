# DEPLOYMENT_RUNBOOK

## 0. 统一元数据
| 字段 | 值 |
| --- | --- |
| 文档标识 | `A5-DEPLOYMENT-RUNBOOK` |
| 子任务 | `A5（运维手册-部署/日常）` |
| 目标系统 | `Ubuntu 22.04+ / WSL2(Ubuntu)` |
| Java 基线 | `OpenJDK 21` |
| 部署架构 | `Mapbot-Alpha-V1 + MapBot_Reforged(NeoForge)` |
| 维护角色 | `运维值班/发布执行人` |
| 最后更新 | `2026-02-14` |
| 关联配置 | `Mapbot-Alpha-V1/config/alpha.properties`, `config/mapbot-common.toml` |
| 关联日志 | `Mapbot-Alpha-V1/logs/alpha.log`, `logs/latest.log` |

## 0.1 强制章节：统一失败判据阈值表（全局）
> 本表在部署手册与运维手册保持一致；若不满足阈值，统一判定为“失败/需处置”。

| ID | 指标 | 统一阈值（Fail 条件） | 快速命令（示例） |
| --- | --- | --- | --- |
| F01 | 依赖命令完整性 | 缺少任一：`java tmux curl jq ss rg` | `for c in java tmux curl jq ss rg; do command -v $c >/dev/null || echo MISS:$c; done` |
| F02 | Java 主版本 | 非 `21` 即失败 | `java -version` |
| F03 | Alpha 构建 | `./gradlew build` 非 0 退出码 | `(cd "$ALPHA_DIR" && ./gradlew --no-daemon build)` |
| F04 | Reforged 构建 | `./gradlew build` 非 0 退出码 | `(cd "$REFORGED_DIR" && ./gradlew --no-daemon build)` |
| F05 | Alpha 启动就绪时延 | 启动后 `180s` 内未出现“Bridge 服务器已启动/核心已就绪” | `timeout 180 bash -c 'until rg -q "Bridge 服务器已启动|核心已就绪" "$ALPHA_DIR/logs/alpha.log"; do sleep 2; done'` |
| F06 | 子服 Bridge 注册时延 | 启动后 `180s` 内未出现 `\[Bridge\] 注册成功` | `timeout 180 bash -c 'until rg -q "\\[Bridge\\] 注册成功" "$MC_SERVER_DIR/logs/latest.log"; do sleep 2; done'` |
| F07 | 登录验活 | `/api/login` 10s 内拿不到 token | `curl -sS -X POST http://127.0.0.1:25560/api/login ... | jq -r '.token // empty'` |
| F08 | 状态 API | `/api/status` 非 200 或非 JSON | `curl -sS http://127.0.0.1:25560/api/status -H "Authorization: Bearer $ALPHA_TOKEN" | jq .` |
| F09 | 服务器注册视图 | `/api/servers` 未出现目标 `serverId` | `curl -sS http://127.0.0.1:25560/api/servers -H "Authorization: Bearer $ALPHA_TOKEN" | jq` |
| F10 | 管理命令 `#status` | 发出后 `60s` 内无正常回包 | 人工：管理群发送 `#status` |
| F11 | 管理命令 `#list` | 发出后 `60s` 内无正常回包 | 人工：管理群发送 `#list` |
| F12 | Bridge 鉴权错误率 | 验收窗口内出现任一 `unauthorized` | `tail -n 200 "$ALPHA_DIR/logs/alpha.log" | rg 'unauthorized|拒绝未授权 Bridge 注册'` |
| F13 | 端口保留段误用 | 除 `connection.listenPort=25560` 外，业务端口落入 `25560-25566` | `grep -E '^(connection.listenPort|bridge.listenPort|minecraft.targetPort)=' "$ALPHA_DIR/config/alpha.properties"` |
| F14 | 跨机时钟偏差 | `abs(diff)>120s` | `echo $((ALPHA_TS-MC_TS))` |
| F15 | `#reload` 子服成功率 | 返回 `成功 x/y` 且 `x<y` | 人工：管理群发送 `#reload` |
| F16 | `#reload` 生效证据 | 未同时看到 Alpha 热重载 + 子服重载日志 | `tail -n 200 "$ALPHA_DIR/logs/alpha.log" | rg '安全配置已热重载|子服重载'` |
| F17 | 部署后错误增长 | 10 分钟内新增 `ERROR` > 20 行 | `tail -n 2000 "$ALPHA_DIR/logs/alpha.log" | rg -c 'ERROR'` |
| F18 | 回滚演练时长 | 从停服到复验通过 > 15 分钟 | 演练脚本开始/结束打点 |

## 0.2 强制章节：现网高风险阈值（5项）动态校准
> 下列 5 项最容易在不同机器/流量下失配，建议每周校准一次。

| 阈值ID | 固定阈值 | 失配原因 | 动态校准方案 |
| --- | --- | --- | --- |
| F05 | Alpha 就绪 `<=180s` | WSL 磁盘慢、冷缓存、Gradle 首启慢 | 取最近 10 次启动耗时 `P95`，新阈值=`max(180, ceil(P95*1.5))` |
| F06 | 子服注册 `<=180s` | 大地图/模组加载导致注册延迟 | 取最近 10 次注册耗时 `P95`，新阈值=`max(180, ceil(P95*1.5))` |
| F10/F11 | 命令回包 `<=60s` | 群消息网关抖动或高峰拥堵 | 采样 30 次 `#status/#list` 回包时延，阈值设为 `min(180, ceil(P99*2))` |
| F15 | `#reload` 成功率 `x/y` | 分母 `y` 包含临时下线服导致误报 | 分母改为“执行瞬间在线子服数”，离线服不计入失败 |
| F17 | 10分钟 ERROR `>20` | debug 模式或历史噪音导致误判 | 建立“白名单错误模式”并用 `baseline_mean + 3σ` 替代固定 20 |

动态校准命令模板：
```bash
# 1) 记录一次实际耗时（示例：Alpha 就绪）
START_TS="$(date +%s)"
timeout 300 bash -c 'until rg -q "Bridge 服务器已启动|核心已就绪" "$ALPHA_DIR/logs/alpha.log"; do sleep 2; done'
END_TS="$(date +%s)"
echo $((END_TS-START_TS)) >> /tmp/mapbot_alpha_boot_samples.txt

# 2) 计算 P95（需要 10+ 样本）
sort -n /tmp/mapbot_alpha_boot_samples.txt | awk '{
  a[NR]=$1
}
END{
  if (NR==0) exit 1
  p=int((NR*0.95)+0.999)
  if (p<1) p=1
  if (p>NR) p=NR
  print "P95=" a[p]
}'
```

## 1. 强制章节：适用范围与路径约定
- 本手册用于 Alpha 中枢模式（推荐）：`OneBot -> Alpha -> Reforged 子服`。
- 单服直连模式仅用于临时测试（`mapbot-common.toml` 中 `alphaHost=""`，Reforged 直连 OneBot）。
- 本文命令默认在 Ubuntu/WSL bash 执行。

```bash
export REPO=/mnt/d/axm/mcs/MapBot2NeoForge
export ALPHA_DIR="$REPO/Mapbot-Alpha-V1"
export REFORGED_DIR="$REPO/MapBot_Reforged"
export MC_SERVER_DIR=/srv/minecraft
```

## 1.1 强制章节：WSL 与原生 Linux 差异清单（含替代命令）
| 主题 | 原生 Linux 常用 | WSL 常见差异 | WSL 替代命令 |
| --- | --- | --- | --- |
| 端口排查 | `ss -ltnp` 即可 | 端口可能被 Windows 侧进程占用 | `powershell.exe -NoProfile -Command "Get-NetTCPConnection -State Listen | ? LocalPort -in 25560,25661,25570,7000,3000"` |
| 路径规范 | 常用 `/opt`、`/srv` | 常在 `/mnt/<盘符>/...`；性能受挂载影响 | 构建源码可在 `/mnt/d/...`，运行数据建议放 `~/srv/minecraft` |
| systemd | 可直接 `systemctl` | 部分 WSL 发行版未启用 systemd | `sudo service redis-server start` 或 `redis-server --daemonize yes` |
| 时钟同步 | `timedatectl set-ntp true` | 可能跟宿主机漂移，`timedatectl` 不可用 | `date -u`；必要时在 Windows 执行 `w32tm /resync` 后 `wsl.exe --shutdown` |
| 文件权限 | `chmod/chown` 完整生效 | `/mnt/*` 默认权限映射，`chmod` 可能不完全生效 | 运行目录迁移到 Linux FS：`mkdir -p ~/srv/minecraft && cp -a /mnt/d/... ~/srv/minecraft` |
| CRLF 换行 | 通常 LF | Windows 编辑器可能写入 CRLF | `sed -i 's/\r$//' config/*.properties config/*.toml` |


## 2. 强制章节：前置条件
1. 安装基础依赖（Java 21 必须）。

```bash
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk tmux jq curl openssl
java -version
```

2. 若启用 Redis（`alpha.properties` 中 `redis.enabled=true`），先启动 Redis。

```bash
sudo apt-get install -y redis-server
sudo systemctl enable --now redis-server
redis-cli ping
```

3. 检查关键端口占用（部署前应明确已有占用者）。

```bash
ss -ltnp | rg ':(25560|25661|25570|7000|3000)\b' || true
```

4. 确保 Minecraft 服务目录存在且可写。

```bash
mkdir -p "$MC_SERVER_DIR"/{mods,config,logs,backup}
```

## 3. 强制章节：安装
1. 构建 Alpha Core。

```bash
cd "$ALPHA_DIR"
./gradlew --no-daemon clean build
```

2. 构建 Reforged 模组。

```bash
cd "$REFORGED_DIR"
./gradlew --no-daemon clean build
```

3. 备份旧版本（jar + 配置）。

```bash
BACKUP_DIR="$MC_SERVER_DIR/backup/mapbot-$(date +%F_%H%M%S)"
mkdir -p "$BACKUP_DIR"
cp -a "$MC_SERVER_DIR/mods"/mapbot-*.jar "$BACKUP_DIR/" 2>/dev/null || true
cp -a "$MC_SERVER_DIR/config/mapbot-common.toml" "$BACKUP_DIR/" 2>/dev/null || true
cp -a "$ALPHA_DIR/config/alpha.properties" "$BACKUP_DIR/" 2>/dev/null || true
```

4. 部署新 Reforged jar 到 MC 服务端 `mods`。

```bash
REFORGED_JAR="$(ls -1 "$REFORGED_DIR"/build/libs/mapbot-*.jar | head -n 1)"
cp -f "$REFORGED_JAR" "$MC_SERVER_DIR/mods/"
ls -lh "$MC_SERVER_DIR/mods/" | rg 'mapbot-.*\.jar'
```

## 4. 强制章节：配置
1. 生成统一 Bridge Token，并写入 Alpha 与 Reforged。

```bash
BRIDGE_TOKEN="$(openssl rand -hex 24)"
TOKEN_SECRET="$(openssl rand -hex 16)"
echo "BRIDGE_TOKEN=$BRIDGE_TOKEN"
```

2. Alpha Core 配置（`$ALPHA_DIR/config/alpha.properties`）。

```bash
cat > "$ALPHA_DIR/config/alpha.properties" <<'EOF'
connection.wsUrl=ws\://127.0.0.1\:7000
connection.reconnectInterval=5
connection.listenPort=25560
bridge.listenPort=25661
minecraft.targetHost=127.0.0.1
minecraft.targetPort=25570

messaging.playerGroupId=875585697
messaging.adminGroupId=885810515
messaging.botQQ=2133782376

redis.enabled=false
redis.host=127.0.0.1
redis.port=6379
redis.password=
redis.database=0

auth.bridge.token=REPLACE_BRIDGE_TOKEN
auth.bridge.allowedServerIds=survival
auth.tokenSecret=REPLACE_TOKEN_SECRET
auth.bootstrapAdmin.enabled=true
auth.bootstrapAdmin.username=admin
auth.bootstrapAdmin.password=CHANGE_ME_STRONG_PASSWORD
auth.bootstrapAdmin.role=ADMIN

debug.debugMode=false
EOF
sed -i "s/REPLACE_BRIDGE_TOKEN/$BRIDGE_TOKEN/g" "$ALPHA_DIR/config/alpha.properties"
sed -i "s/REPLACE_TOKEN_SECRET/$TOKEN_SECRET/g" "$ALPHA_DIR/config/alpha.properties"
```

3. Reforged 配置（`$MC_SERVER_DIR/config/mapbot-common.toml`）。

```bash
cat > "$MC_SERVER_DIR/config/mapbot-common.toml" <<'EOF'
[connection]
wsUrl = "ws://127.0.0.1:3000"
reconnectInterval = 5

[messaging]
playerGroupId = 875585697
adminGroupId = 885810515
botQQ = 2133782376

[alpha]
serverId = "survival"
alphaHost = "127.0.0.1"
alphaPort = 25661
alphaToken = "REPLACE_BRIDGE_TOKEN"
transferHost = "mc.example.com"
transferPort = 25565

[debug]
debugMode = false
EOF
sed -i "s/REPLACE_BRIDGE_TOKEN/$BRIDGE_TOKEN/g" "$MC_SERVER_DIR/config/mapbot-common.toml"
```

4. 配置一致性最小检查。

```bash
grep -E '^(auth.bridge.token|auth.bridge.allowedServerIds)=' "$ALPHA_DIR/config/alpha.properties"
rg -n '^\s*(serverId|alphaHost|alphaPort|alphaToken)\s*=' "$MC_SERVER_DIR/config/mapbot-common.toml"
```

## 5. 强制章节：启动顺序
1. 先启动 OneBot/NapCat（确保 Alpha 的 `connection.wsUrl` 可连通）。
2. 若启用 Redis，确认 Redis 已启动。
3. 启动 Alpha Core。
4. 启动 Minecraft(NeoForge) 服务器。

```bash
# 1) Alpha
tmux kill-session -t alpha 2>/dev/null || true
tmux new-session -d -s alpha "cd $ALPHA_DIR && ./gradlew --no-daemon run"

# 2) MC (示例命令，请替换 neoforge-server.jar 为你的实际启动 jar)
tmux kill-session -t mc 2>/dev/null || true
tmux new-session -d -s mc "cd $MC_SERVER_DIR && java -Xms2G -Xmx4G -jar neoforge-server.jar nogui"
```

## 6. 强制章节：验证步骤
1. 会话与进程存活。

```bash
tmux ls | rg 'alpha|mc'
ps -ef | rg 'MapbotAlpha|neoforge|java' | rg -v rg
```

2. 端口监听状态正确。

```bash
ss -ltnp | rg ':(25560|25661|25570|7000)\b'
```

3. Alpha 日志验证 Bridge 服务与注册。

```bash
tail -n 200 "$ALPHA_DIR/logs/alpha.log" | rg 'Bridge 服务器已启动|Bridge 注册鉴权通过|服务器已注册|拒绝未授权 Bridge 注册'
```

4. Reforged 日志验证 Bridge 建连与注册结果。

```bash
tail -n 200 "$MC_SERVER_DIR/logs/latest.log" | rg '\[Bridge\] 正在连接到 Alpha Core|\[Bridge\] 已连接到 Alpha Core|\[Bridge\] 注册成功|\[Bridge\] 注册被 Alpha 拒绝'
```

5. Alpha API 快速验活（需要管理员账号）。

```bash
ALPHA_TOKEN="$(curl -sS -X POST 'http://127.0.0.1:25560/api/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"CHANGE_ME_STRONG_PASSWORD"}' | jq -r '.token // empty')"
curl -sS 'http://127.0.0.1:25560/api/status' -H "Authorization: Bearer $ALPHA_TOKEN" | jq
curl -sS 'http://127.0.0.1:25560/api/servers' -H "Authorization: Bearer $ALPHA_TOKEN" | jq
```

6. 业务验证：在管理群发送 `#status`、`#list`，确认可回包。

## 7. 强制章节：冷启动验收脚本（12+步）
> 目标：从“机器空状态”到“`#status` / `#list` 已验证通过”的一次性验收闭环。  
> 使用方式：先按需修改脚本顶部变量，再执行脚本。

```bash
cat > /tmp/mapbot_cold_start_accept.sh <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

REPO="${REPO:-/mnt/d/axm/mcs/MapBot2NeoForge}"
ALPHA_DIR="${ALPHA_DIR:-$REPO/Mapbot-Alpha-V1}"
REFORGED_DIR="${REFORGED_DIR:-$REPO/MapBot_Reforged}"
MC_SERVER_DIR="${MC_SERVER_DIR:-/srv/minecraft}"
ALPHA_HTTP_PORT="${ALPHA_HTTP_PORT:-25560}"
ALPHA_ADMIN_USER="${ALPHA_ADMIN_USER:-admin}"
ALPHA_ADMIN_PASS="${ALPHA_ADMIN_PASS:-CHANGE_ME_STRONG_PASSWORD}"
EXPECT_SERVER_ID="${EXPECT_SERVER_ID:-survival}"

step() { printf '\n[STEP %02d] %s\n' "$1" "$2"; }
fail() { echo "[FAIL] $*" >&2; exit 1; }

step 1 "依赖命令检查"
for c in java tmux curl jq ss rg; do
  command -v "$c" >/dev/null || fail "缺少命令: $c"
done

step 2 "Java 主版本检查（要求=21）"
JAVA_MAJOR="$(java -version 2>&1 | awk -F '[\".]' '/version/ {print $2; exit}')"
[ "$JAVA_MAJOR" = "21" ] || fail "Java 版本不符合要求: $JAVA_MAJOR"

step 3 "目录存在性检查"
for d in "$ALPHA_DIR" "$REFORGED_DIR" "$MC_SERVER_DIR"; do
  [ -d "$d" ] || fail "目录不存在: $d"
done

step 4 "构建 Alpha"
(cd "$ALPHA_DIR" && ./gradlew --no-daemon clean build >/tmp/mapbot_alpha_build.log 2>&1) || fail "Alpha 构建失败"

step 5 "构建 Reforged"
(cd "$REFORGED_DIR" && ./gradlew --no-daemon clean build >/tmp/mapbot_reforged_build.log 2>&1) || fail "Reforged 构建失败"

step 6 "部署 Reforged jar 到 mods"
REFORGED_JAR="$(ls -1 "$REFORGED_DIR"/build/libs/mapbot-*.jar | head -n 1)"
[ -n "$REFORGED_JAR" ] || fail "未找到 Reforged 构建产物"
cp -f "$REFORGED_JAR" "$MC_SERVER_DIR/mods/" || fail "复制 jar 失败"

step 7 "关键配置完整性检查"
grep -q '^auth.bridge.token=' "$ALPHA_DIR/config/alpha.properties" || fail "缺少 auth.bridge.token"
grep -q '^auth.bridge.allowedServerIds=' "$ALPHA_DIR/config/alpha.properties" || fail "缺少 auth.bridge.allowedServerIds"
rg -q '^\s*alphaToken\s*=' "$MC_SERVER_DIR/config/mapbot-common.toml" || fail "缺少 alphaToken"
rg -q '^\s*serverId\s*=' "$MC_SERVER_DIR/config/mapbot-common.toml" || fail "缺少 serverId"

step 8 "启动前端口冲突预检查"
ss -ltnp | rg ':(25560|25661|25570)\b' && echo "[WARN] 检测到端口已占用，请确认是否为旧进程" || true

step 9 "启动 Alpha"
tmux kill-session -t alpha 2>/dev/null || true
tmux new-session -d -s alpha "cd $ALPHA_DIR && ./gradlew --no-daemon run"

step 10 "等待 Alpha 就绪（Bridge server + 核心监听）"
timeout 180 bash -c '
  until [ -f "'"$ALPHA_DIR"'/logs/alpha.log" ] && rg -q "Bridge 服务器已启动|核心已就绪" "'"$ALPHA_DIR"'/logs/alpha.log"; do
    sleep 2
  done
' || fail "Alpha 未在 180 秒内就绪"

step 11 "启动 Minecraft 子服"
tmux kill-session -t mc 2>/dev/null || true
tmux new-session -d -s mc "cd $MC_SERVER_DIR && java -Xms2G -Xmx4G -jar neoforge-server.jar nogui"

step 12 "等待 Bridge 注册成功"
timeout 180 bash -c '
  until [ -f "'"$MC_SERVER_DIR"'/logs/latest.log" ] && rg -q "\\[Bridge\\] 注册成功" "'"$MC_SERVER_DIR"'/logs/latest.log"; do
    sleep 2
  done
' || fail "子服 Bridge 注册未成功"

step 13 "Alpha API 验活（/api/login + /api/status + /api/servers）"
ALPHA_TOKEN="$(
  curl -sS -X POST "http://127.0.0.1:${ALPHA_HTTP_PORT}/api/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${ALPHA_ADMIN_USER}\",\"password\":\"${ALPHA_ADMIN_PASS}\"}" \
  | jq -r '.token // empty'
)"
[ -n "$ALPHA_TOKEN" ] || fail "Alpha 登录失败"
curl -sS "http://127.0.0.1:${ALPHA_HTTP_PORT}/api/status" -H "Authorization: Bearer $ALPHA_TOKEN" | jq . >/dev/null || fail "status 接口失败"
curl -sS "http://127.0.0.1:${ALPHA_HTTP_PORT}/api/servers" -H "Authorization: Bearer $ALPHA_TOKEN" \
  | jq -e ".[] | select(.id==\"${EXPECT_SERVER_ID}\")" >/dev/null || fail "未发现期望 serverId=${EXPECT_SERVER_ID}"

step 14 "人工验证 #status"
echo "请在管理群发送: #status"
read -r -p "确认已收到正常回复后输入 YES: " ACK_STATUS
[ "$ACK_STATUS" = "YES" ] || fail "#status 验证未通过"

step 15 "人工验证 #list"
echo "请在管理群发送: #list"
read -r -p "确认已收到正常回复后输入 YES: " ACK_LIST
[ "$ACK_LIST" = "YES" ] || fail "#list 验证未通过"

echo
echo "[PASS] 冷启动验收完成（含 #status/#list）"
EOF

chmod +x /tmp/mapbot_cold_start_accept.sh
bash /tmp/mapbot_cold_start_accept.sh
```

## 8. 强制章节：回滚步骤
1. 回滚触发条件（任一满足即回滚）：
- Bridge 注册持续失败（`register_ack success=false` / `unauthorized`）。
- 核心命令不可用（`#status`、`#list` 无返回）。
- 启动后 10 分钟内错误日志持续增长。

2. 停服务（先 MC 后 Alpha）。

```bash
tmux send-keys -t mc 'stop' C-m
sleep 10
tmux send-keys -t alpha 'stop' C-m
sleep 3
tmux kill-session -t mc 2>/dev/null || true
tmux kill-session -t alpha 2>/dev/null || true
```

3. 恢复备份（使用部署前生成的 `BACKUP_DIR`）。

```bash
cp -f "$BACKUP_DIR"/mapbot-*.jar "$MC_SERVER_DIR/mods/" 2>/dev/null || true
cp -f "$BACKUP_DIR"/mapbot-common.toml "$MC_SERVER_DIR/config/" 2>/dev/null || true
cp -f "$BACKUP_DIR"/alpha.properties "$ALPHA_DIR/config/" 2>/dev/null || true
```

4. 按“启动顺序”重新启动并重复“验证步骤”。

## 9. 强制章节：反向测试（避免误报）
场景：服务器空载时（无人在线），`#list` 返回 `0/x` 是正常，不应当误判为故障。

```bash
# 操作: 在管理群发送 #list
# 期望: 回复包含 [在线玩家] 0/<maxPlayers>
# 同时日志中不应出现 Bridge 连接故障关键字
tail -n 120 "$ALPHA_DIR/logs/alpha.log" | rg '拒绝未授权 Bridge 注册|请求超时|连接失败' && echo "[WARN] 请人工确认是否历史残留日志"
tail -n 120 "$MC_SERVER_DIR/logs/latest.log" | rg '注册被 Alpha 拒绝|alphaToken 未配置|连接/会话异常' && echo "[WARN] 请人工确认是否历史残留日志"
```

## 10. 强制章节：最小回滚演练脚本（一键回滚并复验）
> 目标：在演练环境快速验证“回滚链路可用”，要求 15 分钟内完成。
> 脚本内置 3 类自动降级：`备份损坏`、`权限不足`、`端口占用`。

```bash
cat > /tmp/mapbot_rollback_drill.sh <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

REPO="${REPO:-/mnt/d/axm/mcs/MapBot2NeoForge}"
ALPHA_DIR="${ALPHA_DIR:-$REPO/Mapbot-Alpha-V1}"
MC_SERVER_DIR="${MC_SERVER_DIR:-/srv/minecraft}"
BACKUP_DIR="${BACKUP_DIR:-}"
EXPECT_SERVER_ID="${EXPECT_SERVER_ID:-survival}"
ALPHA_HTTP_PORT=25560
DEGRADED=0
START_TS="$(date +%s)"

log(){ echo "[INFO] $*"; }
warn(){ echo "[WARN] $*" >&2; DEGRADED=1; }
fail(){ echo "[FAIL] $*" >&2; exit 1; }

copy_with_fallback() {
  local src="$1"
  local dst="$2"
  if cp -f "$src" "$dst" 2>/dev/null; then
    return 0
  fi
  if command -v sudo >/dev/null && sudo -n true 2>/dev/null; then
    sudo cp -f "$src" "$dst" && return 0
  fi
  warn "复制失败（权限不足）: $src -> $dst，降级为保留现有文件"
  return 1
}

[ -n "$BACKUP_DIR" ] || fail "请先导出 BACKUP_DIR"
[ -d "$BACKUP_DIR" ] || fail "BACKUP_DIR 不存在: $BACKUP_DIR"

if [ ! -s "$BACKUP_DIR/mapbot-common.toml" ] || [ ! -s "$BACKUP_DIR/alpha.properties" ]; then
  warn "检测到关键备份文件损坏/缺失，降级为“仅重启+复验”模式"
fi
if ! ls "$BACKUP_DIR"/mapbot-*.jar >/dev/null 2>&1; then
  warn "检测到 jar 备份缺失，降级为“配置回滚”模式（jar 保持现状）"
fi

log "[1/6] 停止当前服务"
tmux send-keys -t mc 'stop' C-m 2>/dev/null || true
sleep 8
tmux send-keys -t alpha 'stop' C-m 2>/dev/null || true
sleep 3
tmux kill-session -t mc 2>/dev/null || true
tmux kill-session -t alpha 2>/dev/null || true

log "[2/6] 恢复备份文件（带降级）"
if ls "$BACKUP_DIR"/mapbot-*.jar >/dev/null 2>&1; then
  for j in "$BACKUP_DIR"/mapbot-*.jar; do
    copy_with_fallback "$j" "$MC_SERVER_DIR/mods/" || true
  done
fi
[ -s "$BACKUP_DIR/mapbot-common.toml" ] && copy_with_fallback "$BACKUP_DIR/mapbot-common.toml" "$MC_SERVER_DIR/config/" || true
[ -s "$BACKUP_DIR/alpha.properties" ] && copy_with_fallback "$BACKUP_DIR/alpha.properties" "$ALPHA_DIR/config/" || true

log "[2.1/6] 关键端口占用检查（带降级）"
PORT_CONFLICT=0
for p in 25560 25661 25570; do
  if ss -ltnp | rg -q ":$p\\b"; then
    if ! ss -ltnp | rg ":$p\\b" | rg -q 'java|Mapbot|neoforge'; then
      PORT_CONFLICT=1
    fi
  fi
done
if [ "$PORT_CONFLICT" -eq 1 ]; then
  warn "检测到关键端口被非预期进程占用，降级切换到 35560/35661/35570"
  sed -i 's/^connection.listenPort=.*/connection.listenPort=35560/' "$ALPHA_DIR/config/alpha.properties" || warn "更新 connection.listenPort 失败"
  sed -i 's/^bridge.listenPort=.*/bridge.listenPort=35661/' "$ALPHA_DIR/config/alpha.properties" || warn "更新 bridge.listenPort 失败"
  sed -i 's/^minecraft.targetPort=.*/minecraft.targetPort=35570/' "$ALPHA_DIR/config/alpha.properties" || warn "更新 minecraft.targetPort 失败"
  sed -i 's/^\([[:space:]]*alphaPort[[:space:]]*=[[:space:]]*\).*/\135661/' "$MC_SERVER_DIR/config/mapbot-common.toml" || warn "更新 alphaPort 失败"
  ALPHA_HTTP_PORT=35560
fi

log "[3/6] 重新启动 Alpha + MC"
tmux new-session -d -s alpha "cd $ALPHA_DIR && ./gradlew --no-daemon run"
tmux new-session -d -s mc "cd $MC_SERVER_DIR && java -Xms2G -Xmx4G -jar neoforge-server.jar nogui"

log "[4/6] 等待注册"
timeout 180 bash -c '
  until [ -f "'"$MC_SERVER_DIR"'/logs/latest.log" ] && rg -q "\\[Bridge\\] 注册成功" "'"$MC_SERVER_DIR"'/logs/latest.log"; do
    sleep 2
  done
' || fail "回滚后 Bridge 未注册成功"

log "[5/6] API 复验"
ALPHA_TOKEN="$(curl -sS -X POST "http://127.0.0.1:${ALPHA_HTTP_PORT}/api/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"CHANGE_ME_STRONG_PASSWORD"}' | jq -r '.token // empty')"
[ -n "$ALPHA_TOKEN" ] || fail "回滚后 Alpha 登录失败"
curl -sS "http://127.0.0.1:${ALPHA_HTTP_PORT}/api/servers" -H "Authorization: Bearer $ALPHA_TOKEN" \
  | jq -e ".[] | select(.id==\"${EXPECT_SERVER_ID}\")" >/dev/null || fail "回滚后未发现 serverId=${EXPECT_SERVER_ID}"

log "[6/6] 统计耗时"
END_TS="$(date +%s)"
ELAPSED="$((END_TS-START_TS))"
echo "rollback_elapsed_sec=$ELAPSED"
[ "$ELAPSED" -le 900 ] || fail "回滚演练超时（>900s）"
if [ "$DEGRADED" -eq 1 ]; then
  echo "[PASS-DEGRADED] 最小回滚演练完成（触发降级路径，请复查告警）"
else
  echo "[PASS] 最小回滚演练完成"
fi
EOF

chmod +x /tmp/mapbot_rollback_drill.sh
BACKUP_DIR=/srv/minecraft/backup/mapbot-YYYY-MM-DD_HHMMSS bash /tmp/mapbot_rollback_drill.sh
```

自动降级处理对照表：
| 失败点 | 触发信号 | 自动降级动作 |
| --- | --- | --- |
| 备份损坏 | 关键备份文件缺失/空文件 | 进入“仅重启+复验”或“仅配置回滚”模式并打 `WARN` |
| 权限不足 | `cp` 失败 | 尝试 `sudo -n cp`，再失败则保留现状继续并标记 `DEGRADED` |
| 端口占用 | 25560/25661/25570 被非预期进程占用 | 自动切换到 35560/35661/35570 并更新对应配置 |

## 11. 强制章节：新人误操作拦截清单（防呆优先级）
| 优先级 | 拦截项 | 误操作表现 | 防呆动作（执行前检查） |
| --- | --- | --- | --- |
| P0 | 未备份先发布 | 直接覆盖 jar/配置 | `test -d "$BACKUP_DIR" || exit 1` |
| P0 | token 明文误改 | Alpha/子服 token 不一致 | 先跑一致性检查：`token: OK` 才允许重启 |
| P0 | 改错环境 | 在生产机做演练修改 | `hostname && pwd` 双确认 |
| P0 | 误杀全部 Java | `pkill -9 java` 造成全服中断 | 禁用该命令，统一用 `tmux send-keys stop` |
| P0 | 把业务端口改进保留段 | `bridge.listenPort=25561` 等 | 修改后先跑保留段校验再启动 |
| P1 | WSL 下盲用 systemd | `systemctl` 失败导致误判服务挂 | 先判定 systemd，再用 `service` 兜底 |
| P1 | 忽略日志窗口 | 只看命令返回不看日志 | 启停后固定执行 `tail + rg` 核验 |
| P1 | CRLF 配置污染 | 配置解析异常/不可见字符 | 发布前统一 `sed -i 's/\r$//'` |
| P1 | 无权限写入运行目录 | 配置无法落盘 | `stat -c '%a %U:%G' "$MC_SERVER_DIR"` |
| P2 | 忽略时钟偏差 | 跨机排障反复不收敛 | 跨机先比对 UTC 秒级偏差 |
| P2 | 把反向测试当故障 | 空服 `#list` 判故障 | 对照第 9 节反向测试基线 |

## 11.1 强制章节：不可逆操作前确认模板
> 适用操作：删档、覆盖生产配置、回滚生产、批量权限变更、手工 kill 关键进程。

模板（执行前必须填写）：
```text
[CHANGE-GUARD v1]
操作类型: ____________________
目标环境: ____________________
目标主机: ____________________
影响范围: ____________________
回退点(BACKUP_DIR): ____________________
预估中断时长: ____________________
审批人: ____________________
执行人: ____________________
确认口令: I_UNDERSTAND_<OP>_<ENV>_<YYYYMMDD>
```

防呆确认命令（示例）：
```bash
EXPECT_ACK="I_UNDERSTAND_ROLLBACK_PROD_$(date +%Y%m%d)"
read -r -p "请输入确认口令: " OP_ACK
[ "$OP_ACK" = "$EXPECT_ACK" ] || { echo "[ABORT] 确认口令不匹配"; exit 1; }
echo "[OK] 进入执行阶段"
```
