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
static EC_GROUP *openssl_ec_group;
/**
 * 椭圆曲线比特长度
 */
static int openssl_point_bit_length;

void CRYPTO_CHECK(bool condition);

void openssl_init(int curve_id);

jobject openssl_precompute(JNIEnv *env, jstring jPointString);

void openssl_destroy_precompute(JNIEnv *env, jobject jWindowHandler);

jstring openssl_single_fixed_point_multiply(JNIEnv *env, jobject jWindowHandler, jstring jBnString);

jobjectArray openssl_fixed_point_multiply(JNIEnv *env, jobject jWindowHandler, jobjectArray jBnStringArray);

jstring openssl_single_multiply(JNIEnv *env, jstring jPointString, jstring jBnString);

jobjectArray openssl_multiply(JNIEnv *env, jstring jBnString, jobjectArray jBnStringArray);

void openssl_reset();

#endif //MPC4J_NATIVE_TOOL_OPENSSL_ECC_H
