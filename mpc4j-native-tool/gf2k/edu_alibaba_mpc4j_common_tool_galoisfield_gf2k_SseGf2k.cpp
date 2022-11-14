/*
 * Created by Weiran Liu on 2022/1/15.
 *
 * 2022/10/19 updates:
 * Thanks the anonymous USENIX Security 2023 AE reviewer for the suggestion.
 * All heap allocations (e.g., auto *p = new uint8_t[]) are replaced with stack allocations (e.g., uint8_t p[]).
 */

#include "edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_SseGf2k.h"
#include "defines.h"
#include "gf2k.h"

/**
 * 将__m128i转换为字节数组。指令集GF(2^128)域乘法结果的位置会发生置换，转回时也要注意格式。
 *
 * @param data 输出字节数组地址。
 * @param block __m128i。
 */
void block_to_bytes(uint8_t *data, __m128i block) {
    auto *block_ptr = (uint8_t *) &block;
    data[0] = block_ptr[0];
    data[1] = block_ptr[1];
    data[2] = block_ptr[2];
    data[3] = block_ptr[3];
    data[4] = block_ptr[4];
    data[5] = block_ptr[5];
    data[6] = block_ptr[6];
    data[7] = block_ptr[7];
    data[8] = block_ptr[8];
    data[9] = block_ptr[9];
    data[10] = block_ptr[10];
    data[11] = block_ptr[11];
    data[12] = block_ptr[12];
    data[13] = block_ptr[13];
    data[14] = block_ptr[14];
    data[15] = block_ptr[15];
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_SseGf2k_nativeMul
        (JNIEnv *env, jobject context, jbyteArray ja, jbyteArray jb) {
    jbyte *jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    __m128i a = make_block((uint8_t *) jaBuffer);
    (*env).ReleaseByteArrayElements(ja, jaBuffer, 0);
    jbyte *jbBuffer = (*env).GetByteArrayElements(jb, nullptr);
    __m128i b = make_block((uint8_t *) jbBuffer);
    (*env).ReleaseByteArrayElements(jb, jbBuffer, 0);
    __m128i c;
    gf2k_multiply(a, b, &c);
    uint8_t c_byte[BLOCK_BYTE_LENGTH];
    block_to_bytes(c_byte, c);
    jbyteArray jc = (*env).NewByteArray((jsize) BLOCK_BYTE_LENGTH);
    (*env).SetByteArrayRegion(jc, 0, BLOCK_BYTE_LENGTH, (const jbyte *) c_byte);

    return jc;
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_SseGf2k_nativeMuli
        (JNIEnv *env, jobject context, jbyteArray ja, jbyteArray jb) {
    jbyte *jaBuffer = (*env).GetByteArrayElements(ja, nullptr);
    __m128i a = make_block((uint8_t *) jaBuffer);
    (*env).ReleaseByteArrayElements(ja, jaBuffer, 0);
    jbyte *jbBuffer = (*env).GetByteArrayElements(jb, nullptr);
    __m128i b = make_block((uint8_t *) jbBuffer);
    (*env).ReleaseByteArrayElements(jb, jbBuffer, 0);
    __m128i c;
    gf2k_multiply(a, b, &c);
    uint8_t c_byte[BLOCK_BYTE_LENGTH];
    block_to_bytes(c_byte, c);
    // 不用创造新的对象，直接赋值即可
    (*env).SetByteArrayRegion(ja, 0, BLOCK_BYTE_LENGTH, (const jbyte *) c_byte);
}