//
// Created by Liqiang Peng on 2022/11/5.
//

#include "edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeUtils.h"
#include "seal/seal.h"
#include "../serialize.h"
#include "../utils.h"

using namespace std;
using namespace seal;

JNIEXPORT jlong JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeUtils_checkCreatePlainModulus(
    JNIEnv *env, jclass, jint poly_modulus_degree, jint plain_modulus_size) {
    uint64_t plain_modulus;
    jclass exception = env->FindClass("java/lang/Exception");
    try {
        plain_modulus = PlainModulus::Batching(poly_modulus_degree, plain_modulus_size).value();
    } catch (...) {
        return env->ThrowNew(exception, "Failed to find enough qualifying primes.");
    }
    EncryptionParameters parms = generate_encryption_parameters(scheme_type::bfv, poly_modulus_degree, plain_modulus);
    SEALContext context(parms, true);
    if (!context.parameters_set()) {
        return env->ThrowNew(exception, "SEAL parameters not valid.");
    }
    if (!context.first_context_data()->qualifiers().using_batching) {
        return env->ThrowNew(exception, "SEAL parameters do not support batching.");
    }
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    PublicKey public_key;
    key_gen.create_public_key(public_key);
    Encryptor encryptor(context, public_key);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    encryptor.set_secret_key(secret_key);
    Decryptor decryptor(context, secret_key);
    size_t slot_count = encoder.slot_count();
    vector<uint64_t> coeffs(slot_count);
    vector<uint64_t> a0(slot_count), a1(slot_count), b0(slot_count), b1(slot_count), r(slot_count);
    for (int j = 0; j < slot_count; j++) {
        a0[j] = random_uint64() % plain_modulus;
        a1[j] = random_uint64() % plain_modulus;
        b0[j] = random_uint64() % plain_modulus;
        b1[j] = random_uint64() % plain_modulus;
        r[j] = random_uint64() % plain_modulus;
    }
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    vector<Ciphertext> ct(2);
    Plaintext plaintext_a0, plaintext_b0;
    encoder.encode(a0, plaintext_a0);
    encoder.encode(b0, plaintext_b0);
    encryptor.encrypt_symmetric(plaintext_a0, ct[0]);
    encryptor.encrypt_symmetric(plaintext_b0, ct[1]);
    for (auto & i : ct) {
        // Only one ciphertext-plaintext multiplication is needed after this
        evaluator.mod_switch_to_inplace(i, parms_id);
        // All ciphertexts must be in NTT form
        evaluator.transform_to_ntt_inplace(i);
    }
    Plaintext plaintext_a1, plaintext_b1, plaintext_r;
    encoder.encode(a1, plaintext_a1);
    encoder.encode(b1, plaintext_b1);
    encoder.encode(r, plaintext_r);
    evaluator.transform_to_ntt_inplace(plaintext_a1, parms_id);
    evaluator.transform_to_ntt_inplace(plaintext_b1, parms_id);
    evaluator.multiply_plain_inplace(ct[0], plaintext_b1);
    evaluator.multiply_plain_inplace(ct[1], plaintext_a1);
    evaluator.add_inplace(ct[0], ct[1]);
    Ciphertext ct_d;
    evaluator.transform_from_ntt_inplace(ct[0]);
    evaluator.add_plain(ct[0], plaintext_r, ct_d);
    while (ct_d.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(ct_d);
    }
    if (decryptor.invariant_noise_budget(ct_d) <= 0) {
        return env->ThrowNew(exception, "Noise budget is not enough.");
    }
    return (jlong) plain_modulus;
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeUtils_keyGen(
    JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus) {
    EncryptionParameters parms = generate_encryption_parameters(scheme_type::bfv, poly_modulus_degree, plain_modulus);
    SEALContext context = SEALContext(parms);
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    Serializable public_key = key_gen.create_public_key();
    return serialize_public_key_secret_key(env, parms, public_key, secret_key);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeUtils_encryption(
    JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray public_key_bytes, jbyteArray secret_key_bytes,
    jlongArray coeff_array0, jlongArray coeff_array1) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context(parms);
    auto exception = env->FindClass("java/lang/Exception");
    PublicKey public_key = deserialize_public_key(env, public_key_bytes, context);
    if (!is_metadata_valid_for(public_key, context)) {
        env->ThrowNew(exception, "invalid public key for this SEALContext!");
    }
    SecretKey secret_key = deserialize_secret_key(env, secret_key_bytes, context);
    if (!is_metadata_valid_for(secret_key, context)) {
        env->ThrowNew(exception, "invalid secret key for this SEALContext!");
    }
    Encryptor encryptor(context, public_key);
    encryptor.set_secret_key(secret_key);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    int size = env->GetArrayLength(coeff_array0);
    long *ptr0 = env->GetLongArrayElements(coeff_array0, JNI_FALSE);
    long *ptr1 = env->GetLongArrayElements(coeff_array1, JNI_FALSE);
    vector<uint64_t> vec0(ptr0, ptr0 + size), vec1(ptr1, ptr1 + size);
    Plaintext plaintext0, plaintext1;
    encoder.encode(vec0, plaintext0);
    encoder.encode(vec1, plaintext1);
    vector<Ciphertext> ct(2);
    encryptor.encrypt_symmetric(plaintext0, ct[0]);
    encryptor.encrypt_symmetric(plaintext1, ct[1]);
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    for (auto & i : ct) {
        // Only one ciphertext-plaintext multiplication is needed after this
        evaluator.mod_switch_to_inplace(i, parms_id);
        // All ciphertexts must be in NTT form
        evaluator.transform_to_ntt_inplace(i);
    }
    return serialize_ciphertexts(env, ct);
}

JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeUtils_decryption(
    JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray secret_key_bytes, jbyteArray ciphertext_bytes) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context(parms);
    auto exception = env->FindClass("java/lang/Exception");
    SecretKey secret_key = deserialize_secret_key(env, secret_key_bytes, context);
    if (!is_metadata_valid_for(secret_key, context)) {
        env->ThrowNew(exception, "invalid secret key for this SEALContext!");
    }
    Ciphertext ciphertext = deserialize_ciphertext(env, ciphertext_bytes, context);
    if (!is_metadata_valid_for(ciphertext, context)) {
        env->ThrowNew(exception, "invalid ciphertext for this SEALContext!");
    }
    Decryptor decryptor(context, secret_key);
    Plaintext plaintext;
    decryptor.decrypt(ciphertext, plaintext);
    BatchEncoder encoder(context);
    vector<uint64_t> coeffs;
    coeffs.resize(parms.poly_modulus_degree());
    encoder.decode(plaintext, coeffs);
    jlongArray result;
    result = env->NewLongArray((jsize) parms.poly_modulus_degree());
    jlong temp[parms.poly_modulus_degree()];
    for (int i = 0; i < parms.poly_modulus_degree(); i++) {
        temp[i] = (jlong) coeffs[i];
    }
    env->SetLongArrayRegion(result, 0, (jsize) parms.poly_modulus_degree(), temp);
    return result;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeUtils_computeResponse(
    JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray cipher1_bytes, jbyteArray cipher2_bytes,
    jlongArray plain1, jlongArray plain2, jlongArray r) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    vector<Ciphertext> ct;
    ct.resize(2);
    ct[0] = deserialize_ciphertext(env, cipher1_bytes, context);
    ct[1] = deserialize_ciphertext(env, cipher2_bytes, context);
    auto exception = env->FindClass("java/lang/Exception");
    if (!is_metadata_valid_for(ct[0], context) ||  !is_metadata_valid_for(ct[1], context)) {
        env->ThrowNew(exception, "invalid ciphertext for this SEALContext!");
    }
    int size = env->GetArrayLength(plain1);
    long *ptr_plain1 = env->GetLongArrayElements(plain1, JNI_FALSE);
    long *ptr_plain2 = env->GetLongArrayElements(plain2, JNI_FALSE);
    long *ptr_r = env->GetLongArrayElements(r, JNI_FALSE);
    vector<uint64_t> vec_plain1(ptr_plain1, ptr_plain1 + size);
    vector<uint64_t> vec_plain2(ptr_plain2, ptr_plain2 + size);
    vector<uint64_t> vec_r(ptr_r, ptr_r + size);
    Plaintext plaintext_a1, plaintext_b1, random_mask;
    encoder.encode(vec_plain1, plaintext_a1);
    encoder.encode(vec_plain2, plaintext_b1);
    encoder.encode(vec_r, random_mask);
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    evaluator.transform_to_ntt_inplace(plaintext_a1, parms_id);
    evaluator.transform_to_ntt_inplace(plaintext_b1, parms_id);
    evaluator.multiply_plain_inplace(ct[0], plaintext_b1);
    evaluator.multiply_plain_inplace(ct[1], plaintext_a1);
    // add random mask
    evaluator.add_inplace(ct[0], ct[1]);
    evaluator.transform_from_ntt_inplace(ct[0]);
    Ciphertext ct_d;
    evaluator.add_plain(ct[0], random_mask, ct_d);
    while (ct_d.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(ct_d);
    }
    return serialize_ciphertext(env, ct_d);
}