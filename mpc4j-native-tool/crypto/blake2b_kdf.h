//
// Created by Weiran Liu on 2021/12/31.
//

#ifndef MPC4J_NATIVE_TOOL_BLAKE2B_KDF_H
#define MPC4J_NATIVE_TOOL_BLAKE2B_KDF_H

#include "../common/defines.h"

#ifdef __x86_64__
#include "blake2/sse/blake2.h"
#elif __aarch64__
#include "blake2/neon/blake2.h"
#endif

/**
 * 计算输入数据的blake2b密钥派生结果。
 *
 * @param key 密钥。
 * @param seed 种子。
 * @param byteLength 种子的字节长度。
 */
inline void blake2b_kdf(unsigned char *key, const void *seed, int byteLength) {
    blake2b_state state;
    blake2b_init(&state, BLOCK_BYTE_LENGTH);
    blake2b_update(&state, seed, byteLength);
    blake2b_final(&state, key, state.outlen);
}

#endif //MPC4J_NATIVE_TOOL_BLAKE2B_KDF_H
