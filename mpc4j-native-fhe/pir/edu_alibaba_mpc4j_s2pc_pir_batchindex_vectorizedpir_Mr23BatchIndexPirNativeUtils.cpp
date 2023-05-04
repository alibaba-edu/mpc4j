//
// Created by pengliqiang on 2023/3/10.
//

#include "edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils.h"
#include "seal/seal.h"
#include "../serialize.h"
#include "../utils.h"

using namespace std;
using namespace seal;

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_generateSealContext(
        JNIEnv *env, jclass, jint poly_modulus_degree, jint plain_modulus_size) {
    EncryptionParameters parms = EncryptionParameters(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(PlainModulus::Batching(poly_modulus_degree, plain_modulus_size));
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

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_keyGen(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jint dimension_size) {
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

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_preprocessDatabase(
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

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_generateQuery(
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

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_generateReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobject query_list, jobject db_list, jbyteArray pk_bytes,
        jbyteArray relin_keys_bytes, jbyteArray galois_keys_bytes, jint first_two_dimension_size) {
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
    uint32_t degree = context.first_context_data()->parms().poly_modulus_degree();
    auto g = (int32_t) ((degree / 2) / first_two_dimension_size);
    vector<Ciphertext> rotated_ciphertexts(first_two_dimension_size);
    for (int32_t i = 0; i < first_two_dimension_size; i++) {
        evaluator.rotate_rows(query[0], - (i * g), *galois_keys, rotated_ciphertexts[i]);
        evaluator.transform_to_ntt_inplace(rotated_ciphertexts[i]);
    }
    // first dimension
    uint32_t cols = encoded_db.size() / first_two_dimension_size;
    vector<Ciphertext> db_prime(cols);
    Ciphertext zero;
    encryptor.encrypt_zero(zero);
    evaluator.transform_to_ntt_inplace(zero);
    for (int i = 0; i < cols; i++) {
        if (encoded_db[i * first_two_dimension_size].is_zero()) {
            db_prime[i] = zero;
        } else {
            evaluator.multiply_plain(rotated_ciphertexts[0], encoded_db[i * first_two_dimension_size], db_prime[i]);
        }
        for (int j = 1; j < first_two_dimension_size; j++) {
            if (encoded_db[i * first_two_dimension_size + j].is_zero()) {
                continue;
            } else {
                Ciphertext temp;
                evaluator.multiply_plain(rotated_ciphertexts[j], encoded_db[i * first_two_dimension_size + j], temp);
                evaluator.add_inplace(db_prime[i], temp);
            }
        }
        evaluator.transform_from_ntt_inplace(db_prime[i]);
    }
    // second dimension
    Ciphertext second_dimension_cipher;
    evaluator.multiply(query[1], db_prime[0], second_dimension_cipher);
    evaluator.mod_switch_to_next_inplace(second_dimension_cipher);
    evaluator.relinearize_inplace(second_dimension_cipher, relin_keys);
    for (int k = 1; k < db_prime.size(); k++) {
        Ciphertext t;
        evaluator.multiply(query[1], db_prime[k], t);
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
    return serialize_ciphertext(env, second_dimension_cipher);
}

JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_decryptReply(
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
    for(uint32_t i = 0; i < degree; i++){
        arr[i] = (jlong) vec[i];
    }
    env->ReleaseLongArrayElements(jarr, arr, 0);
    return jarr;
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_batchindex_vectorizedpir_Mr23BatchIndexPirNativeUtils_mergeResponse(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray galois_keys_bytes,jobject response_list, jint g) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    GaloisKeys* galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    Evaluator evaluator(context);
    BatchEncoder batch_encoder(context);
    Ciphertext merged_response, r_cipher;
    vector<Ciphertext> response = deserialize_ciphertexts(env, response_list, context);
    uint32_t row_size = parms.poly_modulus_degree() / 2;
    auto count = (uint32_t) log2(row_size / g);
    vector<Plaintext> rotate_plain(response.size());
    auto parms_id = response[0].parms_id();
    for (uint32_t i = 0; i < response.size(); i++) {
        rotate_plain[i].resize(parms.poly_modulus_degree());
        rotate_plain[i].set_zero();
        vector<uint64_t> vec(parms.poly_modulus_degree(), 0ULL);
        uint32_t l = i * g;
        for (uint32_t k = l; k < l + g; k++) {
            vec[k] = 1;
            vec[k + row_size] = 1;
        }
        batch_encoder.encode(vec, rotate_plain[i]);
        evaluator.transform_to_ntt_inplace(rotate_plain[i], parms_id);
    }
    for (uint32_t j = 0; j < count; j++) {
        evaluator.rotate_rows(response[0], - g * (1 << j), *galois_keys, r_cipher);
        evaluator.add_inplace(response[0], r_cipher);
    }
    evaluator.transform_to_ntt_inplace(response[0]);
    evaluator.multiply_plain(response[0], rotate_plain[0], merged_response);
    for (uint32_t i = 1; i < response.size(); i++) {
        for (uint32_t j = 0; j < count; j++) {
            evaluator.rotate_rows(response[i], - g * (1 << j), *galois_keys, r_cipher);
            evaluator.add_inplace(response[i], r_cipher);
        }
        evaluator.transform_to_ntt_inplace(response[i]);
        evaluator.multiply_plain_inplace(response[i], rotate_plain[i]);
        evaluator.add_inplace(merged_response, response[i]);
    }
    evaluator.transform_from_ntt_inplace(merged_response);
    while (merged_response.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(merged_response);
    }
    try_clear_irrelevant_bits(context.last_context_data()->parms(), merged_response);
    return serialize_ciphertext(env, merged_response);
}