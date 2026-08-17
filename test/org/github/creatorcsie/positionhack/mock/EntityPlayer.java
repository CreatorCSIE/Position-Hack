package org.github.creatorcsie.positionhack.mock;

/**
 * 模拟 EntityPlayer（继承自 EntityLiving）。
 */
public abstract class EntityPlayer extends EntityLiving {

    /** fishEntity（钓鱼实体）：类型是 Entity 子类、值为 null（未钓鱼）。
     *  正是真实 B1.7.3 崩溃的源头——类型级 AABB 兜底会把它误当成碰撞箱。 */
    public EntityFish D;

    public EntityPlayer() {
        this.a = "/char.png";
    }
}