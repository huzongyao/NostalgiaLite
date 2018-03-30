#ifndef SETTINGS_H_
#define SETTINGS_H_

#define BRIDGE_PACKAGE(x) Java_nostalgia_framework_base_JniBridge_ ## x
#define BUFFER_TYPE unsigned char
#define PALETTE_TYPE unsigned int

#define GET_PIXEL(buf, idx) 0xff000000 | (buf[idx + (160*(144+26))] << 8) |  buf[idx + 160*(144+26)*2] << 16 | (buf[idx] << 0)

#endif


