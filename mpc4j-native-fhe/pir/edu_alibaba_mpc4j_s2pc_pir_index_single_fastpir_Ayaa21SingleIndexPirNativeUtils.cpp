//
// Created by pengliqiang on 2023/1/25.
//

#include "edu_alibaba_mpc4j_s2pc_pir_index_single_fastpir_Ayaa21SingleIndexPirNativeUtils.h"
#include "seal/seal.h"
#include "../serialize.h"
#include <iomanip>
#include "../index_pir.h"

using namespace seal;
using namespace std;

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_fastpir_Ayaa21SingleIndexPirNativeUtils_generateEncryptionParams(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus, jlongArray coeff_mod_arr) {
    uint32_t size = env->GetArrayLength(coeff_mod_arr);
    auto *ptr = reinterpret_cast<uint64_t *>(env->GetLongArrayElements(coeff_mod_arr, JNI_FALSE));
    vector<uint64_t> vec(ptr, ptr + size);
    vector<Modulus> modulus(size);
    for (uint32_t i = 0; i < size; i++) {
        modulus[i] = vec[i];
    }
    EncryptionParameters parms = EncryptionParameters(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(plain_modulus);
    parms.set_coeff_modulus(modulus);
    return serialize_encryption_parms(env, parms);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_fastpir_Ayaa21SingleIndexPirNativeUtils_keyGen(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jintArray steps_arr) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    KeyGenerator key_gen(context);
    const SecretKey& secret_key = key_gen.secret_key();
    Serializable<PublicKey> public_key = key_gen.create_public_key();
    uint32_t size = env->GetArrayLength(steps_arr);
    auto *ptr = reinterpret_cast<int32_t *>(env->GetIntArrayElements(steps_arr, JNI_FALSE));
    vector<int32_t> steps(ptr, ptr + size);
    Serializable<GaloisKeys> galois_keys = key_gen.create_galois_keys(steps);
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    jbyteArray pk_byte = serialize_public_key(env, public_key);
    jbyteArray sk_byte = serialize_secret_key(env, secret_key);
    jbyteArray galois_keys_bytes = serialize_galois_keys(env, galois_keys);
    env->CallBooleanMethod(list_obj, list_add, pk_byte);
    env->CallBooleanMethod(list_obj, list_add, sk_byte);
    env->CallBooleanMethod(list_obj, list_add, galois_keys_bytes);
    return list_obj;
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_fastpir_Ayaa21SingleIndexPirNativeUtils_nttTransform(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobjectArray plaintext_list) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    vector<Plaintext> database = deserialize_plaintexts_from_coeff(env, plaintext_list, context);
    for (auto & plaintext : database) {
        evaluator.transform_to_ntt_inplace(plaintext, context.first_parms_id());
    }
    return serialize_plaintexts(env, database);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_fastpir_Ayaa21SingleIndexPirNativeUtils_generateQuery(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jint index, jint query_size) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    BatchEncoder batch_encoder(context);
    Encryptor encryptor(context, public_key, secret_key);
    vector<Serializable<Ciphertext>> query;
    Plaintext pt;
    uint32_t slot_count = batch_encoder.slot_count();
    uint32_t row_size = slot_count / 2;
    for (uint32_t i = 0; i < query_size; i++) {
        vector<uint64_t> pod_matrix(slot_count, 0ULL);
        if ((index / row_size) == i) {
            pod_matrix[index % row_size] = 1;
            pod_matrix[row_size + (index % row_size)] = 1;
        }
        batch_encoder.encode(pod_matrix, pt);
        query.push_back(encryptor.encrypt_symmetric(pt));
    }
    return serialize_ciphertexts(env, query);
}

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_fastpir_Ayaa21SingleIndexPirNativeUtils_generateResponse(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray galois_bytes, jobject query_bytes,
        jobjectArray database_bytes, jint num_columns_per_obj) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    GaloisKeys* galois_keys = deserialize_galois_keys(env, galois_bytes, context);
    vector<Ciphertext> query = deserialize_ciphertexts(env, query_bytes, context);
    for (auto & i : query) {
        evaluator.transform_to_ntt_inplace(i);
    }
    vector<Plaintext> database = deserialize_plaintexts_array(env, database_bytes, context);
    Ciphertext response = get_sum(query, evaluator, *galois_keys, database, 0, num_columns_per_obj - 1);
    return serialize_ciphertext(env, response);
}

[[
maybe_unused]] JNIEXPORT
jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_fastpir_Ayaa21SingleIndexPirNativeUtils_decodeResponse(
        JNIEnv * env, jclass, jbyteArray parms_bytes, jbyteArray sk_bytes, jbyteArray response_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Ciphertext response = deserialize_ciphertext(env, response_bytes, context);
    Decryptor decryptor(context, secret_key);
    BatchEncoder batch_encoder(context);
    Plaintext pt;
    decryptor.decrypt(response, pt);
    vector<uint64_t> coeffs;
    batch_encoder.decode(pt, coeffs);
    jlongArray result;
    auto size = (jsize) coeffs.size();
    result = env->NewLongArray(size);
    jlong temp[size];
    for (uint32_t i = 0; i < size; i++) {
        temp[i] = (jlong) coeffs[i];
    }
    env->SetLongArrayRegion(result, 0, size, temp);
    return result;
}