//
// Created by Weiran Liu on 2024/6/3.
//
#include "edu_alibaba_mpc4j_common_tool_galoisfield_sgf2k_NtlSubSgf2k.h"
#include "ntl_sgf2k_utils.h"

/**
 * subfield modulus
 */
NTL::GF2X sgf2k_002_subfield_modulus;
/**
 * field modulus
 */
NTL::GF2EXModulus sgf2k_002_field_modulus;
/**
 * subfield modulus
 */
NTL::GF2X sgf2k_004_subfield_modulus;
/**
 * field modulus
 */
NTL::GF2EXModulus sgf2k_004_field_modulus;
/**
 * subfield modulus
 */
NTL::GF2X sgf2k_008_subfield_modulus;
/**
 * field modulus
 */
NTL::GF2EXModulus sgf2k_008_field_modulus;
/**
 * subfield modulus
 */
NTL::GF2X sgf2k_016_subfield_modulus;
/**
 * field modulus
 */
NTL::GF2EXModulus sgf2k_016_field_modulus;
/**
 * subfield modulus
 */
NTL::GF2X sgf2k_032_subfield_modulus;
/**
 * field modulus
 */
NTL::GF2EXModulus sgf2k_032_field_modulus;
/**
 * subfield modulus
 */
NTL::GF2X sgf2k_064_subfield_modulus;
/**
 * field modulus
 */
NTL::GF2EXModulus sgf2k_064_field_modulus;

static void init_subfield(JNIEnv *env, uint32_t subfield_l) {
    switch (subfield_l) {
        case 2:
            NTL::GF2E::init(sgf2k_002_subfield_modulus);
            break;
        case 4:
            NTL::GF2E::init(sgf2k_004_subfield_modulus);
            break;
        case 8:
            NTL::GF2E::init(sgf2k_008_subfield_modulus);
            break;
        case 16:
            NTL::GF2E::init(sgf2k_016_subfield_modulus);
            break;
        case 32:
            NTL::GF2E::init(sgf2k_032_subfield_modulus);
            break;
        case 64:
            NTL::GF2E::init(sgf2k_064_subfield_modulus);
            break;
        default:
            jclass exception_class = (*env).FindClass("java/lang/IllegalStateException");
            (*env).ThrowNew(exception_class, "Invalid subfield L, must be ∈ {2, 4, 8, 16, 32, 64}");
            break;
    }
}

static NTL::GF2EXModulus get_field_modulus(JNIEnv *env, uint32_t subfield_l) {
    switch (subfield_l) {
        case 2:
            return sgf2k_002_field_modulus;
        case 4:
            return sgf2k_004_field_modulus;
        case 8:
            return sgf2k_008_field_modulus;
        case 16:
            return sgf2k_016_field_modulus;
        case 32:
            return sgf2k_032_field_modulus;
        case 64:
            return sgf2k_064_field_modulus;
        default:
            jclass exception_class = (*env).FindClass("java/lang/IllegalStateException");
            (*env).ThrowNew(exception_class, "Invalid subfield L, must be ∈ {2, 4, 8, 16, 32, 64}");
            break;
    }
    return {};
}

JNIEXPORT void JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_sgf2k_NtlSubSgf2k_nativeInit
    (JNIEnv *env, jobject context, jint j_subfield_l, jbyteArray j_subfield_minimal_polynomial,
     jobjectArray j_field_minimal_polynomial) {
    const uint32_t subfield_l = j_subfield_l;
    const uint32_t subfield_byte_l = (subfield_l + 8 - 1) / 8;
    const uint32_t subfield_modulus_byte_l = (subfield_l + 8) / 8;
    // read subfield minimal polynomial
    NTL::GF2X subfield_modulus;
    switch (subfield_l) {
        case 2:
            sgf2k_read_subfield_element(env, j_subfield_minimal_polynomial, subfield_modulus_byte_l, sgf2k_002_subfield_modulus);
            break;
        case 4:
            sgf2k_read_subfield_element(env, j_subfield_minimal_polynomial, subfield_modulus_byte_l, sgf2k_004_subfield_modulus);
            break;
        case 8:
            sgf2k_read_subfield_element(env, j_subfield_minimal_polynomial, subfield_modulus_byte_l, sgf2k_008_subfield_modulus);
            break;
        case 16:
            sgf2k_read_subfield_element(env, j_subfield_minimal_polynomial, subfield_modulus_byte_l, sgf2k_016_subfield_modulus);
            break;
        case 32:
            sgf2k_read_subfield_element(env, j_subfield_minimal_polynomial, subfield_modulus_byte_l, sgf2k_032_subfield_modulus);
            break;
        case 64:
            sgf2k_read_subfield_element(env, j_subfield_minimal_polynomial, subfield_modulus_byte_l, sgf2k_064_subfield_modulus);
            break;
        default:
            jclass exception_class = (*env).FindClass("java/lang/IllegalStateException");
            (*env).ThrowNew(exception_class, "Invalid subfield L, must be ∈ {2, 4, 8, 16, 32, 64, 128}");
    }
    // read field minimal polynomial
    init_subfield(env, subfield_l);
    NTL::GF2EX gf2ex_field_minimal_polynomial;
    sgf2k_read_field_element(env, j_field_minimal_polynomial, subfield_byte_l, gf2ex_field_minimal_polynomial);
    switch (subfield_l) {
        case 2:
            sgf2k_002_field_modulus = NTL::GF2EXModulus(gf2ex_field_minimal_polynomial);
            break;
        case 4:
            sgf2k_004_field_modulus = NTL::GF2EXModulus(gf2ex_field_minimal_polynomial);
            break;
        case 8:
            sgf2k_008_field_modulus = NTL::GF2EXModulus(gf2ex_field_minimal_polynomial);
            break;
        case 16:
            sgf2k_016_field_modulus = NTL::GF2EXModulus(gf2ex_field_minimal_polynomial);
            break;
        case 32:
            sgf2k_032_field_modulus = NTL::GF2EXModulus(gf2ex_field_minimal_polynomial);
            break;
        case 64:
            sgf2k_064_field_modulus = NTL::GF2EXModulus(gf2ex_field_minimal_polynomial);
            break;
        default:
            jclass exception_class = (*env).FindClass("java/lang/IllegalStateException");
            (*env).ThrowNew(exception_class, "Invalid subfield L, must be ∈ {2, 4, 8, 16, 32, 64, 128}");
    }
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_sgf2k_NtlSubSgf2k_nativeFieldMul
    (JNIEnv *env, jobject context, jint j_subfield_l, jobjectArray j_subfield_ps, jobjectArray j_subfield_qs) {
    const uint32_t subfield_l = j_subfield_l;
    const uint32_t subfield_byte_l = (subfield_l + 8 - 1) / 8;
    const uint32_t r = 128 / subfield_l;
    init_subfield(env, subfield_l);
    // load p
    NTL::GF2EX p;
    sgf2k_read_field_element(env, j_subfield_ps, subfield_byte_l, p);
    // load q
    NTL::GF2EX q;
    sgf2k_read_field_element(env, j_subfield_qs, subfield_byte_l, q);
    // t = p * q
    NTL::GF2EXModulus field_modulus = get_field_modulus(env, subfield_l);
    NTL::GF2EX t;
    NTL::MulMod(t, p, q, field_modulus);
    // return t
    jclass jByteArrayType = (*env).FindClass("[B");
    jobjectArray j_subfield_rs = (*env).NewObjectArray((int) r, jByteArrayType, nullptr);
    sgf2k_write_field_element(env, t, r, subfield_byte_l, j_subfield_rs);
    (*env).DeleteLocalRef(jByteArrayType);

    return j_subfield_rs;
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_sgf2k_NtlSubSgf2k_nativeFieldInv
    (JNIEnv *env, jobject context, jint j_subfield_l, jobjectArray j_subfield_ps) {
    const uint32_t subfield_l = j_subfield_l;
    const uint32_t subfield_byte_l = (subfield_l + 8 - 1) / 8;
    const uint32_t r = 128 / subfield_l;
    init_subfield(env, subfield_l);
    // load p
    NTL::GF2EX p;
    sgf2k_read_field_element(env, j_subfield_ps, subfield_byte_l, p);
    // t = q^{-1}
    NTL::GF2EXModulus field_modulus = get_field_modulus(env, subfield_l);
    NTL::GF2EX t;
    NTL::InvMod(t, p, field_modulus);
    // return t
    jclass jByteArrayType = (*env).FindClass("[B");
    jobjectArray j_subfield_rs = (*env).NewObjectArray((int) r, jByteArrayType, nullptr);
    sgf2k_write_field_element(env, t, r, subfield_byte_l, j_subfield_rs);
    (*env).DeleteLocalRef(jByteArrayType);

    return j_subfield_rs;
}

JNIEXPORT jobjectArray JNICALL Java_edu_alibaba_mpc4j_common_tool_galoisfield_sgf2k_NtlSubSgf2k_nativeFieldDiv
    (JNIEnv *env, jobject context, jint j_subfield_l, jobjectArray j_subfield_ps, jobjectArray j_subfield_qs) {
    const uint32_t subfield_l = j_subfield_l;
    const uint32_t subfield_byte_l = (subfield_l + 8 - 1) / 8;
    const uint32_t r = 128 / subfield_l;
    init_subfield(env, subfield_l);
    // load p
    NTL::GF2EX p;
    sgf2k_read_field_element(env, j_subfield_ps, subfield_byte_l, p);
    // load q
    NTL::GF2EX q;
    sgf2k_read_field_element(env, j_subfield_qs, subfield_byte_l, q);
    // t = p * q^{-1}
    NTL::GF2EXModulus field_modulus = get_field_modulus(env, subfield_l);
    NTL::GF2EX invQ;
    NTL::InvMod(invQ, q, field_modulus);
    NTL::GF2EX t;
    NTL::MulMod(t, p, invQ, field_modulus);
    // return t
    jclass jByteArrayType = (*env).FindClass("[B");
    jobjectArray j_subfield_rs = (*env).NewObjectArray(r, jByteArrayType, nullptr);
    sgf2k_write_field_element(env, t, r, subfield_byte_l, j_subfield_rs);
    (*env).DeleteLocalRef(jByteArrayType);

    return j_subfield_rs;
}