package org.github.creatorcsie.positionhack.agent;

import java.applet.Applet;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;

/**
 * 在 Minecraft 的 JVM 内定位 MinecraftApplet 实例。
 * 旧版 Minecraft（1.5.2 及以前）都由启动器以 Applet 方式放入 AWT 组件树中，
 * 因此递归遍历所有 Frame 即可拿到实例，无需依赖任何 tweak 注入。
 */
public final class MinecraftFinder {

    private MinecraftFinder() {
    }

    /** 轮询查找 MinecraftApplet，超过 timeoutMs 仍未找到则返回 null。 */
    public static Applet findMinecraftApplet(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        int attempt = 0;
        while (System.currentTimeMillis() < deadline) {
            Applet found = tryFind();
            if (found != null) {
                return found;
            }
            attempt++;
            // 每 ~5 秒写一条日志，方便确认 agentmain 确实在运行
            if ((attempt % 10) == 0) {
                Frame[] frames = null;
                try {
                    frames = Frame.getFrames();
                } catch (Throwable ignored) {
                }
                AgentMain.log("waiting for MinecraftApplet... (poll " + attempt + ") frames=" + (frames == null ? "?" : String.valueOf(frames.length)));
            }
            Thread.sleep(500L);
        }
        return null;
    }

    private static Applet tryFind() {
        try {
            // 通过反射调用，避免把 AWT 的依赖硬编码死（实际上 agent 一定跑在 AWT 环境里，直接调用也可以）
            Frame[] frames = Frame.getFrames();
            if (frames != null) {
                for (Frame frame : frames) {
                    Applet result = search(frame);
                    if (result != null) {
                        return result;
                    }
                }
            }
        } catch (Throwable ignored) {
            // AWT 可能尚未初始化，忽略并等待下一次轮询
        }
        return null;
    }

    private static Applet search(Container container) {
        Component[] components = container.getComponents();
        if (components == null) {
            return null;
        }
        for (Component component : components) {
            if (component instanceof Applet && isMinecraftApplet(component.getClass().getName())) {
                return (Applet) component;
            }
            if (component instanceof Container) {
                Applet result = search((Container) component);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static boolean isMinecraftApplet(String className) {
        String lower = className.toLowerCase();
        return lower.contains("minecraftapplet")
                || "net.minecraft.client.minecraftapplet".equals(lower)
                || "com.mojang.minecraft.minecraftapplet".equals(lower);
    }

    /** 诊断用：列出当前所有 Frame 及其组件树中的 Applet/Canvas，帮助定位注入目标。 */
    public static String dumpComponents() {
        StringBuilder sb = new StringBuilder();
        try {
            Frame[] frames = Frame.getFrames();
            sb.append(frames == null ? "Frame.getFrames() returned null" : ("Frames: " + frames.length));
            if (frames != null) {
                for (Frame frame : frames) {
                    sb.append("\n  Frame '").append(frame.getTitle() == null ? "" : frame.getTitle()).append("' visible=").append(frame.isVisible()).append(" size=").append(frame.getWidth()).append('x').append(frame.getHeight());
                    dumpComponentTree(frame, sb, "    ");
                }
            }
        } catch (Throwable t) {
            sb.append("\ndumpComponents error: ").append(t);
        }
        return sb.toString();
    }

    private static void dumpComponentTree(Container container, StringBuilder sb, String indent) {
        Component[] components = container.getComponents();
        if (components == null) {
            return;
        }
        for (Component component : components) {
            sb.append('\n').append(indent).append(component.getClass().getName()).append(" [").append(component.getClass().getClassLoader() == null ? "bootstrap" : component.getClass().getClassLoader().toString()).append(']');
            if (component instanceof Container) {
                dumpComponentTree((Container) component, sb, indent + "  ");
            }
        }
    }
}