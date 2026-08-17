package org.github.creatorcsie.positionhack.agent;

import java.applet.Applet;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

import javax.swing.JOptionPane;

/**
 * Java Agent 入口。
 * 注入器调用 VirtualMachine.loadAgent 后，JVM 会在一个独立的 agentmain 线程中调用本方法，
 * 此时 hack 逻辑直接运行在 Minecraft 的 JVM 进程内，可以随意反射其内部对象。
 * 这样无需安装 RetroWrapper / 修改 Minecraft 的任何文件。
 */
public final class AgentMain {

    private AgentMain() {
    }

    public static void agentmain(String args, Instrumentation inst) {
        log("=== PositionHack agent start, java=" + System.getProperty("java.version") + " os=" + System.getProperty("os.name"));
        Thread worker = new Thread(new Runnable() {
            public void run() {
                try {
                    Applet applet = MinecraftFinder.findMinecraftApplet(60000L);
                    if (applet == null) {
                        log("NOT FOUND MinecraftApplet within 60s. Dump follows:\n" + MinecraftFinder.dumpComponents());
                        throw new IllegalStateException("在 60 秒内没有找到 MinecraftApplet。\n请确认注入的是正在运行旧版 Minecraft（1.5.2 及以前）的 JVM 进程。\n\n" + MinecraftFinder.dumpComponents());
                    }
                    log("found applet: " + applet.getClass().getName() + " loaded by " + applet.getClass().getClassLoader());

                    PlayerLocator.Located located = PlayerLocator.locate(applet);
                    log("located minecraft: " + located.minecraft.getClass().getName()
                            + " | player field: " + located.playerField
                            + " | aabb field: " + located.aabbField);
                    log("locate diagnostic:\n" + PlayerLocator.lastDiagnostic());

                    RetroPlayer player = new RetroPlayer(located.minecraft, located.playerField, located.aabbField);
                    new HackWindow(player);
                    log("hack window created and visible");
                    System.out.println("[PositionHack] Injection succeeded, hack window opened");
                } catch (Throwable throwable) {
                    log("FAILED: " + stackTraceOf(throwable));
                    showError(throwable);
                    System.err.println("[PositionHack] Injection failed: " + stackTraceOf(throwable));
                }
            }
        }, "position-hack-setup");
        worker.setDaemon(true);
        worker.start();
    }

    private static void showError(Throwable throwable) {
        try {
            JOptionPane.showMessageDialog(null,
                    "Position Hack 注入失败：\n" + throwable.getMessage() + "\n\n" + stackTraceOf(throwable),
                    "Position Hack", JOptionPane.ERROR_MESSAGE);
        } catch (Throwable ignored) {
            System.err.println("[PositionHack] 注入失败：\n" + stackTraceOf(throwable));
        }
    }

    private static String stackTraceOf(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /** 写诊断日志到 <user.home>/position-hack-agent.log，方便看不到弹窗/控制台时排查。 */
    static synchronized void log(String line) {
        try {
            FileWriter fw = new FileWriter(System.getProperty("user.home") + "/position-hack-agent.log", true);
            PrintWriter pw = new PrintWriter(fw);
            pw.println("[PositionHack] " + line);
            pw.close();
        } catch (Exception e) {
            e.printStackTrace(new PrintStream(System.err));
        }
    }
}