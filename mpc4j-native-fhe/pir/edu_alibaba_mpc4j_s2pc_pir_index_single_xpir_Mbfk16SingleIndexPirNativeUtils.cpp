//
// Created by Liqiang Peng on 2022/9/13.
//

#include "edu_alibaba_mpc4j_s2pc_pir_index_single_xpir_Mbfk16SingleIndexPirNativeUtils.h"
#include "seal/seal.h"
#include "../serialize.h"
#include <iomanip>
#include "../index_pir.h"

using namespace seal;
using namespace std;

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_xpir_Mbfk16SingleIndexPirNativeUtils_generateEncryptionParams(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus) {
    EncryptionParameters parms = EncryptionParameters(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(plain_modulus);
    parms.set_coeff_modulus(CoeffModulus::BFVDefault(poly_modulus_degree, sec_level_type::tc128));
    return serialize_encryption_parms(env, parms);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_xpir_Mbfk16SingleIndexPirNativeUtils_keyGen(
        JNIEnv *env, jclass, jbyteArray parms_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    KeyGenerator key_gen(context);
    const SecretKey& secret_key = key_gen.secret_key();
    Serializable<PublicKey> public_key = key_gen.create_public_key();
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    jbyteArray pk_byte = serialize_public_key(env, public_key);
    jbyteArray sk_byte = serialize_secret_key(env, secret_key);
    env->CallBooleanMethod(list_obj, list_add, pk_byte);
    env->CallBooleanMethod(list_obj, list_add, sk_byte);
    return list_obj;
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_xpir_Mbfk16SingleIndexPirNativeUtils_nttTransform(
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
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_xpir_Mbfk16SingleIndexPirNativeUtils_generateQuery(
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
    vector<uint32_t> indices(ptr0, ptr0 + dimension);
    uint32_t size = env->GetArrayLength(nevc_array);
    jint *ptr1 = env->GetIntArrayElements(nevc_array, JNI_FALSE);
    vector<uint32_t> nvec(ptr1, ptr1 + size);
    if (size != dimension) {
        env->ThrowNew(exception, "size is incorrect!");
    }
    vector<Serializable<Ciphertext>> query;
    uint32_t coeff_count = parms.poly_modulus_degree();
    for (uint32_t i = 0; i < dimension; i++) {
        for (uint32_t j = 0; j < nvec[i]; j++) {
            Plaintext pt(coeff_count);
            pt.set_zero();
            if (indices[i] == j) {
                pt[0] = 1;
            }
            query.push_back(encryptor.encrypt_symmetric(pt));
        }
    }
    return serialize_ciphertexts(env, query);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_xpir_Mbfk16SingleIndexPirNativeUtils_generateReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobject ciphertexts_list, jobjectArray plaintexts_list,
        jintArray nvec_array) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    auto exception = env->FindClass("java/lang/Exception");
    vector<Plaintext> database = deserialize_plaintexts_array(env, plaintexts_list, context);
    vector<Ciphertext> query = deserialize_ciphertexts(env, ciphertexts_list, context);
    jint *ptr = env->GetIntArrayElements(nvec_array, JNI_FALSE);
    uint32_t d = env->GetArrayLength(nvec_array);
    vector<uint32_t> nvec(ptr, ptr + d);
    vector<vector<Ciphertext>> query_list(d);
    uint32_t flag = 0;
    for (uint32_t i = 0; i < d; i++) {
        query_list[i].reserve(nvec[i]);
        for (uint32_t j = 0; j < nvec[i]; j++) {
            query_list[i].push_back(query[flag++]);
        }
    }
    uint32_t product = 1;
    for (uint32_t i : nvec) {
        product *= i;
    }
    vector<Plaintext> *cur = &database;
    vector<Plaintext> intermediate_plain;
    uint32_t expansion_ratio = compute_expansion_ratio(parms);
    for (uint32_t i = 0; i < nvec.size(); i++) {
        if (query_list[i].size() != nvec[i]) {
            env->ThrowNew(exception, "size mismatch!");
        }
        for (auto & jj : query_list[i]) {
            evaluator.transform_to_ntt_inplace(jj);
        }
        if (i > 0) {
            for (auto & jj : *cur) {
                evaluator.transform_to_ntt_inplace(jj,context.first_parms_id());
            }
        }
        product /= nvec[i];
        vector<Ciphertext> intermediateCtxts(product);
        Ciphertext temp;
        for (uint32_t k = 0; k < product; k++) {
            evaluator.multiply_plain(query_list[i][0], (*cur)[k],intermediateCtxts[k]);
            for (uint32_t j = 1; j < nvec[i]; j++) {
                evaluator.multiply_plain(query_list[i][j], (*cur)[k + j * product], temp);
                evaluator.add_inplace(intermediateCtxts[k], temp); // Adds to first component.
            }
        }
        for (auto & intermediateCtxt : intermediateCtxts) {
            evaluator.transform_from_ntt_inplace(intermediateCtxt);
        }
        if (i == nvec.size() - 1) {
            return serialize_ciphertexts(env, intermediateCtxts);
        } else {
            intermediate_plain.clear();
            intermediate_plain.reserve(expansion_ratio * product);
            cur = &intermediate_plain;
            for (uint32_t rr = 0; rr < product; rr++) {
                evaluator.mod_switch_to_inplace(intermediateCtxts[rr],context.last_parms_id());
                vector<Plaintext> plains = decompose_to_plaintexts(context.last_context_data()->parms(), intermediateCtxts[rr]);
                for (auto & plain : plains) {
                    intermediate_plain.emplace_back(plain);
                }
            }
            product = intermediate_plain.size();
        }
    }
    // This should never get here
    env->ThrowNew(exception, "generate response failed!");
    return nullptr;
}

[[maybe_unused]] JNIEXPORT
jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_xpir_Mbfk16SingleIndexPirNativeUtils_decryptReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray sk_bytes, jobject response_list, jint d) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    auto exception = env->FindClass("java/lang/Exception");
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Decryptor decryptor(context, secret_key);
    parms = context.last_context_data()->parms();
    parms_id_type parms_id = context.last_parms_id();
    uint32_t exp_ratio = compute_expansion_ratio(parms);
    uint32_t recursion_level = d;
    vector<Ciphertext> temp = deserialize_ciphertexts(env, response_list, context);
    uint32_t ciphertext_size = temp[0].size();
    for (uint32_t i = 0; i < recursion_level; i++) {
        vector<Ciphertext> newtemp;
        vector<Plaintext> tempplain;
        for (uint32_t j = 0; j < temp.size(); j++) {
            Plaintext ptxt;
            decryptor.decrypt(temp[j], ptxt);
            tempplain.push_back(ptxt);
            if ((j + 1) % (exp_ratio * ciphertext_size) == 0 && j > 0) {
                // Combine into one ciphertext.
                Ciphertext combined(context, parms_id);
                compose_to_ciphertext(parms, tempplain, combined);
                newtemp.push_back(combined);
                tempplain.clear();
            }
        }
        if (i == recursion_level - 1) {
            if (temp.size() != 1) {
                env->ThrowNew(exception, "decode response failed!");
            }
            jlongArray result = env->NewLongArray((jsize) tempplain[0].coeff_count());
            jlong coeff_array[tempplain[0].coeff_count()];
            for (uint32_t ii = 0; ii < tempplain[0].coeff_count(); ii++) {
                coeff_array[ii] = (jlong) tempplain[0][ii];
            }
            env->SetLongArrayRegion(result, 0, (jsize) tempplain[0].coeff_count(), coeff_array);
            return result;
        } else {
            tempplain.clear();
            temp = newtemp;
        }
    }
    // This should never be called
    env->ThrowNew(exception, "decode response failed!");
    return nullptr;
}

[[maybe_unused]] JNIEXPORT
jint JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_xpir_Mbfk16SingleIndexPirNativeUtils_expansionRatio(
        JNIEnv *env, jclass, jbyteArray parms_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    return (jint) compute_expansion_ratio(context.last_context_data()->parms()) << 1;
}