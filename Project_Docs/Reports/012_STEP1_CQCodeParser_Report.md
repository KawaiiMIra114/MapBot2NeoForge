# Task ID: #012-STEP1 CQ码解析器

## 执行时间
2026-01-18 00:15 (UTC+8)

---

## Status: ✅ SUCCESS

---

## 创建的文件

| 路径 | 操作 | 说明 |
|------|------|------|
| `utils/CQCodeParser.java` | 新建 | CQ 码解析工具类 |

---

## 实现功能

### parse(String raw)
解析 CQ 码，返回可读文本:
| CQ 类型 | 转换结果 |
|---------|----------|
| `[CQ:image,file=...]` | `[图片]` |
| `[CQ:image,summary=动画表情...]` | `[动画表情]` |
| `[CQ:reply,id=...]` | *(移除)* |
| `[CQ:at,qq=xxx]` | `@xxx` |
| `[CQ:at,qq=all]` | `@全体成员` |
| `[CQ:face,...]` | `[表情]` |
| `[CQ:record,...]` | `[语音]` |
| `[CQ:video,...]` | `[视频]` |

### extractAtTargets(String raw)
提取消息中所有被 @ 的 QQ 号，返回 `List<Long>`

### isAtTarget(String raw, long targetQQ)
检查消息是否 @ 了指定 QQ 号

---

## 技术实现

### 正则表达式
```java
Pattern CQ_PATTERN = Pattern.compile("\\[CQ:(\\w+)([^\\]]*)]");
```

### 动画表情识别
通过检查 `summary=` 参数是否包含 `动画表情` 或 `&#` (HTML 实体编码) 判断

---

## 待续

STEP 1 已完成，需在 STEP 3 中修改 `InboundHandler.java` 调用此解析器
