//
// Created by Weiran Liu on 2022/8/5.
//

#include "edu_alibaba_mpc4j_common_tool_polynomial_zp64_NtlZp64Poly.h"
#include "ntl_zp64.h"

JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp64_NtlZp64Poly_nativeInterpolate
        (JNIEnv *env, jclass context, jlong jPrime, jint jNum, jlongArray jxArray, jlongArray jyArray) {
    // 设置有限域
    NTL::ZZ prime(jPrime);
    NTL::ZZ_pContext pContext = NTL::ZZ_pContext(prime);
    // 将上下文设置为存储的pContext，参见https://libntl.org/doc/tour-ex7.html
    pContext.restore();
    // 读取集合x与集合y
    std::vector<long> setX;
    jLongArrayToSet(env, jxArray, setX);
    std::vector<long> setY;
    jLongArrayToSet(env, jyArray, setY);
    // 插值
    std::vector<long> coeffs;
    zp64_interpolate(static_cast<uint64_t>(jNum), setX, setY, coeffs);
    setX.clear();
    setY.clear();
    // 返回结果
    jlongArray jCoeffArray;
    setTojLongArray(env, coeffs, jNum, jCoeffArray);
    coeffs.clear();

    return jCoeffArray;
}

JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp64_NtlZp64Poly_nativeRootInterpolate
        (JNIEnv *env, jclass context, jlong jPrime, jint jNum, jlongArray jxArray, jlong jy) {
    // 设置有限域
    NTL::ZZ prime(jPrime);
    NTL::ZZ_pContext pContext = NTL::ZZ_pContext(prime);
    // 将上下文设置为存储的pContext，参见https://libntl.org/doc/tour-ex7.html
    pContext.restore();
    // 读取集合x
    std::vector<long> setX;
    jLongArrayToSet(env, jxArray, setX);
    // 插值
    std::vector<long> coeffs;
    zp64_root_interpolate(static_cast<uint64_t>(jNum), setX, jy, coeffs);
    setX.clear();
    // 返回结果
    jlongArray jCoeffArray;
    setTojLongArray(env, coeffs, jNum + 1, jCoeffArray);
    coeffs.clear();

    return jCoeffArray;
}

JNIEXPORT jlong JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp64_NtlZp64Poly_nativeSingleEvaluate
        (JNIEnv *env, jclass context, jlong jPrime, jlongArray jCoeffArray, jlong jx) {
    // 设置有限域
    NTL::ZZ prime(jPrime);
    NTL::ZZ_pContext pContext = NTL::ZZ_pContext(prime);
    // 将上下文设置为存储的pContext，参见https://libntl.org/doc/tour-ex7.html
    pContext.restore();
    // 读取系数
    std::vector<long> coeffs;
    jLongArrayToSet(env, jCoeffArray, coeffs);
    // 求值
    return zp64_evaluate(coeffs, jx);
}

JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp64_NtlZp64Poly_nativeEvaluate
        (JNIEnv *env, jclass context, jlong jPrime, jlongArray jCoeffArray, jlongArray jxArray) {
    // 设置有限域
    NTL::ZZ prime(jPrime);
    NTL::ZZ_pContext pContext = NTL::ZZ_pContext(prime);
    // 将上下文设置为存储的pContext，参见https://libntl.org/doc/tour-ex7.html
    pContext.restore();
    // 读取系数
    std::vector<long> coeffs;
    jLongArrayToSet(env, jCoeffArray, coeffs);
    // 读取x
    std::vector<long> setX;
    jLongArrayToSet(env, jxArray, setX);
    // 求值
    std::vector<long> setY(setX.size());
    for (uint64_t index = 0; index < setX.size(); index++) {
        setY[index] = zp64_evaluate(coeffs, setX[index]);
    }
    coeffs.clear();
    setX.clear();
    // 返回结果
    jlongArray jyArray;
    setTojLongArray(env, setY, static_cast<jint>(setY.size()), jyArray);
    setY.clear();

    return jyArray;
}
