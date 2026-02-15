# 03 最小证据集模板 (Minimum Evidence Bundle Template)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-14 |
| Artifact | 03/05 |
| RUN_ID | 20260215T220800Z |

## 证据集字段定义

### 必备字段
| 字段 | 类型 | 来源 | 说明 |
|---|---|---|---|
| incident_id | String | 自动生成 | 唯一事件 ID (UUID) |
| timestamp_start | ISO8601 | 告警触发时间 | 事件开始 |
| timestamp_end | ISO8601 | 告警恢复时间 | 事件结束 |
| severity | Enum | 告警级别 | S1/S2/S3/S4 |
| alert_name | String | 告警规则名 | 触发的规则 |
| threshold | String | 告警阈值 | 规则阈值 |
| actual_value | String | 实际值 | 触发时的指标值 |
| affected_module | String | 模块名 | 受影响模块 |
| request_id_sample | String[] | 最近 10 个 requestId | 故障期间的请求样本 |
| policy_version | String | AuthManager | 当前策略版本 |
| schema_version | String | DataManager | 数据 schema 版本 |
| snapshot_version | String | ConfigManager | 配置快照版本 |

### 指标快照 (15 分钟窗口)
| 指标 | 粒度 | 说明 |
|---|---|---|
| auth_decision_total | 1分钟桶 | 15 个数据点 |
| auth_decision_latency_ms | P50/P95/P99 | 15 个数据点 |
| bridge_request_total | 1分钟桶 | 15 个数据点 |
| active_connections | 1分钟采样 | 15 个数据点 |
| consistency_conflict_total | 1分钟桶 | 15 个数据点 |

## 证据路径规范
```
Project_Docs/incidents/
  {YYYYMMDD}/
    {incident_id}/
      incident_meta.json       # 元数据 (上述字段)
      metrics_snapshot.json    # 15分钟指标快照
      logs_window.txt          # 告警前后 15 分钟日志
      rca_notes.md             # RCA 记录 (手动填写)
      resolution.md            # 解决方案
```

## 采集脚本接口
```java
// IncidentEvidence.java (设计)
public class IncidentEvidence {
    String incidentId;
    Instant timestampStart;
    String severity;
    String alertName;
    Map<String, Object> metricsSnapshot;
    List<String> requestIdSamples;
    String policyVersion;
    String schemaVersion;

    public void collect() { /* 自动采集 */ }
    public void save(Path dir) { /* 持久化 */ }
}
```

## 检索方式
1. 按 incident_id 直接定位目录
2. 按日期 + severity 筛选
3. 按 affected_module 搜索

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| EVD-01 | 无 IncidentEvidence 实现 | High |
| EVD-02 | 无 requestId 样本采集 | High |
| EVD-03 | 无版本字段自动采集 | Medium |
| EVD-04 | 无 incidents 目录规范实现 | Medium |
