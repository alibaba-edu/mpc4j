//
// Created by Weiran Liu on 2021/12/31.
// 应用openssl实现的SHA256哈希函数，源代码来自于：<https://github.com/emp-toolkit/emp-tool/tree/master/emp-tool/utils/hash.h>.
//

#ifndef MPC4J_NATIVE_TOOL_SHA256HASH_H
#define MPC4J_NATIVE_TOOL_SHA256HASH_H

#include <openssl/sha.h>

/**
 * 计算输入数据的SHA256哈希值。
 *
 * @param digest 返回结果。
 * @param data 数据。
 * @param byteLength 数据的字节长度。
 */
inline void sha256_hash(unsigned char *digest, const void *data, int byteLength) {
    (void) SHA256((const unsigned char *) data, byteLength, digest);
}

#endif //MPC4J_NATIVE_TOOL_SHA256HASH_H
