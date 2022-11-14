/*
 * Created by Weiran Liu on 2022/4/27.
 *
 * 2022/10/19 updates:
 * Thanks the anonymous USENIX Security 2023 AE reviewer for the suggestion.
 * All heap allocations (e.g., auto *p = new uint8_t[]) are replaced with stack allocations (e.g., uint8_t p[]).
 */

#include <NTL/GF2E.h>
#include "edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_NtlGf2k.h"
#include "defines.h"

/**
 * GF(2^128)使用的模数为f(x) = x^128 + x^7 + x^2 + x + 1，NTL需要使用小端表示
 */
const u_int8_t GF2K_MIN_BYTES[BLOCK_BYTE_LENGTH + 1] = {
        0x87, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x01,
};

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_NtlGf2k_nativeMul
        (JNIEnv *env, jobject context, jbyteArray ja, jbyteArray jb) {
    // 设置有限域
    NTL::GF2X finiteField = NTL::GF2XFromBytes(GF2K_MIN_BYTES, BLOCK_BYTE_LENGTH + 1);
    NTL::GF2E::init(finiteField);
    // 转换a
    jbyte *jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    uint8_t dataA[BLOCK_BYTE_LENGTH];
    memcpy(dataA, jaBuffer, BLOCK_BYTE_LENGTH);
    reverseBytes(dataA, BLOCK_BYTE_LENGTH);
    NTL::GF2X aFromBytes = NTL::GF2XFromBytes(dataA, (long) BLOCK_BYTE_LENGTH);
    NTL::GF2E a = to_GF2E(aFromBytes);
    (*env).ReleaseByteArrayElements(ja, jaBuffer, 0);
    // 转换b
    jbyte *jbBuffer = (*env).GetByteArrayElements(jb, nullptr);
    uint8_t dataB[BLOCK_BYTE_LENGTH];
    memcpy(dataB, jbBuffer, BLOCK_BYTE_LENGTH);
    reverseBytes(dataB, BLOCK_BYTE_LENGTH);
    NTL::GF2X bFromBytes = NTL::GF2XFromBytes(dataB, (long) BLOCK_BYTE_LENGTH);
    NTL::GF2E b = to_GF2E(bFromBytes);
    (*env).ReleaseByteArrayElements(jb, jbBuffer, 0);
    // 乘法
    NTL::GF2E c = a * b;
    // 返回结果
    BytesFromGF2X(dataA, NTL::rep(c), (long) BLOCK_BYTE_LENGTH);
    reverseBytes(dataA, BLOCK_BYTE_LENGTH);
    jbyteArray jc = (*env).NewByteArray((jsize) BLOCK_BYTE_LENGTH);
    (*env).SetByteArrayRegion(jc, 0, BLOCK_BYTE_LENGTH, (const jbyte *) dataA);

    return jc;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_NtlGf2k_nativeMuli
        (JNIEnv *env, jobject context, jbyteArray ja, jbyteArray jb) {
    // 设置有限域
    NTL::GF2X finiteField = NTL::GF2XFromBytes(GF2K_MIN_BYTES, BLOCK_BYTE_LENGTH + 1);
    NTL::GF2E::init(finiteField);
    // 转换a
    jbyte *jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    uint8_t dataA[BLOCK_BYTE_LENGTH];
    memcpy(dataA, jaBuffer, BLOCK_BYTE_LENGTH);
    reverseBytes(dataA, BLOCK_BYTE_LENGTH);
    NTL::GF2X aFromBytes = NTL::GF2XFromBytes(dataA, (long) BLOCK_BYTE_LENGTH);
    NTL::GF2E a = to_GF2E(aFromBytes);
    (*env).ReleaseByteArrayElements(ja, jaBuffer, 0);
    // 转换b
    jbyte *jbBuffer = (*env).GetByteArrayElements(jb, nullptr);
    uint8_t dataB[BLOCK_BYTE_LENGTH];
    memcpy(dataB, jbBuffer, BLOCK_BYTE_LENGTH);
    reverseBytes(dataB, BLOCK_BYTE_LENGTH);
    NTL::GF2X bFromBytes = NTL::GF2XFromBytes(dataB, (long) BLOCK_BYTE_LENGTH);
    NTL::GF2E b = to_GF2E(bFromBytes);
    (*env).ReleaseByteArrayElements(jb, jbBuffer, 0);
    // 乘法
    NTL::GF2E c = a * b;
    // 返回结果
    BytesFromGF2X(dataA, NTL::rep(c), (long) BLOCK_BYTE_LENGTH);
    reverseBytes(dataA, BLOCK_BYTE_LENGTH);
    // 不能(*env).ReleaseByteArrayElements(ja, jaBuffer, 0)，否则结果会有错误
    (*env).SetByteArrayRegion(ja, 0, BLOCK_BYTE_LENGTH, (const jbyte *) dataA);
}