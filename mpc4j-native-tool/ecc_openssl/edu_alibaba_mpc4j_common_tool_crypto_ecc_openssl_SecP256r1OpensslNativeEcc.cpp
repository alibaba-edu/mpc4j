//
// Created by Weiran Liu on 2022/9/2.
//

#include "openssl_ecc.h"
#include "edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc.h"

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_init
        (JNIEnv *env, jobject context) {
    // do nothing
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_precompute
        (JNIEnv *env, jobject context, jstring jPointString) {
    return openssl_precompute(env, 1, jPointString);
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_destroyPrecompute
        (JNIEnv *env, jobject context, jobject jWindowHandler) {
    openssl_destroy_precompute(env, jWindowHandler);
}

JNIEXPORT jstring JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_precomputeMultiply
        (JNIEnv *env, jobject context, jobject jWindowHandler, jstring jBnString) {
    return openssl_precompute_multiply(env, 1, jWindowHandler, jBnString);
}

JNIEXPORT jstring JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_multiply
        (JNIEnv *env, jobject context, jstring jPointString, jstring jBnString) {
    return openssl_multiply(env, 1, jPointString, jBnString);
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc_reset
        (JNIEnv *env, jobject context) {
    // do nothing
}