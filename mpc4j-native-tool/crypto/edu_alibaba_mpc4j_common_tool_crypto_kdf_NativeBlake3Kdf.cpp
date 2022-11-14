/*
 * Created by Weiran Liu on 2022/1/7.
 *
 * 2022/10/19 updates:
 * Thanks the anonymous USENIX Security 2023 AE reviewer for the suggestion.
 * All heap allocations (e.g., auto *p = new uint8_t[]) are replaced with stack allocations (e.g., uint8_t p[]).
 */

#include "edu_alibaba_mpc4j_common_tool_crypto_kdf_NativeBlake3Kdf.h"
#include "blake3_kdf.h"
#include <cstdint>

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_kdf_NativeBlake3Kdf_nativeDeriveKey
    (JNIEnv *env, jobject context, jbyteArray jseed) {
    // 读取输入
    jsize length = (*env).GetArrayLength(jseed);
    jbyte* jseedBuffer = (*env).GetByteArrayElements(jseed, nullptr);
    auto * seed = (uint8_t*) jseedBuffer;
    // 计算哈希
    uint8_t key[BLOCK_BYTE_LENGTH];
    blake3_kdf(key, seed, length);
    // 释放资源并返回结果
    (*env).ReleaseByteArrayElements(jseed, jseedBuffer, 0);
    jbyteArray jkey = (*env).NewByteArray(BLOCK_BYTE_LENGTH);
    (*env).SetByteArrayRegion(jkey, 0, BLOCK_BYTE_LENGTH, (const jbyte*)key);

    return jkey;
}
