//
// Created by Weiran Liu on 2022/8/21.
//
#include <openssl/ec.h>
#include <openssl/err.h>
#include "stdc++.h"
#include <openssl/obj_mac.h>
#include <jni.h>
#include "../common/defines.h"

#ifndef MPC4J_NATIVE_TOOL_OPENSSL_ECC_H
#define MPC4J_NATIVE_TOOL_OPENSSL_ECC_H

/**
 * 预计算窗口大小
 */
const size_t OPENSSL_WIN_SIZE = 16;
/**
 * 群元素
 */
static EC_GROUP *openssl_ec_group[] = {
    EC_GROUP_new_by_curve_name(NID_secp256k1),
    EC_GROUP_new_by_curve_name(NID_X9_62_prime256v1),
    EC_GROUP_new_by_curve_name(NID_sm2),
};
/**
 * 椭圆曲线比特长度
 */
static int openssl_point_bit_length[] = {
    EC_GROUP_order_bits(openssl_ec_group[0]),
    EC_GROUP_order_bits(openssl_ec_group[1]),
    EC_GROUP_order_bits(openssl_ec_group[2]),
};

void CRYPTO_CHECK(bool condition);

jobject openssl_precompute(JNIEnv *env, int curveIndex, jstring jPointString);

void openssl_destroy_precompute(JNIEnv *env, jobject jWindowHandler);

jstring openssl_precompute_multiply(JNIEnv *env, int curveIndex, jobject jWindowHandler, jstring jBnString);

jstring openssl_multiply(JNIEnv *env, int curveIndex, jstring jPointString, jstring jBnString);

#endif //MPC4J_NATIVE_TOOL_OPENSSL_ECC_H
