//
// Created by Liqiang Peng on 2022/7/14.
//

#include "edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeClient.h"
#include "../apsi.h"

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeClient_genEncryptionParameters(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus, jintArray coeff_modulus_bits) {
    return genEncryptionParameters(env, poly_modulus_degree, plain_modulus, coeff_modulus_bits);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeClient_generateQuery(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray pk_bytes,jbyteArray sk_bytes, jobjectArray jenc_arrays) {
    return generateQuery(env, jenc_arrays, params_bytes, pk_bytes, sk_bytes);
}

JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_cmg21_Cmg21KwPirNativeClient_decodeReply(
        JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray sk_bytes, jbyteArray response) {
    return decodeReply(env, response, params_bytes, sk_bytes);
}