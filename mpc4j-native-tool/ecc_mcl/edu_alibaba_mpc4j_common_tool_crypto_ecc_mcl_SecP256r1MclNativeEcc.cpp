#include "mcl_ecc.h"
#include "edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256r1MclNativeEcc.h"

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256r1MclNativeEcc_init
  (JNIEnv *env, jobject context) {
    mcl_init(MCL_NIST_P256);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256r1MclNativeEcc_precompute
  (JNIEnv *env, jobject context, jstring jEcString) {
    return mcl_precompute(env, jEcString);
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256r1MclNativeEcc_destroyPrecompute
  (JNIEnv *env, jobject context, jobject jWindowHandler) {
    mcl_destroy_precompute(env, jWindowHandler);
}

JNIEXPORT jstring JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256r1MclNativeEcc_singleFixedPointMultiply
  (JNIEnv *env, jobject context, jobject jWindowHandler, jstring jZnString) {
    return mcl_single_fixed_point_multiply(env, jWindowHandler, jZnString);
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256r1MclNativeEcc_fixedPointMultiply
  (JNIEnv *env, jobject context, jobject jWindowHandler, jobjectArray jZnStringArray) {
    return mcl_fixed_point_multiply(env, jWindowHandler, jZnStringArray);
}

JNIEXPORT jstring JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256r1MclNativeEcc_singleMultiply
  (JNIEnv *env, jobject context, jstring jEcString, jstring jZnString) {
    return mcl_single_multiply(env, jEcString, jZnString);
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256r1MclNativeEcc_multiply
  (JNIEnv *env, jobject, jstring jEcString, jobjectArray jZnStringArray) {
    return mcl_multiply(env, jEcString, jZnStringArray);
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256r1MclNativeEcc_reset
        (JNIEnv *env, jobject context) {
    // do nothing
}