//
// Created by Weiran Liu on 2022/7/7.
//
#include <mcl/ec.hpp>
#include <mcl/fp.hpp>
#include <mcl/op.hpp>
#include <jni.h>
#include <vector>

#ifndef MPC4J_NATIVE_TOOL_MCL_ECC_H
#define MPC4J_NATIVE_TOOL_MCL_ECC_H

struct TagZn;
typedef mcl::FpT<> Fp;
typedef mcl::FpT<TagZn> Zn;
typedef mcl::EcT<Fp> Ec;

/**
 * 预计算窗口大小
 */
const size_t WIN_SIZE = 16;
/**
 * 编码进制为16进制
 */
const size_t RADIX = 16;

jobject precompute(JNIEnv *env, jstring jecString);

void destroyPrecompute(JNIEnv *env, jobject windowHandler);

jstring singleFixedPointMultiply(JNIEnv *env, jobject jwindowHandler, jstring jznString);

jobjectArray fixedPointMultiply(JNIEnv *env, jobject jwindowHandler, jobjectArray jznStringArray);

jstring singleMultiply(JNIEnv *env, jstring jecString, jstring jznString);

jobjectArray multiply(JNIEnv *env, jstring jecString, jobjectArray jznStringArray);

#endif //MPC4J_NATIVE_TOOL_MCL_ECC_H
