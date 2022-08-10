//
// Created by Weiran Liu on 2022/1/5.
//
#include "defines.h"
#include <NTL/GF2E.h>
#include <cstring>

void initGF2E(JNIEnv *env, jbyteArray jMinBytes) {
    // 读取最小多项式系数
    uint64_t minBytesLength = (*env).GetArrayLength(jMinBytes);
    jbyte* jMinBytesBuffer = (*env).GetByteArrayElements(jMinBytes, nullptr);
    auto* minBytes = new uint8_t[minBytesLength];
    memcpy(minBytes, jMinBytesBuffer, minBytesLength);
    reverseBytes(minBytes, minBytesLength);
    (*env).ReleaseByteArrayElements(jMinBytes, jMinBytesBuffer, 0);
// 设置有限域
    NTL::GF2X finiteField = NTL::GF2XFromBytes(minBytes, (long)minBytesLength);
    NTL::GF2E::init(finiteField);
    delete[] minBytes;
}

void jByteArrayToSet(JNIEnv *env, jobjectArray jBytesArray, uint64_t byteLength, std::vector<uint8_t*> &set) {
    uint64_t length = (*env).GetArrayLength(jBytesArray);
    set.resize(length);
    for (uint64_t i = 0; i < length; i++) {
        // 读取第i个数据
        auto jElement = (jbyteArray)(*env).GetObjectArrayElement(jBytesArray, (jsize)i);
        jbyte* jElementBuffer = (*env).GetByteArrayElements(jElement, nullptr);
        auto* data = new uint8_t [byteLength];
        memcpy(data, jElementBuffer, byteLength);
        // Java是大端表示，需要先reverse
        reverseBytes(data, byteLength);
        set[i] = data;
        // 释放jx，jxBuffer，jy，jyBuffer
        (*env).ReleaseByteArrayElements(jElement, jElementBuffer, 0);
    }
}

void setTojByteArray(JNIEnv *env, std::vector<uint8_t*> &set, uint64_t byteLength, jint jNum, jobjectArray &jArray) {
    jclass jByteArrayType = (*env).FindClass("[B");
    // 为转换结果分配内存
    jArray = (*env).NewObjectArray(jNum, jByteArrayType, nullptr);
    // 复制结果
    for (uint64_t i = 0; i < set.size(); i++) {
        jbyteArray jElement = (*env).NewByteArray((jsize)byteLength);
        jbyte* jElementBuffer = (*env).GetByteArrayElements(jElement, nullptr);
        // Java是大端表示，需要先reverse
        reverseBytes(set[i], byteLength);
        // 拷贝结果
        memcpy(jElementBuffer, set[i], byteLength);
        (*env).SetObjectArrayElement(jArray, (jsize)i, jElement);
        // 释放内存
        (*env).ReleaseByteArrayElements(jElement, jElementBuffer, 0);
    }
    // 补足剩余的系数
    for (uint64_t i = set.size(); i < jNum; i++) {
        jbyteArray jZeroElement = (*env).NewByteArray((jsize)byteLength);
        jbyte* jZeroElementBuffer = (*env).GetByteArrayElements(jZeroElement, nullptr);
        (*env).SetObjectArrayElement(jArray, (jsize)i, jZeroElement);
        // 释放jCoeff，jCoeffBuffer
        (*env).ReleaseByteArrayElements(jZeroElement, jZeroElementBuffer, 0);
    }
}

void jLongArrayToSet(JNIEnv *env, jlongArray jLongArray, std::vector<long> &set) {
    uint64_t length = (*env).GetArrayLength(jLongArray);
    set.resize(length);
    jlong* jLongPtr = (*env).GetLongArrayElements(jLongArray, JNI_FALSE);
    auto* data = new long[length];
    memcpy(data, jLongPtr, length * sizeof(long));
    for (uint64_t i = 0; i < length; i++) {
        // 读取第i个数据
        set[i] = data[i];
    }
    // 释放资源
    (*env).ReleaseLongArrayElements(jLongArray, jLongPtr, 0);
}

void setTojLongArray(JNIEnv *env, std::vector<long> &set, jint jNum, jlongArray &jLongArray) {
    auto * data = new long[jNum];
    // 复制结果
    for (uint64_t i = 0; i < set.size(); i++) {
        data[i] = set[i];
    }
    // 补足剩余的系数
    for (uint64_t i = set.size(); i < jNum; i++) {
        data[i] = 0L;
    }
    // 为转换结果分配内存
    jLongArray = (*env).NewLongArray(jNum);
    (*env).SetLongArrayRegion(jLongArray, 0, jNum, data);
}