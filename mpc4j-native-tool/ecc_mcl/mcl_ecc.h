//
// Created by Weiran Liu on 2022/7/7.
//
#include <mcl/ec.hpp>
#include <mcl/fp.hpp>
#include <mcl/op.hpp>
#include <jni.h>
#include <vector>
#include "../common/defines.h"

#ifndef MPC4J_NATIVE_TOOL_ECC_MCL_H
#define MPC4J_NATIVE_TOOL_ECC_MCL_H

struct TagZn;
typedef mcl::FpT<> Fp;
typedef mcl::FpT<TagZn> Zn;
typedef mcl::EcT<Fp> Ec;

/**
 * 预计算窗口大小
 */
const size_t MCL_WIN_SIZE = 16;
/**
 * 编码进制为16进制
 */
const size_t MCL_RADIX = 16;

void mcl_init(int curveType);

jobject mcl_precompute(JNIEnv *env, jstring jEcString);

void mcl_destroy_precompute(JNIEnv *env, jobject jWindowHandler);

jstring mcl_single_fixed_point_multiply(JNIEnv *env, jobject jWindowHandler, jstring jZnString);

jobjectArray mcl_fixed_point_multiply(JNIEnv *env, jobject jWindowHandler, jobjectArray jZnStringArray);

jstring mcl_single_multiply(JNIEnv *env, jstring jEcString, jstring jZnString);

jobjectArray mcl_multiply(JNIEnv *env, jstring jEcString, jobjectArray jZnStringArray);

#endif //MPC4J_NATIVE_TOOL_ECC_MCL_H
