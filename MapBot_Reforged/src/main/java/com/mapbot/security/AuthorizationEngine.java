package com.mapbot.security;

import com.mapbot.data.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 鉴权引擎 (单例)
 * 遵循《COMMAND_AUTHORIZATION_CONTRACT》全部规范条款。
 *
 * 职责:
 * 1. 角色判定 — 从 DataManager 拉取调用者的 PermissionRole
 * 2. 命令鉴权 — 根据 CommandCategory 判定 caller_role >= min_role
 * 3. Rate-Limit — 普通用户 3 秒冷却；越权请求 5 分钟 >= 5 次触发限速告警
 * 4. Audit Log — 所有鉴权事件强制写入审计日志
 */
public class AuthorizationEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Security");
    private static final Logger AUDIT = LoggerFactory.getLogger("MapBot/Audit");

    /** 单例实例 */
    public static final AuthorizationEngine INSTANCE = new AuthorizationEngine();

    // ================== Rate-Limit 配置 ==================

    /** 普通用户命令冷却时间 (毫秒) */
    private static final long COMMAND_COOLDOWN_MS = 3000L;

    /** 越权监控窗口 (毫秒): 5 分钟 */
    private static final long VIOLATION_WINDOW_MS = 5 * 60 * 1000L;

    /** 越权触发阈值: 窗口内 >= 5 次 */
    private static final int VIOLATION_THRESHOLD = 5;

    // ================== 内部状态 ==================

    /** 用户上次执行命令的时间戳 (QQ -> 时间戳) */
    private final Map<Long, Long> lastCommandTime = new ConcurrentHashMap<>();

    /** 用户越权记录 (QQ -> 越权时间戳队列) */
    private final Map<Long, ConcurrentLinkedDeque<Long>> violationRecords = new ConcurrentHashMap<>();

    /** 全局鉴权请求计数器 (用于生成 request_id) */
    private final AtomicLong requestCounter = new AtomicLong(0);

    private AuthorizationEngine() {}

    /**
     * 鉴权入口 — 判定调用者是否有权执行指定命令。
     *
     * @param senderQQ     调用者 QQ 号 (principal_id)
     * @param commandId    命令标识符
     * @param category     命令分类
     * @return 鉴权结果
     */
    public AuthResult authorize(long senderQQ, String commandId, CommandCategory category) {
        long requestId = requestCounter.incrementAndGet();
        String reqIdStr = "REQ-" + requestId;
        PermissionRole callerRole = getCallerRole(senderQQ);
        PermissionRole minRole = category.getMinRole();
        long now = System.currentTimeMillis();

        // 1. 最小权限检查
        if (!callerRole.isAtLeast(minRole)) {
            // 越权 — 记录违规
            recordViolation(senderQQ, now);
            boolean rateLimited = isRateLimited(senderQQ, now);

            // 审计日志: 拒绝
            writeAuditLog(reqIdStr, senderQQ, callerRole, commandId, category, "deny",
                    rateLimited ? "AUTH-403 + RATE-LIMITED" : "AUTH-403: 权限不足",
                    now);

            if (rateLimited) {
                LOGGER.warn("[RATE-LIMIT] 用户 {} 在 5 分钟内越权 >= {} 次，已触发速率限制告警",
                        senderQQ, VIOLATION_THRESHOLD);
            }

            return AuthResult.denied(reqIdStr, callerRole, minRole, rateLimited);
        }

        // 2. 普通用户命令冷却检查 (仅对 USER 角色生效)
        if (callerRole == PermissionRole.USER) {
            Long lastTime = lastCommandTime.get(senderQQ);
            if (lastTime != null && (now - lastTime) < COMMAND_COOLDOWN_MS) {
                long remaining = COMMAND_COOLDOWN_MS - (now - lastTime);
                writeAuditLog(reqIdStr, senderQQ, callerRole, commandId, category, "deny",
                        "RATE-LIMIT: 命令冷却中 (剩余 " + remaining + "ms)", now);
                return AuthResult.cooldown(reqIdStr, remaining);
            }
        }

        // 3. 通过 — 记录时间戳
        lastCommandTime.put(senderQQ, now);

        // 审计日志: 允许
        writeAuditLog(reqIdStr, senderQQ, callerRole, commandId, category, "allow",
                "权限充足", now);

        return AuthResult.allowed(reqIdStr, callerRole);
    }

    /**
     * 获取调用者的 PermissionRole。
     * 从 DataManager 拉取旧版整数值，通过 fromLegacyLevel 映射到新枚举。
     *
     * @param senderQQ 调用者 QQ
     * @return 对应的 PermissionRole
     */
    public PermissionRole getCallerRole(long senderQQ) {
        int legacyLevel = DataManager.INSTANCE.getPermissionLevel(senderQQ);
        return PermissionRole.fromLegacyLevel(legacyLevel);
    }

    // ================== Rate-Limit 内部方法 ==================

    /**
     * 记录一次越权违规
     */
    private void recordViolation(long senderQQ, long now) {
        violationRecords.computeIfAbsent(senderQQ, k -> new ConcurrentLinkedDeque<>()).add(now);
        // 清除窗口外的旧记录
        ConcurrentLinkedDeque<Long> records = violationRecords.get(senderQQ);
        while (!records.isEmpty() && (now - records.peekFirst()) > VIOLATION_WINDOW_MS) {
            records.pollFirst();
        }
    }

    /**
     * 判断用户是否触发了越权限速 (5分钟内 >= 5次)
     */
    private boolean isRateLimited(long senderQQ, long now) {
        ConcurrentLinkedDeque<Long> records = violationRecords.get(senderQQ);
        if (records == null) return false;
        // 再次清除窗口外的旧记录
        while (!records.isEmpty() && (now - records.peekFirst()) > VIOLATION_WINDOW_MS) {
            records.pollFirst();
        }
        return records.size() >= VIOLATION_THRESHOLD;
    }

    // ================== 审计日志 ==================

    /**
     * 写入审计事件日志。
     * 遵循《COMMAND_AUTHORIZATION_CONTRACT》5.6 审计要求，包含最少字段。
     */
    private void writeAuditLog(String requestId, long principalId, PermissionRole callerRole,
                               String commandId, CommandCategory category,
                               String decision, String reason, long eventTime) {
        // 格式化为结构化审计条目
        AUDIT.info("[AUDIT] request_id={} | event_time={} | principal_id={} | caller_role={} | " +
                        "command_id={} | command_category={} | decision={} | decision_reason={}",
                requestId,
                Instant.ofEpochMilli(eventTime).toString(),
                principalId,
                callerRole.name(),
                commandId,
                category.name(),
                decision,
                reason);
    }

    // ================== 鉴权结果 ==================

    /**
     * 鉴权结果载体
     */
    public static class AuthResult {
        private final boolean allowed;
        private final String requestId;
        private final PermissionRole callerRole;
        private final PermissionRole requiredRole;
        private final boolean rateLimited;
        private final long cooldownRemainingMs;
        private final String message;

        private AuthResult(boolean allowed, String requestId, PermissionRole callerRole,
                           PermissionRole requiredRole, boolean rateLimited,
                           long cooldownRemainingMs, String message) {
            this.allowed = allowed;
            this.requestId = requestId;
            this.callerRole = callerRole;
            this.requiredRole = requiredRole;
            this.rateLimited = rateLimited;
            this.cooldownRemainingMs = cooldownRemainingMs;
            this.message = message;
        }

        static AuthResult allowed(String requestId, PermissionRole callerRole) {
            return new AuthResult(true, requestId, callerRole, null, false, 0,
                    null);
        }

        static AuthResult denied(String requestId, PermissionRole callerRole,
                                 PermissionRole requiredRole, boolean rateLimited) {
            String msg = rateLimited
                    ? "[AUTH-403] 权限不足且已触发速率限制，请稍后再试"
                    : String.format("[AUTH-403] 此命令需要 %s 权限 (当前: %s)", requiredRole.name(), callerRole.name());
            return new AuthResult(false, requestId, callerRole, requiredRole, rateLimited, 0, msg);
        }

        static AuthResult cooldown(String requestId, long remainingMs) {
            return new AuthResult(false, requestId, PermissionRole.USER, null, false, remainingMs,
                    String.format("[冷却中] 请等待 %.1f 秒后再执行命令", remainingMs / 1000.0));
        }

        public boolean isAllowed() { return allowed; }
        public String getRequestId() { return requestId; }
        public PermissionRole getCallerRole() { return callerRole; }
        public boolean isRateLimited() { return rateLimited; }
        public String getMessage() { return message; }
    }
}
