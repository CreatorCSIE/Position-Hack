package org.github.creatorcsie.positionhack.mock;

/**
 * 模拟 Indev 版 AxisAlignedBB（碰撞箱，float 模式）：
 * 第一个字段是 private float（会被过滤掉），接下来 6 个 public float 依次为 x,y,z,x2,y2,z2。
 */
public final class AxisAlignedBB {

    private float a = 0.0F;   // epsilon
    public float b;           // minX
    public float c;           // minY
    public float d;           // minZ
    public float e;           // maxX
    public float f;           // maxY
    public float g;           // maxZ

    public AxisAlignedBB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        b = minX;
        c = minY;
        d = minZ;
        e = maxX;
        f = maxY;
        g = maxZ;
    }
}