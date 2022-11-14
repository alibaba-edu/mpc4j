/*
 * Created by Weiran Liu on 2021/12/31.
 *
 * 2022/10/19 updates:
 * Thanks the anonymous USENIX Security 2023 AE reviewer for the suggestion.
 * All heap allocations (e.g., auto *p = new uint8_t[]) are replaced with stack allocations (e.g., uint8_t p[]).
 */

#include "edu_alibaba_mpc4j_common_tool_crypto_hash_NativeSha256Hash.h"
#include "sha256_hash.h"
#include <cstdint>

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_hash_NativeSha256Hash_digest
    (JNIEnv *env, jobject context, jbyteArray jmessage) {
    // 读取输入
    jsize length = (*env).GetArrayLength(jmessage);
    jbyte* jmessageBuffer = (*env).GetByteArrayElements(jmessage, nullptr);
    auto * input = (uint8_t*) jmessageBuffer;
    // 计算哈希
    uint8_t output[SHA256_DIGEST_LENGTH];
    sha256_hash(output, input, length);
    // 释放资源并返回结果
    (*env).ReleaseByteArrayElements(jmessage, jmessageBuffer, 0);
    jbyteArray jhash = (*env).NewByteArray(SHA256_DIGEST_LENGTH);
    (*env).SetByteArrayRegion(jhash, 0, SHA256_DIGEST_LENGTH, (const jbyte*)output);

    return jhash;
}