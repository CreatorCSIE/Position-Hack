package org.github.creatorcsie.positionhack.agent;

import java.applet.Applet;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 启发式反射定位（支持范围：B1.7.3 及以前的旧版 MC）：
 * 1. MinecraftApplet 类中声明类型不含 java/awt 且不是 long 的字段 = Minecraft 主类实例；
 * 2. 沿 Minecraft 类继承链扫描，候选字段类型需"足够多 float/double"（实体特征），
 *    且继承链上引用了一个 AABB 类（有 6~8 个连续 float/double 字段），
 *    以此排除 PlayerController / EntityRenderer 等干扰项；
 * 3. 在玩家类型的继承链上找 AABB 字段（类型符合 AABB 特征，值非 null 优先）。
 * 玩家坐标读写在 AABB 的 6 个连续 float/double 字段上进行。
 */
public final class PlayerLocator {

    private PlayerLocator() {
    }

    /** 定位结果：保存后续读写所需的所有反射句柄。 */
    public static final class Located {
        public final Object minecraft;
        public final Field playerField;
        public final Field aabbField;

        Located(Object minecraft, Field playerField, Field aabbField) {
            this.minecraft = minecraft;
            this.playerField = playerField;
            this.aabbField = aabbField;
        }
    }

    /** 最近一次定位过程的逐候选诊断（线程本地）。 */
    private static final ThreadLocal<StringBuilder> LAST_DIAGNOSTIC = new ThreadLocal<StringBuilder>();

    public static String lastDiagnostic() {
        StringBuilder sb = LAST_DIAGNOSTIC.get();
        return sb == null ? "" : sb.toString();
    }

    /** 供外部（AgentMain）打印某类型的字段分布，帮助定位。 */
    public static String dumpForDebug(Class<?> clazz) {
        return dumpFields(clazz, null);
    }

    public static Located locate(Applet applet) throws Exception {
        // 1) Minecraft 主类字段
        Field minecraftField = null;
        for (Field f : applet.getClass().getDeclaredFields()) {
            Class<?> type = f.getType();
            String typeName = type.getName();
            if (type != long.class && !typeName.contains("awt") && !typeName.contains("java")) {
                minecraftField = f;
                break;
            }
        }
        if (minecraftField == null) {
            throw new IllegalStateException("在 " + applet.getClass().getName() + " 中找不到 Minecraft 主类字段");
        }
        minecraftField.setAccessible(true);
        Object minecraft = minecraftField.get(applet);
        if (minecraft == null) {
            throw new IllegalStateException("Minecraft 主类字段 " + minecraftField.getName() + " 为 null（游戏可能还没启动完成）");
        }

        // 2) 玩家字段：两段式（值级优先 + 类型级兜底）
        Field playerField = null;
        Field fallbackPlayer = null;
        StringBuilder diag = new StringBuilder();
        LAST_DIAGNOSTIC.set(diag);
        for (Class<?> c = minecraft.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                Class<?> type = f.getType();
                if (type.isPrimitive() || type.isArray() || type.isEnum() || type.isInterface() || type == String.class) {
                    continue;
                }
                if (!isPlayerLike(type)) {
                    continue;
                }
                boolean strict = referencesAabb(type);
                diag.append("cand field ").append(f.getName()).append(" type=").append(type.getName()).append(" strict=").append(strict);
                if (playerField == null) {
                    Object playerCheck = null;
                    try {
                        f.setAccessible(true);
                        playerCheck = f.get(minecraft);
                    } catch (Exception ignored) {
                    }
                    diag.append(" val=").append(playerCheck == null ? "null" : playerCheck.getClass().getSimpleName());
                    if (playerCheck != null) {
                        Field aabb = findAabbOnType(playerCheck.getClass(), playerCheck);
                        diag.append(" aabb=").append(aabb == null ? "null" : aabb);
                        if (aabb != null) {
                            playerField = f;
                            diag.append(" <- SELECTED");
                            break;
                        }
                    }
                }
                if (fallbackPlayer == null) {
                    fallbackPlayer = f;
                    diag.append(" [fallback]");
                }
                diag.append('\n');
            }
            if (playerField != null) {
                break;
            }
        }
        if (playerField == null && fallbackPlayer == null) {
            throw new IllegalStateException("在 " + minecraft.getClass().getName()
                    + "（含父类）中找不到有效的玩家实体字段。\n逐候选诊断:\n" + diag
                    + "\nMinecraft 字段(带值):\n" + dumpFields(minecraft.getClass(), minecraft)
                    + "\n各候选类型继承链详情:\n" + dumpChainOfReferences(minecraft.getClass()));
        }
        if (playerField == null) {
            playerField = fallbackPlayer;
            diag.append(" <- FALLBACK USED");
        }
        playerField.setAccessible(true);

        // 3) AABB 字段：在玩家类型的继承链上找（值非 null 优先）
        Field aabbField = null;
        Object playerInst = null;
        try {
            playerInst = playerField.get(minecraft);
        } catch (Exception ignored) {
        }
        if (playerInst != null) {
            aabbField = findAabbOnType(playerInst.getClass(), playerInst);
        }
        if (aabbField == null) {
            aabbField = findAabbOnType(playerField.getType(), null);
        }
        if (aabbField == null) {
            throw new IllegalStateException("在玩家类 " + playerField.getType().getName()
                    + "（含父类）中找不到 AABB（碰撞箱）字段。\n玩家类字段:\n" + dumpFields(playerField.getType(), playerInst));
        }
        aabbField.setAccessible(true);
        diag.append("\nfinal player=").append(playerField).append(" | aabb=").append(aabbField);

        return new Located(minecraft, playerField, aabbField);
    }

    /** 继承链上是否有 AABB 类型的引用字段（实体特征，Renderer 无）。 */
    private static boolean referencesAabb(Class<?> clazz) {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                Class<?> t = f.getType();
                if (t.isPrimitive() || t.isArray() || t.isEnum() || t.isInterface() || t == String.class) {
                    continue;
                }
                if (isAabbLike(t)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 在类型继承链上找 AABB 字段：字段类型符合 AABB 特征（6~8 连续 float/double）。
     *  instance 非 null 时优先返回值非 null 的字段（真实碰撞箱）。 */
    private static Field findAabbOnType(Class<?> clazz, Object instance) {
        // 第一遍：值非 null 优先
        if (instance != null) {
            for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    Class<?> t = f.getType();
                    if (t.isPrimitive() || t.isArray() || t.isEnum() || t.isInterface() || t == String.class) {
                        continue;
                    }
                    if (!isAabbLike(t)) {
                        continue;
                    }
                    try {
                        Object v = fieldVal(f, instance);
                        if (v != null) {
                            return f;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        // 第二遍：类型级兜底
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                Class<?> t = f.getType();
                if (t.isPrimitive() || t.isArray() || t.isEnum() || t.isInterface() || t == String.class) {
                    continue;
                }
                if (isAabbLike(t)) {
                    return f;
                }
            }
        }
        return null;
    }

    private static Object fieldVal(Field f, Object instance) {
        try {
            f.setAccessible(true);
            return f.get(instance);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 类中"连续同类（全 float 或全 double）"数值字段 run 长度在 6~8 = AABB 特征。
     *  AxisAlignedBB 恰 6 个（Indev 含 epsilon 为 7）；Entity 自身 9+ 个连续坐标字段不满足。
     *  额外约束：AABB 是纯数据类，其继承链上的数值字段总数 < 10；
     *  像 EntityFish 之类带 6 个连续 float 的 Entity 子类，继承链上一共有 10+ 个数值字段，会被排除。 */
    static boolean isAabbLike(Class<?> clazz) {
        Field[] fields;
        try {
            fields = clazz.getDeclaredFields();
        } catch (Throwable ignored) {
            return false;
        }
        int run = 0;
        Class<?> runType = null;
        int maxRun = 0;
        for (Field f : fields) {
            if ((f.getType() != float.class && f.getType() != double.class)
                    || Modifier.isStatic(f.getModifiers())) {
                run = 0;
                runType = null;
                continue;
            }
            if (runType == null || f.getType() != runType) {
                runType = f.getType();
                run = 1;
            } else {
                run++;
            }
            if (run > maxRun) {
                maxRun = run;
            }
        }
        return maxRun >= 6 && maxRun <= 8 && countFloatOrDoubleInHierarchy(clazz) < 10;
    }

    /** 实体特征：类型及其父类声明的 float/double 字段总数。 */
    private static int countFloatOrDoubleInHierarchy(Class<?> clazz) {
        int count = 0;
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() == float.class || f.getType() == double.class) {
                    count++;
                }
            }
        }
        return count;
    }

    /** 候选是否为"实体/玩家"：继承链上的 float/double 字段总数足够多。 */
    private static boolean isPlayerLike(Class<?> clazz) {
        return countFloatOrDoubleInHierarchy(clazz) >= 10;
    }

    /** 列出类及其父类的全部字段（带当前值判定），用于诊断。 */
    private static String dumpFields(Class<?> clazz, Object instance) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                String valDesc = "";
                if (!f.getType().isPrimitive()) {
                    try {
                        f.setAccessible(true);
                        Object v = f.get(instance);
                        valDesc = " = " + (v == null ? "NULL" : v.getClass().getSimpleName());
                    } catch (Throwable ignored) {
                        valDesc = " = <no-access>";
                    }
                }
                sb.append("  ").append(f.getName()).append(" : ").append(f.getType().getName()).append(valDesc).append('\n');
                if (++count >= 60) {
                    sb.append("  ...（其余省略）\n");
                    return sb.toString();
                }
            }
        }
        return sb.toString();
    }

    /** 诊断用：列出 Minecraft 主类里每个引用类型字段的继承链及字段分布。 */
    private static String dumpChainOfReferences(Class<?> mcClass) {
        StringBuilder sb = new StringBuilder();
        int refCount = 0;
        for (Class<?> c = mcClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                Class<?> type = f.getType();
                if (type.isPrimitive() || type.isArray() || type.isEnum() || type.isInterface() || type == String.class) {
                    continue;
                }
                if (++refCount > 20) {
                    sb.append("  ...（其余候选省略）\n");
                    return sb.toString();
                }
                sb.append("  === ").append(f.getName()).append(" : ").append(type.getName()).append(" ===\n");
                for (Class<?> cc = type; cc != null && cc != Object.class; cc = cc.getSuperclass()) {
                    sb.append("      class ").append(cc.getName()).append('\n');
                    int fcount = 0;
                    for (Field ff : cc.getDeclaredFields()) {
                        sb.append("        ").append(ff.getName()).append(" : ").append(ff.getType().getSimpleName());
                        if (ff.getType() == float.class || ff.getType() == double.class) {
                            sb.append("  <-- f/d");
                        }
                        sb.append('\n');
                        if (++fcount > 30) {
                            sb.append("        ...（其余字段省略）\n");
                            break;
                        }
                    }
                }
            }
        }
        return sb.toString();
    }
}