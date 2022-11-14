/*
 * Created by Weiran Liu on 2021/12/11.
 *
 * 2022/10/19 updates:
 * Thanks the anonymous USENIX Security 2023 AE reviewer for the suggestion.
 * In setTojByteArray and jByteArrayToSet, we need to free the memory (in set) allocated in the interpolation function.
 * All heap allocations (e.g., auto *p = new uint8_t[]) are replaced with stack allocations (e.g., uint8_t p[]).
 */

#include <vector>
#include "edu_alibaba_mpc4j_common_tool_polynomial_gf2e_NtlNativeGf2ePoly.h"
#include "../common/defines.h"
#include "ntl_gf2x.h"

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_gf2e_NtlNativeGf2ePoly_interpolate
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jint jNum, jobjectArray jxArray,
         jobjectArray jyArray) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 读取集合x与集合y
    std::vector<uint8_t *> setX;
    jByteArrayToSet(env, jxArray, (uint64_t) jByteL, setX);
    std::vector<uint8_t *> setY;
    jByteArrayToSet(env, jyArray, (uint64_t) jByteL, setY);
    // 插值
    std::vector<uint8_t *> polynomial;
    gf2x_interpolate(jByteL, jNum, setX, setY, polynomial);
    freeByteArraySet(setX);
    setX.clear();
    freeByteArraySet(setY);
    setY.clear();
    // 返回结果
    jobjectArray jPolynomial;
    setTojByteArray(env, polynomial, jByteL, jNum, jPolynomial);
    freeByteArraySet(polynomial);
    polynomial.clear();

    return jPolynomial;
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_gf2e_NtlNativeGf2ePoly_rootInterpolate
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jint jNum, jobjectArray jxArray,
         jbyteArray jy) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 读取集合x
    std::vector<uint8_t *> setX;
    jByteArrayToSet(env, jxArray, (uint64_t) jByteL, setX);
    // 读取y
    jbyte *jyBuffer = (*env).GetByteArrayElements(jy, nullptr);
    uint8_t y[jByteL];
    memcpy(y, jyBuffer, jByteL);
    reverseBytes(y, jByteL);
    (*env).ReleaseByteArrayElements(jy, jyBuffer, 0);
    // 插值
    std::vector<uint8_t *> polynomial;
    gf2x_root_interpolate(jByteL, jNum, setX, y, polynomial);
    freeByteArraySet(setX);
    setX.clear();
    // 返回结果
    jobjectArray jPolynomial;
    setTojByteArray(env, polynomial, jByteL, jNum + 1, jPolynomial);
    freeByteArraySet(polynomial);
    polynomial.clear();

    return jPolynomial;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_gf2e_NtlNativeGf2ePoly_singleEvaluate
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jobjectArray jPolynomial, jbyteArray jx) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 系数集合
    std::vector<uint8_t *> polynomial;
    jByteArrayToSet(env, jPolynomial, jByteL, polynomial);
    // 读取x
    jbyte *jxBuffer = (*env).GetByteArrayElements(jx, nullptr);
    uint8_t x[jByteL];
    memcpy(x, jxBuffer, jByteL);
    reverseBytes(x, jByteL);
    (*env).ReleaseByteArrayElements(jx, jxBuffer, 0);
    // 求值
    uint8_t y[jByteL];
    gf2x_evaluate(jByteL, polynomial, x, y);
    reverseBytes(y, jByteL);
    freeByteArraySet(polynomial);
    polynomial.clear();
    // 返回结果
    jbyteArray jy = (*env).NewByteArray((jsize) jByteL);
    (*env).SetByteArrayRegion(jy, 0, jByteL, (const jbyte *) y);

    return jy;
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_gf2e_NtlNativeGf2ePoly_evaluate
        (JNIEnv *env, jclass context, jbyteArray jMinBytes, jint jByteL, jobjectArray jPolynomial,
         jobjectArray jxArray) {
    // 初始化有限域
    initGF2E(env, jMinBytes);
    // 系数集合
    std::vector<uint8_t *> polynomial;
    jByteArrayToSet(env, jPolynomial, jByteL, polynomial);
    // 读取x
    std::vector<uint8_t *> setX;
    jByteArrayToSet(env, jxArray, jByteL, setX);
    // 求值
    std::vector<uint8_t *> setY(setX.size());
    for (uint64_t i = 0; i < setX.size(); i++) {
        setY[i] = new uint8_t[jByteL];
        gf2x_evaluate(jByteL, polynomial, setX[i], setY[i]);
    }
    freeByteArraySet(polynomial);
    polynomial.clear();
    freeByteArraySet(setX);
    setX.clear();
    // 返回结果
    jobjectArray jyArray;
    setTojByteArray(env, setY, jByteL, (jint) setY.size(), jyArray);
    freeByteArraySet(setY);
    setY.clear();

    return jyArray;
}