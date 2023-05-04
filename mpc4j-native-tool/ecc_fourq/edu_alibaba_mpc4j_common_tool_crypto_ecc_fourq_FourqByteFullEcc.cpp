/*
 * @Description: 
 * @Author: Qixian Zhou
 * @Date: 2023-04-05 16:12:44
 */


#include "FourQ.h"
#include "FourQ_api.h"
#include "FourQ_internal.h"

#include <cstring>
#include "edu_alibaba_mpc4j_common_tool_crypto_ecc_fourq_FourqByteFullEcc.h"

/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_fourq_FourqByteFullEcc
 * Method:    nativeMul
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_fourq_FourqByteFullEcc_nativeMul
    (JNIEnv *env, jobject context, jbyteArray jEcByteArray, jbyteArray jZnByteArray) {
    // parse point
    jbyte *ecBuffer = (*env).GetByteArrayElements(jEcByteArray, nullptr);
    uint8_t p[32];
    memcpy(p, ecBuffer, 32);
    (*env).ReleaseByteArrayElements(jEcByteArray, ecBuffer, 0);
    point_t A;
    ECCRYPTO_STATUS status = decode(p, A);
    if (status != ECCRYPTO_SUCCESS) {
        auto exception = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(exception, "decode failed, invalid point.");
    }
    // parse scalar
    jbyte *znBuffer = (*env).GetByteArrayElements(jZnByteArray, nullptr);
    uint8_t k[32];
    memcpy(k, znBuffer, 32);
    (*env).ReleaseByteArrayElements(jZnByteArray, znBuffer, 0);
    // R = k * A , clear_cofactor is set to false by default.
    point_t R;
    bool mul_status = ecc_mul(A, (digit_t *) k, R, false);
    if (!mul_status) {
        auto exception = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(exception, "ecc_mul failed, invalid point.");
    }
    // encode and return 
    uint8_t res[32];
    encode(R, res);
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize) 32);
    (*env).SetByteArrayRegion(jMulByteArray, 0, 32, (const jbyte *) res);

    return jMulByteArray;
}


/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_fourq_FourqByteFullEcc
 * Method:    nativeBaseMul
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_fourq_FourqByteFullEcc_nativeBaseMul
    (JNIEnv *env, jobject context, jbyteArray jZnByteArray) {
    // parse scalar
    jbyte *znBuffer = (*env).GetByteArrayElements(jZnByteArray, nullptr);
    uint8_t k[32];
    memcpy(k, znBuffer, 32);
    (*env).ReleaseByteArrayElements(jZnByteArray, znBuffer, 0);

    // R = k * G , G is the generator 
    point_t R;
    bool mul_status = ecc_mul_fixed((digit_t *) k, R);
    if (!mul_status) {
        auto exception = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(exception, "ecc_base_mul failed, invalid point.");
    }
    // encode and return 
    uint8_t res[32];
    encode(R, res);
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize) 32);
    (*env).SetByteArrayRegion(jMulByteArray, 0, 32, (const jbyte *) res);

    return jMulByteArray;
}

/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_fourq_FourqByteFullEcc
 * Method:    nativeIsValidPoint
 * Signature: ([B[B)[B
 */
JNIEXPORT jboolean JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_fourq_FourqByteFullEcc_nativeIsValidPoint
    (JNIEnv *env, jobject context, jbyteArray jEcByteArray) {
    // parse point
    jbyte *ecBuffer = (*env).GetByteArrayElements(jEcByteArray, nullptr);
    uint8_t p[32];
    memcpy(p, ecBuffer, 32);
    (*env).ReleaseByteArrayElements(jEcByteArray, ecBuffer, 0);
    // convert p to Point
    point_t A;
    ECCRYPTO_STATUS status = decode(p, A);

    jboolean res = JNI_FALSE;
    if (status != ECCRYPTO_SUCCESS) {
        return res;
    } else {
        res = JNI_TRUE;
        return res;
    }

}
/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_fourq_FourqByteFullEcc
 * Method:    nativeNeg
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_fourq_FourqByteFullEcc_nativeNeg
    (JNIEnv *env, jobject context, jbyteArray jEcByteArray) {
    // parse point
    jbyte *ecBuffer = (*env).GetByteArrayElements(jEcByteArray, nullptr);
    uint8_t p[32];
    memcpy(p, ecBuffer, 32);
    (*env).ReleaseByteArrayElements(jEcByteArray, ecBuffer, 0);
    // convert p to Point
    point_t A;
    ECCRYPTO_STATUS status = decode(p, A);
    if (status != ECCRYPTO_SUCCESS) {
        auto exception = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(exception, "decode failed, invalid point.");
    }
    // neg
    fp2neg1271(A->x);
    // encode and return
    uint8_t res[32];
    encode(A, res);
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize) 32);
    (*env).SetByteArrayRegion(jMulByteArray, 0, 32, (const jbyte *) res);

    return jMulByteArray;

}

/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_fourq_FourqByteFullEcc
 * Method:    nativeAdd
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_fourq_FourqByteFullEcc_nativeAdd
    (JNIEnv *env, jobject context, jbyteArray jEcByteArray_p, jbyteArray jEcByteArray_q) {
    // parse point p
    jbyte *ecBuffer_p = (*env).GetByteArrayElements(jEcByteArray_p, nullptr);
    uint8_t p[32];
    memcpy(p, ecBuffer_p, 32);
    (*env).ReleaseByteArrayElements(jEcByteArray_p, ecBuffer_p, 0);
    point_t A;
    ECCRYPTO_STATUS status_p = decode(p, A);
    if (status_p != ECCRYPTO_SUCCESS) {
        auto exception = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(exception, "decode failed, invalid point.");
    }
    // parse point q
    jbyte *ecBuffer_q = (*env).GetByteArrayElements(jEcByteArray_q, nullptr);
    uint8_t q[32];
    memcpy(q, ecBuffer_q, 32);
    (*env).ReleaseByteArrayElements(jEcByteArray_q, ecBuffer_q, 0);
    point_t B;
    ECCRYPTO_STATUS status_q = decode(q, B);
    if (status_q != ECCRYPTO_SUCCESS) {
        auto exception = env->FindClass("java/lang/IllegalArgumentException");
        env->ThrowNew(exception, "decode failed, invalid point.");
    }
    // R = A + B
    // void eccadd(point_extproj_precomp_t Q, point_extproj_t P);
    // need conversion between point_t and point_extproj_precomp_t

    point_extproj_t AA;
    point_extproj_t BB;

    // convert point_t to point_extproj_t
    point_setup(A, AA);
    point_setup(B, BB);
    // convert point_extproj_t to point_extproj_precomp_t
    // R1_to_R2(point_extproj_t P, point_extproj_precomp_t Q) 
    point_extproj_precomp_t BBQ;
    R1_to_R2(BB, BBQ);
    // eccadd
    eccadd(BBQ, AA);
    // convert to point_t
    point_t R;
    // void eccnorm(point_extproj *P, point_affine *Q)
    eccnorm(AA, R);
    mod1271(R->x[0]);
    // Fully reduced P
    mod1271(R->x[1]);
    mod1271(R->y[0]);
    mod1271(R->y[1]);
    // encode and return 
    uint8_t res[32];
    encode(R, res); // 核心方法
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize) 32);
    (*env).SetByteArrayRegion(jMulByteArray, 0, 32, (const jbyte *) res);

    return jMulByteArray;

}

/*
 * Class:     edu_alibaba_mpc4j_common_tool_crypto_ecc_fourq_FourqByteFullEcc
 * Method:    nativeHashToCurve
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_common_tool_crypto_ecc_fourq_FourqByteFullEcc_nativeHashToCurve
    (JNIEnv *env, jobject context, jbyteArray message_hashed) {
    // parse message_hashed
    jbyte *ecBuffer = (*env).GetByteArrayElements(message_hashed, nullptr);
    uint8_t m[32];
    memcpy(m, ecBuffer, 32);
    (*env).ReleaseByteArrayElements(message_hashed, ecBuffer, 0);
    // 32-byte
    f2elm_t r;
    memcpy(r, m, 32);
    // Reduce r; note that this does not produce a perfectly uniform distribution
    // modulo 2^127-1, but it is good enough.
    mod1271(r[0]);
    mod1271(r[1]);

    point_t Q;
    HashToCurve(r, Q);
    // encode and return
    uint8_t res[32];
    encode(Q, res);
    jbyteArray jMulByteArray = (*env).NewByteArray((jsize) 32);
    (*env).SetByteArrayRegion(jMulByteArray, 0, 32, (const jbyte *) res);

    return jMulByteArray;
}

