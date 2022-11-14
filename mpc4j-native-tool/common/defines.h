/*
 * Created by Weiran Liu on 2021/12/11.
 *
 * 2022/10/19 updates:
 * Thanks the anonymous USENIX Security 2023 AE reviewer for the suggestion.
 * We now use std::reverse to implement reverseBytes.
 */

#include <cstdint>
#include <jni.h>
#include <vector>
#include <string>
#include <NTL/GF2E.h>
#include <cstring>
#include <algorithm>

#ifndef MPC4J_NATIVE_TOOL_DEFINES_H
#define MPC4J_NATIVE_TOOL_DEFINES_H

#define BLOCK_BYTE_LENGTH 16

/**
 * 调换字节数组的顺序。
 *
 * @param data 字节数组。
 * @param size 字节数组的长度。
 */
inline void reverseBytes(uint8_t* data, uint64_t size) {
    uint8_t *low = data;
    uint8_t *high = data + size;
    std::reverse(low, high);
}

inline uint64_t ceilLog2(uint64_t x) {
    static const uint64_t t[6] = {
            0xFFFFFFFF00000000ull,
            0x00000000FFFF0000ull,
            0x000000000000FF00ull,
            0x00000000000000F0ull,
            0x000000000000000Cull,
            0x0000000000000002ull
    };

    uint64_t y = (((x & (x - 1)) == 0) ? 0 : 1);
    uint64_t j = 32;
    uint64_t i;

    for (i = 0; i < 6; i++) {
        uint64_t k = (((x & t[i]) == 0) ? 0 : j);
        y += k;
        x >>= k;
        j >>= 1;
    }

    return y;
}

void initGF2E(JNIEnv *env, jbyteArray jMinBytes);

/**
 * 将jobjectArray表示的二维字节数组数据解析为set<byte[]>。
 *
 * @param env JNI环境。
 * @param jBytesArray 用jobjectArray表示的二维字节数组。
 * @param byteLength 二维字节数组每个维度的长度。
 * @param set 转换结果存储地址。
 */
void jByteArrayToSet(JNIEnv *env, jobjectArray jBytesArray, uint64_t byteLength, std::vector<uint8_t*> &set);

/**
 * 将用set<byte[]>表示的数据转换为jobjectArray表示的二维字节数组。
 *
 * @param env JNI环境。
 * @param set set<byte[]>表示的数据。
 * @param byteLength 二维字节数组每个维度的长度。
 * @param jNum 转换后的byte[][]数组长度。
 * @param jArray 转换结果存储地址。
 */
void setTojByteArray(JNIEnv *env, std::vector<uint8_t*> &set, uint64_t byteLength, jint jNum, jobjectArray &jArray);

/**
 * 释放字节数组集合中各个元素的内存。
 *
 * @param set 字节数组集合
 */
void freeByteArraySet(std::vector<uint8_t*> &set);

/**
 * 将jlongArray表示的二维长整数数组解析为set<long>。
 *
 * @param env JNI环境。
 * @param jLongArray 用jlongArray表示的二维长整型数组。
 * @param set 转换结果存储地址。
 */
void jLongArrayToSet(JNIEnv *env, jlongArray jLongArray, std::vector<long> &set);

/**
 * 将用set<long>表示的数据转换为jlongArray表示的二维长整型数组。
 *
 * @param env JNI环境。
 * @param set set<long>表示的数据。
 * @param jNum 转换后的long[]数组长度。
 * @param jLongArray 转换结果存储地址。
 */
void setTojLongArray(JNIEnv *env, std::vector<long> &set, jint jNum, jlongArray &jLongArray);

/**
 * 将用jobjectArray表示的二维字符串数组解析为set<std::string>。
 *
 * @param env JNI环境。
 * @param jStringArray 用jStringArray表示的二维长整型数组。
 * @param set 转换结果存储地址。
 */
void jStringArrayToSet(JNIEnv *env, jobjectArray jStringArray, std::vector<std::string> &set);

/**
 * 将用set<std::string>表示的数据转换为jobjectArray表示的二维字符串数组。
 *
 * @param env JNI环境。
 * @param set set<std::string>表示的数据。
 * @param jLongArray 转换结果存储地址。
 */
void setTojStringArray(JNIEnv *env, std::vector<std::string> &set, jobjectArray &jStringArray);

#endif //MPC4J_NATIVE_TOOL_DEFINES_H
