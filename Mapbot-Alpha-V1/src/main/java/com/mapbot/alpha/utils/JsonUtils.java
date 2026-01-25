package com.mapbot.alpha.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * JSON 工具类 - 统一使用 Gson 处理所有 JSON 操作
 */
public class JsonUtils {
    
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .create();
    
    private static final Gson PRETTY_GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    
    /**
     * 对象转 JSON 字符串
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
    
    /**
     * 对象转 JSON 字符串 (格式化)
     */
    public static String toPrettyJson(Object obj) {
        return PRETTY_GSON.toJson(obj);
    }
    
    /**
     * JSON 字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
    
    /**
     * 创建成功响应
     */
    public static String success() {
        return "{\"success\":true}";
    }
    
    /**
     * 创建成功响应带数据
     */
    public static String success(Object data) {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", true);
        obj.add("data", GSON.toJsonTree(data));
        return GSON.toJson(obj);
    }
    
    /**
     * 创建错误响应
     */
    public static String error(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("error", message);
        return GSON.toJson(obj);
    }
    
    /**
     * 创建带内容的响应
     */
    public static String content(String content) {
        JsonObject obj = new JsonObject();
        obj.addProperty("content", content);
        return GSON.toJson(obj);
    }
    
    /**
     * 获取 Gson 实例
     */
    public static Gson getGson() {
        return GSON;
    }
}
