/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils */

#ifndef _Included_edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils
#define _Included_edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils
 * Method:    genEncryptionParameters
 * Signature: (IJ[I)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils_genEncryptionParameters
  (JNIEnv *, jclass, jint, jlong, jintArray);

/*
 * Class:     edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils
 * Method:    keyGen
 * Signature: ([B)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils_keyGen
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils
 * Method:    preprocessDatabase
 * Signature: ([B[[JI)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils_preprocessDatabase
  (JNIEnv *, jclass, jbyteArray, jobjectArray, jint);

/*
 * Class:     edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils
 * Method:    computeEncryptedPowers
 * Signature: ([B[BLjava/util/List;[[I[II)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils_computeEncryptedPowers
  (JNIEnv *, jclass, jbyteArray, jbyteArray, jobject, jobjectArray, jintArray, jint);

/*
 * Class:     edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils
 * Method:    optComputeMatches
 * Signature: ([B[BLjava/util/List;Ljava/util/List;I[J)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils_optComputeMatches
  (JNIEnv *, jclass, jbyteArray, jbyteArray, jobject, jobject, jint, jlongArray);

/*
 * Class:     edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils
 * Method:    naiveComputeMatches
 * Signature: ([BLjava/util/List;Ljava/util/List;[J)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils_naiveComputeMatches
  (JNIEnv *, jclass, jbyteArray, jobject, jobject, jlongArray);

/*
 * Class:     edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils
 * Method:    generateQuery
 * Signature: ([B[B[[J)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils_generateQuery
  (JNIEnv *, jclass, jbyteArray, jbyteArray, jobjectArray);

/*
 * Class:     edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils
 * Method:    decodeReply
 * Signature: ([B[B[B)[J
 */
JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils_decodeReply
  (JNIEnv *, jclass, jbyteArray, jbyteArray, jbyteArray);

#ifdef __cplusplus
}
#endif
#endif