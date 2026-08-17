package org.github.creatorcsie.positionhack.mock;

/**
 * 模拟 EntityFish（钓鱼实体）：Entity 子类自身带 6 个连续 float 字段 + 干扰字段，
 * 恰好满足"6~8 连续数值字段"特征——但它是实体不是 AABB（真实 B1.7.3 的
 * EntityPlayer.fishEntity 就是这个坑：玩家 null 走类型级兜底时被误判为碰撞箱）。
 */
public class EntityFish extends Entity {

    public boolean bobber = false;
    public float p;
    public float q;
    public float r;
    public float s;
    public float t;
    public float u;
}