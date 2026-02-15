# 04 Disconnect FastFail and Pending Reclaim

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-08 D1 |
| RUN_ID | 20260215T201100Z |
| 依据 | FAILURE_MODEL.md §3.4 + CRITIQUE §3.5 |

## 1. 当前问题

### 1.1 BridgeClient (Reforged)
```java
// 当前 handleDisconnect (L558):
// ...关闭 socket...

// 当前 disconnect (L638):
pendingRequests.forEach((id, future) -> future.complete(""));  // ⚠ 空字符串
pendingRequests.clear();
```
**问题**: `complete("")` 让调用方无法区分"成功但结果为空"和"因断连失败"。

### 1.2 BridgeFileProxy (Alpha)
- 断连时**不回收** pendingRequests → 请求挂起直到超时 10s → 超时雪崩。

### 1.3 BridgeProxy (Alpha)
- 断连时 `complete("")` 同上问题。
- 无结构化失败码。

## 2. 改造设计

### 2.1 统一断连失败码
| 字段 | 值 |
|---|---|
| 新增错误码 | BRG_TRANSPORT_303 |
| 含义 | FAILED_DISCONNECT (连接断开导致请求失败) |
| retryable | true (重连后可重试) |

### 2.2 BridgeClient (Reforged) 改造
```java
// 改造后 disconnect():
pendingRequests.forEach((id, future) -> {
    future.completeExceptionally(
        new BridgeDisconnectException("BRG_TRANSPORT_303")
    );
});
pendingRequests.clear();
LOGGER.warn("断连快失败: 回收 {} 个 pending 请求", count);
```

### 2.3 BridgeProxy (Alpha) 改造
```java
// 新增: 连接断开时回收 pending
public void failAllPending(String serverId) {
    int count = 0;
    Iterator<Map.Entry<String, CompletableFuture<String>>> it =
        pendingRequests.entrySet().iterator();
    while (it.hasNext()) {
        Map.Entry<String, CompletableFuture<String>> entry = it.next();
        entry.getValue().completeExceptionally(
            new BridgeDisconnectException("BRG_TRANSPORT_303")
        );
        it.remove();
        count++;
    }
    LOGGER.warn("Alpha pending 快失败: serverId={}, 回收={}", serverId, count);
}
```

### 2.4 BridgeFileProxy (Alpha) 改造
```java
// 新增: 连接断开时回收文件请求 pending
public static void failAllFilePending() {
    pendingRequests.forEach((id, future) -> {
        future.completeExceptionally(
            new BridgeDisconnectException("BRG_TRANSPORT_303: file request")
        );
    });
    int count = pendingRequests.size();
    pendingRequests.clear();
    LOGGER.warn("文件请求 pending 快失败: 回收={}", count);
}
```

## 3. 状态迁移

### 3.1 正常流程
```
PENDING → [收到回执] → SUCCESS/FAILED
```

### 3.2 断连流程 (改造后)
```
PENDING → [断连事件] → FAILED_DISCONNECT (立即)
         → [重连后] → 可重试 (retryable=true)
```

### 3.3 超时流程 (不变)
```
PENDING → [超时] → FAILED_TIMEOUT (BRG_TIMEOUT_501)
```

## 4. 清理策略
| 类型 | 时机 | 动作 |
|---|---|---|
| 断连回收 | handleDisconnect 事件 | completeExceptionally 全部 pending |
| 超时回收 | CompletableFuture.get(timeout) | TimeoutException |
| 定时清理 | 60s 间隔 | 移除已完成/已失败的条目 |

## 5. 日志审计
| 事件 | 日志级别 | 内容 |
|---|---|---|
| 断连回收 | WARN | `pending 快失败: serverId={}, 回收={}` |
| 单条失败 | DEBUG | `request failed_disconnect: id={}, action={}` |
| 回收统计 | INFO | `断连后 pending 归零: serverId={}` |

## 6. 代码改造点
| 文件 | 改造 |
|---|---|
| BridgeClient (Reforged) | complete("") → completeExceptionally |
| BridgeProxy (Alpha) | 新增 failAllPending(serverId) |
| BridgeFileProxy (Alpha) | 新增 failAllFilePending() |
| BridgeServer (Alpha) | 连接断开事件调用 failAllPending |
| BridgeErrorMapper (Alpha+Reforged) | 新增 BRG_TRANSPORT_303 |
| 新增: BridgeDisconnectException.java | 断连异常类 |
