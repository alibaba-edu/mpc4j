//
// Created by pengliqiang on 2023/3/6.
//

#include "edu_alibaba_mpc4j_s2pc_pir_index_single_vectorizedpir_Mr23SingleIndexPirNativeUtils.h"
#include "seal/seal.h"
#include "../serialize.h"

using namespace std;
using namespace seal;

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_vectorizedpir_Mr23SingleIndexPirNativeUtils_generateEncryptionParams(
        JNIEnv *env, jclass, jint poly_modulus_degree, jint plain_modulus_size) {
    EncryptionParameters parms = EncryptionParameters(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(PlainModulus::Batching(poly_modulus_degree, plain_modulus_size + 1));
    parms.set_coeff_modulus(CoeffModulus::BFVDefault(poly_modulus_degree, sec_level_type::tc128));
    SEALContext context = SEALContext(parms);
    jclass exception = env->FindClass("java/lang/Exception");
    if (!context.parameters_set()) {
        env->ThrowNew(exception, "SEAL parameters not valid.");
        return nullptr;
    }
    if (!context.first_context_data()->qualifiers().using_batching) {
        env->ThrowNew(exception, "SEAL parameters do not support batching.");
        return nullptr;
    }
    if (!context.using_keyswitching()) {
        env->ThrowNew(exception, "SEAL parameters do not support key switching.");
        return nullptr;
    }
    return serialize_encryption_parms(env, parms);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_vectorizedpir_Mr23SingleIndexPirNativeUtils_keyGen(
        JNIEnv *env, jclass, jbyteArray parms_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context = SEALContext(parms);
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    Serializable<PublicKey> public_key = key_gen.create_public_key();
    Serializable<RelinKeys> relin_keys = key_gen.create_relin_keys();
    Serializable<GaloisKeys> galois_keys = key_gen.create_galois_keys();
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    jbyteArray galois_keys_bytes = serialize_galois_keys(env, galois_keys);
    jbyteArray relin_keys_bytes = serialize_relin_keys(env, relin_keys);
    jbyteArray pk_bytes = serialize_public_key(env, public_key);
    jbyteArray sk_bytes = serialize_secret_key(env, secret_key);
    env->CallBooleanMethod(list_obj, list_add, pk_bytes);
    env->CallBooleanMethod(list_obj, list_add, sk_bytes);
    env->CallBooleanMethod(list_obj, list_add, relin_keys_bytes);
    env->CallBooleanMethod(list_obj, list_add, galois_keys_bytes);
    return list_obj;
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_vectorizedpir_Mr23SingleIndexPirNativeUtils_preprocessDatabase(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobjectArray coeffs_list, jint first_dimension_size) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    BatchEncoder batch_encoder(context);
    Evaluator evaluator(context);
    auto pid = context.first_parms_id();
    vector<Plaintext> encoded_db = deserialize_plaintexts(env, coeffs_list, context);
    for (auto & i : encoded_db){
        evaluator.transform_to_ntt_inplace(i, pid);
    }
    uint32_t current_size = encoded_db.size();
    uint32_t cols = (current_size + first_dimension_size - 1) / first_dimension_size;
    Plaintext zero(parms.poly_modulus_degree());
    vector<uint64_t> vec(parms.poly_modulus_degree(), 0ULL);
    batch_encoder.encode(vec, zero);
    evaluator.transform_to_ntt_inplace(zero, pid);
    for (uint32_t i = current_size; i < cols * first_dimension_size; i++) {
        encoded_db.push_back(zero);
    }
    return serialize_plaintexts(env, encoded_db);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_vectorizedpir_Mr23SingleIndexPirNativeUtils_generateQuery(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jintArray indices_array,
        jint n_slot) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Encryptor encryptor(context, public_key, secret_key);
    BatchEncoder batch_encoder(context);
    uint32_t slot_count = batch_encoder.slot_count();
    jint dimension = env->GetArrayLength(indices_array);
    auto *ptr = reinterpret_cast<uint32_t *>(env->GetIntArrayElements(indices_array, JNI_FALSE));
    vector<uint32_t> indices(ptr, ptr + dimension);
    uint32_t degree = context.first_context_data()->parms().poly_modulus_degree();
    vector<Serializable<Ciphertext>> query;
    uint32_t g = (degree / 2) / n_slot;
    for (uint32_t i = 0; i < dimension; i++) {
        vector<uint64_t> vec(slot_count, 0ULL);
        vec[indices[i] * g] = 1;
        Plaintext pt(degree);
        batch_encoder.encode(vec, pt);
        query.push_back(encryptor.encrypt_symmetric(pt));
    }
    return serialize_ciphertexts(env, query);
}

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_vectorizedpir_Mr23SingleIndexPirNativeUtils_generateReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobject query_list, jobjectArray db_list, jbyteArray pk_bytes,
        jbyteArray relin_keys_bytes, jbyteArray galois_keys_bytes, jint first_two_dimension_size) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    RelinKeys relin_keys = deserialize_relin_keys(env, relin_keys_bytes, context);
    GaloisKeys* galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    Evaluator evaluator(context);
    Encryptor encryptor(context, public_key);
    vector<Ciphertext> query = deserialize_ciphertexts(env, query_list, context);
    vector<Plaintext> encoded_db = deserialize_plaintexts_array(env, db_list, context);
    BatchEncoder batch_encoder(context);
    uint32_t degree = context.first_context_data()->parms().poly_modulus_degree();
    auto g = (int32_t) ((degree / 2) / first_two_dimension_size);
    vector<Ciphertext> rotated_ciphertexts(first_two_dimension_size);
    for (int32_t i = 0; i < first_two_dimension_size; i++) {
        evaluator.rotate_rows(query[0], - (i * g), *galois_keys, rotated_ciphertexts[i]);
        evaluator.transform_to_ntt_inplace(rotated_ciphertexts[i]);
    }
    // first dimension
    uint32_t cols = encoded_db.size() / first_two_dimension_size;
    Ciphertext zero;
    encryptor.encrypt_zero(zero);
    evaluator.transform_to_ntt_inplace(zero);
    vector<Ciphertext> first_dimension_ciphers(cols);
    for (int i = 0; i < cols; i++) {
        if (encoded_db[i * first_two_dimension_size].is_zero()) {
            first_dimension_ciphers[i] = zero;
        } else {
            evaluator.multiply_plain(rotated_ciphertexts[0], encoded_db[i * first_two_dimension_size], first_dimension_ciphers[i]);
        }
        for (int j = 0; j < first_two_dimension_size; j++) {
            if (encoded_db[i * first_two_dimension_size + j].is_zero()) {
                continue;
            } else {
                Ciphertext temp;
                evaluator.multiply_plain(rotated_ciphertexts[j], encoded_db[i * first_two_dimension_size + j], temp);
                evaluator.add_inplace(first_dimension_ciphers[i], temp);
            }
        }
        evaluator.transform_from_ntt_inplace(first_dimension_ciphers[i]);
    }
    // second dimension
    Ciphertext second_dimension_cipher;
    evaluator.multiply(query[1], first_dimension_ciphers[0], second_dimension_cipher);
    evaluator.mod_switch_to_next_inplace(second_dimension_cipher);
    evaluator.relinearize_inplace(second_dimension_cipher, relin_keys);
    for (int k = 1; k < first_dimension_ciphers.size(); k++) {
        Ciphertext t;
        evaluator.multiply(query[1], first_dimension_ciphers[k], t);
        evaluator.mod_switch_to_next_inplace(t);
        evaluator.relinearize_inplace(t, relin_keys);
        evaluator.rotate_rows_inplace(t, -k * g, *galois_keys);
        evaluator.add_inplace(second_dimension_cipher, t);
    }
    // third dimension
    evaluator.mod_switch_to_inplace(query[2], second_dimension_cipher.parms_id());
    evaluator.multiply_inplace(second_dimension_cipher, query[2]);
    evaluator.mod_switch_to_next_inplace(second_dimension_cipher);
    evaluator.relinearize_inplace(second_dimension_cipher, relin_keys);
    if (second_dimension_cipher.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(second_dimension_cipher);
    }
    return serialize_ciphertext(env, second_dimension_cipher);
}

[[maybe_unused]] JNIEXPORT
jlong JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_vectorizedpir_Mr23SingleIndexPirNativeUtils_decryptReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray sk_bytes, jbyteArray response_bytes, jint offset,
        jint n_slot) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Ciphertext response = deserialize_ciphertext(env, response_bytes, context);
    Decryptor decryptor(context, secret_key);
    BatchEncoder batch_encoder(context);
    Plaintext pt;
    jclass exception = env->FindClass("java/lang/Exception");
    if (decryptor.invariant_noise_budget(response) == 0) {
        env->ThrowNew(exception, "noise budget is zero.");
    }
    decryptor.decrypt(response, pt);
    vector<uint64_t> vec;
    batch_encoder.decode(pt, vec);
    uint32_t degree = parms.poly_modulus_degree();
    uint32_t g = (degree / 2) / n_slot;
    return (jlong) vec[offset * g];
}