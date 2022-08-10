//
// Created by Weiran Liu on 2022/1/7.
//

#ifndef MPC4J_NATIVE_TOOL_BLAKE3_KDF_H
#define MPC4J_NATIVE_TOOL_BLAKE3_KDF_H

#include "../common/defines.h"
#include "blake3/blake3.h"

const char context[] = "mpc4j-native-tool session tokens v1";
/**
 * 计算输入数据的blake3密钥派生结果。源码参见：https://github.com/BLAKE3-team/BLAKE3/tree/master/c
 *
 * @param key 密钥。
 * @param seed 种子。
 * @param byteLength 种子的字节长度。
 */
inline void blake3_kdf(unsigned char *key, const void *seed, int byteLength) {
    // A good default format for the context string is "[application] [commit timestamp] [purpose]",
    // e.g., "example.com 2019-12-25 16:18:03 session tokens v1".
    // Initialize the hasher.
    blake3_hasher hasher;
    blake3_hasher_init_derive_key(&hasher, context);
    blake3_hasher_update(&hasher, seed, byteLength);
    // Finalize the hash. BLAKE3_OUT_LEN is the default output length, 32 bytes.
    blake3_hasher_finalize(&hasher, key, BLOCK_BYTE_LENGTH);
}

#endif //MPC4J_NATIVE_TOOL_BLAKE3_KDF_H
