package org.github.creatorcsie.positionhack.mock;

/**
 * 模拟 Indev 版实体基类 Entity（字段布局抄自真实反混淆源码，字段名混淆为字母）：
 * 开头两个非 float 字段后，是一长串 float 字段，然后 AABB（碰撞箱）字段。
 * "第一个 float 之后第一个非 primitive 字段" = 碰撞箱。
 */
public abstract class Entity {

    public boolean a = false;      // preventEntitySpawning
    protected Object b;            // worldObj
    public float c;                // prevPosX
    public float d;                // prevPosY
    public float e;                // prevPosZ
    public float f;                // posX
    public float g;                // posY
    public float h;                // posZ
    public float i;                // motionX
    public float j;                // motionY
    public float k;                // motionZ
    public float l;                // rotationYaw
    public float m;                // rotationPitch
    public float n;                // prevRotationYaw
    public float o;                // prevRotationPitch
    public AxisAlignedBB boundingBox;

    public Entity() {
        setPosition(10.0F, 20.0F, 30.0F);
    }

    protected final void setPosition(float posX, float posY, float posZ) {
        f = posX;
        g = posY;
        h = posZ;
        c = posX;
        d = posY;
        e = posZ;
        boundingBox = new AxisAlignedBB(posX, posY, posZ, posX + 1.0F, posY + 1.0F, posZ + 1.0F);
    }
}