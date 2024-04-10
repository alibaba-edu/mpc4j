//
// Created by Weiran Liu on 2022/8/21.
//

#include "openssl_ecc.h"
#include "openssl_window_method.hpp"

void pointFromString(int curveIndex, const std::string &pointString, EC_POINT *point, BN_CTX *ctx) {
    auto *buffer = new char[pointString.length()];
    strcpy(buffer, pointString.c_str());
    EC_POINT_hex2point(openssl_ec_group[curveIndex], buffer, point, ctx);
    delete[] buffer;
}

std::string pointToString(int curveIndex, const EC_POINT *point, BN_CTX *ctx) {
    std::stringstream ss;
    ss << EC_POINT_point2hex(openssl_ec_group[curveIndex], point, POINT_CONVERSION_UNCOMPRESSED, ctx);
    return ss.str();
}

void bnFromString(const std::string &bnString, BIGNUM *bn) {
    BN_hex2bn(&bn, bnString.c_str());
}

void CRYPTO_CHECK(bool condition) {
    if (!condition) {
        char buffer[256];
        ERR_error_string_n(ERR_get_error(), buffer, sizeof(buffer));
        std::cerr << std::string(buffer);
    }
}

jobject openssl_precompute(JNIEnv *env, int curveIndex, jstring jPointString) {
    BN_CTX *ctx = BN_CTX_new();
    // 读取椭圆曲线点
    const char *jPointStringHandler = (*env).GetStringUTFChars(jPointString, JNI_FALSE);
    const std::string pointString = std::string(jPointStringHandler);
    (*env).ReleaseStringUTFChars(jPointString, jPointStringHandler);
    EC_POINT *point = EC_POINT_new(openssl_ec_group[curveIndex]);
    pointFromString(curveIndex, pointString, point, ctx);
    // 预计算
    auto *windowHandler = new WindowMethod(curveIndex, point, openssl_point_bit_length[curveIndex], OPENSSL_WIN_SIZE);
    EC_POINT_free(point);
    BN_CTX_free(ctx);
    return (*env).NewDirectByteBuffer(windowHandler, 8);
}

void openssl_destroy_precompute(JNIEnv *env, jobject jWindowHandler) {
    delete (WindowMethod *)(*env).GetDirectBufferAddress(jWindowHandler);
}

jstring openssl_precompute_multiply(JNIEnv *env, int curveIndex, jobject jWindowHandler, jstring jBnString) {
    BN_CTX *ctx = BN_CTX_new();
    // 读取幂指数
    const char* jBnStringHandler = (*env).GetStringUTFChars(jBnString, JNI_FALSE);
    std::string bnString = std::string(jBnStringHandler);
    (*env).ReleaseStringUTFChars(jBnString, jBnStringHandler);
    BIGNUM *bn = BN_new();
    bnFromString(bnString, bn);
    // 定点计算
    auto *windowHandler = (WindowMethod *)(*env).GetDirectBufferAddress(jWindowHandler);
    EC_POINT *point = EC_POINT_new(openssl_ec_group[curveIndex]);
    (*windowHandler).multiply(point, bn);
    BN_free(bn);
    // 返回结果
    std::string pointString = pointToString(curveIndex, point, ctx);
    EC_POINT_free(point);
    BN_CTX_free(ctx);
    return (*env).NewStringUTF(pointString.data());
}

jstring openssl_multiply(JNIEnv *env, int curveIndex, jstring jPointString, jstring jBnString) {
    BN_CTX *ctx = BN_CTX_new();
    // 读取幂指数
    const char* jBnStringHandler = (*env).GetStringUTFChars(jBnString, JNI_FALSE);
    std::string bnString = std::string(jBnStringHandler);
    (*env).ReleaseStringUTFChars(jBnString, jBnStringHandler);
    BIGNUM *bn = BN_new();
    bnFromString(bnString, bn);
    // 读取椭圆曲线点
    const char* jPointStringHandler = (*env).GetStringUTFChars(jPointString, JNI_FALSE);
    const std::string pointString = std::string(jPointStringHandler);
    (*env).ReleaseStringUTFChars(jPointString, jPointStringHandler);
    EC_POINT *point = EC_POINT_new(openssl_ec_group[curveIndex]);
    pointFromString(curveIndex, pointString, point, ctx);
    // 计算乘法
    EC_POINT *mulPoint = EC_POINT_new(openssl_ec_group[curveIndex]);
    EC_POINT_mul(openssl_ec_group[curveIndex], mulPoint, nullptr, point, bn, ctx);
    BN_free(bn);
    EC_POINT_free(point);
    // 返回结果
    std::string mulPointString = pointToString(curveIndex, mulPoint, ctx);
    EC_POINT_free(mulPoint);
    BN_CTX_free(ctx);
    return (*env).NewStringUTF(mulPointString.data());
}