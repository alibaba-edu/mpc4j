//
// Created by Weiran Liu on 2021/12/13.
//

#include "mcl_ecc.h"
#include "edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc.h"

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_init
    (JNIEnv *env, jobject context) {
    mcl_init(MCL_SECP256K1);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_precompute
    (JNIEnv *env, jobject context, jstring jEcString) {
    return mcl_precompute(env, jEcString);
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_destroyPrecompute
    (JNIEnv *env, jobject context, jobject jWindowHandler) {
    mcl_destroy_precompute(env, jWindowHandler);
}

JNIEXPORT jstring JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_precomputeMultiply
    (JNIEnv *env, jobject context, jobject jWindowHandler, jstring jZnString) {
    return mcl_precompute_multiply(env, jWindowHandler, jZnString);
}

JNIEXPORT jstring JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_multiply
    (JNIEnv *env, jobject context, jstring jEcString, jstring jZnString) {
    return mcl_multiply(env, jEcString, jZnString);
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_reset
        (JNIEnv *env, jobject context) {
    // do nothing
}