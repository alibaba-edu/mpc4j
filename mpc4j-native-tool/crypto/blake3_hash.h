//
// Created by Weiran Liu on 2022/1/7.
//

#ifndef MPC4J_NATIVE_TOOL_BLAKE3_HASH_H
#define MPC4J_NATIVE_TOOL_BLAKE3_HASH_H

#include "blake3/blake3.h"

/**
 * 计算输入数据的blake3哈希值，源码参见：https://github.com/BLAKE3-team/BLAKE3/tree/master/c
 *
 * @param digest 返回结果。
 * @param data 数据。
 * @param byteLength 数据的字节长度。
 */
inline void blake3_hash(unsigned char *digest, const void *data, int byteLength) {
    // Initialize the hasher.
    blake3_hasher hasher;
    blake3_hasher_init(&hasher);
    blake3_hasher_update(&hasher, data, byteLength);
    // Finalize the hash. BLAKE3_OUT_LEN is the default output length, 32 bytes.
    blake3_hasher_finalize(&hasher, digest, BLAKE3_OUT_LEN);
}

#endif //MPC4J_NATIVE_TOOL_BLAKE3_HASH_H
