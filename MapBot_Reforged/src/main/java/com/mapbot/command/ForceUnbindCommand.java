package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.security.CommandCategory;
import com.mapbot.logic.InboundHandler;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 强制解绑命令 (管理员用)
 * #adminunbind <QQ>
 */
public class ForceUnbindCommand implements ICommand {
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.GOVERNANCE;
    }

    @Override
    public String getDescription() {
        return "强制解绑指定QQ的账号: #adminunbind <QQ>";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        if (args.trim().isEmpty()) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 用法: #adminunbind <QQ>");
            return;
        }

        try {
            long targetQQ = Long.parseLong(args.trim());
            String uuid = DataManager.INSTANCE.getBinding(targetQQ);
            
            if (uuid == null) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[提示] 该QQ未绑定账号");
                return;
            }

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> {
                    DataManager.INSTANCE.unbind(targetQQ);
                    try {
                        com.mojang.authlib.GameProfile profile = server.getProfileCache().get(java.util.UUID.fromString(uuid))
                                .orElse(new com.mojang.authlib.GameProfile(java.util.UUID.fromString(uuid), "Unknown"));
                        server.getPlayerList().getWhiteList().remove(profile);
                        server.getPlayerList().getWhiteList().save();
                    } catch (Exception ignored) {}
                    InboundHandler.sendReplyToQQ(sourceGroupId, "[成功] 已强制解绑 QQ " + targetQQ);
                });
            } else {
                // Fix #10: 服务器未就绪时反馈错误，不再静默返回
                InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪，无法执行解绑操作");
            }
        } catch (NumberFormatException e) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] QQ号格式错误");
        }
    }
}
