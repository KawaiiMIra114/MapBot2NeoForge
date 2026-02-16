package com.mapbot.network;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Bridge 错误映射器（Reforged 侧）
 * 优先级：结构化错误码 > 字符串映射 > BRG_INTERNAL_999
 */
final class BridgeErrorMapper {
    static final int FRAME_MAX_BYTES = 64 * 1024;
    static final int BASE64_RAW_MAX_BYTES = 46 * 1024;

    static final String BRG_AUTH_101 = "BRG_AUTH_101";
    static final String BRG_AUTH_102 = "BRG_AUTH_102";
    static final String BRG_VALIDATION_201 = "BRG_VALIDATION_201";
    static final String BRG_VALIDATION_202 = "BRG_VALIDATION_202";
    static final String BRG_VALIDATION_203 = "BRG_VALIDATION_203";
    static final String BRG_VALIDATION_204 = "BRG_VALIDATION_204";
    static final String BRG_VALIDATION_205 = "BRG_VALIDATION_205";
    static final String BRG_TRANSPORT_301 = "BRG_TRANSPORT_301";
    static final String BRG_TRANSPORT_302 = "BRG_TRANSPORT_302";
    static final String BRG_EXECUTION_401 = "BRG_EXECUTION_401";
    static final String BRG_EXECUTION_402 = "BRG_EXECUTION_402";
    static final String BRG_TIMEOUT_501 = "BRG_TIMEOUT_501";
    static final String BRG_TIMEOUT_502 = "BRG_TIMEOUT_502";
    static final String BRG_INTERNAL_999 = "BRG_INTERNAL_999";

    private static final Pattern CODE_PATTERN = Pattern.compile(
        "^BRG_(AUTH|VALIDATION|TRANSPORT|EXECUTION|TIMEOUT|INTERNAL)_[0-9]{3}$"
    );

    private BridgeErrorMapper() {
    }

    static ErrorMeta map(String explicitErrorCode, String rawError, boolean retryableDefault) {
        String normalizedRaw = normalizeRaw(rawError);
        String fromRaw = mapRawToCode(normalizedRaw);
        String code;
        boolean mappingConflict = false;
        if (isValidCode(explicitErrorCode)) {
            code = explicitErrorCode;
            if (fromRaw != null && !fromRaw.equals(code)) {
                mappingConflict = true;
            }
        } else if (fromRaw != null) {
            code = fromRaw;
        } else {
            code = BRG_INTERNAL_999;
        }
        boolean retryable = isRetryableCode(code, retryableDefault);
        return new ErrorMeta(code, normalizedRaw, retryable, mappingConflict);
    }

    static boolean isFrameTooLarge(String jsonWithoutNewline) {
        if (jsonWithoutNewline == null) return false;
        int bytes = jsonWithoutNewline.getBytes(StandardCharsets.UTF_8).length + 1;
        return bytes > FRAME_MAX_BYTES;
    }

    static boolean looksLikeError(String text) {
        if (text == null || text.isBlank()) return true;
        String s = text.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("fail:")) return true;
        if (s.contains("[错误]") || s.contains("错误")) return true;
        if (s.contains("timeout") || s.contains("超时")) return true;
        if (s.contains("unauthorized")) return true;
        return false;
    }

    private static String normalizeRaw(String rawError) {
        if (rawError == null || rawError.isBlank()) return "unknown_error";
        return rawError.trim();
    }

    private static boolean isValidCode(String code) {
        if (code == null || code.isBlank()) return false;
        return CODE_PATTERN.matcher(code.trim()).matches();
    }

    private static boolean isRetryableCode(String code, boolean defaultRetryable) {
        if (code == null) return defaultRetryable;
        if (code.startsWith("BRG_TIMEOUT_")) return true;
        if (code.startsWith("BRG_TRANSPORT_")) return true;
        return defaultRetryable;
    }

    private static String mapRawToCode(String rawError) {
        if (rawError == null || rawError.isBlank()) return null;
        String s = rawError.toLowerCase(Locale.ROOT);

        if (s.contains("unauthorized")) return BRG_AUTH_101;
        if (s.contains("path outside server directory") || s.contains("mutation whitelist")) return BRG_AUTH_102;

        if (s.contains("register_required")) return BRG_VALIDATION_201;
        if (s.contains("invalid_json") || s.contains("empty_message")) return BRG_VALIDATION_202;
        if (s.contains("玩家名为空") || s.contains("目标服务器名为空") || s.contains("目标转移地址为空")) return BRG_VALIDATION_203;
        if (s.contains("玩家名格式非法")) return BRG_VALIDATION_204;
        if (s.contains("size_limit_exceeded") || s.contains("frame_too_large")
            || s.contains("base64_raw_size_exceeded")
            || s.contains("payload too large") || s.contains("file too large")) return BRG_VALIDATION_205;

        if (s.contains("server not connected") || s.contains("服务器离线")) return BRG_TRANSPORT_301;
        if (s.contains("bridge 未连接") || s.contains("服务器未就绪")) return BRG_TRANSPORT_302;

        if (s.contains("跨服执行超时")) return BRG_TIMEOUT_502;
        if (s.contains("timeout") || s.contains("超时")) return BRG_TIMEOUT_501;

        if (s.contains("命令执行失败") || s.contains("指令执行失败")) return BRG_EXECUTION_401;
        if (s.contains("fail:offline") || s.contains("未知物品")) return BRG_EXECUTION_402;

        return null;
    }

    static final class ErrorMeta {
        final String errorCode;
        final String rawError;
        final boolean retryable;
        final boolean mappingConflict;

        private ErrorMeta(String errorCode, String rawError, boolean retryable, boolean mappingConflict) {
            this.errorCode = errorCode;
            this.rawError = rawError;
            this.retryable = retryable;
            this.mappingConflict = mappingConflict;
        }
    }
}
