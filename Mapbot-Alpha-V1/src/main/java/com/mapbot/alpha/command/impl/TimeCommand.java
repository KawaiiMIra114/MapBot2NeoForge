package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.command.QqRoleResolver;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.logic.PlaytimeStore;
import com.mapbot.alpha.security.ContractRole;

/**
 * 在线时长查询（按绑定账号）
 *
 * - #time: 查询自己绑定账号的在线时长（四个周期）
 * - #time @用户 / #time <QQ号>: admin/owner 查询他人
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

        var r = PlaytimeStore.INSTANCE.getPlaytime(uuid);
        return String.format(
            "[在线时长]\n" +
            "QQ: %d\n" +
            "UUID: %s\n" +
            "今日: %s\n" +
            "本周: %s\n" +
            "本月: %s\n" +
            "总计: %s",
            targetQQ,
            uuid,
            PlaytimeStore.formatDurationMs(r.dailyMs),
            PlaytimeStore.formatDurationMs(r.weeklyMs),
            PlaytimeStore.formatDurationMs(r.monthlyMs),
            PlaytimeStore.formatDurationMs(r.totalMs)
        );
    }

    @Override
    public String getHelp() {
        return "查询在线时长: #time";
    }
}

