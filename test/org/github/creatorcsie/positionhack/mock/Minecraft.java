package org.github.creatorcsie.positionhack.mock;

/**
 * 模拟 Indev 版 Minecraft 主类（字段名混淆为 a/b/c/d，与真实混淆版行为一致）。
 * 关键陷阱：第一个字段 a 的类型是 PlayerController（类名含 Player 但不是实体），
 * 实体的判断必须基于"继承链上是否有 Entity 基类"而非类名字符串。
 */
public final class Minecraft {

    public PlayerController a = new PlayerController(this); // 陷阱：不是实体
    private boolean b = false;
    public Object c;                                        // theWorld
    public EntityPlayerSP d;                                // thePlayer（实体）
    public Object e;                                        // renderGlobal
    public boolean appletMode = true;

    public void spawnPlayer() {
        d = new EntityPlayerSP();
    }
}