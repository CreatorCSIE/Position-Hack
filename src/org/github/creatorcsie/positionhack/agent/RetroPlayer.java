package org.github.creatorcsie.positionhack.agent;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 玩家坐标读写器（AABB 方案，适用于 B1.7.3 及以前的旧版 MC）。
 * 旧版 MC 的实体坐标承载在碰撞箱 AABB 的 6 个 public float/double 字段上：
 * x,y,z,x2,y2,z2（依次为 minX..., maxX...）。
 * 传送 = 把 x/y/z 设为目标值，并把 x2/y2/z2 平移同样的偏移量，以保持碰撞箱尺寸不变。
 */
public final class RetroPlayer {

    private final Object minecraft;
    private final Field playerField;
    private final Field aabbField;
    private final Field x;
    private final Field y;
    private final Field z;
    private final Field x2;
    private final Field y2;
    private final Field z2;
    private final boolean modeFloat;

    private volatile Object aabb;

    public RetroPlayer(Object minecraft, Field playerField, Field aabbField) throws Exception {
        this.minecraft = minecraft;
        this.playerField = playerField;
        this.aabbField = aabbField;

        Field xT = null;
        Field yT = null;
        Field zT = null;
        Field x2T = null;
        Field y2T = null;
        Field z2T = null;
        boolean floatMode = false;
        // 坐标字段的特征：AxisAlignedBB 类中"第一个连续同型 float/double run"的前 6 个。
        // 优先要求 public（真实坐标字段都是 public，private float 之类是内部字段如 epsilon）；
        // 个别混淆版本可能改动可见性，拿不够 6 个时再放开可见性约束重试一次（setAccessible 兜底）。
        Field[] coords = pickSix(aabbField.getType(), true);
        if (coords == null) {
            coords = pickSix(aabbField.getType(), false);
        }
        if (coords == null) {
            throw new IllegalStateException("AABB 类 " + aabbField.getType().getName()
                    + " 中没有 6 个连续的 float/double 字段");
        }
        xT = coords[0];
        yT = coords[1];
        zT = coords[2];
        x2T = coords[3];
        y2T = coords[4];
        z2T = coords[5];
        floatMode = (coords[0].getType() == float.class);
        x = xT;
        y = yT;
        z = zT;
        x2 = x2T;
        y2 = y2T;
        z2 = z2T;
        modeFloat = floatMode;
        // 字段可能是 private/混淆后非 public 修饰，放行
        setAccessible(x, y, z, x2, y2, z2);
    }

    private static void setAccessible(Field... fields) {
        for (Field f : fields) {
            if (f != null) {
                try {
                    f.setAccessible(true);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** 取 AABB 类中"第一个连续同型 float/double run"的 6 个坐标字段（按声明顺序）。
     *  requirePublic 时还要全部 public；否则只要求非 static。取不到 6 个返回 null。 */
    private static Field[] pickSix(Class<?> aabbClass, boolean requirePublic) {
        Field[] coords = new Field[6];
        Class<?> runType = null;
        int runLen = 0;
        for (Field f : aabbClass.getDeclaredFields()) {
            Class<?> t = f.getType();
            if (Modifier.isStatic(f.getModifiers())
                    || (t != float.class && t != double.class)
                    || (requirePublic && !Modifier.isPublic(f.getModifiers()))) {
                if (runLen >= 6) {
                    break;
                }
                runLen = 0;
                runType = null;
                continue;
            }
            if (runType != null && t != runType) {
                runLen = 0;
            }
            runType = t;
            coords[runLen] = f;
            runLen++;
            if (runLen >= 6) {
                break;
            }
        }
        return runLen >= 6 ? coords : null;
    }

    /** 从 Minecraft 实例中取当前玩家的 AABB；玩家尚未生成时返回 null。 */
    public Object getAABBOrNull() throws IllegalAccessException {
        Object playerObject = playerField.get(minecraft);
        if (playerObject == null) {
            return null;
        }
        Object box = aabbField.get(playerObject);
        if (box != null) {
            this.aabb = box;
        }
        return box;
    }

    public Object getAabb() {
        return aabb;
    }

    public void setAabb(Object aabb) {
        this.aabb = aabb;
    }

    public double getX() throws IllegalAccessException {
        return read(x);
    }

    public double getY() throws IllegalAccessException {
        return read(y);
    }

    public double getZ() throws IllegalAccessException {
        return read(z);
    }

    private double read(Field f) throws IllegalAccessException {
        return modeFloat ? f.getFloat(aabb) : f.getDouble(aabb);
    }

    private double readX2() throws IllegalAccessException {
        return modeFloat ? x2.getFloat(aabb) : x2.getDouble(aabb);
    }

    private double readY2() throws IllegalAccessException {
        return modeFloat ? y2.getFloat(aabb) : y2.getDouble(aabb);
    }

    private double readZ2() throws IllegalAccessException {
        return modeFloat ? z2.getFloat(aabb) : z2.getDouble(aabb);
    }

    /** 传送玩家到 (dx,dy,dz)，保持碰撞箱尺寸不变。 */
    public void teleport(double dx, double dy, double dz) throws IllegalAccessException {
        final double ax = readX2() - getX();
        final double ay = readY2() - getY();
        final double az = readZ2() - getZ();
        final double dax = dx + ax;
        final double day = dy + ay;
        final double daz = dz + az;

        if (modeFloat) {
            x.setFloat(aabb, (float) dx);
            y.setFloat(aabb, (float) dy);
            z.setFloat(aabb, (float) dz);
            x2.setFloat(aabb, (float) dax);
            y2.setFloat(aabb, (float) day);
            z2.setFloat(aabb, (float) daz);
        } else {
            x.setDouble(aabb, dx);
            y.setDouble(aabb, dy);
            z.setDouble(aabb, dz);
            x2.setDouble(aabb, dax);
            y2.setDouble(aabb, day);
            z2.setDouble(aabb, daz);
        }
    }
}