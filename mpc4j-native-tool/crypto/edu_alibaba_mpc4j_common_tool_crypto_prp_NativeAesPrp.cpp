/*
 * Created by Weiran Liu.
 *
 * 2022/10/19 updates:
 * Thanks the anonymous USENIX Security 2023 AE reviewer for the suggestion.
 * All heap allocations (e.g., auto *p = new uint8_t[]) are replaced with stack allocations (e.g., uint8_t p[]).
 */

#include <cstring>
#include "edu_alibaba_mpc4j_common_tool_crypto_prp_NativeAesPrp.h"
#include "aes.h"

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_prp_NativeAesPrp_nativeSetKey
        (JNIEnv *env, jobject context, jbyteArray jKeyByteArray) {
    // 读取密钥
    jbyte* jKeyByteArrayBuffer = (*env).GetByteArrayElements(jKeyByteArray, nullptr);
    auto * key = (uint8_t*) jKeyByteArrayBuffer;
    // Thanks the anonymous USENIX Security 2023 AE reviewer for the suggestion.
    // Here we use malloc, we need to free it later in nativeDestroyKey, rather than delete it.
    auto *aesKey = (AES_KEY*)malloc(sizeof(AES_KEY));
    aes_set_key(key, aesKey);
    // 释放资源并返回结果
    (*env).ReleaseByteArrayElements(jKeyByteArray, jKeyByteArrayBuffer, 0);
    return (*env).NewDirectByteBuffer(aesKey, 0);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_prp_NativeAesPrp_nativeEncrypt
        (JNIEnv *env, jobject context, jobject jKeyPointer, jbyteArray jPlaintextByteArray) {
    auto * aesKey = (AES_KEY *)(*env).GetDirectBufferAddress(jKeyPointer);
    jbyte* jPlaintextByteArrayBuffer = (*env).GetByteArrayElements(jPlaintextByteArray, nullptr);
    uint8_t ciphertext[BLOCK_BYTE_LENGTH];
    memcpy(ciphertext, jPlaintextByteArrayBuffer, BLOCK_BYTE_LENGTH);
    (*env).ReleaseByteArrayElements(jPlaintextByteArray, jPlaintextByteArrayBuffer, 0);
    aes_ecb_encrypt(ciphertext, aesKey);
    jbyteArray jCiphertextByteArray = (*env).NewByteArray(BLOCK_BYTE_LENGTH);
    (*env).SetByteArrayRegion(
            jCiphertextByteArray, 0, BLOCK_BYTE_LENGTH, reinterpret_cast<const jbyte*>(ciphertext)
    );

    return jCiphertextByteArray;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_prp_NativeAesPrp_nativeDecrypt
        (JNIEnv *env, jobject context, jobject jKeyPointer, jbyteArray jCiphertextByteArray) {
    auto * aesKey = (AES_KEY *)(*env).GetDirectBufferAddress(jKeyPointer);
    jbyte* jCiphertextByteArrayBuffer = (*env).GetByteArrayElements(jCiphertextByteArray, nullptr);
    uint8_t plaintext[BLOCK_BYTE_LENGTH];
    memcpy(plaintext, jCiphertextByteArrayBuffer, BLOCK_BYTE_LENGTH);
    (*env).ReleaseByteArrayElements(jCiphertextByteArray, jCiphertextByteArrayBuffer, 0);
    aes_ecb_decrypt(plaintext, aesKey);
    jbyteArray jPlaintextByteArray = (*env).NewByteArray(BLOCK_BYTE_LENGTH);
    (*env).SetByteArrayRegion(
            jPlaintextByteArray, 0, BLOCK_BYTE_LENGTH, reinterpret_cast<const jbyte*>(plaintext)
    );

    return jPlaintextByteArray;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_prp_NativeAesPrp_nativeDestroyKey
        (JNIEnv *env, jobject context, jobject jKeyPointer) {
    free((AES_KEY *)(*env).GetDirectBufferAddress(jKeyPointer));
}