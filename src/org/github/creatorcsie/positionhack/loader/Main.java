package org.github.creatorcsie.positionhack.loader;

import java.io.File;

import javax.swing.SwingUtilities;

import com.sun.tools.attach.VirtualMachine;

/**
 * 注入器入口（运行在独立进程中，不依赖 Minecraft）。
 * 用法：
 *   java -cp "position-hack.jar;tools.jar" org.github.creatorcsie.positionhack.loader.Main   启动 GUI
 *   java -cp "position-hack.jar;tools.jar" org.github.creatorcsie.positionhack.loader.Main --attach <pid>  直接注入
 */
public final class Main {

    private Main() {
    }

    /** 日志回调。 */
    public interface LogSink {
        void log(String line);
    }

    public static void main(String[] args) {
        if ((args.length == 2) && "--attach".equals(args[0])) {
            final String pid = args[1];
            try {
                attachTo(pid, new LogSink() {
                    public void log(String line) {
                        System.out.println(line);
                    }
                });
            } catch (Exception e) {
                System.err.println("注入失败：" + e);
                e.printStackTrace();
                System.exit(1);
            }
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new InjectorGui();
            }
        });
    }

    /**
     * 把当前 jar 作为 agent 注入到指定 PID 的 JVM（Minecraft）中。
     * loadAgent 会把 jar 里的 agentmain 跑起来，坐标窗口随之在 Minecraft 进程内弹出。
     */
    public static void attachTo(String pid, LogSink sink) throws Exception {
        VirtualMachine vm = VirtualMachine.attach(pid);
        try {
            String jarPath = currentJarPath();
            sink.log("Injecting " + jarPath + " -> PID " + pid + " ...");
            vm.loadAgent(jarPath);
            sink.log("Injection succeeded! The position hack window should now appear inside Minecraft.");
        } finally {
            vm.detach();
        }
    }

    /** 取得当前正在运行的 jar 完整路径（loadAgent 要求传入 jar 文件）。 */
    public static String currentJarPath() throws Exception {
        File location = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (location.isDirectory()) {
            throw new IllegalStateException("当前从 class 目录运行，无法注入。请先运行 compile.bat 生成 position-hack.jar，再从 jar 启动。");
        }
        return location.getAbsolutePath();
    }
}