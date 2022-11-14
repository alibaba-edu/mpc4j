//
// Created by Weiran Liu on 2022/11/2.
//
#include <jni.h>
#include "defines.h"
#include "ntl_zp.h"

#ifndef MPC4J_NATIVE_TOOL_NTL_ZP_UTIL_H
#define MPC4J_NATIVE_TOOL_NTL_ZP_UTIL_H

void zp_byte_array_to_prime(JNIEnv *env, uint8_t* primeByteArray, jbyteArray jprimeByteArray, int primeByteLength);

jobjectArray zp_root_interpolate(JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jint jexpectNum, jobjectArray jxArray, jbyteArray jy);

jbyteArray zp_single_evaluate(JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jobjectArray jCoeffArray, jbyteArray jx);

void zp_polynomial_pad_dummy_item(NTL::ZZ_pX& polynomial, NTL::vec_ZZ_p& x, uint64_t expect_num);

#endif //MPC4J_NATIVE_TOOL_NTL_ZP_UTIL_H