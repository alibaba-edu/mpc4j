//
// Created by Liqiang Peng on 2022/7/14.
//

#include <jni.h>
#include "seal/seal.h"

#ifndef MPC3J_NATIVE_FHE_APSI_H
#define MPC3J_NATIVE_FHE_APSI_H

jobject JNICALL genEncryptionParameters(JNIEnv *env, jint poly_modulus_degree, jlong plain_modulus,
                                        jintArray coeff_modulus_bits);

jobject JNICALL generateQuery(JNIEnv *env, jobjectArray jenc_arrays, jbyteArray params_bytes, jbyteArray pk_bytes,
                              jbyteArray sk_bytes);

jlongArray JNICALL decodeReply(JNIEnv *env, jbyteArray response, jbyteArray params_bytes, jbyteArray sk_bytes);

jobject JNICALL computeEncryptedPowers(JNIEnv *env, jobject query_list, jbyteArray relin_keys_bytes,
                                       jbyteArray params_bytes, jobjectArray jparent_powers,
                                       jintArray jsource_power_index, jint ps_low_power);

jbyteArray JNICALL computeMatches(JNIEnv *env, jobjectArray database_coeffs, jobject query_list,
                                  jbyteArray relin_keys_bytes, jbyteArray pk_bytes, jbyteArray params_bytes,
                                  jint ps_low_power);

jbyteArray JNICALL computeMatchesNaiveMethod(JNIEnv *env, jobjectArray database_coeffs, jobject query_list,
                                             jbyteArray relin_keys_bytes, jbyteArray params_bytes, jbyteArray pk_bytes);

jbyteArray JNICALL computeMatches(JNIEnv *env, jobjectArray database_coeffs, jobject query_list,
                                  jbyteArray relin_keys_bytes, jbyteArray params_bytes, jint ps_low_power);

jbyteArray JNICALL computeMatchesNaiveMethod(JNIEnv *env, jobjectArray database_coeffs, jobject query_list,
                                             jbyteArray relin_keys_bytes, jbyteArray params_bytes);

jint JNICALL checkSealParams(JNIEnv *env, jint poly_modulus_degree, jlong plain_modulus, jintArray coeff_modulus_bits,
                             jobjectArray jparent_powers, jintArray jsource_power_index, jint ps_low_power,
                             jint max_bin_size);

#endif //MPC3J_NATIVE_FHE_APSI_H
