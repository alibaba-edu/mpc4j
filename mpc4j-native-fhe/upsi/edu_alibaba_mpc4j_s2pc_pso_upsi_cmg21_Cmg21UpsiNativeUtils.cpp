//
// Created by Liqiang Peng on 2022/11/5.
//

#include "edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeUtils.h"
#include "../apsi.h"
#include "../serialize.h"
#include "../utils.h"
#include "../polynomials.h"

using namespace std;
using namespace seal;

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeUtils_genEncryptionParameters(
    JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus, jintArray coeff_modulus_bits) {
    uint32_t coeff_size = env->GetArrayLength(coeff_modulus_bits);
    jint* coeff_ptr = env->GetIntArrayElements(coeff_modulus_bits, JNI_FALSE);
    vector<int> bit_sizes(coeff_ptr, coeff_ptr + coeff_size);
    EncryptionParameters parms = generate_encryption_parameters(scheme_type::bfv, poly_modulus_degree, plain_modulus, bit_sizes);
    SEALContext context = SEALContext(parms);
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    Serializable relin_keys = key_gen.create_relin_keys();
    Serializable public_key = key_gen.create_public_key();
    return serialize_relin_public_secret_keys(env, parms, relin_keys, public_key, secret_key);
}

JNIEXPORT jboolean JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeUtils_checkSealParams(
    JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus, jintArray coeff_modulus_bits,
    jobjectArray jparent_powers, jintArray jsource_power_index, jint ps_low_power, jint max_bin_size) {
    uint32_t coeff_size = env->GetArrayLength(coeff_modulus_bits);
    jint* ptr = env->GetIntArrayElements(coeff_modulus_bits, JNI_FALSE);
    vector<int> bit_sizes(ptr, ptr + coeff_size);
    EncryptionParameters parms = generate_encryption_parameters(scheme_type::bfv, poly_modulus_degree, plain_modulus, bit_sizes);
    SEALContext context = SEALContext(parms);
    jclass exception = env->FindClass("java/lang/Exception");
    if (!context.parameters_set()) {
        return env->ThrowNew(exception, "SEAL parameters not valid.");
    }
    if (!context.first_context_data()->qualifiers().using_batching) {
        return env->ThrowNew(exception, "SEAL parameters do not support batching.");
    }
    if (!context.using_keyswitching()) {
        return env->ThrowNew(exception, "SEAL parameters do not support key switching.");
    }
    KeyGenerator key_gen = KeyGenerator(context);
    const SecretKey &secret_key = key_gen.secret_key();
    RelinKeys relin_keys;
    key_gen.create_relin_keys(relin_keys);
    PublicKey public_key;
    key_gen.create_public_key(public_key);
    Encryptor encryptor(context, public_key);
    Evaluator evaluator(context);
    BatchEncoder encoder(context);
    encryptor.set_secret_key(secret_key);
    Decryptor decryptor(context, secret_key);
    auto random_factory = UniformRandomGeneratorFactory::DefaultFactory();
    auto random = random_factory->create();
    size_t slot_count = encoder.slot_count();
    vector<Plaintext> database(max_bin_size+1);
    for (int i = 0 ; i <= max_bin_size; i++) {
        vector<uint64_t> database_coeffs(slot_count);
        for (size_t j = 0; j < slot_count; j++) {
            database_coeffs[j] = random->generate() % plain_modulus;
        }
        encoder.encode(database_coeffs, database[i]);
    }
    vector<uint64_t> plaintext_coeffs(slot_count);
    jint* index_ptr = env->GetIntArrayElements(jsource_power_index, JNI_FALSE);
    vector<uint32_t> source_power_index;
    source_power_index.reserve(env->GetArrayLength(jsource_power_index));
    for (int i = 0; i < env->GetArrayLength(jsource_power_index); i++) {
        source_power_index.push_back(index_ptr[i]);
    }
    for (size_t i = 0; i < slot_count; i++) {
        plaintext_coeffs[i] = random->generate() % plain_modulus;
    }
    bool is_small_modulus;
    if (plain_modulus < (1L << 32)) {
        is_small_modulus = true;
    } else {
        is_small_modulus = false;
    }
    vector<Ciphertext> query_powers(source_power_index.size());
    for (int i = 0; i < source_power_index.size(); i++) {
        Plaintext plaintext_power;
        vector<uint64_t> power_coeffs(slot_count);
        for (int j = 0; j < slot_count; j++) {
            power_coeffs[i] = mod_exp(plaintext_coeffs[j], source_power_index[i], plain_modulus, is_small_modulus);
        }
        encoder.encode(power_coeffs, plaintext_power);
        encryptor.encrypt_symmetric(plaintext_power, query_powers[i]);
    }
    uint32_t target_power_size = env->GetArrayLength(jparent_powers);
    vector<vector<uint32_t>> parent_powers(target_power_size);
    for (int i = 0; i < target_power_size; i++) {
        parent_powers[i].reserve(2);
        auto rows = (jintArray) env->GetObjectArrayElement(jparent_powers, i);
        jint* cols = env->GetIntArrayElements(rows, JNI_FALSE);
        parent_powers[i].push_back(cols[0]);
        parent_powers[i].push_back(cols[1]);
    }
    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
    vector<Ciphertext> encrypted_powers = compute_encrypted_powers(parms, query_powers, parent_powers, source_power_index, ps_low_power, relin_keys);
    encrypted_powers.resize(target_power_size);
    int noise_budget;
    if (ps_low_power > 0) {
        // Paterson-Stockmeyer algorithm
        for (int i = 0; i < database.size(); i++) {
            if ((i % (ps_low_power + 1)) != 0) {
                evaluator.transform_to_ntt_inplace(database[i], low_powers_parms_id);
            }
        }
        Ciphertext f_evaluated = polynomial_evaluation(parms, encrypted_powers, database, ps_low_power, relin_keys, public_key);
        noise_budget = decryptor.invariant_noise_budget(f_evaluated);
    } else {
        for (int i = 1; i < database.size(); i++) {
            evaluator.transform_to_ntt_inplace(database[i], high_powers_parms_id);
        }
        Ciphertext f_evaluated = polynomial_evaluation(parms, encrypted_powers, database, public_key);
        noise_budget = decryptor.invariant_noise_budget(f_evaluated);
    }
    return noise_budget > 0;
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeUtils_computeEncryptedPowers(
    JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray relin_keys_bytes, jobject query_list,
    jobjectArray jparent_powers, jintArray jsource_power_index, jint ps_low_power) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context = SEALContext(parms);
    RelinKeys relin_keys = deserialize_relin_key(env, relin_keys_bytes, context);
    jclass exception = env->FindClass("java/lang/Exception");
    if (!is_metadata_valid_for(relin_keys, context)) {
        env->ThrowNew(exception, "invalid relinearization key for this SEALContext");
    }
    Evaluator evaluator(context);
    vector<Ciphertext> query = deserialize_ciphertexts(env, query_list, context);
    // compute all the powers of the receiver's input.
    jint* index_ptr = env->GetIntArrayElements(jsource_power_index, JNI_FALSE);
    vector<uint32_t> source_power_index;
    source_power_index.reserve(env->GetArrayLength(jsource_power_index));
    for (int i = 0; i < env->GetArrayLength(jsource_power_index); i++) {
        source_power_index.push_back(index_ptr[i]);
    }
    uint32_t target_power_size = env->GetArrayLength(jparent_powers);
    vector<vector<uint32_t>> parent_powers(target_power_size);
    for (int i = 0; i < target_power_size; i++) {
        parent_powers[i].reserve(2);
        auto rows = (jintArray) env->GetObjectArrayElement(jparent_powers, i);
        jint* cols = env->GetIntArrayElements(rows, JNI_FALSE);
        parent_powers[i].push_back(cols[0]);
        parent_powers[i].push_back(cols[1]);
    }
    vector<Ciphertext> encrypted_powers = compute_encrypted_powers(parms, query, parent_powers, source_power_index, ps_low_power, relin_keys);
    return serialize_ciphertexts(env, encrypted_powers);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeUtils_optComputeMatches(
    JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray relin_keys_bytes, jobjectArray database_coeffs,
    jobject query_list, jint ps_low_power) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context(parms);
    jclass exception = env->FindClass("java/lang/Exception");
    RelinKeys relin_keys = deserialize_relin_key(env, relin_keys_bytes, context);
    if (!is_metadata_valid_for(relin_keys, context)) {
        env->ThrowNew(exception, "invalid relinearization key for this SEALContext");
    }
    Evaluator evaluator(context);
    // encrypted query powers
    vector<Ciphertext> query_powers = deserialize_ciphertexts(env, query_list, context);
    auto high_powers_parms_id = get_parms_id_for_chain_idx(context, 1);
    auto low_powers_parms_id = get_parms_id_for_chain_idx(context, 2);
    vector<Plaintext> plaintexts = deserialize_plaintexts(env, database_coeffs, context);
    int ps_high_degree = ps_low_power + 1;
    for (int i = 0; i < plaintexts.size(); i++) {
        if ((i % ps_high_degree) != 0) {
            evaluator.transform_to_ntt_inplace(plaintexts[i], low_powers_parms_id);
        }
    }
    Ciphertext f_evaluated = polynomial_evaluation(parms, query_powers, plaintexts, ps_low_power, relin_keys);
    return serialize_ciphertext(env, f_evaluated);
}

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeUtils_naiveComputeMatches(
    JNIEnv *env, jclass, jbyteArray params_bytes, jobjectArray database_coeffs, jobject query_list) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    // encrypted query powers
    vector<Ciphertext> query_powers = deserialize_ciphertexts(env, query_list, context);
    auto parms_id = get_parms_id_for_chain_idx(context, 1);
    vector<Plaintext> plaintexts = deserialize_plaintexts(env, database_coeffs, context);
    for (int i = 1; i < plaintexts.size(); i++) {
        evaluator.transform_to_ntt_inplace(plaintexts[i], parms_id);
    }
    Ciphertext f_evaluated = polynomial_evaluation(parms, query_powers, plaintexts);
    return serialize_ciphertext(env, f_evaluated);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeUtils_generateQuery(
    JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jobjectArray coeffs_array) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context = SEALContext(parms);
    jclass exception = env->FindClass("java/lang/Exception");
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    if (!is_metadata_valid_for(public_key, context) || !is_metadata_valid_for(secret_key, context)) {
        env->ThrowNew(exception, "invalid public key or secret key for this SEALContext");
    }
    vector<Plaintext> plain_query = deserialize_plaintexts_from_coeff(env, coeffs_array, context);
    for (auto & plaintext : plain_query) {
        if (!is_metadata_valid_for(plaintext, context)) {
            env->ThrowNew(exception, "invalid plaintext for this SEALContext");
        }
    }
    BatchEncoder encoder(context);
    Encryptor encryptor(context, public_key);
    encryptor.set_secret_key(secret_key);
    vector<Serializable<Ciphertext>> query;
    query.reserve(plain_query.size());
    for (auto & i : plain_query) {
        Serializable ciphertext = encryptor.encrypt_symmetric(i);
        query.push_back(ciphertext);
    }
    return serialize_ciphertexts(env, query);
}

JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pso_upsi_cmg21_Cmg21UpsiNativeUtils_decodeReply(
    JNIEnv *env, jclass, jbyteArray params_bytes, jbyteArray sk_bytes, jbyteArray response_byte) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_bytes);
    SEALContext context = SEALContext(parms);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    jclass exception = env->FindClass("java/lang/Exception");
    if (!is_metadata_valid_for(secret_key, context)) {
        env->ThrowNew(exception, "invalid secret key for this SEALContext");
    }
    BatchEncoder encoder(context);
    Decryptor decryptor(context, secret_key);
    uint32_t slot_count = encoder.slot_count();
    vector<uint64_t> result;
    Ciphertext response = deserialize_ciphertext(env, response_byte, context);
    if (!is_metadata_valid_for(response, context)) {
        env->ThrowNew(exception, "invalid ciphertext for this SEALContext");
    }
    Plaintext decrypted;
    vector<uint64_t> dec_vec(slot_count);
    decryptor.decrypt(response, decrypted);
    encoder.decode(decrypted, dec_vec);
    jlongArray coeffs;
    coeffs = env->NewLongArray((jsize) dec_vec.size());
    jlong fill[dec_vec.size()];
    for (int i = 0; i < dec_vec.size(); i++) {
        fill[i] = (jlong) dec_vec[i];
    }
    env->SetLongArrayRegion(coeffs, 0, (jsize) dec_vec.size(), fill);
    return coeffs;
}