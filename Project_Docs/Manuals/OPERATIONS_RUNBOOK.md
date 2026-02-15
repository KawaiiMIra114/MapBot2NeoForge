# OPERATIONS_RUNBOOK

## 0. 统一元数据
| 字段 | 值 |
| --- | --- |
| 文档标识 | `A5-OPERATIONS-RUNBOOK` |
| 子任务 | `A5（运维手册-部署/日常）` |
| 目标系统 | `Ubuntu 22.04+ / WSL2(Ubuntu)` |
| Java 基线 | `OpenJDK 21` |
| 部署架构 | `Mapbot-Alpha-V1 + MapBot_Reforged(NeoForge)` |
| 维护角色 | `运维值班/发布执行人` |
| 最后更新 | `2026-02-14` |
| 关联配置 | `Mapbot-Alpha-V1/config/alpha.properties`, `config/mapbot-common.toml` |
| 关联日志 | `Mapbot-Alpha-V1/logs/alpha.log`, `logs/latest.log` |

## 0.1 强制章节：统一失败判据阈值表（全局）
> 本表与 `DEPLOYMENT_RUNBOOK` 保持一致；运维与发布统一使用同一失败标准。

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
- 本手册覆盖日常值班、健康巡检、`#reload` 验证、端口冲突与 Bridge 鉴权排障。
- 默认进程由 `tmux` 托管：`alpha`（Alpha Core）与 `mc`（Minecraft）。

```bash
export REPO=/mnt/d/axm/mcs/MapBot2NeoForge
export ALPHA_DIR="$REPO/Mapbot-Alpha-V1"
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

## 2. 强制章节：常用操作
1. 查看运行会话与进程。

```bash
tmux ls | rg 'alpha|mc'
ps -ef | rg 'MapbotAlpha|neoforge|java' | rg -v rg
```

2. 启动/停止/重启服务（按顺序执行）。

```bash
# 启动
tmux new-session -d -s alpha "cd $ALPHA_DIR && ./gradlew --no-daemon run"
tmux new-session -d -s mc "cd $MC_SERVER_DIR && java -Xms2G -Xmx4G -jar neoforge-server.jar nogui"

# 停止
tmux send-keys -t mc 'stop' C-m
sleep 10
tmux send-keys -t alpha 'stop' C-m

# 重启
tmux kill-session -t mc 2>/dev/null || true
tmux kill-session -t alpha 2>/dev/null || true
tmux new-session -d -s alpha "cd $ALPHA_DIR && ./gradlew --no-daemon run"
tmux new-session -d -s mc "cd $MC_SERVER_DIR && java -Xms2G -Xmx4G -jar neoforge-server.jar nogui"
```

3. 实时看日志（值班常用）。

```bash
tail -Fn 200 "$ALPHA_DIR/logs/alpha.log"
tail -Fn 200 "$MC_SERVER_DIR/logs/latest.log"
```

4. QQ 常用运维命令（在管理群执行）。
- `#status` / `#list`：状态与在线玩家。
- `#reload`：热重载配置（见第 4 节验证）。
- `#stopserver [秒数]` / `#cancelstop`：停服控制。

## 3. 强制章节：健康检查
1. 端口级健康检查。

```bash
ss -ltnp | rg ':(25560|25661|25570|7000|3000)\b'
```

2. Alpha API 健康检查（推荐每班次至少一次）。

```bash
ALPHA_TOKEN="$(curl -sS -X POST 'http://127.0.0.1:25560/api/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"CHANGE_ME_STRONG_PASSWORD"}' | jq -r '.token // empty')"
curl -sS 'http://127.0.0.1:25560/api/status' -H "Authorization: Bearer $ALPHA_TOKEN" | jq
curl -sS 'http://127.0.0.1:25560/api/servers' -H "Authorization: Bearer $ALPHA_TOKEN" | jq
```

3. 日志关键字健康检查（成功态应命中至少一条）。

```bash
tail -n 200 "$ALPHA_DIR/logs/alpha.log" | rg 'OneBot 连接成功|Bridge 服务器已启动|Bridge 注册鉴权通过|服务器已注册'
tail -n 200 "$MC_SERVER_DIR/logs/latest.log" | rg '\[Bridge\] 已连接到 Alpha Core|\[Bridge\] 注册成功'
```

4. 失败态关键字快速筛查。

```bash
tail -n 200 "$ALPHA_DIR/logs/alpha.log" | rg '拒绝未授权 Bridge 注册|Bridge 鉴权未启用|请求超时|ERROR|WARN'
tail -n 200 "$MC_SERVER_DIR/logs/latest.log" | rg '注册被 Alpha 拒绝|alphaToken 未配置|连接/会话异常|ERROR|WARN'
```

## 4. 强制章节：`#reload` 行为验证
1. 预期行为（Alpha 中枢模式）：
- Alpha 执行 `AlphaConfig.reload + AuthManager.reloadSecurityConfig`。
- Alpha 通过 Bridge 下发 `reload_config` 到子服。
- Reforged 子服执行 `DataManager.init + LootConfig.init`。

2. 验证矩阵（按对象拆分）。
- 矩阵 A：Alpha 配置重载生效（以 `connection.reconnectInterval` 为例）

```bash
cp "$ALPHA_DIR/config/alpha.properties" "$ALPHA_DIR/config/alpha.properties.bak.reloadA"
sed -i 's/^connection.reconnectInterval=.*/connection.reconnectInterval=9/' "$ALPHA_DIR/config/alpha.properties"
# 在管理群发送: #reload

ALPHA_TOKEN="$(curl -sS -X POST 'http://127.0.0.1:25560/api/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"CHANGE_ME_STRONG_PASSWORD"}' | jq -r '.token // empty')"
curl -sS 'http://127.0.0.1:25560/api/config' -H "Authorization: Bearer $ALPHA_TOKEN" | jq '.reconnectInterval'
# 期望输出: 9
```

- 矩阵 B：Bridge 鉴权重载生效（`allowedServerIds`）

```bash
cp "$ALPHA_DIR/config/alpha.properties" "$ALPHA_DIR/config/alpha.properties.bak.reloadB"
sed -i 's/^auth.bridge.allowedServerIds=.*/auth.bridge.allowedServerIds=deny_all_temp/' "$ALPHA_DIR/config/alpha.properties"
# 在管理群发送: #reload

# 强制子服重连触发重新注册
tmux send-keys -t mc 'stop' C-m
sleep 10
tmux kill-session -t mc 2>/dev/null || true
tmux new-session -d -s mc "cd $MC_SERVER_DIR && java -Xms2G -Xmx4G -jar neoforge-server.jar nogui"

tail -n 200 "$ALPHA_DIR/logs/alpha.log" | rg '拒绝未授权 Bridge 注册|unauthorized'
# 期望: 命中 unauthorized（说明新鉴权规则已生效）
```

- 矩阵 C：子服 mapbot 配置重载生效（`mapbot_loot.json`）

```bash
cp "$MC_SERVER_DIR/config/mapbot_loot.json" "$MC_SERVER_DIR/config/mapbot_loot.json.bak.reloadC"
jq '.entries = [ .entries[0] ]' "$MC_SERVER_DIR/config/mapbot_loot.json.bak.reloadC" > "$MC_SERVER_DIR/config/mapbot_loot.json"
# 在管理群发送: #reload

tail -n 200 "$MC_SERVER_DIR/logs/latest.log" | rg '已加载奖池: 1 个稀有度组'
# 期望: 命中该日志（说明 LootConfig 读取了新文件内容）
```

3. 收口与回退（矩阵执行后必须恢复）。

```bash
mv -f "$ALPHA_DIR/config/alpha.properties.bak.reloadA" "$ALPHA_DIR/config/alpha.properties" 2>/dev/null || true
mv -f "$ALPHA_DIR/config/alpha.properties.bak.reloadB" "$ALPHA_DIR/config/alpha.properties" 2>/dev/null || true
mv -f "$MC_SERVER_DIR/config/mapbot_loot.json.bak.reloadC" "$MC_SERVER_DIR/config/mapbot_loot.json" 2>/dev/null || true
# 在管理群再次发送: #reload
```

4. 操作步骤（常规验收）。
- 在管理群发送：`#reload`。
- 期望群回复包含：
  - `[成功] Alpha 配置已重新加载`
  - `Bridge鉴权: 已启用...`
  - `[子服重载] 成功 x/y`

5. 日志验收（必须同时通过 Alpha 与 Reforged）。

```bash
tail -n 200 "$ALPHA_DIR/logs/alpha.log" | rg '安全配置已热重载|子服重载|reload'
tail -n 200 "$MC_SERVER_DIR/logs/latest.log" | rg '数据文件路径|已加载奖池|reload'
```

6. 单服直连模式补充：
- 由 Reforged 直接处理 `#reload`，预期回复：
  - `[系统] 配置和数据已重载 (DataManager + LootConfig)`

7. 负向用例（应失败且失败正确，至少执行 5 个）。

| 用例ID | 注入方式 | 操作 | 预期失败结果 |
| --- | --- | --- | --- |
| N01 | 非管理员账号 | 在群发送 `#reload` | 返回权限拒绝（如“需要管理员权限”） |
| N02 | 子服离线 | 停止 `mc` 会话后发送 `#reload` | 返回 `[子服重载] 无在线服务器` |
| N03 | `allowedServerIds` 排除当前 `serverId` | 改 Alpha 配置并 `#reload`，再重启子服 | Alpha 日志命中 `unauthorized` |
| N04 | Alpha `auth.bridge.token` 与子服 `alphaToken` 故意不一致 | 改配置并 `#reload`，再重启子服 | Alpha 日志命中 `unauthorized`，子服注册失败 |
| N05 | 子服 `alphaToken` 置空 | 改 `mapbot-common.toml` 后重启子服 | 子服日志命中 `alphaToken 未配置`，桥接不可用 |
| N06 | 跨机时钟偏差超阈值（模拟） | 人为制造 >120s 时差后重连 | 判定为失败，先修时钟再重试 |

```bash
# N02: 子服离线场景
tmux send-keys -t mc 'stop' C-m
sleep 10
tmux kill-session -t mc 2>/dev/null || true
# 管理群发送 #reload，期望: [子服重载] 无在线服务器

# N03: allowedServerIds 排除
cp "$ALPHA_DIR/config/alpha.properties" "$ALPHA_DIR/config/alpha.properties.bak.N03"
sed -i 's/^auth.bridge.allowedServerIds=.*/auth.bridge.allowedServerIds=deny_all_temp/' "$ALPHA_DIR/config/alpha.properties"
# 管理群发送 #reload 后重启 mc，再看日志:
tail -n 200 "$ALPHA_DIR/logs/alpha.log" | rg 'unauthorized|拒绝未授权 Bridge 注册'

# N04: token 不一致
cp "$ALPHA_DIR/config/alpha.properties" "$ALPHA_DIR/config/alpha.properties.bak.N04"
sed -i 's/^auth.bridge.token=.*/auth.bridge.token=INTENTIONAL_MISMATCH/' "$ALPHA_DIR/config/alpha.properties"
# 管理群发送 #reload 后重启 mc，再看日志:
tail -n 200 "$ALPHA_DIR/logs/alpha.log" | rg 'unauthorized|拒绝未授权 Bridge 注册'

# N05: 子服 alphaToken 置空
cp "$MC_SERVER_DIR/config/mapbot-common.toml" "$MC_SERVER_DIR/config/mapbot-common.toml.bak.N05"
sed -i 's/^\(\s*alphaToken\s*=\s*\)\".*\"/\1\"\"/' "$MC_SERVER_DIR/config/mapbot-common.toml"
tmux kill-session -t mc 2>/dev/null || true
tmux new-session -d -s mc "cd $MC_SERVER_DIR && java -Xms2G -Xmx4G -jar neoforge-server.jar nogui"
tail -n 200 "$MC_SERVER_DIR/logs/latest.log" | rg 'alphaToken 未配置'
```

8. 假阴性/假阳性样例与降低方法。

| 类型 | 场景 | 误判原因 | 降低方法 |
| --- | --- | --- | --- |
| 假阴性 FN1 | 实际失败，但被判通过 | `rg` 命中历史成功日志（非本次 `#reload`） | 使用“时间标记线”后仅扫描标记之后日志 |
| 假阴性 FN2 | 实际鉴权未生效，但被判通过 | 改了 `allowedServerIds` 但未触发子服重连，未出现 `unauthorized` | 强制重连子服再判定（`stop + restart mc`） |
| 假阳性 FP1 | 实际通过，但被判失败 | 日志轮转后 grep 只查当前文件 | 同时查 `alpha.log` 与当日日志切片，或以命令回包为主 |
| 假阳性 FP2 | 实际通过，但被判失败 | API 短时超时/抖动导致单次请求失败 | 增加 3 次重试与退避（2s/4s/8s） |

防误判命令模板：
```bash
# 1) 设定本次测试标记（防 FN1）
MARKER="reload_test_$(date +%s)"
echo "$MARKER" >> "$ALPHA_DIR/logs/alpha.log"

# 2) 执行 #reload 后仅扫描标记之后
awk -v m="$MARKER" 'f{print} $0~m{f=1}' "$ALPHA_DIR/logs/alpha.log" | rg '安全配置已热重载|子服重载|unauthorized'

# 3) API 重试（防 FP2）
for t in 2 4 8; do
  if curl -sS 'http://127.0.0.1:25560/api/status' -H "Authorization: Bearer $ALPHA_TOKEN" | jq . >/dev/null; then
    echo "api_status_ok"; break
  fi
  sleep "$t"
done
```

## 5. 强制章节：端口冲突排障决策树
1. 入口：发现端口占用。

```bash
ss -ltnp | rg ':(25560|25561|25562|25563|25564|25565|25566|25661|25570|7000|3000)\b' || true
for p in 25560 25661 25570 7000 3000; do
  echo "==== PORT $p ===="
  fuser -n tcp "$p" 2>/dev/null || true
done
```

2. 决策树（发现占用 -> 判断保留端口 -> 迁移与复验）。

```text
[发现占用]
  |
  +-- 端口是否在 25560-25566 ?
       |
       +-- 是:
       |    |
       |    +-- 端口=25560 且占用者=Alpha(Netty) -> 这是预期，不算故障
       |    |
       |    +-- 其他情况 -> 判定为错误占用，迁移配置到非保留段（如 25661/25570）
       |
       +-- 否:
            |
            +-- 占用者是否为预期 MapBot 进程且与配置一致?
            |    |
            |    +-- 是 -> 正常占用，不算故障
            |    +-- 否 -> 停止冲突进程或迁移 MapBot 端口
            |
            +-- 修改后重启 Alpha/MC 并复验 ss + 日志
```

3. 迁移与复验命令模板。

```bash
# 迁移示例：把 Alpha Bridge/MC 目标端口固定为非保留段
sed -i 's/^bridge.listenPort=.*/bridge.listenPort=25661/' "$ALPHA_DIR/config/alpha.properties"
sed -i 's/^minecraft.targetPort=.*/minecraft.targetPort=25570/' "$ALPHA_DIR/config/alpha.properties"

tmux kill-session -t alpha 2>/dev/null || true
tmux kill-session -t mc 2>/dev/null || true
tmux new-session -d -s alpha "cd $ALPHA_DIR && ./gradlew --no-daemon run"
tmux new-session -d -s mc "cd $MC_SERVER_DIR && java -Xms2G -Xmx4G -jar neoforge-server.jar nogui"

ss -ltnp | rg ':(25560|25661|25570)\b'
tail -n 200 "$ALPHA_DIR/logs/alpha.log" | rg 'Bridge 服务器已启动|服务器已注册|拒绝未授权 Bridge 注册'
```

## 6. 强制章节：Bridge `unauthorized` 最小排障闭环
1. 闭环步骤（必须按顺序）。
- Step A：核对 token 一致。
- Step B：核对 `serverId` 是否在 `allowedServerIds`。
- Step C：核对时钟偏差（跨机 >120 秒视为异常）。
- Step D：核对网络连通（MC -> Alpha Bridge 端口）。
- Step E：重载 + 重连 + 日志复验，形成闭环结论。

2. 一次执行命令（A/B/D）。

```bash
tail -n 200 "$ALPHA_DIR/logs/alpha.log" | rg 'Bridge 注册鉴权通过|拒绝未授权 Bridge 注册|unauthorized|register_required'
tail -n 200 "$MC_SERVER_DIR/logs/latest.log" | rg '\[Bridge\] 注册成功|\[Bridge\] 注册被 Alpha 拒绝|alphaToken 未配置'
ALPHA_TOKEN="$(grep '^auth.bridge.token=' "$ALPHA_DIR/config/alpha.properties" | cut -d= -f2- | tr -d '\r[:space:]')"
ALLOWED="$(grep '^auth.bridge.allowedServerIds=' "$ALPHA_DIR/config/alpha.properties" | cut -d= -f2- | tr -d '\r[:space:]')"
MC_TOKEN="$(sed -n 's/^\s*alphaToken\s*=\s*\"\(.*\)\"/\1/p' "$MC_SERVER_DIR/config/mapbot-common.toml" | head -n1 | tr -d '\r[:space:]')"
MC_SERVER_ID="$(sed -n 's/^\s*serverId\s*=\s*\"\(.*\)\"/\1/p' "$MC_SERVER_DIR/config/mapbot-common.toml" | head -n1 | tr -d '\r[:space:]')"

[ "$ALPHA_TOKEN" = "$MC_TOKEN" ] && echo "token: OK" || echo "token: MISMATCH"
echo ",$ALLOWED," | rg ",$MC_SERVER_ID," >/dev/null && echo "serverId: ALLOWED" || echo "serverId: NOT_ALLOWED"
timeout 2 bash -c 'cat < /dev/null > /dev/tcp/127.0.0.1/25661' && echo "bridge-port: reachable" || echo "bridge-port: unreachable"
```

3. 时钟偏差检查（Step C）。

```bash
# 单机/WSL 同机场景（最常见）
date -u '+UTC %F %T'

# 跨机场景示例（替换主机名）
ALPHA_TS="$(ssh alpha-host 'date -u +%s')"
MC_TS="$(ssh mc-host 'date -u +%s')"
echo "clock_diff_sec=$(( ALPHA_TS - MC_TS ))"
# 若绝对值 > 120，先修时钟再继续
```

4. 修复与复验（Step E）。

```bash
# 示例修复：token/serverId/allowedServerIds
MC_TOKEN="$(sed -n 's/^\s*alphaToken\s*=\s*\"\(.*\)\"/\1/p' "$MC_SERVER_DIR/config/mapbot-common.toml" | head -n1 | tr -d '\r[:space:]')"
MC_SERVER_ID="$(sed -n 's/^\s*serverId\s*=\s*\"\(.*\)\"/\1/p' "$MC_SERVER_DIR/config/mapbot-common.toml" | head -n1 | tr -d '\r[:space:]')"
sed -i "s/^auth.bridge.allowedServerIds=.*/auth.bridge.allowedServerIds=${MC_SERVER_ID}/" "$ALPHA_DIR/config/alpha.properties"
sed -i "s/^auth.bridge.token=.*/auth.bridge.token=${MC_TOKEN}/" "$ALPHA_DIR/config/alpha.properties"
# 在管理群发送: #reload

# 触发重连
tmux send-keys -t mc 'stop' C-m
sleep 10
tmux kill-session -t mc 2>/dev/null || true
tmux new-session -d -s mc "cd $MC_SERVER_DIR && java -Xms2G -Xmx4G -jar neoforge-server.jar nogui"

# 闭环验收：必须看到 success，不应再有 unauthorized
tail -n 200 "$ALPHA_DIR/logs/alpha.log" | rg 'Bridge 注册鉴权通过|服务器已注册'
tail -n 200 "$MC_SERVER_DIR/logs/latest.log" | rg '\[Bridge\] 注册成功'
```

## 7. 强制章节：反向测试（避免误报）
场景：当前无子服在线时执行 `#reload`，返回“无在线服务器”是预期，不应误判为故障。

```bash
# 1) 停掉子服，仅保留 Alpha 在线
tmux send-keys -t mc 'stop' C-m
sleep 10
tmux kill-session -t mc 2>/dev/null || true

# 2) 在管理群发送 #reload
# 期望包含: [子服重载] 无在线服务器

# 3) 复验 Alpha 仍健康
ALPHA_TOKEN="$(curl -sS -X POST 'http://127.0.0.1:25560/api/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"CHANGE_ME_STRONG_PASSWORD"}' | jq -r '.token // empty')"
curl -sS 'http://127.0.0.1:25560/api/status' -H "Authorization: Bearer $ALPHA_TOKEN" | jq
```
