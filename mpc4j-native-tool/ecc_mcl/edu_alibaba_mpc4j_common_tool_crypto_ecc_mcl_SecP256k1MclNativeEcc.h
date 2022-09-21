/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc */

#ifndef _Included_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc
#define _Included_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_init
  (JNIEnv *, jobject);

/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc
 * Method:    precompute
 * Signature: (Ljava/lang/String;)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_precompute
  (JNIEnv *, jobject, jstring);

/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc
 * Method:    destroyPrecompute
 * Signature: (Ljava/nio/ByteBuffer;)V
 */
JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_destroyPrecompute
  (JNIEnv *, jobject, jobject);

/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc
 * Method:    singleFixedPointMultiply
 * Signature: (Ljava/nio/ByteBuffer;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_singleFixedPointMultiply
  (JNIEnv *, jobject, jobject, jstring);

/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc
 * Method:    fixedPointMultiply
 * Signature: (Ljava/nio/ByteBuffer;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_fixedPointMultiply
  (JNIEnv *, jobject, jobject, jobjectArray);

/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc
 * Method:    singleMultiply
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_singleMultiply
  (JNIEnv *, jobject, jstring, jstring);

/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc
 * Method:    multiply
 * Signature: (Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_multiply
  (JNIEnv *, jobject, jstring, jobjectArray);

/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc
 * Method:    reset
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc_reset
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif