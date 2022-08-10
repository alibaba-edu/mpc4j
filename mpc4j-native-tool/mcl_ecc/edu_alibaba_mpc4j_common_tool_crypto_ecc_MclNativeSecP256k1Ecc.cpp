//
// Created by Weiran Liu on 2021/12/13.
//

#include "mcl_ecc.h"
#include "edu_alibaba_mpc4j_common_tool_crypto_ecc_MclNativeSecP256k1Ecc.h"

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_MclNativeSecP256k1Ecc_init
    (JNIEnv *env, jobject context) {
    mcl::initCurve<Ec, Zn>(MCL_SECP256K1);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_MclNativeSecP256k1Ecc_precompute
    (JNIEnv *env, jobject context, jstring jecString) {
    return precompute(env, jecString);
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_MclNativeSecP256k1Ecc_destroyPrecompute
    (JNIEnv *env, jobject context, jobject windowHandler) {
    destroyPrecompute(env, windowHandler);
}

JNIEXPORT jstring JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_MclNativeSecP256k1Ecc_singleFixedPointMultiply
    (JNIEnv *env, jobject context, jobject jwindowHandler, jstring jznString) {
    return singleFixedPointMultiply(env, jwindowHandler, jznString);
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_MclNativeSecP256k1Ecc_fixedPointMultiply
    (JNIEnv *env, jobject context, jobject jwindowHandler, jobjectArray jznStringArray) {
    return fixedPointMultiply(env, jwindowHandler, jznStringArray);
}

JNIEXPORT jstring JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_MclNativeSecP256k1Ecc_singleMultiply
    (JNIEnv *env, jobject context, jstring jecString, jstring jznString) {
    return singleMultiply(env, jecString, jznString);
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_MclNativeSecP256k1Ecc_multiply
    (JNIEnv *env, jobject context, jstring jecString, jobjectArray jznStringArray) {
    return multiply(env, jecString, jznStringArray);
}