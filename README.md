NostalgiaLite(游戏模拟器)
==================
Three game emulation FC(Nes), GG, GBC for Android.

[![Travis](https://img.shields.io/appveyor/ci/gruntjs/grunt.svg)](https://github.com/huzongyao/NostalgiaLite/releases/latest)

* 工程包含三个运行在安卓平台的游戏模拟器，FC(Nes)模拟器，GG模拟器，GBC模拟器
* 直接下载体验：[下载APK](https://github.com/huzongyao/NostalgiaLite/releases/latest)

#### Detail
* The code are from Nostalgia.NES, and it's an open source project
* For more information, you can visit their official website: http://nostalgiaemulators.com/
* I have changed and simplified some code and compiled it on android studio
* If you want to compile it, you will need android ndk: https://developer.android.com/ndk/index.html
* Now I compile native code with android cmake

#### 详情
* 主要代码是从这儿下载的：http://nostalgiaemulators.com/
* 我做了一些修改，使工程可以在android studio中编译
* 编译需要使用NDK： https://developer.android.com/ndk/index.html
* 使用CMake编译本地代码
* 可以下载[测试ROM](https://github.com/huzongyao/NostalgiaLite/releases/tag/v1.0)来玩：

#### Features (功能特性)

| 特性 | 说明 |
|------|------|
| 🎮 **多平台支持** | 支持FC(NES)、GG、GBC三种游戏平台 |
| 📁 **安全的ROM导入** | 使用系统文件选择器，用户主动选择ROM文件 |
| 🔗 **文件分享支持** | 支持从其他应用分享ROM文件到模拟器 |
| 📸 **截图保存** | 一键截图并保存到系统图库 |
| 🔒 **隐私保护** | 无需存储权限，符合Android隐私要求 |
| 📱 **Android 15+支持** | 支持16KB页面大小，符合最新Google Play要求 |

#### How to Use (使用方法)

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

#### Technical Updates (技术更新)

* **Gradle版本**: 8.11.1 (支持JDK 21)
* **Android Gradle Plugin**: 8.9.1
* **NDK版本**: 26.1.10909125
* **16KB页面大小支持**: 已配置，符合Android 15+要求

#### screenshot
| Nes        	| GBC           | GG  	|
| ------------- |:-------------:| -----:|
| ![screenshot](https://github.com/huzongyao/NostalgiaLite/blob/master/misc/screen-nes.gif?raw=true)| ![screenshot](https://github.com/huzongyao/NostalgiaLite/blob/master/misc/screen-gbc.gif?raw=true)| ![screenshot](https://github.com/huzongyao/NostalgiaLite/blob/master/misc/screen-gg.gif?raw=true) |

### About Me
 * GitHub: [https://huzongyao.github.io/](https://huzongyao.github.io/)
 * ITEye博客：[https://hzy3774.iteye.com/](https://hzy3774.iteye.com/)
 * 新浪微博: [https://weibo.com/hzy3774](https://weibo.com/hzy3774)

### Contact To Me
 * QQ: [377406997](https://wpa.qq.com/msgrd?v=3&uin=377406997&site=qq&menu=yes)
 * Gmail: [hzy3774@gmail.com](mailto:hzy3774@gmail.com)
 * Foxmail: [hzy3774@qq.com](mailto:hzy3774@qq.com)
 * WeChat: hzy3774

### Others
 * 想捐钱我喝杯热水(¥0.01起捐)
 * Donate me to buy a cup of hot water(Start from ¥0.01)</br>
 ![donate](https://github.com/huzongyao/JChineseChess/blob/master/misc/donate.png?raw=true)
