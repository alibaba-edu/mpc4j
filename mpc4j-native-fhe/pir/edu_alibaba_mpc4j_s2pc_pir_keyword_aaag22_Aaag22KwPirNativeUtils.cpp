//
// Created by pengliqiang on 2023/6/20.
//

#include "edu_alibaba_mpc4j_s2pc_pir_keyword_aaag22_Aaag22KwPirNativeUtils.h"
#include "seal/seal.h"
#include "../serialize.h"
#include "../index_pir.h"

using namespace std;
using namespace seal;

#define MOD_SWITCH_COUNT 9

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_aaag22_Aaag22KwPirNativeUtils_genEncryptionParameters(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus, jintArray coeff_modulus_bits) {
    uint32_t coeff_size = env->GetArrayLength(coeff_modulus_bits);
    jint* coeff_ptr = env->GetIntArrayElements(coeff_modulus_bits, JNI_FALSE);
    vector<int32_t> bit_sizes(coeff_ptr, coeff_ptr + coeff_size);
    EncryptionParameters parms = EncryptionParameters(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(plain_modulus);
    parms.set_coeff_modulus(CoeffModulus::Create(poly_modulus_degree, std::move(bit_sizes)));
    return serialize_encryption_parms(env, parms);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_aaag22_Aaag22KwPirNativeUtils_keyGen (
        JNIEnv *env, jclass, jbyteArray parms_bytes, jint pir_num_columns_per_obj, jint num_col) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    KeyGenerator key_gen(context);
    const SecretKey& secret_key = key_gen.secret_key();
    Serializable<PublicKey> public_key = key_gen.create_public_key();
    Serializable<RelinKeys> relin_keys = key_gen.create_relin_keys();
    auto poly_modulus_degree = (int32_t) parms.poly_modulus_degree();
    vector<int32_t> rotation_steps;
    rotation_steps.push_back(0);
    for (int32_t i = poly_modulus_degree / (2 * num_col); i < poly_modulus_degree / 2; i *= 2) {
        rotation_steps.push_back(i);
    }
    for (int32_t i = 1; i < (pir_num_columns_per_obj / 2); i *= 2) {
        rotation_steps.push_back(-i);
    }
    Serializable<GaloisKeys> galois_keys = key_gen.create_galois_keys(rotation_steps);
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    jbyteArray sk_byte = serialize_secret_key(env, secret_key);
    jbyteArray pk_byte = serialize_public_key(env, public_key);
    jbyteArray relin_keys_bytes = serialize_relin_keys(env, relin_keys);
    jbyteArray galois_keys_bytes = serialize_galois_keys(env, galois_keys);
    env->CallBooleanMethod(list_obj, list_add, sk_byte);
    env->CallBooleanMethod(list_obj, list_add, pk_byte);
    env->CallBooleanMethod(list_obj, list_add, relin_keys_bytes);
    env->CallBooleanMethod(list_obj, list_add, galois_keys_bytes);
    return list_obj;
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_aaag22_Aaag22KwPirNativeUtils_nttTransform(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobjectArray plaintext_list) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    vector<Plaintext> plaintexts = deserialize_plaintexts_from_coeff(env, plaintext_list, context);
    auto context_data = context.first_context_data();
    for (uint32_t k = 0; k < MOD_SWITCH_COUNT; k++) {
        context_data = context_data->next_context_data();
    }
    for (auto & plaintext : plaintexts) {
        evaluator.transform_to_ntt_inplace(plaintext, context_data->parms_id());
    }
    return serialize_plaintexts(env, plaintexts);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_aaag22_Aaag22KwPirNativeUtils_preprocessMask(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jint num_col) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    vector<Plaintext> masks;
    uint32_t degree = parms.poly_modulus_degree();
    for (uint32_t i = 0; i < num_col; i++)
    {
        vector<uint64_t> mat(degree, 0ULL);
        Plaintext pt;
        for (uint32_t j = i * (degree / (2 * num_col)); j < (i + 1) * (degree / (2 * num_col)); j++) {
            mat[j] = mat[j + (degree / 2)] = 1;
        }
        encoder.encode(mat, pt);
        evaluator.transform_to_ntt_inplace(pt, context.first_parms_id());
        masks.push_back(pt);
    }
    return serialize_plaintexts(env, masks);
}

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_aaag22_Aaag22KwPirNativeUtils_generateQuery(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jlongArray coeffs) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Encryptor encryptor(context, public_key);
    encryptor.set_secret_key(secret_key);
    BatchEncoder encoder(context);
    Plaintext query_pt = deserialize_plaintext_from_coeff(env, coeffs, context);
    Serializable<Ciphertext> query_ct = encryptor.encrypt_symmetric(query_pt);
    return serialize_ciphertext(env, query_ct);
}

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_aaag22_Aaag22KwPirNativeUtils_processColumn(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray relin_keys_bytes,
        jlongArray database_bytes, jbyteArray expanded_query_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    Encryptor encryptor(context, public_key);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    RelinKeys relin_keys = deserialize_relin_keys(env, relin_keys_bytes, context);
    Ciphertext expanded_query = deserialize_ciphertext(env, expanded_query_bytes, context);
    Plaintext db = deserialize_plaintext_from_coeff(env, database_bytes, context);
    Ciphertext sub, prod, one_ct, result;
    uint32_t degree = parms.poly_modulus_degree();
    vector<uint64_t> one_mat;
    for (uint32_t i = 0; i < degree; i++) {
        one_mat.push_back(1);
    }
    Plaintext one_pt;
    encoder.encode(one_mat, one_pt);
    encryptor.encrypt(one_pt, one_ct);
    for (uint32_t k = 0; k < MOD_SWITCH_COUNT; k++) {
        evaluator.mod_switch_to_next_inplace(one_ct);
    }
    evaluator.sub_plain(expanded_query, db, sub);
    for (uint32_t k = 0; k < 16; k++) {
        evaluator.square_inplace(sub);
        evaluator.relinearize_inplace(sub, relin_keys);
    }
    for(uint32_t k = 0; k < MOD_SWITCH_COUNT; k++) {
        evaluator.mod_switch_to_next_inplace(sub);
    }
    evaluator.sub(one_ct, sub, result);
    return serialize_ciphertext(env, result);
}

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_aaag22_Aaag22KwPirNativeUtils_processRow(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray relin_keys_bytes, jbyteArray galois_keys_bytes,
        jobject column_results_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    RelinKeys relin_keys = deserialize_relin_keys(env, relin_keys_bytes, context);
    GaloisKeys *galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    vector<Ciphertext> column_results = deserialize_ciphertexts(env, column_results_bytes, context);
    for (uint32_t i = 1; i < column_results.size(); i++) {
        evaluator.multiply_inplace(column_results[0], column_results[i]);
        evaluator.relinearize_inplace(column_results[0], relin_keys);
    }
    Ciphertext temp_ct = column_results[0];
    evaluator.rotate_columns_inplace(temp_ct, *galois_keys);
    evaluator.multiply_inplace(column_results[0], temp_ct);
    evaluator.relinearize_inplace(column_results[0], relin_keys);
    evaluator.transform_to_ntt_inplace(column_results[0]);
    return serialize_ciphertext(env, column_results[0]);
}

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_aaag22_Aaag22KwPirNativeUtils_processPir(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray galois_keys_bytes, jobject pir_database_bytes,
        jobject row_results_bytes, jint pir_num_columns_per_obj) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    GaloisKeys *galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    vector<Ciphertext> row_results = deserialize_ciphertexts(env, row_results_bytes, context);
    vector<Plaintext> pir_database = deserialize_plaintexts(env, pir_database_bytes, context);
    Ciphertext result = get_sum(row_results, evaluator, *galois_keys, pir_database, 0, (pir_num_columns_per_obj / 2) - 1) ;
    while (result.parms_id() != context.last_parms_id()) {
        evaluator.mod_switch_to_next_inplace(result);
    }
    return serialize_ciphertext(env, result);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_aaag22_Aaag22KwPirNativeUtils_expandQuery(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray galois_keys_bytes, jobject masks_bytes,
        jbyteArray query_bytes, jint num_col) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    GaloisKeys *galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    vector<Plaintext> masks = deserialize_plaintexts(env, masks_bytes, context);
    Ciphertext query = deserialize_ciphertext(env, query_bytes, context);
    evaluator.transform_to_ntt_inplace(query);
    auto degree = (int32_t) parms.poly_modulus_degree();
    vector<Ciphertext> expanded_query(num_col);
    for (int32_t i = 0; i < num_col; i++) {
        expanded_query[i] = query;
        evaluator.multiply_plain_inplace(expanded_query[i], masks[i]);
        evaluator.transform_from_ntt_inplace(expanded_query[i]);
        Ciphertext temp_ct;
        for (int32_t j = degree / (2 * num_col); j < degree / 2; j *= 2) {
            temp_ct = expanded_query[i];
            evaluator.rotate_rows_inplace(temp_ct, j, *galois_keys);
            evaluator.add_inplace(expanded_query[i], temp_ct);
        }
    }
    return serialize_ciphertexts(env, expanded_query);
}

[[maybe_unused]] JNIEXPORT
jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_keyword_aaag22_Aaag22KwPirNativeUtils_decodeReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray sk_bytes, jbyteArray response_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    BatchEncoder encoder(context);
    Ciphertext response = deserialize_ciphertext(env, response_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Decryptor decryptor(context, secret_key);
    int32_t noise_budget = decryptor.invariant_noise_budget(response);
    jclass exception = env->FindClass("java/lang/Exception");
    if (noise_budget == 0) {
        env->ThrowNew(exception, "noise budget is 0.");
        return nullptr;
    }
    Plaintext pt;
    decryptor.decrypt(response, pt);
    vector<uint64_t> vec;
    encoder.decode(pt, vec);
    uint32_t degree = parms.poly_modulus_degree();
    jlongArray jarr = env->NewLongArray((jsize) degree);
    jlong *arr = env->GetLongArrayElements(jarr, JNI_FALSE);
    for(uint32_t i = 0; i < degree; i++){
        arr[i] = (jlong) vec[i];
    }
    env->ReleaseLongArrayElements(jarr, arr, 0);
    return jarr;
}