//
// Created by pengliqiang on 2023/3/10.
//

#include "edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils.h"
#include "seal/seal.h"
#include "../serialize.h"
#include "../index_pir.h"
#include "../utils.h"

using namespace std;
using namespace seal;

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils_keyGen(
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
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils_preprocessDatabase(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobjectArray coeffs_list) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    BatchEncoder batch_encoder(context);
    Evaluator evaluator(context);
    auto pid = context.first_parms_id();
    vector<Plaintext> encoded_db = deserialize_plaintexts(env, coeffs_list, context);
    for (auto & i : encoded_db){
        evaluator.transform_to_ntt_inplace(i, pid);
    }
    return serialize_plaintexts(env, encoded_db);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils_generateQuery(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jobjectArray query_array) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Encryptor encryptor(context, public_key);
    encryptor.set_secret_key(secret_key);
    vector<Plaintext> plaintexts = deserialize_plaintexts(env, query_array, context);
    vector<Serializable<Ciphertext>> query;
    for (auto & plaintext : plaintexts) {
        query.push_back(encryptor.encrypt_symmetric(plaintext));
    }
    return serialize_ciphertexts(env, query);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils_generateReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobject query_list, jobject db_list, jbyteArray pk_bytes,
        jbyteArray relin_keys_bytes, jbyteArray galois_keys_bytes, jint first_two_dimension_size,
        jint third_dimension_size, jint num_slots_per_entry) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    RelinKeys relin_keys = deserialize_relin_keys(env, relin_keys_bytes, context);
    GaloisKeys* galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    Evaluator evaluator(context);
    Encryptor encryptor(context, public_key);
    vector<Ciphertext> query = deserialize_ciphertexts(env, query_list, context);
    vector<Plaintext> encoded_db = deserialize_plaintexts(env, db_list, context);
    BatchEncoder batch_encoder(context);
    uint32_t degree = parms.poly_modulus_degree();
    auto g = (int32_t) ((degree / 2) / first_two_dimension_size);
    vector<Ciphertext> rotated_query(first_two_dimension_size);
    for (int32_t i = 0; i < first_two_dimension_size; i++) {
        evaluator.rotate_rows(query[0], - (i * g), *galois_keys, rotated_query[i]);
        evaluator.transform_to_ntt_inplace(rotated_query[i]);
    }
    // first dimension
    vector<Ciphertext> first_dimension_ciphers;
    Ciphertext ct_acc, ct, ct1;
    auto &coeff_modulus = context.first_context_data()->parms().coeff_modulus();
    size_t coeff_count = parms.poly_modulus_degree();
    size_t coeff_mod_count = coeff_modulus.size();
    size_t encrypted_ntt_size = rotated_query[0].size();
    for (int32_t col_id = 0; col_id < encoded_db.size(); col_id += first_two_dimension_size) {
        std::vector<std::vector<__uint128_t>> buffer(encrypted_ntt_size, std::vector<__uint128_t>(coeff_count * coeff_mod_count, 1));
        for (int32_t i = 0; i < first_two_dimension_size; i++) {
            for (size_t poly_id = 0; poly_id < encrypted_ntt_size; poly_id++) {
                multiply_poly_acum(rotated_query[i].data(poly_id), encoded_db[col_id + i].data(), coeff_count * coeff_mod_count, buffer[poly_id].data());
            }
        }
        ct_acc = rotated_query[0];
        for (int32_t poly_id = 0; poly_id < encrypted_ntt_size; poly_id++) {
            auto ct_ptr = ct_acc.data(poly_id);
            auto pt_ptr = buffer[poly_id];
            for (int32_t mod_id = 0; mod_id < coeff_mod_count; mod_id++) {
                auto mod_idx = (mod_id * coeff_count);
                for (int coeff_id = 0; coeff_id < coeff_count; coeff_id++) {
                    pt_ptr[coeff_id + mod_idx] = pt_ptr[coeff_id + mod_idx] % static_cast<__uint128_t>(coeff_modulus[mod_id].value());
                    ct_ptr[coeff_id + mod_idx] = static_cast<uint64_t>(pt_ptr[coeff_id + mod_idx]);
                }
            }
        }
        evaluator.transform_from_ntt_inplace(ct_acc);
        first_dimension_ciphers.push_back(ct_acc);
    }
    // second dimension
    vector<Ciphertext> second_dimension_cipher;
    for (int32_t idx = 0; idx < first_dimension_ciphers.size(); idx += third_dimension_size) {
        if (first_dimension_ciphers[idx].is_transparent()) {
            encryptor.encrypt_zero(ct_acc);
            evaluator.mod_switch_to_next_inplace(ct_acc);
        } else {
            evaluator.multiply(query[1], first_dimension_ciphers[idx], ct_acc);
            evaluator.relinearize_inplace(ct_acc, relin_keys);
        }

        for (int32_t i = 1; i < third_dimension_size; i++) {
            if (!first_dimension_ciphers[idx + i].is_transparent()) {
                evaluator.multiply(query[1], first_dimension_ciphers[idx + i], ct1);
                evaluator.relinearize_inplace(ct1, relin_keys);
                evaluator.rotate_rows_inplace(ct1, -i * g, *galois_keys);
                evaluator.add_inplace(ct_acc, ct1);
            }
        }
        second_dimension_cipher.push_back(ct_acc);
    }
    // third dimension
    vector<Ciphertext> result;
    evaluator.mod_switch_to_inplace(query[2], second_dimension_cipher[0].parms_id());
    for (auto & idx : second_dimension_cipher) {
        evaluator.multiply(query.back(), idx, ct);
        evaluator.relinearize_inplace(ct, relin_keys);
        result.push_back(ct);
    }
    vector<Ciphertext> merged_result = merge_response(context, *galois_keys, result, num_slots_per_entry, first_two_dimension_size);
    for (auto & i : merged_result) {
        while (i.parms_id() != context.last_parms_id()) {
            evaluator.mod_switch_to_next_inplace(i);
        }
        try_clear_irrelevant_bits(parms, i);
    }
    return serialize_ciphertexts(env, merged_result);
}

[[maybe_unused]] JNIEXPORT
jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_batch_vectorizedpir_Mr23BatchIndexPirNativeUtils_decryptReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray sk_bytes, jbyteArray response_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Ciphertext response = deserialize_ciphertext(env, response_bytes, context);
    Decryptor decryptor(context, secret_key);
    BatchEncoder batch_encoder(context);
    Plaintext pt;
    int32_t noise_budget = decryptor.invariant_noise_budget(response);
    jclass exception = env->FindClass("java/lang/Exception");
    if (noise_budget == 0) {
        env->ThrowNew(exception, "noise budget is 0.");
        return nullptr;
    }
    decryptor.decrypt(response, pt);
    vector<uint64_t> vec;
    batch_encoder.decode(pt, vec);
    uint32_t degree = context.first_context_data()->parms().poly_modulus_degree();
    jlongArray jarr = env->NewLongArray((jsize) degree);
    jlong *arr = env->GetLongArrayElements(jarr, JNI_FALSE);
    for(uint32_t i = 0; i < degree; i++) {
        arr[i] = (jlong) vec[i];
    }
    env->ReleaseLongArrayElements(jarr, arr, 0);
    return jarr;
}