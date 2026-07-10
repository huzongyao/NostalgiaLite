NostalgiaLite
=============

[![Release](https://img.shields.io/github/v/release/huzongyao/NostalgiaLite?label=release)](https://github.com/huzongyao/NostalgiaLite/releases/latest)

NostalgiaLite 是三款经典掌机/主机模拟器的 Android 移植工程，包含 FC/NES、Game Boy Color、Sega Game Gear 三个独立应用模块，并共用同一套模拟器框架层。

直接体验：[下载最新 APK](https://github.com/huzongyao/NostalgiaLite/releases/latest)

## 支持平台

| 模块 | 应用包名 | 平台 | 核心引擎 | 常见 ROM |
|------|----------|------|----------|----------|
| `appnes` | `nostalgia.appnes` | FC / NES | fceux | `.nes`, `.zip` |
| `appgbc` | `nostalgia.appgbc` | Game Boy / Game Boy Color | libgambatte | `.gb`, `.gbc`, `.zip` |
| `appgg` | `nostalgia.appgg` | Sega Game Gear / Master System | sms_plus | `.gg`, `.sms`, `.zip` |

## 功能特性

| 功能 | 说明 |
|------|------|
| 多平台模拟 | 三个应用模块共享 `framework`，各自接入独立 C/C++ 模拟器内核 |
| ROM 导入 | 支持系统文件选择器、本地文件扫描、从其他应用分享 ROM |
| ZIP 支持 | 可识别 ZIP 压缩包内的 ROM，并缓存 ZIP 与游戏条目的关系 |
| 即时存档 | 支持多槽位保存/读取，并为存档生成截图 |
| 时间回溯 | 通过历史状态缓存回退到之前的游戏状态 |
| 金手指 | 支持平台特定金手指和原始地址金手指 |
| 截图 | 使用 MediaStore 保存到系统图库的 Pictures 目录 |
| 输入控制 | 支持触摸虚拟按键、键盘和可扩展控制器映射 |

## 项目结构

```text
NostalgiaLite/
├── framework/                    # 公共框架库
│   ├── src/main/java/nostalgia/framework/base/
│   │   ├── JniEmulator.java      # Java 层模拟器基类
│   │   ├── JniBridge.java        # JNI native 方法声明
│   │   └── EmulatorActivity.java # 游戏运行 Activity 基类
│   ├── src/main/java/nostalgia/framework/controllers/
│   │   └── ...                   # 触摸、键盘、快捷存档等输入控制
│   ├── src/main/java/nostalgia/framework/data/
│   │   └── ...                   # Room 数据库、DAO、实体与 Repository
│   ├── src/main/java/nostalgia/framework/ui/
│   │   └── ...                   # 游戏列表、设置、金手指、时间回溯等 UI
│   └── src/main/java/nostalgia/framework/utils/
│       └── ...                   # 文件、日志、数据库、设备能力等工具类
├── appnes/                       # FC/NES 应用模块，C++ 层封装 fceux
├── appgbc/                       # GBC 应用模块，C++ 层封装 libgambatte
├── appgg/                        # GG/SMS 应用模块，C++ 层封装 sms_plus
├── misc/                         # 截图、素材等辅助文件
└── gradle/                       # Gradle Wrapper
```

## 技术架构

核心调用链：

```text
Activity / Controller
        ↓
Emulator 接口
        ↓
JniEmulator
        ↓
JniBridge native 方法
        ↓
C/C++ 平台模拟器内核
```

主要技术点：

| 技术点 | 说明 |
|--------|------|
| Java + C++ JNI | Java 负责应用、UI、输入和生命周期，C/C++ 负责模拟器核心 |
| CMake Native Build | 每个应用模块都有独立 `src/main/cpp/CMakeLists.txt` |
| OpenGL ES 2.0 | 用于游戏画面渲染和着色器处理 |
| PCM 音频流 | `AudioTrack` 输出模拟器生成的音频数据 |
| Room 数据库 | 管理 ROM、ZIP 条目、存档和游戏列表缓存 |
| AndroidX | UI、兼容层和 Material 组件 |

## 构建环境

当前工程配置：

| 项目 | 版本 / 配置 |
|------|-------------|
| Android Gradle Plugin | `8.9.1` |
| Gradle Wrapper | `8.11.1` |
| Java 语言级别 | Java 8 |
| App `compileSdk` / `targetSdk` | `36` / `36` |
| Framework `compileSdk` / `targetSdk` | `33` / `33` |
| `minSdk` | `15` |
| NDK | 建议使用 `26.1.10909125` |
| ABI | `armeabi-v7a`, `arm64-v8a`, `x86` |

## 编译命令

在项目根目录执行：

```bash
./gradlew :appnes:assembleDebug
./gradlew :appgbc:assembleDebug
./gradlew :appgg:assembleDebug
```

构建 Release 包：

```bash
./gradlew :appnes:assembleRelease
./gradlew :appgbc:assembleRelease
./gradlew :appgg:assembleRelease
```

构建产物命名格式为：

```text
<module>-<variant>-V<versionCode>.apk
```

例如：

```text
appnes-debug-V1501.apk
```

> 当前仓库包含 `demokey` 签名配置，适合本地调试和测试发布流程；正式发布前请替换为自己的签名文件，并避免提交私有密钥。

## 使用说明

1. 安装对应平台的 APK。
2. 在游戏列表页点击导入按钮，选择 ROM 文件或 ZIP 压缩包。
3. 也可以从文件管理器、网盘等应用将 ROM 分享到 NostalgiaLite。
4. 进入游戏后使用屏幕虚拟按键操作，菜单中可进行截图、存档、读档、金手指和时间回溯。

## 开发说明

| 约定 | 说明 |
|------|------|
| 公共能力优先放在 `framework` | 三个模拟器共用的逻辑应避免在应用模块中重复实现 |
| 第三方内核谨慎修改 | `fceux`、`libgambatte`、`sms_plus` 目录尽量只做必要适配 |
| JNI 边界保持清晰 | Java 层只通过 `JniBridge` 调用 native 方法，平台差异由各应用模块实现 |
| 日志保持有价值 | 错误和关键流程保留日志，避免在高频渲染/输入路径输出无意义日志 |
| ROM 文件不入库 | 测试 ROM、个人存档、构建产物不应提交到仓库 |

## 第三方引擎

| 引擎 | 用途 | 目录 |
|------|------|------|
| fceux | FC/NES 模拟 | `appnes/src/main/cpp/fceux/` |
| libgambatte | GB/GBC 模拟 | `appgbc/src/main/cpp/libgambatte/` |
| sms_plus | GG/SMS 模拟 | `appgg/src/main/cpp/sms_plus/` |

## 截图预览

| NES | GBC | GG |
|:---:|:---:|:---:|
| ![NES截图](https://github.com/huzongyao/NostalgiaLite/blob/master/misc/screen-nes.gif?raw=true) | ![GBC截图](https://github.com/huzongyao/NostalgiaLite/blob/master/misc/screen-gbc.gif?raw=true) | ![GG截图](https://github.com/huzongyao/NostalgiaLite/blob/master/misc/screen-gg.gif?raw=true) |

## 版权与 ROM 说明

本项目仅提供模拟器程序本身，不包含任何商业游戏 ROM。请仅使用你合法拥有的游戏备份文件。

## 关于作者

* GitHub: [https://huzongyao.github.io/](https://huzongyao.github.io/)
* ITEye 博客：[https://hzy3774.iteye.com/](https://hzy3774.iteye.com/)
* 新浪微博: [https://weibo.com/hzy3774](https://weibo.com/hzy3774)

## 联系方式

* QQ: [377406997](https://wpa.qq.com/msgrd?v=3&uin=377406997&site=qq&menu=yes)
* Gmail: [hzy3774@gmail.com](mailto:hzy3774@gmail.com)
* Foxmail: [hzy3774@qq.com](mailto:hzy3774@qq.com)
* 微信: hzy3774

## 捐赠支持

* 想捐钱我喝杯热水（¥0.01 起捐）

![donate](https://github.com/huzongyao/JChineseChess/blob/master/misc/donate.png?raw=true)
