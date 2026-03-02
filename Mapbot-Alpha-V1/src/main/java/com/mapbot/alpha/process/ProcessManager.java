package com.mapbot.alpha.process;

import com.mapbot.alpha.config.AlphaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Locale;

/**
 * Minecraft 进程守护管理器 (Task #08 重写)
 *
 * 以 Windows cmd.exe /c start /WAIT 方式弹出独立终端窗口运行 MC 服务端。
 * 具备崩溃退避拦截：10秒内退出视为瞬崩，连续3次瞬崩终止守护。
 */
public class ProcessManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Process");
    public static final ProcessManager INSTANCE = new ProcessManager();

    /** 瞬崩判定阈值（运行时间低于此值视为瞬崩） */
    private static final long INSTANT_CRASH_THRESHOLD_MS = 10_000;
    /** 瞬崩时的退避等待时间 */
    private static final long INSTANT_CRASH_BACKOFF_MS = 30_000;
    /** 正常退出后的重启等待时间 */
    private static final long NORMAL_RESTART_DELAY_MS = 5_000;
    /** 连续瞬崩次数上限 */
    private static final int MAX_CONSECUTIVE_CRASHES = 3;

    private volatile Process serverProcess;
    private volatile boolean daemonRunning = false;
    private volatile long startTime;
    private Thread daemonThread;

    /**
     * 启动守护循环（在独立线程中运行）
     */
    public synchronized void startDaemon() {
        if (daemonRunning) {
            LOGGER.warn("[DAEMON] 守护循环已在运行中");
            return;
        }

        String command = AlphaConfig.getDaemonCommand();
        String workDir = AlphaConfig.getDaemonWorkDir();

        // 命令后缀校验
        if (!isValidScript(command)) {
            LOGGER.error("[DAEMON] 拒绝启动：daemon.command '{}' 后缀不合法，仅允许 .bat/.cmd/.sh/.exe", command);
            return;
        }

        // 工作目录校验
        File workDirFile = new File(workDir);
        if (!workDirFile.exists() || !workDirFile.isDirectory()) {
            LOGGER.error("[DAEMON] 拒绝启动：daemon.workDir '{}' 不存在或不是目录", workDir);
            return;
        }

        // 脚本文件校验
        File scriptFile = new File(workDirFile, command);
        if (!scriptFile.exists()) {
            LOGGER.error("[DAEMON] 拒绝启动：脚本文件 '{}' 不存在", scriptFile.getAbsolutePath());
            return;
        }

        daemonRunning = true;
        daemonThread = new Thread(() -> runDaemonLoop(workDir, command), "Mapbot-Daemon");
        daemonThread.setDaemon(true);
        daemonThread.start();

        LOGGER.info("[DAEMON] 守护循环已启动 (workDir={}, command={})", workDir, command);
        LOGGER.info("[DAEMON] MC 服务器将在独立窗口运行，Alpha 不再接管控制台输入输出");
    }

    /**
     * 停止守护循环（不立即杀死子进程，只是阻止下一次重启）
     */
    public void stopDaemon() {
        daemonRunning = false;
        LOGGER.info("[DAEMON] 守护循环已标记停止，当前进程结束后将不再重启");
    }

    /**
     * 强制销毁子进程（ShutdownHook 使用）
     */
    public void destroyProcess() {
        Process p = serverProcess;
        if (p != null && p.isAlive()) {
            LOGGER.info("[DAEMON] 正在销毁 MC 子进程...");
            p.destroyForcibly();
        }
    }

    public boolean isRunning() {
        return serverProcess != null && serverProcess.isAlive();
    }

    public long getUptimeMs() {
        if (!isRunning()) return 0;
        return System.currentTimeMillis() - startTime;
    }

    public boolean isDaemonActive() {
        return daemonRunning;
    }

    // ─────────────────── 守护循环核心 ───────────────────

    private void runDaemonLoop(String workDir, String command) {
        int consecutiveCrashes = 0;

        while (daemonRunning) {
            try {
                LOGGER.info("[DAEMON] 正在启动 MC 服务器...");
                startTime = System.currentTimeMillis();

                // Windows: 弹出独立终端窗口, start /WAIT 让 waitFor 正常阻塞
                ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c", "start", "/WAIT", "MapBot-Minecraft-Server", command
                );
                pb.directory(new File(workDir));
                pb.redirectErrorStream(true);

                serverProcess = pb.start();

                // 消耗子进程的 stdout（cmd.exe /c start 通常无输出，但防止管道阻塞）
                try (var is = serverProcess.getInputStream()) {
                    is.transferTo(java.io.OutputStream.nullOutputStream());
                } catch (Exception ignored) {}

                int exitCode = serverProcess.waitFor();
                long runDuration = System.currentTimeMillis() - startTime;

                LOGGER.info("[DAEMON] MC 服务器已退出 (exitCode={}, 运行时长={}ms)", exitCode, runDuration);

                // 守护已被手动停止
                if (!daemonRunning) {
                    LOGGER.info("[DAEMON] 守护循环已停止，不再重启");
                    break;
                }

                // 检查 daemon.enabled 是否仍为 true（支持热重载关闭守护）
                if (!AlphaConfig.isDaemonEnabled()) {
                    LOGGER.info("[DAEMON] daemon.enabled 已被设为 false，停止守护");
                    daemonRunning = false;
                    break;
                }

                // 瞬崩检测
                if (runDuration < INSTANT_CRASH_THRESHOLD_MS) {
                    consecutiveCrashes++;
                    LOGGER.warn("[DAEMON] 检测到瞬间崩溃 ({}ms < {}ms), 连续瞬崩次数: {}/{}",
                        runDuration, INSTANT_CRASH_THRESHOLD_MS, consecutiveCrashes, MAX_CONSECUTIVE_CRASHES);

                    if (consecutiveCrashes >= MAX_CONSECUTIVE_CRASHES) {
                        LOGGER.error("[DAEMON] 连续 {} 次瞬间崩溃，终止守护循环！请检查服务器配置。", MAX_CONSECUTIVE_CRASHES);
                        daemonRunning = false;
                        break;
                    }

                    LOGGER.info("[DAEMON] 等待 {}ms 后重试...", INSTANT_CRASH_BACKOFF_MS);
                    Thread.sleep(INSTANT_CRASH_BACKOFF_MS);
                } else {
                    // 正常退出，重置瞬崩计数
                    consecutiveCrashes = 0;
                    LOGGER.info("[DAEMON] 等待 {}ms 后重启...", NORMAL_RESTART_DELAY_MS);
                    Thread.sleep(NORMAL_RESTART_DELAY_MS);
                }

            } catch (InterruptedException e) {
                LOGGER.info("[DAEMON] 守护线程被中断");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.error("[DAEMON] 启动服务器失败", e);
                daemonRunning = false;
                break;
            }
        }

        LOGGER.info("[DAEMON] 守护循环已结束");
    }

    // ─────────────────── 工具方法 ───────────────────

    private boolean isValidScript(String command) {
        if (command == null || command.isBlank()) return false;
        String lower = command.toLowerCase(Locale.ROOT);
        return lower.endsWith(".bat") || lower.endsWith(".cmd")
            || lower.endsWith(".sh") || lower.endsWith(".exe");
    }
}
