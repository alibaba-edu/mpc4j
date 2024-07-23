//
// Created by Weiran Liu on 2024/6/3.
//

#include "ntl_sgf2k_utils.h"

void sgf2k_read_subfield_element(JNIEnv *env, jbyteArray j_byte_array, uint32_t byte_length, NTL::GF2X &dest) {
    jbyte *j_byte_array_buffer = (*env).GetByteArrayElements(j_byte_array, nullptr);
    uint8_t subfield_element_bytes[byte_length];
    memcpy(subfield_element_bytes, j_byte_array_buffer, byte_length);
    reverseBytes(subfield_element_bytes, byte_length);
    dest = NTL::GF2XFromBytes(subfield_element_bytes, (long) byte_length);
    (*env).ReleaseByteArrayElements(j_byte_array, j_byte_array_buffer, 0);
}

void sgf2k_read_field_element(JNIEnv *env, jobjectArray j_byte_array, uint64_t byte_length, NTL::GF2EX &dest) {
    uint32_t length = (*env).GetArrayLength(j_byte_array);
    NTL::vec_GF2E vector;
    vector.SetLength(length);
    for (uint32_t i = 0; i < length; i++) {
        // 读取第i个数据
        auto j_coefficient_bytes = (jbyteArray)(*env).GetObjectArrayElement(j_byte_array, (jsize)i);
        jbyte* j_coefficient_buffer = (*env).GetByteArrayElements(j_coefficient_bytes, nullptr);
        uint8_t coefficient_bytes[byte_length];
        memcpy(coefficient_bytes, j_coefficient_buffer, byte_length);
        reverseBytes(coefficient_bytes, byte_length);
        NTL::GF2X gf2x_coefficient = NTL::GF2XFromBytes(coefficient_bytes, (long) byte_length);
        NTL::GF2E gf2e_polynomial = NTL::to_GF2E(gf2x_coefficient);
        vector[i] = gf2e_polynomial;
        (*env).ReleaseByteArrayElements(j_coefficient_bytes, j_coefficient_buffer, 0);
    }
    dest = NTL::to_GF2EX(vector);
}

void sgf2k_write_field_element(JNIEnv *env, const NTL::GF2EX& src, uint32_t length, uint32_t byte_length, jobjectArray &dest) {
    for (uint32_t i = 0; i < length; i++) {
        NTL::GF2E temp = NTL::coeff(src, i);
        uint8_t coefficient[byte_length];
        BytesFromGF2E(coefficient, temp, byte_length);
        reverseBytes(coefficient, byte_length);
        jbyteArray j_coefficient_bytes = (*env).NewByteArray((jsize) byte_length);
        jbyte *j_coefficient_buffer = (*env).GetByteArrayElements(j_coefficient_bytes, nullptr);
        memcpy(j_coefficient_buffer, coefficient, byte_length);
        (*env).SetObjectArrayElement(dest, (jsize) i, j_coefficient_bytes);
        (*env).ReleaseByteArrayElements(j_coefficient_bytes, j_coefficient_buffer, 0);
    }
}