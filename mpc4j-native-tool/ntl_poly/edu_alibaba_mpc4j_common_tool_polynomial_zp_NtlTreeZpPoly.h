/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly */

#ifndef _Included_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly
#define _Included_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly
 * Method:    nativeBuildBinaryTree
 * Signature: ([B[[B)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeBuildBinaryTree
  (JNIEnv *, jclass, jbyteArray, jobjectArray);

/*
 * Class:     edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly
 * Method:    nativeBuildDerivativeInverses
 * Signature: (Ljava/nio/ByteBuffer;I)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeBuildDerivativeInverses
  (JNIEnv *, jclass, jobject, jint);

/*
 * Class:     edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly
 * Method:    nativeDestroyBinaryTree
 * Signature: (Ljava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeDestroyBinaryTree
  (JNIEnv *, jclass, jobject);

/*
 * Class:     edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly
 * Method:    nativeDestroyDerivativeInverses
 * Signature: (Ljava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeDestroyDerivativeInverses
  (JNIEnv *, jclass, jobject);

/*
 * Class:     edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly
 * Method:    nativeInterpolate
 * Signature: ([BLjava/nio/ByteBuffer;Ljava/nio/ByteBuffer;[[B)[[B
 */
JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeInterpolate
  (JNIEnv *, jclass, jbyteArray, jobject, jobject, jobjectArray);

/*
 * Class:     edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly
 * Method:    nativeSingleEvaluate
 * Signature: ([B[[B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeSingleEvaluate
  (JNIEnv *, jclass, jbyteArray, jobjectArray, jbyteArray);

/*
 * Class:     edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly
 * Method:    nativeTreeEvaluate
 * Signature: ([B[[BLjava/nio/ByteBuffer;I)[[B
 */
JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly_nativeTreeEvaluate
  (JNIEnv *, jclass, jbyteArray, jobjectArray, jobject, jint);

#ifdef __cplusplus
}
#endif
#endif
