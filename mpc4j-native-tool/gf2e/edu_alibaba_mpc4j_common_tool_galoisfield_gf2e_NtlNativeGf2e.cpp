/*
 * Created by Weiran Liu on 2022/5/19.
 *
 * 2022/10/19 updates:
 * Thanks the anonymous USENIX Security 2023 AE reviewer for the suggestion.
 * All heap allocations (e.g., auto *p = new uint8_t[]) are replaced with stack allocations (e.g., uint8_t p[]).
 * We reduce a lot of codes by passing lambdas as the template argument that does the relevant operation.
 */
#include <NTL/GF2E.h>
#include "edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e.h"
#include "defines.h"

auto ntl_mul = [](const NTL::GF2E& a, const NTL::GF2E& b) {
    return a * b;
};

auto ntl_div = [](const NTL::GF2E& a, const NTL::GF2E& b) {
    return a / b;
};

template<typename Operation>
static jbyteArray NtlGf2eOperationHelper(JNIEnv* env, jclass context, jbyteArray jMinBytes, jint jByteL,
                                          jbyteArray ja, jbyteArray jb, Operation&& operation) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 转换a
    jbyte *jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    uint8_t dataA[jByteL];
    memcpy(dataA, jaBuffer, jByteL);
    reverseBytes(dataA, jByteL);
    NTL::GF2X aFromBytes = NTL::GF2XFromBytes(dataA, (long) jByteL);
    NTL::GF2E a = to_GF2E(aFromBytes);
    (*env).ReleaseByteArrayElements(ja, jaBuffer, 0);
    // 转换b
    jbyte *jbBuffer = (*env).GetByteArrayElements(jb, nullptr);
    uint8_t dataB[jByteL];
    memcpy(dataB, jbBuffer, jByteL);
    reverseBytes(dataB, jByteL);
    NTL::GF2X bFromBytes = NTL::GF2XFromBytes(dataB, (long) jByteL);
    NTL::GF2E b = to_GF2E(bFromBytes);
    (*env).ReleaseByteArrayElements(jb, jbBuffer, 0);
    // 运算
    NTL::GF2E c = operation(a, b);
    // 返回结果
    BytesFromGF2X(dataA, NTL::rep(c), (long) jByteL);
    reverseBytes(dataA, jByteL);
    jbyteArray jc = (*env).NewByteArray((jsize) jByteL);
    (*env).SetByteArrayRegion(jc, 0, jByteL, (const jbyte *) dataA);

    return jc;
}

template<typename Operation>
static void NtlGf2eInplaceOperationHelper(JNIEnv* env, jclass context, jbyteArray jMinBytes, jint jByteL,
                                          jbyteArray ja, jbyteArray jb, Operation&& operation) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 转换a
    jbyte *jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    uint8_t dataA[jByteL];
    memcpy(dataA, jaBuffer, jByteL);
    reverseBytes(dataA, jByteL);
    NTL::GF2X aFromBytes = NTL::GF2XFromBytes(dataA, (long) jByteL);
    NTL::GF2E a = to_GF2E(aFromBytes);
    // 转换b
    jbyte *jbBuffer = (*env).GetByteArrayElements(jb, nullptr);
    uint8_t dataB[jByteL];
    memcpy(dataB, jbBuffer, jByteL);
    reverseBytes(dataB, jByteL);
    NTL::GF2X bFromBytes = NTL::GF2XFromBytes(dataB, (long) jByteL);
    NTL::GF2E b = to_GF2E(bFromBytes);
    (*env).ReleaseByteArrayElements(jb, jbBuffer, 0);
    // 运算
    NTL::GF2E c = operation(a, b);
    // 返回结果
    BytesFromGF2X(dataA, NTL::rep(c), (long) jByteL);
    reverseBytes(dataA, jByteL);
    // 不能(*env).ReleaseByteArrayElements(ja, jaBuffer, 0)，否则结果会有错误
    (*env).SetByteArrayRegion(ja, 0, jByteL, (const jbyte *) dataA);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e_nativeMul
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jbyteArray ja, jbyteArray jb) {
    return NtlGf2eOperationHelper(env, context, jMinBytes, jByteL, ja, jb, ntl_mul);
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e_nativeMuli
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jbyteArray ja, jbyteArray jb) {
    NtlGf2eInplaceOperationHelper(env, context, jMinBytes, jByteL, ja, jb, ntl_mul);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e_nativeDiv
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jbyteArray ja, jbyteArray jb) {
    return NtlGf2eOperationHelper(env, context, jMinBytes, jByteL, ja, jb, ntl_div);
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e_nativeDivi
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jbyteArray ja, jbyteArray jb) {
    NtlGf2eInplaceOperationHelper(env, context, jMinBytes, jByteL, ja, jb, ntl_div);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e_nativeInv
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jbyteArray ja) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 转换a
    jbyte *jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    uint8_t dataA[jByteL];
    memcpy(dataA, jaBuffer, jByteL);
    reverseBytes(dataA, jByteL);
    NTL::GF2X aFromBytes = NTL::GF2XFromBytes(dataA, (long) jByteL);
    NTL::GF2E a = to_GF2E(aFromBytes);
    // 计算a的逆
    NTL::GF2E c = inv(a);
    // 返回结果
    BytesFromGF2X(dataA, NTL::rep(c), (long) jByteL);
    reverseBytes(dataA, jByteL);
    jbyteArray jc = (*env).NewByteArray((jsize) jByteL);
    (*env).SetByteArrayRegion(jc, 0, jByteL, (const jbyte *) dataA);

    return jc;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e_nativeInvi
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jbyteArray ja) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 转换a
    jbyte *jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    uint8_t dataA[jByteL];
    memcpy(dataA, jaBuffer, jByteL);
    reverseBytes(dataA, jByteL);
    NTL::GF2X aFromBytes = NTL::GF2XFromBytes(dataA, (long) jByteL);
    NTL::GF2E a = to_GF2E(aFromBytes);
    // 计算a的逆
    NTL::GF2E c = inv(a);
    // 返回结果
    BytesFromGF2X(dataA, NTL::rep(c), (long) jByteL);
    reverseBytes(dataA, jByteL);
    // 不能(*env).ReleaseByteArrayElements(ja, jaBuffer, 0)，否则结果会有错误
    (*env).SetByteArrayRegion(ja, 0, jByteL, (const jbyte *) dataA);
}