//
// Created by qixian zhou on 2023/5/19.
//
#include "edu_alibaba_mpc4j_s2pc_pir_index_single_constantweightpir_Mk22SingleIndexPirNativeUtils.h"
#include "seal/seal.h"
#include "../utils.h"
#include "../serialize.h"
#include <iomanip>
#include "../index_pir.h"
#include <chrono>

using namespace seal;
using namespace std;


[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_constantweightpir_Mk22SingleIndexPirNativeUtils_generateEncryptionParams(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus) {
    EncryptionParameters parms = EncryptionParameters(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(plain_modulus);
    parms.set_coeff_modulus(CoeffModulus::BFVDefault(poly_modulus_degree,sec_level_type::tc128));
    return serialize_encryption_parms(env, parms);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_constantweightpir_Mk22SingleIndexPirNativeUtils_keyGen(
        JNIEnv *env, jclass, jbyteArray parms_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    KeyGenerator key_gen(context);
    const SecretKey& secret_key = key_gen.secret_key();
    Serializable<PublicKey> public_key = key_gen.create_public_key();
    Serializable<GaloisKeys> galois_keys = generate_galois_keys(context, key_gen);
    Serializable<RelinKeys> relin_keys =  key_gen.create_relin_keys();
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    jbyteArray pk_byte = serialize_public_key(env, public_key);
    jbyteArray sk_byte = serialize_secret_key(env, secret_key);
    jbyteArray galois_keys_bytes = serialize_galois_keys(env, galois_keys);
    jbyteArray relin_keys_bytes = serialize_relin_keys(env, relin_keys);
    env->CallBooleanMethod(list_obj, list_add, pk_byte);
    env->CallBooleanMethod(list_obj, list_add, sk_byte);
    env->CallBooleanMethod(list_obj, list_add, galois_keys_bytes);
    env->CallBooleanMethod(list_obj, list_add, relin_keys_bytes);
    return list_obj;
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_constantweightpir_Mk22SingleIndexPirNativeUtils_nttTransform(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobject plaintext_list) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    vector<Plaintext> plaintexts = deserialize_plaintexts_from_coeff_without_batch_encode(env, plaintext_list, context);
    for (auto & plaintext : plaintexts) {
        evaluator.transform_to_ntt_inplace(plaintext, context.first_parms_id());
    }
    return serialize_plaintexts(env, plaintexts);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_constantweightpir_Mk22SingleIndexPirNativeUtils_generateQuery(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jintArray encoded_index_array, 
        jint used_slots_per_plain, jint num_input_ciphers) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Encryptor encryptor(context, public_key, secret_key);
    uint32_t codewordBitLength = env->GetArrayLength(encoded_index_array);
    jint *ptr0 = env->GetIntArrayElements(encoded_index_array, JNI_FALSE);
    // client PT query index_single in each dimension 
    vector<uint32_t> encoded_index(ptr0, ptr0 + codewordBitLength);
    uint64_t inverse = invert_mod(used_slots_per_plain, parms.plain_modulus());
    vector<Serializable<Ciphertext>> query;
    for (uint32_t i = 0; i < num_input_ciphers; i++) {
        Plaintext pt(parms.poly_modulus_degree());
        pt.set_zero();
        for (uint32_t j = 0; j < used_slots_per_plain; j++) {
            if (i * used_slots_per_plain + j < codewordBitLength && encoded_index[i * used_slots_per_plain + j] == 1) {
                pt[j] = inverse;
            }
        }
        query.push_back(encryptor.encrypt_symmetric(pt));
    }
    return serialize_ciphertexts(env, query);
}

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_constantweightpir_Mk22SingleIndexPirNativeUtils_generateReply(
        JNIEnv * env, jclass, jbyteArray parms_bytes, jbyteArray galois_keys_bytes,jbyteArray relin_keys_bytes, jobject ciphertexts_list,
        jobjectArray plaintexts_list, jobject pt_index_codewords_list, jint num_input_ciphers, 
        jint codeword_bit_length, jint hamming_weight, jint eq_type) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    GaloisKeys* galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    const RelinKeys relin_keys = deserialize_relin_keys(env, relin_keys_bytes, context);
    vector<Plaintext> database = deserialize_plaintexts_array(env, plaintexts_list, context);
    vector<Ciphertext> query_list = deserialize_ciphertexts(env, ciphertexts_list, context);
    uint32_t num_pts = database.size();
    jclass obj_class = env->FindClass("java/util/ArrayList");
    jmethodID get_method = env->GetMethodID(obj_class, "get", "(I)Ljava/lang/Object;");
    jmethodID size_method = env->GetMethodID(obj_class, "size", "()I");
    jint size = env->CallIntMethod(pt_index_codewords_list, size_method);
    vector<vector<uint32_t>> pt_index_codewords(size);
    for (jint i = 0; i < size; i++) {
        auto single_codeword = (jintArray) env->CallObjectMethod(pt_index_codewords_list, get_method, i);
        jint len = env->GetArrayLength(single_codeword);
        jint *ptr = env->GetIntArrayElements(single_codeword, JNI_FALSE);
        vector<uint32_t> tmp(ptr, ptr + len);
        pt_index_codewords[i] = tmp;
    }
    // expansion
    vector<Ciphertext> expanded_query = mk22_expand_input_ciphers(
        parms, *galois_keys, query_list, num_input_ciphers, codeword_bit_length);
    if (expanded_query.size() != codeword_bit_length) {
        throw logic_error("expanded query size should be equal codeword bit length.");
    }
    vector<Ciphertext> selection_vector(num_pts);
    mk22_generate_selection_vector(
        evaluator,&relin_keys, codeword_bit_length, hamming_weight, eq_type, expanded_query,
        pt_index_codewords, selection_vector);
    Ciphertext encrypted_answer;
    encrypted_answer = mk22_faster_inner_product(evaluator, selection_vector, database);
    evaluator.transform_from_ntt_inplace(encrypted_answer);
    evaluator.mod_switch_to_inplace(encrypted_answer, context.last_context_data()->parms_id());
    return serialize_ciphertext(env, encrypted_answer);
}

[[maybe_unused]] JNIEXPORT
jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_constantweightpir_Mk22SingleIndexPirNativeUtils_decryptReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray sk_bytes, jbyteArray response_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Decryptor decryptor(context, secret_key);
    parms_id_type parms_id = context.last_parms_id();
    Ciphertext response = deserialize_ciphertext(env, response_bytes, context);
    // directly decrypt
    int32_t noise_budget = decryptor.invariant_noise_budget(response);
    jclass exception = env->FindClass("java/lang/Exception");
    if (noise_budget == 0) {
        env->ThrowNew(exception, "noise budget is 0.");
        return nullptr;
    }
    Plaintext pt;
    decryptor.decrypt(response, pt);
    jlongArray result = env->NewLongArray((jsize) pt.coeff_count());
    jlong coeff_array[pt.coeff_count()];
    for (uint32_t i = 0; i < pt.coeff_count(); i++) {
        coeff_array[i] = (jlong) pt[i];
    }
    env->SetLongArrayRegion(result, 0, (jsize) pt.coeff_count(), coeff_array);
    return result;
}