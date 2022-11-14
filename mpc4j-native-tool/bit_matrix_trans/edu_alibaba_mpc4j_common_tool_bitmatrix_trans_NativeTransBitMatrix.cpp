/*
 * Created by Weiran Liu on 2021/11/29.
 *
 * 2022/10/19 updates:
 * Thanks the anonymous USENIX Security 2023 AE reviewer for the suggestion.
 * However, the bit_matrix_transpose would take large memory. We need to allocate memory in heap rather than stack.
 */
#include "edu_alibaba_mpc4j_common_tool_bitmatrix_trans_NativeTransBitMatrix.h"
#include "bit_matrix_trans.h"

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_bitmatrix_trans_NativeTransBitMatrix_nativeTranspose
    (JNIEnv *env, jobject context, jbyteArray jInputByteArray, jint nrows, jint ncolumns) {
    jsize length = (*env).GetArrayLength(jInputByteArray);
    jbyte* jInputByteArrayBuffer = (*env).GetByteArrayElements(jInputByteArray, nullptr);
    auto* input = (uint8_t*) jInputByteArrayBuffer;
    auto* output = new uint8_t[length];
    // 转置，把列当成行，行当成列即可
    sse_trans(output, input, ncolumns, nrows);
    (*env).ReleaseByteArrayElements(jInputByteArray, jInputByteArrayBuffer, 0);
    jbyteArray jOutputByteArray = (*env).NewByteArray(length);
    (*env).SetByteArrayRegion(jOutputByteArray, 0, length, reinterpret_cast<const jbyte*>(output));
    delete[] output;

    return jOutputByteArray;
}