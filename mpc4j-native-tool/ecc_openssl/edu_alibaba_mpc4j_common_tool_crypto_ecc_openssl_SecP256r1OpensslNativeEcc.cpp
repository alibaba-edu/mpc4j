//
// Created by Weiran Liu on 2022/9/2.
//

#include "openssl_ecc.h"
#include "edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc.h"

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_init
        (JNIEnv *env, jobject context) {
    openssl_init(NID_X9_62_prime256v1);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_precompute
        (JNIEnv *env, jobject context, jstring jPointString) {
    return openssl_precompute(env, jPointString);
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_destroyPrecompute
        (JNIEnv *env, jobject context, jobject jWindowHandler) {
    openssl_destroy_precompute(env, jWindowHandler);
}

JNIEXPORT jstring JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_singleFixedPointMultiply
        (JNIEnv *env, jobject context, jobject jWindowHandler, jstring jBnString) {
    return openssl_single_fixed_point_multiply(env, jWindowHandler, jBnString);
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_fixedPointMultiply
        (JNIEnv *env, jobject context, jobject jWindowHandler, jobjectArray jBnStringArray) {
    return openssl_fixed_point_multiply(env, jWindowHandler, jBnStringArray);
}

JNIEXPORT jstring JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_singleMultiply
        (JNIEnv *env, jobject context, jstring jPointString, jstring jBnString) {
    return openssl_single_multiply(env, jPointString, jBnString);
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_multiply
        (JNIEnv *env, jobject context, jstring jPointString, jobjectArray jBnStringArray) {
    return openssl_multiply(env, jPointString, jBnStringArray);
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_reset
        (JNIEnv *env, jobject context) {
    openssl_reset();
}