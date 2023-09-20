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
 * moduli polynomial for GF(2^128) is f(x) = x^128 + x^7 + x^2 + x + 1, NTL needs little-endian.
 */
const u_int8_t GF2K_MIN_BYTES[BLOCK_BYTE_LENGTH + 1] = {
        0x87, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x01,
};

void setGf2kField() {
    NTL::GF2X finiteField = NTL::GF2XFromBytes(GF2K_MIN_BYTES, BLOCK_BYTE_LENGTH + 1);
    NTL::GF2E::init(finiteField);
}

NTL::GF2E convertGf2kElement(JNIEnv *env, jbyteArray jElement) {
    jbyte *jElementBuffer = (*env).GetByteArrayElements(jElement, nullptr);
    uint8_t data[BLOCK_BYTE_LENGTH];
    memcpy(data, jElementBuffer, BLOCK_BYTE_LENGTH);
    reverseBytes(data, BLOCK_BYTE_LENGTH);
    NTL::GF2X elementFromBytes = NTL::GF2XFromBytes(data, (long) BLOCK_BYTE_LENGTH);
    NTL::GF2E element = to_GF2E(elementFromBytes);
    (*env).ReleaseByteArrayElements(jElement, jElementBuffer, 0);
    return element;
}

void setGf2kElement(JNIEnv *env, const NTL::GF2E& element, jbyteArray jElement) {
    uint8_t data[BLOCK_BYTE_LENGTH];
    BytesFromGF2X(data, NTL::rep(element), (long) BLOCK_BYTE_LENGTH);
    reverseBytes(data, BLOCK_BYTE_LENGTH);
    (*env).SetByteArrayRegion(jElement, 0, BLOCK_BYTE_LENGTH, (const jbyte *) data);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_NtlGf2k_nativeMul
        (JNIEnv *env, jobject context, jbyteArray ja, jbyteArray jb) {
    setGf2kField();
    NTL::GF2E a = convertGf2kElement(env, ja);
    NTL::GF2E b = convertGf2kElement(env, jb);
    NTL::GF2E c = a * b;
    jbyteArray jc = (*env).NewByteArray((jsize) BLOCK_BYTE_LENGTH);
    setGf2kElement(env, c, jc);
    return jc;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_NtlGf2k_nativeMuli
        (JNIEnv *env, jobject context, jbyteArray ja, jbyteArray jb) {
    setGf2kField();
    NTL::GF2E a = convertGf2kElement(env, ja);
    NTL::GF2E b = convertGf2kElement(env, jb);
    NTL::GF2E c = a * b;
    setGf2kElement(env, c, ja);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_NtlGf2k_nativeInv
    (JNIEnv *env, jobject context, jbyteArray ja) {
    setGf2kField();
    NTL::GF2E a = convertGf2kElement(env, ja);
    NTL::GF2E c = inv(a);
    jbyteArray jc = (*env).NewByteArray((jsize) BLOCK_BYTE_LENGTH);
    setGf2kElement(env, c, jc);
    return jc;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_NtlGf2k_nativeInvi
    (JNIEnv *env, jobject context, jbyteArray ja) {
    setGf2kField();
    NTL::GF2E a = convertGf2kElement(env, ja);
    NTL::GF2E c = inv(a);
    setGf2kElement(env, c, ja);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_NtlGf2k_nativeDiv
    (JNIEnv *env, jobject context, jbyteArray ja, jbyteArray jb) {
    setGf2kField();
    NTL::GF2E a = convertGf2kElement(env, ja);
    NTL::GF2E b = convertGf2kElement(env, jb);
    NTL::GF2E c = a / b;
    jbyteArray jc = (*env).NewByteArray((jsize) BLOCK_BYTE_LENGTH);
    setGf2kElement(env, c, jc);
    return jc;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_NtlGf2k_nativeDivi
    (JNIEnv *env, jobject context, jbyteArray ja, jbyteArray jb) {
    setGf2kField();
    NTL::GF2E a = convertGf2kElement(env, ja);
    NTL::GF2E b = convertGf2kElement(env, jb);
    NTL::GF2E c = a / b;
    setGf2kElement(env, c, ja);
}