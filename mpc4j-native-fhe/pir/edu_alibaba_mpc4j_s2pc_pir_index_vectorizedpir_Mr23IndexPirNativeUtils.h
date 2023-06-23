/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils */

#ifndef _Included_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils
#define _Included_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils
 * Method:    generateSealContext
 * Signature: (II)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils_generateSealContext
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils
 * Method:    keyGen
 * Signature: ([BI)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils_keyGen
  (JNIEnv *, jclass, jbyteArray, jint);

/*
 * Class:     edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils
 * Method:    preprocessDatabase
 * Signature: ([B[J[II)Ljava/util/ArrayList;
 */
JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils_preprocessDatabase
  (JNIEnv *, jclass, jbyteArray, jlongArray, jintArray, jint);

/*
 * Class:     edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils
 * Method:    generateQuery
 * Signature: ([B[B[B[II)Ljava/util/ArrayList;
 */
JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils_generateQuery
  (JNIEnv *, jclass, jbyteArray, jbyteArray, jbyteArray, jintArray, jint);

/*
 * Class:     edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils
 * Method:    generateReply
 * Signature: ([BLjava/util/List;Ljava/util/List;[B[B[BII)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils_generateReply
  (JNIEnv *, jclass, jbyteArray, jobject, jobject, jbyteArray, jbyteArray, jbyteArray, jint, jint);

/*
 * Class:     edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils
 * Method:    decryptReply
 * Signature: ([B[B[BII)J
 */
JNIEXPORT jlong JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_vectorizedpir_Mr23IndexPirNativeUtils_decryptReply
  (JNIEnv *, jclass, jbyteArray, jbyteArray, jbyteArray, jint, jint);

#ifdef __cplusplus
}
#endif
#endif