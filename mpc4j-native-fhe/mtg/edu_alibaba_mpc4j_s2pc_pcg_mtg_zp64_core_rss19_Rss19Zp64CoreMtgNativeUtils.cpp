//
// Created by Liqiang Peng on 2022/11/5.
//

#include "edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeUtils.h"
#include "seal/seal.h"
#include "../serialize.h"
#include "../utils.h"

using namespace std;
using namespace seal;

[[maybe_unused]] JNIEXPORT
jlong JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeUtils_checkCreatePlainModulus(
    JNIEnv *env, jclass, jint poly_modulus_degree, jint plain_modulus_size) {
    uint64_t plain_modulus;
    jclass exception = env->FindClass("java/lang/Exception");
    try {
        plain_modulus = PlainModulus::Batching(poly_modulus_degree, plain_modulus_size).value();
    } catch (...) {
        return env->ThrowNew(exception, "Failed to find enough qualifying primes.");
    }
    EncryptionParameters parms = EncryptionParameters(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(plain_modulus);
    parms.set_coeff_modulus(CoeffModulus::BFVDefault(poly_modulus_degree,sec_level_type::tc128));
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
    uint32_t slot_count = encoder.slot_count();
    vector<uint64_t> coeffs(slot_count);
    vector<uint64_t> a0(slot_count), a1(slot_count), b0(slot_count), b1(slot_count), r(slot_count);
    for (uint32_t j = 0; j < slot_count; j++) {
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
    if (decryptor.invariant_noise_budget(ct_d) == 0) {
        return env->ThrowNew(exception, "Noise budget is not enough.");
    }
    return (jlong) plain_modulus;
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeUtils_keyGen(
    JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus) {
    EncryptionParameters parms = EncryptionParameters(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(plain_modulus);
    parms.set_coeff_modulus(CoeffModulus::BFVDefault(poly_modulus_degree,sec_level_type::tc128));
    SEALContext context = SEALContext(parms);
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    Serializable<PublicKey> public_key = key_gen.create_public_key();
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    jbyteArray parms_bytes = serialize_encryption_parms(env, parms);
    jbyteArray pk_bytes = serialize_public_key(env, public_key);
    jbyteArray sk_bytes = serialize_secret_key(env, secret_key);
    env->CallBooleanMethod(list_obj, list_add, parms_bytes);
    env->CallBooleanMethod(list_obj, list_add, pk_bytes);
    env->CallBooleanMethod(list_obj, list_add, sk_bytes);
    return list_obj;
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeUtils_encryption(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jlongArray coeff_array0,
        jlongArray coeff_array1) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Encryptor encryptor(context, public_key);
    encryptor.set_secret_key(secret_key);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    uint32_t size = env->GetArrayLength(coeff_array0);
    auto *ptr0 = reinterpret_cast<uint64_t *>(env->GetLongArrayElements(coeff_array0, JNI_FALSE));
    auto *ptr1 = reinterpret_cast<uint64_t *>(env->GetLongArrayElements(coeff_array1, JNI_FALSE));
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

[[maybe_unused]] JNIEXPORT
jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeUtils_decryption(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray sk_bytes, jbyteArray ciphertext_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Ciphertext ciphertext = deserialize_ciphertext(env, ciphertext_bytes, context);
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
    for (uint32_t i = 0; i < parms.poly_modulus_degree(); i++) {
        temp[i] = (jlong) coeffs[i];
    }
    env->SetLongArrayRegion(result, 0, (jsize) parms.poly_modulus_degree(), temp);
    return result;
}

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pcg_mtg_zp64_core_rss19_Rss19Zp64CoreMtgNativeUtils_computeResponse(
    JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray cipher1_bytes, jbyteArray cipher2_bytes, jlongArray plain1,
    jlongArray plain2, jlongArray r) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    vector<Ciphertext> ct;
    ct.resize(2);
    ct[0] = deserialize_ciphertext(env, cipher1_bytes, context);
    ct[1] = deserialize_ciphertext(env, cipher2_bytes, context);
    uint32_t size = env->GetArrayLength(plain1);
    auto *ptr_plain1 = reinterpret_cast<uint64_t *>(env->GetLongArrayElements(plain1, JNI_FALSE));
    auto *ptr_plain2 = reinterpret_cast<uint64_t *>(env->GetLongArrayElements(plain2, JNI_FALSE));
    auto *ptr_r = reinterpret_cast<uint64_t *>(env->GetLongArrayElements(r, JNI_FALSE));
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