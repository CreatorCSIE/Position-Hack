package org.github.creatorcsie.positionhack.mock;

/**
 * 模拟 EntityLiving（继承自 Entity，携带 /char.png 纹理常量，对应 RetroWrapper 的 ASM 标记类）。
 */
public abstract class EntityLiving extends Entity {

    protected String a = "/char.png";

    public EntityLiving() {
    }
}