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
    parms.set_plain_modulus(PlainModulus::Batching(poly_modulus_degree, plain_modulus_size));
    parms.set_coeff_modulus(CoeffModulus::Create(poly_modulus_degree, {42, 58, 58, 60}));
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
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_vectorizedpir_Mr23SingleIndexPirNativeUtils_generateQuery(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jintArray indices_array) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Encryptor encryptor(context, public_key, secret_key);
    BatchEncoder batch_encoder(context);
    jint dimension = env->GetArrayLength(indices_array);
    auto *ptr = reinterpret_cast<uint32_t *>(env->GetIntArrayElements(indices_array, JNI_FALSE));
    vector<uint32_t> indices(ptr, ptr + dimension);
    uint32_t degree = context.first_context_data()->parms().poly_modulus_degree();
    vector<Serializable<Ciphertext>> query;
    for (uint32_t i = 0; i < dimension; i++) {
        vector<uint64_t> vec(degree, 0ULL);
        vec[indices[i]] = 1;
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
    vector<Ciphertext> rotated_query(first_two_dimension_size);
    for (int32_t i = 0; i < first_two_dimension_size; i++) {
        evaluator.rotate_rows(query[0], - (i * g), *galois_keys, rotated_query[i]);
        evaluator.transform_to_ntt_inplace(rotated_query[i]);
    }
    // first dimension
    vector<Ciphertext> first_dimension_ciphers;
    Ciphertext ct, ct_acc;
    for (int32_t i = 0; i < encoded_db.size(); i = i + first_two_dimension_size) {
        evaluator.multiply_plain(rotated_query[0], encoded_db[i], ct_acc);
        for (int32_t j = 1; j < first_two_dimension_size; j++) {
            evaluator.multiply_plain(rotated_query[j], encoded_db[i + j], ct);
            evaluator.add_inplace(ct_acc, ct);
        }
        evaluator.transform_from_ntt_inplace(ct_acc);
        first_dimension_ciphers.push_back(ct_acc);
    }
    // second dimension
    Ciphertext second_dimension_cipher;
    evaluator.multiply(query[1], first_dimension_ciphers[0], second_dimension_cipher);
    evaluator.mod_switch_to_next_inplace(second_dimension_cipher);
    evaluator.relinearize_inplace(second_dimension_cipher, relin_keys);
    for (int32_t i = 1; i < first_dimension_ciphers.size(); i++) {
        Ciphertext t;
        evaluator.multiply(query[1], first_dimension_ciphers[i], t);
        evaluator.mod_switch_to_next_inplace(t);
        evaluator.relinearize_inplace(t, relin_keys);
        evaluator.rotate_rows_inplace(t, -i * g, *galois_keys);
        evaluator.add_inplace(second_dimension_cipher, t);
    }
    // third dimension
    evaluator.mod_switch_to_inplace(query[2], second_dimension_cipher.parms_id());
    evaluator.multiply_inplace(second_dimension_cipher, query[2]);
    evaluator.relinearize_inplace(second_dimension_cipher, relin_keys);
    while (second_dimension_cipher.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(second_dimension_cipher);
    }
    return serialize_ciphertext(env, second_dimension_cipher);
}

[[maybe_unused]] JNIEXPORT
jlong JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_vectorizedpir_Mr23SingleIndexPirNativeUtils_decryptReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray sk_bytes, jbyteArray response_bytes, jint offset,
        jint gap) {
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
    return (jlong) vec[offset * gap];
}