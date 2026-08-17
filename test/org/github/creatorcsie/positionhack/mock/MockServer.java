package org.github.creatorcsie.positionhack.mock;

import java.lang.management.ManagementFactory;

import javax.swing.JFrame;

/**
 * 模拟"正在运行中的 Minecraft"：持有 Applet + 玩家 + AABB，进程保持存活。
 * 用于端到端验证注入链路（loader attach -&gt; agentmain -&gt; 定位 -&gt; 弹出坐标窗口）。
 * 运行方式：compile.bat test 后 java -cp build;build-test org...mock.MockServer
 */
public final class MockServer {

    private MockServer() {
    }

    public static void main(String[] args) throws Exception {
        final MinecraftApplet applet = new MinecraftApplet();
        JFrame frame = new JFrame("Mock Minecraft");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(applet);
        frame.pack();
        frame.setVisible(true);
        applet.init();
        applet.start();

        System.out.println("[MockServer] ready, pid=" + ManagementFactory.getRuntimeMXBean().getName());
        Thread.sleep(Long.MAX_VALUE);
    }
}