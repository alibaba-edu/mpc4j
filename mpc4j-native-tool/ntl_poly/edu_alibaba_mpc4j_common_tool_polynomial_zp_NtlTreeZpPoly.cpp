//
// Created by Weiran Liu on 2022/11/2.
//
#include "edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly.h"
#include "ntl_tree_zp.h"
#include "ntl_zp_util.h"
#include "ntl_zp.h"

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeBuildBinaryTree
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jobjectArray jxArray) {
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
    NTL::ZZ_pX* binary_tree_handler = build_binary_tree(primeByteLength, setX);
    freeByteArraySet(setX);
    setX.clear();
    return (*env).NewDirectByteBuffer(binary_tree_handler, 0);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeBuildDerivativeInverses
    (JNIEnv *env, jclass context,  jobject jbinaryTreeHandler, jint jpointNum) {
    auto* binary_tree = (NTL::ZZ_pX*)(*env).GetDirectBufferAddress(jbinaryTreeHandler);
    NTL::ZZ_p* derivative_inverses_handler = zp_derivative_inverses(binary_tree, jpointNum);
    return (*env).NewDirectByteBuffer(derivative_inverses_handler, 0);
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeDestroyBinaryTree
    (JNIEnv *env, jclass context, jobject jbinaryTreeHandler) {
    zp_free_binary_tree((NTL::ZZ_pX*)(*env).GetDirectBufferAddress(jbinaryTreeHandler));
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeDestroyDerivativeInverses
    (JNIEnv *env, jclass context, jobject jDerivativeInversesHandler) {
    zp_free_derivative_inverse((NTL::ZZ_p*)(*env).GetDirectBufferAddress(jDerivativeInversesHandler));
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeInterpolate
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jobject jbinaryTreeHandler, jobject jDerivativeInversesHandler, jobjectArray jyArray) {
    // 读取质数的字节长度
    int primeByteLength = (*env).GetArrayLength(jprimeByteArray);
    int point_num = (*env).GetArrayLength(jyArray);
    // 读取质数
    uint8_t primeByteArray[primeByteLength];
    zp_byte_array_to_prime(env, primeByteArray, jprimeByteArray, primeByteLength);
    // 设置有限域
    NTL::ZZ prime;
    NTL::ZZFromBytes(prime, primeByteArray, static_cast<long>(primeByteLength));
    NTL::ZZ_pContext pContext = NTL::ZZ_pContext(prime);
    // 将上下文设置为存储的pContext，参见https://libntl.org/doc/tour-ex7.html
    pContext.restore();
    // 读取集合y
    std::vector<uint8_t*> setY;
    jByteArrayToSet(env, jyArray, static_cast<uint64_t>(primeByteLength), setY);
    // 读取二叉树和导数
    auto* binary_tree = (NTL::ZZ_pX*)(*env).GetDirectBufferAddress(jbinaryTreeHandler);
    auto* derivative_inverses = (NTL::ZZ_p*)(*env).GetDirectBufferAddress(jDerivativeInversesHandler);
    // 插值
    std::vector<uint8_t*> coefficients;
    zp_tree_interpolate(primeByteLength, binary_tree, derivative_inverses, setY, coefficients);
    freeByteArraySet(setY);
    setY.clear();
    // 返回结果
    jobjectArray jPolynomial;
    setTojByteArray(env, coefficients, static_cast<uint64_t>(primeByteLength), point_num + 1, jPolynomial);
    freeByteArraySet(coefficients);
    coefficients.clear();
    return jPolynomial;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeSingleEvaluate
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jobjectArray jCoeffArray, jbyteArray jx) {
    return zp_single_evaluate(env, context, jprimeByteArray, jCoeffArray, jx);
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeTreeEvaluate
    (JNIEnv *env, jclass context, jbyteArray jprimeByteArray, jobjectArray jPolynomial, jobject jbinaryTreeHandler, jint jpointNum) {
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
    jByteArrayToSet(env, jPolynomial, static_cast<uint64_t>(primeByteLength), polynomial);
    // 读取二叉树
    auto* binary_tree = (NTL::ZZ_pX*)(*env).GetDirectBufferAddress(jbinaryTreeHandler);
    // 求值
    std::vector<uint8_t*> setY(jpointNum);
    zp_tree_evaluate(primeByteLength, polynomial, binary_tree, setY);
    freeByteArraySet(polynomial);
    polynomial.clear();
    // 返回结果
    jobjectArray jyArray;
    setTojByteArray(env, setY, static_cast<uint64_t>(primeByteLength), static_cast<jint>(setY.size()), jyArray);
    freeByteArraySet(setY);
    setY.clear();

    return jyArray;
}

