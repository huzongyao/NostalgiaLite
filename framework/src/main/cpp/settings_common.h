/**
 * 模拟器共享类型定义。
 * <p>定义三个平台（NES/GBC/GG）共用的缓冲区和调色板类型。
 * 各平台特有的 GET_PIXEL 宏保留在各模块本地的 settings.h 中。</p>
 */
#ifndef SETTINGS_COMMON_H_
#define SETTINGS_COMMON_H_

#define BUFFER_TYPE unsigned char
#define PALETTE_TYPE unsigned int

#endif
