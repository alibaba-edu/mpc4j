#include <cstring>
#include "edu_alibaba_mpc4j_common_tool_crypto_prp_NativeAesPrp.h"
#include "aes.h"

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_prp_NativeAesPrp_nativeSetKey
        (JNIEnv *env, jobject context, jbyteArray jKeyByteArray) {
    // 读取密钥
    jbyte* jKeyByteArrayBuffer = (*env).GetByteArrayElements(jKeyByteArray, nullptr);
    auto * key = (uint8_t*) jKeyByteArrayBuffer;
    // 分配内存，预计算密钥
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
    auto * ciphertext = new uint8_t[BLOCK_BYTE_LENGTH];
    memcpy(ciphertext, jPlaintextByteArrayBuffer, BLOCK_BYTE_LENGTH);
    (*env).ReleaseByteArrayElements(jPlaintextByteArray, jPlaintextByteArrayBuffer, 0);
    aes_ecb_encrypt(ciphertext, aesKey);
    jbyteArray jCiphertextByteArray = (*env).NewByteArray(BLOCK_BYTE_LENGTH);
    (*env).SetByteArrayRegion(
            jCiphertextByteArray, 0, BLOCK_BYTE_LENGTH, reinterpret_cast<const jbyte*>(ciphertext)
    );
    delete[] ciphertext;

    return jCiphertextByteArray;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_prp_NativeAesPrp_nativeDecrypt
        (JNIEnv *env, jobject context, jobject jKeyPointer, jbyteArray jCiphertextByteArray) {
    auto * aesKey = (AES_KEY *)(*env).GetDirectBufferAddress(jKeyPointer);
    jbyte* jCiphertextByteArrayBuffer = (*env).GetByteArrayElements(jCiphertextByteArray, nullptr);
    auto * plaintext = new uint8_t[BLOCK_BYTE_LENGTH];
    memcpy(plaintext, jCiphertextByteArrayBuffer, BLOCK_BYTE_LENGTH);
    (*env).ReleaseByteArrayElements(jCiphertextByteArray, jCiphertextByteArrayBuffer, 0);
    aes_ecb_decrypt(plaintext, aesKey);
    jbyteArray jPlaintextByteArray = (*env).NewByteArray(BLOCK_BYTE_LENGTH);
    (*env).SetByteArrayRegion(
            jPlaintextByteArray, 0, BLOCK_BYTE_LENGTH, reinterpret_cast<const jbyte*>(plaintext)
    );
    delete[] plaintext;

    return jPlaintextByteArray;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_prp_NativeAesPrp_nativeDestroyKey
        (JNIEnv *env, jobject context, jobject jKeyPointer) {
    delete (AES_KEY *)(*env).GetDirectBufferAddress(jKeyPointer);
}