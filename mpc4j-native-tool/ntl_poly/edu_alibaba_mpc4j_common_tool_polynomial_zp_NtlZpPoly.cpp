//
// Created by Weiran Liu on 2022/1/5.
//

#include "edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlZpPoly.h"
#include "ntl_zp.h"

void byteArrayToPrime(JNIEnv *env, uint8_t* primeByteArray, jbyteArray jprimeByteArray, int primeByteLength) {
    // 读取质数的字节长度，读取质数
    jbyte* jprimeByteBuffer = (*env).GetByteArrayElements(jprimeByteArray, nullptr);
    memcpy(primeByteArray, jprimeByteBuffer, static_cast<size_t>(primeByteLength));
    reverseBytes(primeByteArray, static_cast<uint64_t>(primeByteLength));
    (*env).ReleaseByteArrayElements(jprimeByteArray, jprimeByteBuffer, 0);
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlZpPoly_nativeInterpolate
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jint jnum, jobjectArray jxArray, jobjectArray jyArray) {
    // 读取质数的字节长度
    int primeByteLength = (*env).GetArrayLength(jprimeByteArray);
    // 读取质数
    auto* primeByteArray = new uint8_t[primeByteLength];
    byteArrayToPrime(env, primeByteArray, jprimeByteArray, primeByteLength);
    // 设置有限域
    NTL::ZZ prime;
    NTL::ZZFromBytes(prime, primeByteArray, static_cast<long>(primeByteLength));
    NTL::ZZ_pContext pContext = NTL::ZZ_pContext(prime);
    // 将上下文设置为存储的pContext，参见https://libntl.org/doc/tour-ex7.html
    pContext.restore();
    // 读取集合x与集合y
    std::vector<uint8_t*> setX;
    jByteArrayToSet(env, jxArray, static_cast<uint64_t>(primeByteLength), setX);
    std::vector<uint8_t*> setY;
    jByteArrayToSet(env, jyArray, static_cast<uint64_t>(primeByteLength), setY);
    // 插值
    std::vector<uint8_t*> coeffs;
    zp_interpolate(primeByteLength, static_cast<uint64_t>(jnum), setX, setY, coeffs);
    setX.clear();
    setY.clear();
    // 返回结果
    jobjectArray jCoeffArray;
    setTojByteArray(env, coeffs, static_cast<uint64_t>(primeByteLength), jnum, jCoeffArray);
    coeffs.clear();

    return jCoeffArray;
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlZpPoly_nativeRootInterpolate
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jint jnum, jobjectArray jxArray, jbyteArray jy) {
    // 读取质数的字节长度
    int primeByteLength = (*env).GetArrayLength(jprimeByteArray);
    // 读取质数
    auto* primeByteArray = new uint8_t[primeByteLength];
    byteArrayToPrime(env, primeByteArray, jprimeByteArray, primeByteLength);
    // 设置有限域
    NTL::ZZ prime;
    NTL::ZZFromBytes(prime, primeByteArray, static_cast<long>(primeByteLength));
    NTL::ZZ_pContext pContext = NTL::ZZ_pContext(prime);
    // 将上下文设置为存储的pContext，参见https://libntl.org/doc/tour-ex7.html
    pContext.restore();
    // 读取集合x
    std::vector<uint8_t*> setX;
    jByteArrayToSet(env, jxArray, static_cast<uint64_t>(primeByteLength), setX);
    // 读取y
    jbyte* jyBuffer = (*env).GetByteArrayElements(jy, nullptr);
    auto* y = new uint8_t[primeByteLength];
    memcpy(y, jyBuffer, primeByteLength);
    reverseBytes(y, primeByteLength);
    (*env).ReleaseByteArrayElements(jy, jyBuffer, 0);
    // 插值
    std::vector<uint8_t*> coeffs;
    zp_root_interpolate(primeByteLength, static_cast<uint64_t>(jnum), setX, y, coeffs);
    setX.clear();
    // 返回结果
    jobjectArray jCoeffArray;
    setTojByteArray(env, coeffs, static_cast<uint64_t>(primeByteLength), jnum + 1, jCoeffArray);
    coeffs.clear();

    return jCoeffArray;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlZpPoly_nativeSingleEvaluate
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jobjectArray jCoeffArray, jbyteArray jx) {
    // 读取质数的字节长度
    int primeByteLength = (*env).GetArrayLength(jprimeByteArray);
    // 读取质数
    auto* primeByteArray = new uint8_t[primeByteLength];
    byteArrayToPrime(env, primeByteArray, jprimeByteArray, primeByteLength);
    // 设置有限域
    NTL::ZZ prime;
    NTL::ZZFromBytes(prime, primeByteArray, static_cast<long>(primeByteLength));
    NTL::ZZ_pContext pContext = NTL::ZZ_pContext(prime);
    // 将上下文设置为存储的pContext，参见https://libntl.org/doc/tour-ex7.html
    pContext.restore();
    // 读取系数
    std::vector<uint8_t *> coeffs;
    jByteArrayToSet(env, jCoeffArray, static_cast<uint64_t>(primeByteLength), coeffs);
    // 读取x
    jbyte* jxBuffer = (*env).GetByteArrayElements(jx, nullptr);
    auto* x = new uint8_t[primeByteLength];
    memcpy(x, jxBuffer, primeByteLength);
    reverseBytes(x, primeByteLength);
    (*env).ReleaseByteArrayElements(jx, jxBuffer, 0);
    // 求值
    auto* y = new uint8_t[primeByteLength];
    zp_evaluate(primeByteLength, coeffs, x, y);
    reverseBytes(y, primeByteLength);
    coeffs.clear();
    delete[] x;
    // 返回结果
    jbyteArray jy = (*env).NewByteArray((jsize)primeByteLength);
    (*env).SetByteArrayRegion(jy, 0, primeByteLength, (const jbyte*)y);
    delete[] y;

    return jy;
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlZpPoly_nativeEvaluate
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jobjectArray jCoeffArray, jobjectArray jxArray) {
    // 读取质数的字节长度
    int primeByteLength = (*env).GetArrayLength(jprimeByteArray);
    // 读取质数
    auto* primeByteArray = new uint8_t[primeByteLength];
    byteArrayToPrime(env, primeByteArray, jprimeByteArray, primeByteLength);
    // 设置有限域
    NTL::ZZ prime;
    NTL::ZZFromBytes(prime, primeByteArray, static_cast<long>(primeByteLength));
    NTL::ZZ_pContext pContext = NTL::ZZ_pContext(prime);
    // 将上下文设置为存储的pContext，参见https://libntl.org/doc/tour-ex7.html
    pContext.restore();
    // 读取系数
    std::vector<uint8_t *> coeffs;
    jByteArrayToSet(env, jCoeffArray, static_cast<uint64_t>(primeByteLength), coeffs);
    // 读取x
    std::vector<uint8_t*> setX;
    jByteArrayToSet(env, jxArray, static_cast<uint64_t>(primeByteLength), setX);
    // 求值
    std::vector<uint8_t*> setY(setX.size());
    for (uint64_t index = 0; index < setX.size(); index++) {
        setY[index] = new uint8_t [primeByteLength];
        zp_evaluate(primeByteLength, coeffs, setX[index], setY[index]);
    }
    coeffs.clear();
    setX.clear();
    // 返回结果
    jobjectArray jyArray;
    setTojByteArray(env, setY, static_cast<uint64_t>(primeByteLength), static_cast<jint>(setY.size()), jyArray);
    setY.clear();

    return jyArray;
}