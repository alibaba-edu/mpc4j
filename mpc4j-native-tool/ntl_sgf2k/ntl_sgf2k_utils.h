//
// Created by Weiran Liu on 2024/6/3.
//

#include <jni.h>
#include <cstdint>
#include <NTL/GF2X.h>
#include <NTL/vec_GF2E.h>
#include <NTL/GF2EX.h>
#include "defines.h"
#include "../ntl_poly/ntl_gf2x.h"

#ifndef MPC4J_NATIVE_TOOL_NTL_SGF2K_UTILS_H
#define MPC4J_NATIVE_TOOL_NTL_SGF2K_UTILS_H

void sgf2k_read_subfield_element(JNIEnv *env, jbyteArray j_byte_array, uint32_t byte_length, NTL::GF2X &dest);

void sgf2k_read_field_element(JNIEnv *env, jobjectArray j_byte_array, uint64_t byte_length, NTL::GF2EX &dest);

void sgf2k_write_field_element(JNIEnv *env, const NTL::GF2EX& src, uint32_t length, uint32_t byte_length, jobjectArray &dest);

#endif //MPC4J_NATIVE_TOOL_NTL_SGF2K_UTILS_H
