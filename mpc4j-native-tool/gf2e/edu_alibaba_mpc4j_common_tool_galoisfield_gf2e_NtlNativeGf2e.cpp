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

NTL::GF2E convertGf2eElement(JNIEnv *env, jbyteArray jElement, jint jByteL) {
    jbyte *jElementBuffer = (*env).GetByteArrayElements(jElement, nullptr);
    uint8_t data[jByteL];
    memcpy(data, jElementBuffer, jByteL);
    reverseBytes(data, jByteL);
    NTL::GF2X elementFromBytes = NTL::GF2XFromBytes(data, (long) jByteL);
    NTL::GF2E element = to_GF2E(elementFromBytes);
    (*env).ReleaseByteArrayElements(jElement, jElementBuffer, 0);
    return element;
}

void setGf2eElement(JNIEnv *env, const NTL::GF2E& element, jbyteArray jElement, jint jByteL) {
    uint8_t data[jByteL];
    BytesFromGF2X(data, NTL::rep(element), (long) jByteL);
    reverseBytes(data, jByteL);
    (*env).SetByteArrayRegion(jElement, 0, jByteL, (const jbyte *) data);
}

template<typename Operation>
static jbyteArray NtlGf2eOperationHelper(JNIEnv* env, jclass context, jbyteArray jMinBytes, jint jByteL,
                                          jbyteArray ja, jbyteArray jb, Operation&& operation) {
    initGF2E(env, jMinBytes);
    NTL::GF2E a = convertGf2eElement(env, ja, jByteL);
    NTL::GF2E b = convertGf2eElement(env, jb, jByteL);
    NTL::GF2E c = operation(a, b);
    jbyteArray jc = (*env).NewByteArray((jsize) jByteL);
    setGf2eElement(env, c, jc, jByteL);
    return jc;
}

template<typename Operation>
static void NtlGf2eInplaceOperationHelper(JNIEnv* env, jclass context, jbyteArray jMinBytes, jint jByteL,
                                          jbyteArray ja, jbyteArray jb, Operation&& operation) {
    initGF2E(env, jMinBytes);
    NTL::GF2E a = convertGf2eElement(env, ja, jByteL);
    NTL::GF2E b = convertGf2eElement(env, jb, jByteL);
    NTL::GF2E c = operation(a, b);
    setGf2eElement(env, c, ja, jByteL);
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
    initGF2E(env, jMinBytes);
    NTL::GF2E a = convertGf2eElement(env, ja, jByteL);
    NTL::GF2E c = inv(a);
    jbyteArray jc = (*env).NewByteArray((jsize) jByteL);
    setGf2eElement(env, c, jc, jByteL);
    return jc;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e_nativeInvi
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jbyteArray ja) {
    initGF2E(env, jMinBytes);
    NTL::GF2E a = convertGf2eElement(env, ja, jByteL);
    NTL::GF2E c = inv(a);
    setGf2eElement(env, c, ja, jByteL);
}