//
// Created by Weiran Liu on 2023/8/28.
//

#include <NTL/GF2E.h>
#include "edu_alibaba_mpc4j_common_tool_galoisfield_gf64_NtlGf64.h"
#include "defines.h"

#define HALF_BLOCK_BYTE_LENGTH 8

/**
 * moduli polynomial for GF(2^64) is f(x) = x^64 + x^4 + x^3 + x + 1, NTL needs little-endian.
 */
const u_int8_t GF64_MIN_BYTES[HALF_BLOCK_BYTE_LENGTH + 1] = {
    0x1B, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
    0x01,
};

void setGf64Field() {
    NTL::GF2X finiteField = NTL::GF2XFromBytes(GF64_MIN_BYTES, HALF_BLOCK_BYTE_LENGTH + 1);
    NTL::GF2E::init(finiteField);
}

NTL::GF2E convertGf64Element(JNIEnv *env, jbyteArray jElement) {
    jbyte *jElementBuffer = (*env).GetByteArrayElements(jElement, nullptr);
    uint8_t data[HALF_BLOCK_BYTE_LENGTH];
    memcpy(data, jElementBuffer, HALF_BLOCK_BYTE_LENGTH);
    reverseBytes(data, HALF_BLOCK_BYTE_LENGTH);
    NTL::GF2X elementFromBytes = NTL::GF2XFromBytes(data, (long) HALF_BLOCK_BYTE_LENGTH);
    NTL::GF2E element = to_GF2E(elementFromBytes);
    (*env).ReleaseByteArrayElements(jElement, jElementBuffer, 0);
    return element;
}

void setGf64Element(JNIEnv *env, const NTL::GF2E& element, jbyteArray jElement) {
    uint8_t data[HALF_BLOCK_BYTE_LENGTH];
    BytesFromGF2X(data, NTL::rep(element), (long) HALF_BLOCK_BYTE_LENGTH);
    reverseBytes(data, HALF_BLOCK_BYTE_LENGTH);
    (*env).SetByteArrayRegion(jElement, 0, HALF_BLOCK_BYTE_LENGTH, (const jbyte *) data);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf64_NtlGf64_nativeMul
    (JNIEnv *env, jobject context, jbyteArray ja, jbyteArray jb) {
    setGf64Field();
    NTL::GF2E a = convertGf64Element(env, ja);
    NTL::GF2E b = convertGf64Element(env, jb);
    NTL::GF2E c = a * b;
    jbyteArray jc = (*env).NewByteArray((jsize) HALF_BLOCK_BYTE_LENGTH);
    setGf64Element(env, c, jc);
    return jc;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf64_NtlGf64_nativeMuli
    (JNIEnv *env, jobject context, jbyteArray ja, jbyteArray jb) {
    setGf64Field();
    NTL::GF2E a = convertGf64Element(env, ja);
    NTL::GF2E b = convertGf64Element(env, jb);
    NTL::GF2E c = a * b;
    setGf64Element(env, c, ja);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf64_NtlGf64_nativeInv
    (JNIEnv *env, jobject context, jbyteArray ja) {
    setGf64Field();
    NTL::GF2E a = convertGf64Element(env, ja);
    NTL::GF2E c = inv(a);
    jbyteArray jc = (*env).NewByteArray((jsize) HALF_BLOCK_BYTE_LENGTH);
    setGf64Element(env, c, jc);
    return jc;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf64_NtlGf64_nativeInvi
    (JNIEnv *env, jobject context, jbyteArray ja) {
    setGf64Field();
    NTL::GF2E a = convertGf64Element(env, ja);
    NTL::GF2E c = inv(a);
    setGf64Element(env, c, ja);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf64_NtlGf64_nativeDiv
    (JNIEnv *env, jobject context, jbyteArray ja, jbyteArray jb) {
    setGf64Field();
    NTL::GF2E a = convertGf64Element(env, ja);
    NTL::GF2E b = convertGf64Element(env, jb);
    NTL::GF2E c = a / b;
    jbyteArray jc = (*env).NewByteArray((jsize) HALF_BLOCK_BYTE_LENGTH);
    setGf64Element(env, c, jc);
    return jc;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf64_NtlGf64_nativeDivi
    (JNIEnv *env, jobject context, jbyteArray ja, jbyteArray jb) {
    setGf64Field();
    NTL::GF2E a = convertGf64Element(env, ja);
    NTL::GF2E b = convertGf64Element(env, jb);
    NTL::GF2E c = a / b;
    setGf64Element(env, c, ja);
}