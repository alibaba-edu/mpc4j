//
// Created by Weiran Liu on 2022/11/2.
//
#include <NTL/ZZ_p.h>
#include "ntl_zp_util.h"

void zp_byte_array_to_prime(JNIEnv *env, uint8_t* primeByteArray, jbyteArray jprimeByteArray, int primeByteLength) {
    // 读取质数的字节长度，读取质数
    jbyte* jprimeByteBuffer = (*env).GetByteArrayElements(jprimeByteArray, nullptr);
    memcpy(primeByteArray, jprimeByteBuffer, static_cast<size_t>(primeByteLength));
    reverseBytes(primeByteArray, static_cast<uint64_t>(primeByteLength));
    (*env).ReleaseByteArrayElements(jprimeByteArray, jprimeByteBuffer, 0);
}

jobjectArray zp_root_interpolate(JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jint jexpectNum, jobjectArray jxArray, jbyteArray jy) {
    // 读取质数的字节长度
    int primeByteLength = (*env).GetArrayLength(jprimeByteArray);
    // 读取质数
    uint8_t primeByteArray[primeByteLength];
    zp_byte_array_to_prime(env, primeByteArray, jprimeByteArray, primeByteLength);
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
    uint8_t y[primeByteLength];
    memcpy(y, jyBuffer, primeByteLength);
    reverseBytes(y, primeByteLength);
    (*env).ReleaseByteArrayElements(jy, jyBuffer, 0);
    // 插值
    std::vector<uint8_t*> polynomial;
    zp_root_interpolate(primeByteLength, jexpectNum, setX, y, polynomial);
    freeByteArraySet(setX);
    setX.clear();
    // 返回结果
    jobjectArray jPolynomial;
    setTojByteArray(env, polynomial, static_cast<uint64_t>(primeByteLength), jexpectNum + 1, jPolynomial);
    freeByteArraySet(polynomial);
    polynomial.clear();

    return jPolynomial;
}

jbyteArray zp_single_evaluate(JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jobjectArray jCoeffArray, jbyteArray jx) {
    // 读取质数的字节长度
    int primeByteLength = (*env).GetArrayLength(jprimeByteArray);
    // 读取质数
    uint8_t primeByteArray[primeByteLength];
    zp_byte_array_to_prime(env, primeByteArray, jprimeByteArray, primeByteLength);
    // 设置有限域
    NTL::ZZ prime;
    NTL::ZZFromBytes(prime, primeByteArray, static_cast<long>(primeByteLength));
    NTL::ZZ_pContext pContext = NTL::ZZ_pContext(prime);
    // 将上下文设置为存储的pContext，参见https://libntl.org/doc/tour-ex7.html
    pContext.restore();
    // 读取系数
    std::vector<uint8_t *> polynomial;
    jByteArrayToSet(env, jCoeffArray, static_cast<uint64_t>(primeByteLength), polynomial);
    // 读取x
    jbyte* jxBuffer = (*env).GetByteArrayElements(jx, nullptr);
    uint8_t x[primeByteLength];
    memcpy(x, jxBuffer, primeByteLength);
    reverseBytes(x, primeByteLength);
    (*env).ReleaseByteArrayElements(jx, jxBuffer, 0);
    // 求值
    uint8_t y[primeByteLength];
    zp_evaluate(primeByteLength, polynomial, x, y);
    reverseBytes(y, primeByteLength);
    freeByteArraySet(polynomial);
    polynomial.clear();
    // 返回结果
    jbyteArray jy = (*env).NewByteArray((jsize)primeByteLength);
    (*env).SetByteArrayRegion(jy, 0, primeByteLength, (const jbyte*)y);

    return jy;
}

void zp_polynomial_pad_dummy_item(NTL::ZZ_pX& polynomial, NTL::vec_ZZ_p& x, uint64_t expect_num) {
    /*
     * Indeed, we do not need to pad dummy items to max_bin_size.
     * We can compute a polynomial over real items that has dummy items.
     *
     * For example, there are 3 items in a bin (xi, yi) => interpolate poly p_1(x) of a degree 2.
     * 1. Generate a root poly pRoot(x) of degree 2 over (xi, 0).
     * 2. Generate a dummy poly dummy(x) of degree max_bin_size - degree of p_1(x).
     * 3. Send coefficients of the polynomial dummy(x) * pRoot(x) + p_1(x).
     *
     * If x^* = x_i => pRoot(xi) = 0 => get p_1(x^*).
     */

    NTL::ZZ_pX root_polynomial;
    NTL::BuildFromRoots(root_polynomial, x);

    NTL::ZZ_pX dummy_polynomial;
    NTL::random(dummy_polynomial, static_cast<long>(expect_num) - x.length());
    NTL::ZZ_pX d_polynomial;
    polynomial = polynomial + dummy_polynomial * root_polynomial;
}