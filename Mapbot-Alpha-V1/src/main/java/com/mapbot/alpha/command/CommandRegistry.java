package com.mapbot.alpha.command;

import com.mapbot.alpha.config.AlphaConfig;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.network.OneBotClient;
import com.mapbot.alpha.security.ContractRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 命令注册中心
 * 从 Reforged 移植到 Alpha Core
 */
public class CommandRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Command");
    
    private static final Map<String, ICommand> commands = new HashMap<>();
    private static final Map<String, String> aliases = new HashMap<>();
    
    /**
     * 注册命令
     */
    public static void register(String name, ICommand cmd) {
        commands.put(name.toLowerCase(), cmd);
        LOGGER.debug("注册命令: #{}", name);
    }
    
    /**
     * 注册别名
     */
    public static void registerAlias(String alias, String target) {
        aliases.put(alias.toLowerCase(), target.toLowerCase());
    }
    
    /**
     * 分发命令
     * @return true 如果命令存在并被处理
     */
    public static boolean dispatch(String cmdName, String args, long senderQQ, long sourceGroupId, boolean privateChat) {
        String name = cmdName.toLowerCase();

        if (aliases.containsKey(name)) {
            name = aliases.get(name);
        }

        ICommand cmd = commands.get(name);
        if (cmd == null) {
            return false;
        }

        ContractRole senderRole = QqRoleResolver.resolveRole(senderQQ);

        if ("addadmin".equals(name) && DataManager.INSTANCE.getAdmins().isEmpty()) {
            try {
                String result = cmd.execute(args, senderQQ, sourceGroupId);
                if (result != null && !result.isEmpty()) {
                    sendReply(sourceGroupId, senderQQ, privateChat, result);
                }
            } catch (Exception e) {
                LOGGER.error("命令执行异常: #{} {}", name, args, e);
                sendReply(sourceGroupId, senderQQ, privateChat, "[错误] 命令执行失败: " + e.getMessage());
            }
            return true;
        }

        if (cmd.adminGroupOnly() && sourceGroupId != AlphaConfig.getAdminGroupId()) {
            // 私聊仅允许 admin/owner 绕过“仅限管理群”限制
            if (!(privateChat && senderRole.hasAtLeast(ContractRole.ADMIN))) {
                sendReply(sourceGroupId, senderQQ, privateChat, "[权限] 此命令仅限管理群使用");
                return true;
            }
        }

        if (!senderRole.hasAtLeast(cmd.requiredRole())) {
            sendReply(
                sourceGroupId,
                senderQQ,
                privateChat,
                String.format(
                    "[权限] 此命令需要 %s 权限 (当前: %s)",
                    QqRoleResolver.roleKey(cmd.requiredRole()),
                    QqRoleResolver.roleKey(senderRole)
                )
            );
            return true;
        }

        // legacy 兼容：老命令仍可声明 requiresAdmin/requiredPermLevel
        if (cmd.requiresAdmin() && !senderRole.hasAtLeast(ContractRole.ADMIN)) {
            sendReply(sourceGroupId, senderQQ, privateChat, "[权限] 此命令需要 admin 权限");
            return true;
        }

        if (cmd.requiredPermLevel() > 0 && !senderRole.hasAtLeast(ContractRole.ADMIN)) {
            sendReply(sourceGroupId, senderQQ, privateChat, "[权限] 此命令需要 admin 权限 (legacy-level)");
            return true;
        }

        try {
            String result = cmd.execute(args, senderQQ, sourceGroupId);
            if (result != null && !result.isEmpty()) {
                sendReply(sourceGroupId, senderQQ, privateChat, result);
            }
        } catch (Exception e) {
            LOGGER.error("命令执行异常: #{} {}", name, args, e);
            sendReply(sourceGroupId, senderQQ, privateChat, "[错误] 命令执行失败: " + e.getMessage());
        }

        return true;
    }
    
    /**
     * 发送命令回复，群聊/私聊根据上下文
     */
    public static void sendReply(long groupId, long senderQQ, boolean privateChat, String message) {
        if (privateChat) {
            OneBotClient.INSTANCE.sendPrivateMessage(senderQQ, message);
        } else {
            OneBotClient.INSTANCE.sendGroupMessage(groupId, message);
        }
    }
    
    /**
     * 获取所有命令
     */
    public static Map<String, ICommand> getCommands() {
        return commands;
    }

    /**
     * 获取某主命令的别名列表（已排序）
     */
    public static List<String> getAliasesFor(String commandName) {
        if (commandName == null || commandName.isBlank()) {
            return Collections.emptyList();
        }
        String target = commandName.toLowerCase();
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> e : aliases.entrySet()) {
            if (target.equals(e.getValue())) {
                result.add(e.getKey());
            }
        }
        Collections.sort(result);
        return result;
    }
    
    /**
     * 获取命令帮助文本
     */
    public static String getHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MapBot 命令帮助 ===\n");
        for (Map.Entry<String, ICommand> e : commands.entrySet()) {
            sb.append("#").append(e.getKey());
            String help = e.getValue().getHelp();
            if (help != null && !help.isEmpty()) {
                sb.append(" - ").append(help);
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }
}
