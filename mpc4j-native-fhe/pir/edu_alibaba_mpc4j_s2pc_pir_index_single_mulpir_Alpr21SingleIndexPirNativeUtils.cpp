//
// Created by qixian zhou on 2023/5/19.
//
#include "edu_alibaba_mpc4j_s2pc_pir_index_single_mulpir_Alpr21SingleIndexPirNativeUtils.h"
#include "seal/seal.h"
#include "../utils.h"
#include "../serialize.h"
#include <iomanip>
#include "../index_pir.h"
#include <chrono>

using namespace seal;
using namespace std;

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_mulpir_Alpr21SingleIndexPirNativeUtils_generateEncryptionParams(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus) {
    EncryptionParameters parms = EncryptionParameters(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(plain_modulus);
    parms.set_coeff_modulus(CoeffModulus::BFVDefault(poly_modulus_degree,sec_level_type::tc128));
    return serialize_encryption_parms(env, parms);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_mulpir_Alpr21SingleIndexPirNativeUtils_keyGen(
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
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_mulpir_Alpr21SingleIndexPirNativeUtils_nttTransform(
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
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_mulpir_Alpr21SingleIndexPirNativeUtils_generateQuery(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jintArray indices_array,
        jintArray nevc_array) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    auto exception = env->FindClass("java/lang/Exception");
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Encryptor encryptor(context, public_key, secret_key);
    uint32_t dimension = env->GetArrayLength(indices_array);
    jint *ptr0 = env->GetIntArrayElements(indices_array, JNI_FALSE);
    // client PT query index_single in each dimension 
    vector<uint32_t> indices(ptr0, ptr0 + dimension);
    uint32_t size = env->GetArrayLength(nevc_array);
    jint *ptr1 = env->GetIntArrayElements(nevc_array, JNI_FALSE);
    vector<int32_t> nvec(ptr1, ptr1 + size);
    if (size != dimension) {
        env->ThrowNew(exception, "size is incorrect!");
    }
    uint32_t dim_sum = std::accumulate(nvec.begin(), nvec.end(), 0);
    // the needed Ciphertext num is ceil(d/N), this is the New Query 
    vector<Serializable<Ciphertext>> result;
    uint32_t result_size = (dim_sum + parms.poly_modulus_degree() - 1) / parms.poly_modulus_degree();
    uint32_t poly_modulus_degree = parms.poly_modulus_degree();
    Plaintext pt(poly_modulus_degree);
    uint32_t offset = 0;
    for(uint32_t c = 0; c < result_size; c++) {
        pt.set_zero();
        while(!indices.empty()) {
            if (indices[0] + offset >= poly_modulus_degree) {
                // no more slots in this poly
                indices[0] -= (poly_modulus_degree - offset);
                nvec[0] -= (int32_t) (poly_modulus_degree - offset);
                offset = 0;
                break;
            }
            // when dim_sum % poly_modulus_degree == 0, m = N
            // when handle 1-dim, at the same time, the byte length of a single raw data is equal to the maximum byte length 
            // that PT can represent, dim_sum = N, in order to avoid m = 0, we handle this situation separately
            uint64_t m;
            if (dim_sum % poly_modulus_degree != 0) {
                m = (c < result_size - 1)
                       ? poly_modulus_degree
                       : get_next_power_of_two(dim_sum % poly_modulus_degree); // if input is just power of 2, return input itself
            }else {
                m = poly_modulus_degree;
            }
            // Set the coefficients corresponding to indices[0], the range of coefficients used here is [0, offset]. 
            // Done Place indices[0] on a PT
            pt[indices[0] + offset] = invert_mod(m, parms.plain_modulus());
            // start handle next indices
            offset += nvec[0];
            indices.erase(indices.begin());
            nvec.erase(nvec.begin());
            // need a new PT to handle indices
            if (offset >= poly_modulus_degree) {
                offset -= poly_modulus_degree;
                break;
            }
        }
        result.push_back(encryptor.encrypt_symmetric(pt));
    }
    return serialize_ciphertexts(env, result);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_mulpir_Alpr21SingleIndexPirNativeUtils_generateReply(
        JNIEnv * env, jclass, jbyteArray parms_bytes, jbyteArray galois_keys_bytes,jbyteArray relin_keys_bytes, jobject ciphertexts_list,
        jobjectArray plaintexts_list, jintArray nvec_array) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    GaloisKeys* galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    const RelinKeys relin_keys = deserialize_relin_keys(env, relin_keys_bytes, context);
    vector<Plaintext> database = deserialize_plaintexts_array(env, plaintexts_list, context);
    vector<Ciphertext> query_list = deserialize_ciphertexts(env, ciphertexts_list, context);
    jint *ptr = env->GetIntArrayElements(nvec_array, JNI_FALSE);
    uint32_t d = env->GetArrayLength(nvec_array);
    vector<int32_t> nvec(ptr, ptr + d);
    // calculate dim_sum
    uint32_t dim_sum = std::accumulate(nvec.begin(), nvec.end(), 0);
    // convert query ct to [ E(0) E(0) ... E(1) ... E(0) E(1)... ], size is dim_sum
    vector<Ciphertext> selection_vector = new_expand_query(parms, query_list, dim_sum, *galois_keys);
    for(auto& single  : selection_vector) {
        evaluator.transform_to_ntt_inplace(single);
    }
    if (dim_sum != selection_vector.size()) {
        throw logic_error("Selection vector size does not match dimensions");
    }
    vector<Ciphertext> results = multiply_mulpir(parms, &relin_keys, database, 0, selection_vector, 0,  nvec, 0);
    return serialize_ciphertexts(env, results);
}

[[maybe_unused]] JNIEXPORT
jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_mulpir_Alpr21SingleIndexPirNativeUtils_decryptReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray sk_bytes, jobject response_list) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    auto exception = env->FindClass("java/lang/Exception");
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Decryptor decryptor(context, secret_key);
    parms_id_type parms_id = context.last_parms_id();
    vector<Ciphertext> temp = deserialize_ciphertexts(env, response_list, context);
    if (temp.size() != 1) {
        env->ThrowNew(exception, "MulPIR Single query's response only one Ciphertext");
    }
    // directly decrypt
    Plaintext pt;
    decryptor.decrypt(temp[0], pt);
    jlongArray result = env->NewLongArray((jsize) pt.coeff_count());
    jlong coeff_array[pt.coeff_count()];
    for (uint32_t i = 0; i < pt.coeff_count(); i++) {
        coeff_array[i] = (jlong) pt[i];
    }
    env->SetLongArrayRegion(result, 0, (jsize) pt.coeff_count(), coeff_array);
    return result;
}