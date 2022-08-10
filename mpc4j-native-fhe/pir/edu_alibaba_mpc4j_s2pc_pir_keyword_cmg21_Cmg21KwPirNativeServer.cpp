//
// Created by Liqiang Peng on 2022/7/14.
//

#include "edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeServer.h"
#include "../apsi.h"

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeServer_computeEncryptedPowers(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray relin_keys_bytes, jobject query_list,
        jobjectArray jparent_powers, jintArray jsource_power_index, jint ps_low_power) {
    return computeEncryptedPowers(env, query_list, relin_keys_bytes, params_bytes, jparent_powers, jsource_power_index,
                                  ps_low_power);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeServer_computeMatches(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray pk_bytes, jbyteArray relin_keys_bytes,
        jobjectArray database_coeffs, jobject query_list, jint ps_low_power) {
    return computeMatches(env, database_coeffs, query_list, relin_keys_bytes, pk_bytes, params_bytes, ps_low_power);
}


JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeServer_computeMatchesNaiveMethod(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray pk_bytes, jbyteArray relin_keys_bytes,
        jobjectArray database_coeffs, jobject query_list) {
    return computeMatchesNaiveMethod(env, database_coeffs, query_list, relin_keys_bytes, params_bytes, pk_bytes);
}