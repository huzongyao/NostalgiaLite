NostalgiaLite（游戏模拟器）
==================

三款经典游戏平台模拟器（FC/NES、GG、GBC）的 Android 移植版。

[![Release](https://img.shields.io/appveyor/ci/gruntjs/grunt.svg)](https://github.com/huzongyao/NostalgiaLite/releases/latest)

## 项目简介

本项目包含三个运行在 Android 平台上的经典游戏模拟器：
- **FC (NES) 模拟器** — 任天堂红白机模拟器，基于 fceux 引擎
- **GG 模拟器** — Sega Game Gear 模拟器，基于 sms_plus 引擎
- **GBC 模拟器** — Game Boy Color 模拟器，基于 libgambatte 引擎

直接下载体验：[下载 APK](https://github.com/huzongyao/NostalgiaLite/releases/latest)

## 项目结构

```
NostalgiaLite/
├── framework/          # 公共框架库模块
│   ├── base/           # 基础类（JNI桥接、模拟器抽象、Activity基类）
│   ├── controllers/    # 输入控制器（触摸、键盘、手柄）
│   ├── data/           # 数据层（Room数据库、DAO、实体）
│   ├── ui/             # UI组件（画廊、设置、金手指、时间回溯）
│   └── utils/          # 工具类（文件、数据库、日志、注解）
├── appnes/             # FC/NES 模拟器应用模块
│   ├── java/           # Java 层（Core、Application、Emulator、Activity）
│   └── cpp/            # C++ 层（JNI桥接、模拟器基类、fceux引擎封装）
├── appgbc/             # GBC 模拟器应用模块
│   ├── java/           # Java 层
│   └── cpp/            # C++ 层（libgambatte引擎封装）
└── appgg/              # GG 模拟器应用模块
    ├── java/           # Java 层
    └── cpp/            # C++ 层（sms_plus引擎封装）
```

## 技术架构

- **语言**: Java + C++ (JNI)
- **构建工具**: Android Gradle Plugin + CMake
- **数据库**: Room（自定义注解 ORM 兼容层）
- **图形渲染**: OpenGL ES 2.0 + GLSL 着色器
- **音频处理**: 双缓冲 PCM 音频流
- **架构模式**: 模拟器抽象层（Emulator → JniEmulator → JniBridge → C++ Emulator）

### 核心技术点

| 技术点 | 说明 |
|--------|------|
| **三缓冲图形交换** | 稳定/工作/副本三缓冲区，避免渲染撕裂 |
| **Bresenham 缩放渲染** | 高效的整数缩放算法，将原始画面放大到目标尺寸 |
| **RGB 三通道分离纹理** | GBC/GG 使用 Alpha 通道分离存储 RGB，着色器合成显示 |
| **双缓冲音频** | 读写分离的环形音频缓冲区，避免音频卡顿 |
| **时间回溯** | 环形缓冲区保存历史状态，支持回退游玩 |
| **金手指系统** | 支持原始地址金手指和平台特定格式 |

## 功能特性

| 特性 | 说明 |
|------|------|
| 🎮 **多平台支持** | 支持FC(NES)、GG、GBC三种游戏平台 |
| 📁 **安全的ROM导入** | 使用系统文件选择器，用户主动选择ROM文件 |
| 🔗 **文件分享支持** | 支持从其他应用分享ROM文件到模拟器 |
| 📸 **截图保存** | 一键截图并保存到系统图库 |
| 💾 **存档管理** | 支持即时存档/读档，多槽位管理 |
| 🎯 **金手指** | 支持多种金手指格式 |
| ⏪ **时间回溯** | 可回退到之前的游戏状态 |
| 🔒 **隐私保护** | 无需存储权限，符合Android隐私要求 |
| 📱 **Android 15+支持** | 支持16KB页面大小，符合最新Google Play要求 |

## 使用方法

1. **导入ROM文件**
   - 点击菜单中的文件夹图标
   - 选择您的ROM文件（支持 .nes, .gb, .gbc, .gg, .sms, .zip）
   - 或者从其他应用分享ROM文件到本应用

2. **游戏控制**
   - 触摸屏幕上的虚拟按键进行游戏
   - 按菜单键打开游戏菜单
   - 支持快速存档/读档功能

3. **截图功能**
   - 在游戏菜单中选择"截图"选项
   - 截图会自动保存到系统图库

## 编译构建

* 需要 Android NDK（当前使用 26.1.10909125）
* 使用 CMake 编译本地代码
* Gradle 版本: 8.11.1（支持 JDK 21）
* Android Gradle Plugin: 8.9.1

### 第三方引擎

| 引擎 | 用途 | 目录 |
|------|------|------|
| fceux | FC/NES 模拟 | `appnes/src/main/cpp/fceux/` |
| libgambatte | GBC 模拟 | `appgbc/src/main/cpp/libgambatte/` |
| sms_plus | GG/SMS 模拟 | `appgg/src/main/cpp/sms_plus/` |

> 注意：以上第三方引擎源代码不在本项目的注释范围内，请勿修改。

## 截图预览

| NES | GBC | GG |
|:---:|:---:|:---:|
| ![NES截图](https://github.com/huzongyao/NostalgiaLite/blob/master/misc/screen-nes.gif?raw=true) | ![GBC截图](https://github.com/huzongyao/NostalgiaLite/blob/master/misc/screen-gbc.gif?raw=true) | ![GG截图](https://github.com/huzongyao/NostalgiaLite/blob/master/misc/screen-gg.gif?raw=true) |

## 关于作者

* GitHub: [https://huzongyao.github.io/](https://huzongyao.github.io/)
* ITEye博客：[https://hzy3774.iteye.com/](https://hzy3774.iteye.com/)
* 新浪微博: [https://weibo.com/hzy3774](https://weibo.com/hzy3774)

## 联系方式

* QQ: [377406997](https://wpa.qq.com/msgrd?v=3&uin=377406997&site=qq&menu=yes)
* Gmail: [hzy3774@gmail.com](mailto:hzy3774@gmail.com)
* Foxmail: [hzy3774@qq.com](mailto:hzy3774@qq.com)
* 微信: hzy3774

## 捐赠支持

* 想捐钱我喝杯热水(¥0.01起捐)

![donate](https://github.com/huzongyao/JChineseChess/blob/master/misc/donate.png?raw=true)
