package org.github.creatorcsie.positionhack.mock;

import java.applet.Applet;

import javax.swing.JFrame;

import org.github.creatorcsie.positionhack.agent.MinecraftFinder;
import org.github.creatorcsie.positionhack.agent.PlayerLocator;
import org.github.creatorcsie.positionhack.agent.RetroPlayer;

/**
 * 用"Indev 真实结构 + 混淆字段名"的模拟类，验证定位 / 读坐标 / 传送逻辑：
 * 1) MinecraftApplet 持有 Minecraft（中间有 Canvas 干扰字段）；
 * 2) Minecraft 第一个字段是 PlayerController（类名含 Player 但不是实体的陷阱），第二个才是玩家实体；
 * 3) 实体是 4 层继承链 EntityPlayerSP-&gt;EntityPlayer-&gt;EntityLiving-&gt;Entity；
 * 4) AABB 为 float 模式（Indev），验证 modeFloat 路径。
 */
public final class MockMain {

    private MockMain() {
    }

    public static void main(String[] args) throws Exception {
        MinecraftApplet applet = new MinecraftApplet();
        JFrame frame = new JFrame("Mock Minecraft Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(applet);
        frame.pack();
        frame.setVisible(true);
        applet.init();
        applet.start();

        // 1) 定位 MinecraftApplet
        Applet found = MinecraftFinder.findMinecraftApplet(5000L);
        if (found == null) {
            throw new AssertionError("MinecraftFinder 未找到 MinecraftApplet");
        }
        System.out.println("[1] MinecraftFinder 找到 Applet: " + found.getClass().getName());

        // 2) 定位 minecraft / player / aabb
        PlayerLocator.Located located = PlayerLocator.locate(found);
        if (!(located.minecraft instanceof Minecraft)) {
            throw new AssertionError("minecraft 定位错误: " + located.minecraft);
        }
        if (!located.playerField.getType().equals(EntityPlayerSP.class)) {
            throw new AssertionError("玩家字段定位错误（可能被 PlayerController 干扰）: " + located.playerField);
        }
        if (!located.aabbField.getType().equals(AxisAlignedBB.class)) {
            throw new AssertionError("AABB 字段定位错误: " + located.aabbField);
        }
        System.out.println("[2] PlayerLocator 定位成功: player=" + located.playerField + " | aabb=" + located.aabbField);

        // 3) 读坐标（AABB 方案）
        RetroPlayer player = new RetroPlayer(located.minecraft, located.playerField, located.aabbField);
        Object aabb = player.getAABBOrNull();
        if (aabb == null) {
            throw new AssertionError("没有取到 AABB 实例");
        }
        player.setAabb(aabb);
        check(player.getX() == 10.0, "getX", player.getX());
        check(player.getY() == 20.0, "getY", player.getY());
        check(player.getZ() == 30.0, "getZ", player.getZ());
        System.out.println("[3] 读取坐标成功: " + player.getX() + ", " + player.getY() + ", " + player.getZ());

        // 4) 传送（碰撞箱宽 1，应保持尺寸不变）
        player.teleport(100.0, 64.0, -200.0);
        check(player.getX() == 100.0, "传送后 x", player.getX());
        check(player.getY() == 64.0, "传送后 y", player.getY());
        check(player.getZ() == -200.0, "传送后 z", player.getZ());
        System.out.println("[4] 传送成功: " + player.getX() + ", " + player.getY() + ", " + player.getZ());

        // 5) 回归用例：玩家未生成（thePlayer=null）时，类型级 AABB 兜底不能把
        //    EntityPlayer.fishEntity（EntityFish）误当成碰撞箱（真实 B1.7.3 崩溃点）
        Minecraft mc = (Minecraft) located.minecraft;
        mc.d = null; // 模拟玩家尚未生成
        PlayerLocator.Located l2 = PlayerLocator.locate(found);
        if (!l2.aabbField.getType().equals(AxisAlignedBB.class)) {
            throw new AssertionError("玩家为 null 时 AABB 字段误选: " + l2.aabbField
                    + "\n诊断:\n" + PlayerLocator.lastDiagnostic());
        }
        System.out.println("[5] 玩家为 null 时类型级兜底仍选中真 AABB: " + l2.aabbField);
        mc.d = new EntityPlayerSP(); // 还原

        System.out.println("全部 Mock 测试通过！");
        frame.dispose();
        System.exit(0);
    }

    private static void check(boolean condition, String name, double actual) {
        if (!condition) {
            throw new AssertionError(name + " 校验失败: " + actual);
        }
    }
}