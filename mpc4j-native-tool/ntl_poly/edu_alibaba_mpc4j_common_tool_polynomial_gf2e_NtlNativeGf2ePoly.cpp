//
// Created by Weiran Liu on 2021/12/11.
//
#include <vector>
#include "edu_alibaba_mpc4j_common_tool_polynomial_gf2e_NtlNativeGf2ePoly.h"
#include "../common/defines.h"
#include "ntl_gf2x.h"

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_gf2e_NtlNativeGf2ePoly_interpolate
    (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jint jNum, jobjectArray jxArray, jobjectArray jyArray) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 读取集合x与集合y
    std::vector<uint8_t *> setX;
    jByteArrayToSet(env, jxArray, (uint64_t)jByteL, setX);
    std::vector<uint8_t*> setY;
    jByteArrayToSet(env, jyArray, (uint64_t)jByteL, setY);
    // 插值
    std::vector<uint8_t *> polynomial;
    gf2x_interpolate(jByteL, jNum, setX, setY, polynomial);
    setX.clear();
    setY.clear();
    // 返回结果
    jobjectArray jPolynomial;
    setTojByteArray(env, polynomial, jByteL, jNum, jPolynomial);
    polynomial.clear();

    return jPolynomial;
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_gf2e_NtlNativeGf2ePoly_rootInterpolate
    (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jint jNum, jobjectArray jxArray, jbyteArray jy) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 读取集合x
    std::vector<uint8_t *> setX;
    jByteArrayToSet(env, jxArray, (uint64_t)jByteL, setX);
    // 读取y
    jbyte* jyBuffer = (*env).GetByteArrayElements(jy, nullptr);
    auto* y = new uint8_t[jByteL];
    memcpy(y, jyBuffer, jByteL);
    reverseBytes(y, jByteL);
    (*env).ReleaseByteArrayElements(jy, jyBuffer, 0);
    // 插值
    std::vector<uint8_t *> polynomial;
    gf2x_root_interpolate(jByteL, jNum, setX, y, polynomial);
    setX.clear();
    delete[] y;
    // 返回结果
    jobjectArray jPolynomial;
    setTojByteArray(env, polynomial, jByteL, jNum + 1, jPolynomial);
    polynomial.clear();

    return jPolynomial;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_gf2e_NtlNativeGf2ePoly_singleEvaluate
    (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jobjectArray jPolynomial, jbyteArray jx) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 系数集合
    std::vector<uint8_t*> polynomial;
    jByteArrayToSet(env, jPolynomial, jByteL, polynomial);
    // 读取x
    jbyte* jxBuffer = (*env).GetByteArrayElements(jx, nullptr);
    auto* x = new uint8_t[jByteL];
    memcpy(x, jxBuffer, jByteL);
    reverseBytes(x, jByteL);
    (*env).ReleaseByteArrayElements(jx, jxBuffer, 0);
    // 求值
    auto* y = new uint8_t[jByteL];
    gf2x_evaluate(jByteL, polynomial, x, y);
    reverseBytes(y, jByteL);
    polynomial.clear();
    delete[] x;
    // 返回结果
    jbyteArray jy = (*env).NewByteArray((jsize)jByteL);
    (*env).SetByteArrayRegion(jy, 0, jByteL, (const jbyte*)y);
    delete[] y;

    return jy;
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_gf2e_NtlNativeGf2ePoly_evaluate
    (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jobjectArray jPolynomial, jobjectArray jxArray) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 系数集合
    std::vector<uint8_t*> polynomial;
    jByteArrayToSet(env, jPolynomial, jByteL, polynomial);
    // 读取x
    std::vector<uint8_t*> setX;
    jByteArrayToSet(env, jxArray, jByteL, setX);
    // 求值
    std::vector<uint8_t*> setY(setX.size());
    for (uint64_t i = 0; i < setX.size(); i++) {
        setY[i] = new uint8_t [jByteL];
        gf2x_evaluate(jByteL, polynomial, setX[i], setY[i]);
    }
    polynomial.clear();
    setX.clear();
    // 返回结果
    jobjectArray jyArray;
    setTojByteArray(env, setY, jByteL, (jint)setY.size(), jyArray);
    setY.clear();

    return jyArray;
}