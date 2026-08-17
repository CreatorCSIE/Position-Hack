package org.github.creatorcsie.positionhack.mock;

/**
 * 模拟 Indev 版 MinecraftApplet（类名/字段名与真实混淆版无关，只保留类型结构）：
 * 第一个字段是 java.awt.Canvas（会被定位逻辑按 awt 跳过），第二个是 Minecraft 主类。
 */
public class MinecraftApplet extends java.applet.Applet {

    private static final long serialVersionUID = 1L;

    private java.awt.Canvas a;
    private Minecraft b;
    private Thread c;

    @Override
    public void init() {
        b = new Minecraft();
        b.spawnPlayer();
    }

    public Minecraft getMinecraft() {
        return b;
    }
}