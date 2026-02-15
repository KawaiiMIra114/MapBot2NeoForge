# Step-02 A2 Baseline Config Snapshots

## CFG-001 Alpha 主配置快照（alpha.properties）
- 采样口径：Alpha 主配置文件的行数、大小、哈希、关键键存在性静态快照。
- 采样命令/查询：
```bash
wc -l Mapbot-Alpha-V1/config/alpha.properties
sha256sum Mapbot-Alpha-V1/config/alpha.properties
stat -c 'size=%s mtime=%y mode=%a' Mapbot-Alpha-V1/config/alpha.properties
rg -n 'auth.bridge.allowedServerIds|auth.bridge.token|messaging.adminGroupId|messaging.playerGroupId|redis.enabled' Mapbot-Alpha-V1/config/alpha.properties
```
- 样本数：1
- 结果值（含分位数）：
  - `lines=21`
  - `size=660 bytes`
  - `sha256=b6e1be2c9bdcbed9b0eef22fda22b9105daab25679b093d6a1f1d932e5691ec1`
  - `key_presence=5/5`
  - 分位数：`p50=p95=min=max=单样本值`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_config_snapshots.log`
- 来源依据：`Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`，`Project_Docs/Architecture/SECURITY_BOUNDARY.md`

## CFG-002 子服通用配置快照（mapbot-common.toml）
- 采样口径：Reforged 侧通用配置行数、大小、哈希、关键键存在性静态快照。
- 采样命令/查询：
```bash
wc -l 1/mapbot-common.toml
sha256sum 1/mapbot-common.toml
stat -c 'size=%s mtime=%y mode=%a' 1/mapbot-common.toml
rg -n 'playerGroupId|adminGroupId|serverId|alphaHost|alphaPort' 1/mapbot-common.toml
```
- 样本数：1
- 结果值（含分位数）：
  - `lines=43`
  - `size=1269 bytes`
  - `sha256=da11c3ae3efe4ae04a5fdc0134d2ca934eb8b69e7e1a02a14cba22e01288a764`
  - `key_presence=5/5`
  - 分位数：`p50=p95=min=max=单样本值`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_config_snapshots.log`
- 来源依据：`Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`，`Project_Docs/Architecture/SYSTEM_CONTEXT.md`

## CFG-003 掉落配置快照（mapbot_loot.json）
- 采样口径：掉落规则配置完整性（行数、大小、哈希）静态快照。
- 采样命令/查询：
```bash
wc -l 1/mapbot_loot.json
sha256sum 1/mapbot_loot.json
stat -c 'size=%s mtime=%y mode=%a' 1/mapbot_loot.json
```
- 样本数：1
- 结果值（含分位数）：
  - `lines=225`
  - `size=5287 bytes`
  - `sha256=cb16046d6a6505e1050eb39169f14b1d6728a97b1fa1acc8edcad8c152aded2e`
  - 分位数：`p50=p95=min=max=单样本值`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_config_snapshots.log`
- 来源依据：`Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`

## CFG-004 数据配置快照（mapbot_data.json）
- 采样口径：数据映射配置完整性（行数、大小、哈希）静态快照。
- 采样命令/查询：
```bash
wc -l 1/mapbot_data.json
sha256sum 1/mapbot_data.json
stat -c 'size=%s mtime=%y mode=%a' 1/mapbot_data.json
```
- 样本数：1
- 结果值（含分位数）：
  - `lines=42`
  - `size=1044 bytes`
  - `sha256=b392ddb8e395fd8058c5cea84ee45cb2d50816e75a265a8f90c08594a56fa6aa`
  - 分位数：`p50=p95=min=max=单样本值`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_config_snapshots.log`
- 来源依据：`Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`

## CFG-005 配置规模聚合分位
- 采样口径：四个配置文件的行数与体积分位统计，作为后续“异常膨胀/异常缩减”比较基线。
- 采样命令/查询：
```bash
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/config_line_samples.txt
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/config_size_samples_bytes.txt
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_config_quantiles.log
```
- 样本数：4（配置文件）
- 结果值（含分位数）：
  - `config_lines: count=4 p50=42 p90=225 p95=225 min=21 max=225`
  - `config_size_bytes: count=4 p50=1044 p90=5287 p95=5287 min=660 max=5287`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_config_quantiles.log`
- 来源依据：`Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`，`Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`

## 敏感字段处理声明
- `auth.bridge.token` 等敏感值在 Step02 文档层不回填明文；仅记录“键存在性 + 原始证据路径”。
- 明文仅存在于受控采样证据文件，后续 B 阶段如需共享，必须先脱敏。
