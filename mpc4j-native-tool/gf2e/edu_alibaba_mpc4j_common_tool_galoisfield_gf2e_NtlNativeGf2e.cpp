//
// Created by Weiran Liu on 2022/5/19.
//
#include <NTL/GF2E.h>
#include "edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e.h"
#include "defines.h"

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e_nativeMul
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jbyteArray ja, jbyteArray jb) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 转换a
    jbyte* jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    auto* dataA = new uint8_t[jByteL];
    memcpy(dataA, jaBuffer, jByteL);
    reverseBytes(dataA, jByteL);
    NTL::GF2X aFromBytes = NTL::GF2XFromBytes(dataA, (long)jByteL);
    NTL::GF2E a = to_GF2E(aFromBytes);
    (*env).ReleaseByteArrayElements(ja, jaBuffer, 0);
    // 转换b
    jbyte* jbBuffer = (*env).GetByteArrayElements(jb, nullptr);
    auto* dataB = new uint8_t[jByteL];
    memcpy(dataB, jbBuffer, jByteL);
    reverseBytes(dataB, jByteL);
    NTL::GF2X bFromBytes = NTL::GF2XFromBytes(dataB, (long)jByteL);
    NTL::GF2E b = to_GF2E(bFromBytes);
    (*env).ReleaseByteArrayElements(jb, jbBuffer, 0);
    // 乘法
    NTL::GF2E c = a * b;
    delete[] dataB;
    // 返回结果
    BytesFromGF2X(dataA, NTL::rep(c), (long)jByteL);
    reverseBytes(dataA, jByteL);
    jbyteArray jc = (*env).NewByteArray((jsize)jByteL);
    (*env).SetByteArrayRegion(jc, 0, jByteL, (const jbyte*)dataA);
    delete[] dataA;
    return jc;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e_nativeMuli
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jbyteArray ja, jbyteArray jb) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 转换a
    jbyte* jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    auto* dataA = new uint8_t[jByteL];
    memcpy(dataA, jaBuffer, jByteL);
    reverseBytes(dataA, jByteL);
    NTL::GF2X aFromBytes = NTL::GF2XFromBytes(dataA, (long)jByteL);
    NTL::GF2E a = to_GF2E(aFromBytes);
    // 转换b
    jbyte* jbBuffer = (*env).GetByteArrayElements(jb, nullptr);
    auto* dataB = new uint8_t[jByteL];
    memcpy(dataB, jbBuffer, jByteL);
    reverseBytes(dataB, jByteL);
    NTL::GF2X bFromBytes = NTL::GF2XFromBytes(dataB, (long)jByteL);
    NTL::GF2E b = to_GF2E(bFromBytes);
    (*env).ReleaseByteArrayElements(jb, jbBuffer, 0);
    // 乘法
    NTL::GF2E c = a * b;
    delete[] dataB;
    // 返回结果
    BytesFromGF2X(dataA, NTL::rep(c), (long)jByteL);
    reverseBytes(dataA, jByteL);
    (*env).SetByteArrayRegion(ja, 0, jByteL, (const jbyte*)dataA);
    // 不能(*env).ReleaseByteArrayElements(ja, jaBuffer, 0)，否则结果会有错误
    delete[] dataA;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e_nativeDiv
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jbyteArray ja, jbyteArray jb) {
// 初始化有限域
    initGF2E(env, jMinBytes);
    // 转换a
    jbyte* jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    auto* dataA = new uint8_t[jByteL];
    memcpy(dataA, jaBuffer, jByteL);
    reverseBytes(dataA, jByteL);
    NTL::GF2X aFromBytes = NTL::GF2XFromBytes(dataA, (long)jByteL);
    NTL::GF2E a = to_GF2E(aFromBytes);
    (*env).ReleaseByteArrayElements(ja, jaBuffer, 0);
    // 转换b
    jbyte* jbBuffer = (*env).GetByteArrayElements(jb, nullptr);
    auto* dataB = new uint8_t[jByteL];
    memcpy(dataB, jbBuffer, jByteL);
    reverseBytes(dataB, jByteL);
    NTL::GF2X bFromBytes = NTL::GF2XFromBytes(dataB, (long)jByteL);
    NTL::GF2E b = to_GF2E(bFromBytes);
    (*env).ReleaseByteArrayElements(jb, jbBuffer, 0);
    // 除法
    NTL::GF2E c = a / b;
    delete[] dataB;
    // 返回结果
    BytesFromGF2X(dataA, NTL::rep(c), (long)jByteL);
    reverseBytes(dataA, jByteL);
    jbyteArray jc = (*env).NewByteArray((jsize)jByteL);
    (*env).SetByteArrayRegion(jc, 0, jByteL, (const jbyte*)dataA);
    delete[] dataA;
    return jc;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e_nativeDivi
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jbyteArray ja, jbyteArray jb) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 转换a
    jbyte* jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    auto* dataA = new uint8_t[jByteL];
    memcpy(dataA, jaBuffer, jByteL);
    reverseBytes(dataA, jByteL);
    NTL::GF2X aFromBytes = NTL::GF2XFromBytes(dataA, (long)jByteL);
    NTL::GF2E a = to_GF2E(aFromBytes);
    // 转换b
    jbyte* jbBuffer = (*env).GetByteArrayElements(jb, nullptr);
    auto* dataB = new uint8_t[jByteL];
    memcpy(dataB, jbBuffer, jByteL);
    reverseBytes(dataB, jByteL);
    NTL::GF2X bFromBytes = NTL::GF2XFromBytes(dataB, (long)jByteL);
    NTL::GF2E b = to_GF2E(bFromBytes);
    (*env).ReleaseByteArrayElements(jb, jbBuffer, 0);
    // 乘法
    NTL::GF2E c = a / b;
    delete[] dataB;
    // 返回结果
    BytesFromGF2X(dataA, NTL::rep(c), (long)jByteL);
    reverseBytes(dataA, jByteL);
    (*env).SetByteArrayRegion(ja, 0, jByteL, (const jbyte*)dataA);
    // 不能(*env).ReleaseByteArrayElements(ja, jaBuffer, 0)，否则结果会有错误
    delete[] dataA;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e_nativeInv
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jbyteArray ja) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 转换a
    jbyte* jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    auto* dataA = new uint8_t[jByteL];
    memcpy(dataA, jaBuffer, jByteL);
    reverseBytes(dataA, jByteL);
    NTL::GF2X aFromBytes = NTL::GF2XFromBytes(dataA, (long)jByteL);
    NTL::GF2E a = to_GF2E(aFromBytes);
    // 计算a的逆
    NTL::GF2E c = inv(a);
    // 返回结果
    BytesFromGF2X(dataA, NTL::rep(c), (long)jByteL);
    reverseBytes(dataA, jByteL);
    jbyteArray jc = (*env).NewByteArray((jsize)jByteL);
    (*env).SetByteArrayRegion(jc, 0, jByteL, (const jbyte*)dataA);
    delete[] dataA;
    return jc;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e_nativeInvi
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jbyteArray ja) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 转换a
    jbyte* jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    auto* dataA = new uint8_t[jByteL];
    memcpy(dataA, jaBuffer, jByteL);
    reverseBytes(dataA, jByteL);
    NTL::GF2X aFromBytes = NTL::GF2XFromBytes(dataA, (long)jByteL);
    NTL::GF2E a = to_GF2E(aFromBytes);
    // 计算a的逆
    NTL::GF2E c = inv(a);
    // 返回结果
    BytesFromGF2X(dataA, NTL::rep(c), (long)jByteL);
    reverseBytes(dataA, jByteL);
    (*env).SetByteArrayRegion(ja, 0, jByteL, (const jbyte*)dataA);
    // 不能(*env).ReleaseByteArrayElements(ja, jaBuffer, 0)，否则结果会有错误
    delete[] dataA;
}