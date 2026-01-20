package com.mapbot.command;

import com.mapbot.logic.InboundHandler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 性能报告命令
 * #report
 */
public class ReportCommand implements ICommand {
    @Override
    public String getDescription() {
        return "查看服务器压缩性能报告: #report";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        try {
            Path path = Paths.get("./duolingo_super_report.json");
            if (!Files.exists(path)) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[提示] 暂无报告，请在游戏内执行 /duo report");
                return;
            }

            String content = Files.readString(path);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();
            
            StringBuilder sb = new StringBuilder("[性能报告]\n");
            if (json.has("compression")) {
                JsonObject comp = json.getAsJsonObject("compression");
                sb.append(String.format("节省流量: %.2f MB\n", comp.get("bytesSaved").getAsLong() / 1024.0 / 1024.0));
                sb.append(String.format("压缩比: %.2fx", comp.get("compressionRatio").getAsDouble()));
            }
            
            InboundHandler.sendReplyToQQ(sourceGroupId, sb.toString());
        } catch (Exception e) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 读取报告失败");
        }
    }
}
