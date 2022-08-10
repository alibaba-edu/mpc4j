//
// Created by Weiran Liu on 2021/12/31.
//

#ifndef MPC4J_NATIVE_TOOL_BLAKE2B_HASH_H
#define MPC4J_NATIVE_TOOL_BLAKE2B_HASH_H

#ifdef __x86_64__
#include "blake2/sse/blake2.h"
#elif __aarch64__
#include "blake2/neon/blake2.h"
#endif

# define BLAKE_2B_160_DIGEST_LENGTH 20

/**
 * 计算输入数据的blake2b哈希值。
 *
 * @param digest 返回结果。
 * @param data 数据。
 * @param byteLength 数据的字节长度。
 */
inline void blake2b_160_hash(unsigned char *digest, const void *data, int byteLength) {
    blake2b_state state;
    blake2b_init(&state, BLAKE_2B_160_DIGEST_LENGTH);
    blake2b_update(&state, data, byteLength);
    blake2b_final(&state, digest, state.outlen);
}

#endif //MPC4J_NATIVE_TOOL_BLAKE2B_HASH_H
