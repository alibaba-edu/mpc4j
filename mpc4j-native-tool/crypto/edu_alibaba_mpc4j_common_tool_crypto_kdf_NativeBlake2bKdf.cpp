//
// Created by Weiran Liu on 2021/12/31.
//

#include "edu_alibaba_mpc4j_common_tool_crypto_kdf_NativeBlake2bKdf.h"
#include "blake2b_kdf.h"
#include <cstdint>

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_kdf_NativeBlake2bKdf_nativeDeriveKey
    (JNIEnv *env, jobject context, jbyteArray jseed) {
    // 读取输入
    jsize length = (*env).GetArrayLength(jseed);
    jbyte* jseedBuffer = (*env).GetByteArrayElements(jseed, nullptr);
    auto * seed = (uint8_t*) jseedBuffer;
    // 计算哈希
    auto * key = new uint8_t[BLOCK_BYTE_LENGTH];
    blake2b_kdf(key, seed, length);
    // 释放资源并返回结果
    (*env).ReleaseByteArrayElements(jseed, jseedBuffer, 0);
    jbyteArray jkey = (*env).NewByteArray(BLOCK_BYTE_LENGTH);
    (*env).SetByteArrayRegion(jkey, 0, BLOCK_BYTE_LENGTH, (const jbyte*)key);
    delete[] key;

    return jkey;
}