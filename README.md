# Position-Hack - 旧版 Minecraft 单机坐标修改器

**一个通过 Attach API 注入的、针对旧版 Minecraft（Beta 1.7.3 及以前）的外置式坐标修改器。**

无需安装 RetroWrapper，不修改 Minecraft 的任何文件——启动游戏后再注入，用反射在游戏内部直接读写玩家坐标。

> [!IMPORTANT]
> **📢 重要提示：**
> 1. 本工具是开源第三方工具，**只配合用户自行准备的正版 Minecraft 使用**，不提供、不包含任何游戏资源。
> 2. 由于程序通过 Java Attach API 注入其他进程，部分杀毒软件可能**误报**。本项目完全开源，请放心运行，或将其加入白名单。
> 3. 注入需要游戏**已进入世界**（玩家已生成），未进入世界时工具会轮询等待，超时 60 秒。

---

## ⭐ 核心功能特性

- **外置式注入**：游戏启动后再注入，随时开随时关，对游戏本体零改动、无残留。
- **跨版本兼容**：基于 AABB（`AxisAlignedBB`）坐标模型 + 继承链启发式定位，**不依赖混淆字段名**，兼容 Beta 1.7.3 及以前的所有版本（Alpha / Classic 实测通过）。
- **脱离 JDK**：注入器打包了 attach API 与原生库（`lib\tools.jar` + `natives\attach.dll`），运行时只需 **JRE 8**。
- **双入口交付**：图形化注入器（GUI）与命令行注入（`--attach <PID>`）都支持。
- **完整诊断日志**：Agent 全程写日志（位于用户主目录 `position-hack-agent.log`），注入失败可快速定位；遇到问题可连同日志一起提交到 [Issues](https://github.com/CreatorCSIE/Position-Hack/issues)。

---

## 🎮 支持版本

| 版本 | 坐标修改 |
| ---- | -------- |
| Beta 1.7.3 及以前（含 Alpha / Classic / Indev） | ✅ |
| Beta 1.8+ | ❌ 坐标模型迁移到实体 `pos` 字段，定向传送会回弹，故不支持 |

---

## 📂 目录结构 (Distribution Layout)

```text
Position-Hack/
├── position-hack.jar        # 最终产物（compile.bat 生成）
├── src/                     # 主源码
│   └── org/github/creatorcsie/positionhack/
│       ├── agent/           # Java Agent（注入后运行在 Minecraft 进程内）
│       └── loader/          # 注入器（GUI 注入器 + 命令行 attach）
├── test/                    # Mock 测试（模拟旧版 Minecraft 结构）
├── lib/tools.jar            # JDK attach API（compile.bat 自动从 JDK 生成）
├── natives/attach.dll       # attach 原生库（compile.bat 自动从 JDK 生成）
├── build/                   # 编译产物（.class，不入库）
├── compile.bat              # 构建脚本（需 JDK 8）
└── run.bat                  # 运行脚本（只需 JRE 8）
```

---

## 🔧 从 Release 下载（推荐，无需 JDK）

普通用户不需要源码，也不用安装 JDK——直接去 [Releases](https://github.com/CreatorCSIE/Position-Hack/releases) 下载**完整构建包**（zip），解压即可使用：

```text
完整构建包内容：
├── position-hack.jar      # 主程序
├── lib\tools.jar          # attach API（已内置，无需 JDK）
├── natives\attach.dll     # attach 原生库（已内置）
└── run.bat                # 一键运行入口
```

1. 下载并解压最新版 zip。
2. 启动你的旧版 Minecraft（Beta 1.7.3 及以前）并**进入世界**。
3. 双击 `run.bat` 打开注入器，选中 Minecraft 的 JVM 点击**注入**；或直接 `run.bat --attach <PID>`。

运行时只需 **JRE 8**（没有任何 Java 程序的话，安装 [Java 8 JRE](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html) 即可）。

---

## 🧰 构建 (Build)

*以下仅面向**开发者 / 想要自己编译**的用户。普通用户请走上方 Releases 下载，无需 JDK。*

需要 [JDK 8](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)（`compile.bat` 顶部可修改 JDK 路径）：

```bat
compile.bat          REM 编译并打包 position-hack.jar（同时从 JDK 生成 lib\tools.jar 与 natives\attach.dll）
compile.bat test     REM 额外编译 Mock 测试到 build-test\
```

*提示：`lib\tools.jar` 与 `natives\attach.dll` 是从 JDK 自动复制出来的运行时依赖，已加入 `.gitignore`，clone 后跑一次 `compile.bat` 即自动生成。*

---

## 💻 运行 (Run)

运行时只需 **JRE 8**（JDK 也可）：

```bat
run.bat                          REM 打开注入器 GUI（列出本机 JVM，选择后点注入）
run.bat --attach <PID>           REM 直接注入指定进程
run.bat mock                     REM 运行 Mock 验证（需先 compile.bat test）
```

**使用流程**：启动 Minecraft 并进入世界 → `run.bat` 打开注入器 → 选择 Minecraft 的 JVM 点击注入 → 游戏内弹出坐标窗口 → 读取当前坐标 / 输入目标坐标瞬移。

---

## 🧠 工作原理

1. 注入器用 `com.sun.tools.attach.VirtualMachine` 附加到 Minecraft 的 JVM，加载 Agent。
2. `MinecraftApplet` 在旧版 Minecraft 中始终存在，通过 `Frame.getFrames()` 遍历 AWT 组件树找到它。
3. 从 Applet 定位 Minecraft 主类实例，再沿继承链寻找"玩家实体"与它的碰撞箱（AABB）。
4. 旧版的玩家坐标承载在 AABB 的 6 个数值字段（`x, y, z, x2, y2, z2`）上：读坐标即读这 6 个字段；传送即改前 3 个字段、平移后 3 个以保持碰撞箱尺寸。

---

## 📄 许可证与版权声明 (License & Copyright)

- **开源协议**：本项目采用 **[GNU General Public License v3 (GPL v3)](LICENSE)** 协议开源。
- **版权所有**：Copyright (c) 2026 **CreatorCSIE**. All rights reserved.
- **法律免责声明**：
  - 本项目为开源第三方工具，与 **Mojang Studios** 或 **Microsoft** 无任何关联。
  - 本仓库**不包含、不分发**任何受版权保护的 Minecraft 二进制资源。使用本工具需要用户自行准备正版 Minecraft。
  - Minecraft 的商标及所有相关资产版权归其各自所有者所有。

---

## ❓ 常见问题

- **为什么构建要 JDK，运行却只要 JRE？** 编译期需要 `tools.jar` 里的 attach API；运行时所需的 attach 类与原生库已随项目分发（`lib\` + `natives\`），因此 JRE 就够了。
- **注入后窗口没出现？** 确认游戏已进入世界（玩家已生成），再查看 `position-hack-agent.log`（用户主目录）中的诊断输出。
- **Beta 1.8+ 为什么不支持？** Beta 1.8 起坐标迁移到实体自身的 `pos` 字段，传送须同步位置、动量、边界等多组字段，实测定向传送会回弹。因此支持范围定在 Beta 1.7.3 及以前。

---

## 🙏 参考与致谢 (Credits)

- 本项目的坐标修改思路参考了 **[RetroWrapper](https://github.com/NeRdTheNed/RetroWrapper)** 项目（其基于 RetroTweaker 向旧版 Minecraft 注入坐标修改功能）。本项目在此基础上实现了外置化（Attach API 注入 Java Agent，无需安装 RetroWrapper），并重构了跨版本兼容的启发式定位逻辑。
- 感谢所有旧版 Minecraft 研究与逆向社区的工具与资料。