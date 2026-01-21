package com.mapbot.alpha.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Minecraft 进程管理器
 * 负责启动、停止及日志捕获
 */
public class ProcessManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Process");
    public static final ProcessManager INSTANCE = new ProcessManager();

    private Process serverProcess;
    private BufferedWriter serverInput;
    private final List<String> logHistory = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG_HISTORY = 1000;

    public void startServer(String workDir, String command) {
        if (serverProcess != null && serverProcess.isAlive()) {
            LOGGER.warn("服务器已在运行中");
            return;
        }

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(command.split("\\s+"));
                pb.directory(new File(workDir));
                pb.redirectErrorStream(true); // 合并 stdout 和 stderr

                serverProcess = pb.start();
                serverInput = new BufferedWriter(new OutputStreamWriter(serverProcess.getOutputStream(), StandardCharsets.UTF_8));

                LOGGER.info("MC 服务器进程已启动 (DIR: {})", workDir);

                try (BufferedReader reader = new ProcessBufferedReader(serverProcess.getInputStream())) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        captureLog(line);
                    }
                }

                int exitCode = serverProcess.waitFor();
                LOGGER.info("MC 服务器已退出，退出码: {}", exitCode);
            } catch (Exception e) {
                LOGGER.error("启动服务器失败", e);
            }
        }, "MC-Runner").start();
    }

    public void sendCommand(String cmd) {
        if (serverInput != null) {
            try {
                serverInput.write(cmd);
                serverInput.newLine();
                serverInput.flush();
                LOGGER.debug("发送命令至 MC: {}", cmd);
            } catch (IOException e) {
                LOGGER.error("发送命令失败", e);
            }
        }
    }

    private void captureLog(String line) {
        // 输出到控制台 (Alpha 自身的日志)
        System.out.println("[MC] " + line);
        
        // 存入历史记录，供 Web 端初始化读取
        logHistory.add(line);
        if (logHistory.size() > MAX_LOG_HISTORY) {
            logHistory.remove(0);
        }
        
        // TODO: 通过 WebSocket 广播给所有已连接的 Web 客户端
    }

    public List<String> getLogHistory() {
        return new ArrayList<>(logHistory);
    }

    public boolean isRunning() {
        return serverProcess != null && serverProcess.isAlive();
    }

    /**
     * 自定义 Reader 以支持特殊字符处理 (可选)
     */
    private static class ProcessBufferedReader extends BufferedReader {
        public ProcessBufferedReader(InputStream in) {
            super(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
