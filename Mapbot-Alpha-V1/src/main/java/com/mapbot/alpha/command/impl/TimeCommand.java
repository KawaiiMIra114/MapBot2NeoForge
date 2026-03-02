package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.command.QqRoleResolver;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.security.ContractRole;

/**
 * 在线时长查询（按绑定账号）(Task #03 重构: Bridge 转发模式)
 *
 * - #time: 查询自己绑定账号的在线时长（四个周期）
 * - #time @用户 / #time <QQ号>: admin/owner 查询他人
 * 
 * Alpha 不再本地读取 PlaytimeStore，全部转发给 Reforged 端处理。
 */
public class TimeCommand implements ICommand {

    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        long targetQQ = senderQQ;

        String raw = args == null ? "" : args.trim();
        if (!raw.isEmpty()) {
            if (!QqRoleResolver.hasAtLeast(senderQQ, ContractRole.ADMIN)) {
                return "[权限] 仅 admin/owner 可查询他人在线时长";
            }

            String digits = raw.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                return "[错误] 用法: #time 或 #time <QQ号/@提及>";
            }
            targetQQ = Long.parseLong(digits);
        }

        String uuid = DataManager.INSTANCE.getBinding(targetQQ);
        if (uuid == null) {
            return (targetQQ == senderQQ) ? "[错误] 你尚未绑定账号，请先 #id <游戏ID>" : "[错误] 目标QQ未绑定账号";
        }

        // 通过 UUID 反查玩家名，然后转发给 Reforged 端查询
        String playerName = BridgeProxy.INSTANCE.resolveNameByUuid(uuid);
        if (playerName == null || playerName.isBlank()) {
            playerName = uuid; // 降级：直接用 UUID 作为参数
        }

        // 用 mode=4 表示查询所有周期（Reforged 端可自行处理并返回格式化的完整结果）
        // 如果 Reforged 不支持 mode=4，则逐一查询
        String result = BridgeProxy.INSTANCE.getPlaytime(playerName, 0);
        if (result == null || result.isEmpty()) {
            return "[错误] 服务器离线或无响应";
        }
        return result;
    }

    @Override
    public String getHelp() {
        return "查询在线时长: #time";
    }
}
