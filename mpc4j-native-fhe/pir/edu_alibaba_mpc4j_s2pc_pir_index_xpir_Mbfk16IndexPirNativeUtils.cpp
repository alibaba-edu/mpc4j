//
// Created by Liqiang Peng on 2022/9/13.
//

#include "edu_alibaba_mpc4j_s2pc_pir_index_xpir_Mbfk16IndexPirNativeUtils.h"
#include "seal/seal.h"
#include "../utils.h"
#include "../serialize.h"
#include <iomanip>
#include "../index_pir.h"

using namespace seal;
using namespace std;

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_Mbfk16IndexPirNativeUtils_generateSealContext(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus) {
    EncryptionParameters parms = generate_encryption_parameters(scheme_type::bfv, poly_modulus_degree, plain_modulus);
    return serialize_encryption_parms(env, parms);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_Mbfk16IndexPirNativeUtils_keyGen(
        JNIEnv *env, jclass, jbyteArray params_byte) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_byte);
    SEALContext context(parms);
    KeyGenerator key_gen(context);
    const SecretKey& secret_key = key_gen.secret_key();
    Serializable public_key = key_gen.create_public_key();
    return serialize_public_key_secret_key(env, public_key, secret_key);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_Mbfk16IndexPirNativeUtils_nttTransform(
        JNIEnv *env, jclass, jbyteArray params_bytes, jobject plaintext_list) {
    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
    SEALContext context(params);
    Evaluator evaluator(context);
    vector<Plaintext> plaintexts = deserialize_plaintext_from_coefficients(env, plaintext_list, context,
                                                                           params.poly_modulus_degree());
    // Transform plaintext to NTT.
    for (auto & plaintext : plaintexts) {
        evaluator.transform_to_ntt_inplace(plaintext, context.first_parms_id());
    }
    return serialize_plaintexts(env, plaintexts);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_Mbfk16IndexPirNativeUtils_generateQuery(
        JNIEnv *env, jclass, jbyteArray params_byte, jbyteArray pk_byte, jbyteArray sk_byte, jintArray message_list) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_byte);
    SEALContext context(parms);
    auto exception = env->FindClass("java/lang/Exception");
    PublicKey public_key = deserialize_public_key(env, pk_byte, context);
    if (!is_metadata_valid_for(public_key, context)) {
        env->ThrowNew(exception, "invalid public key for this SEALContext!");
    }
    SecretKey secret_key = deserialize_secret_key(env, sk_byte, context);
    if (!is_metadata_valid_for(secret_key, context)) {
        env->ThrowNew(exception, "invalid secret key for this SEALContext!");
    }
    Encryptor encryptor(context, public_key, secret_key);
    auto pool = MemoryManager::GetPool();
    int size = env->GetArrayLength(message_list);
    jint *ptr = env->GetIntArrayElements(message_list, JNI_FALSE);
    vector<uint32_t> vec(ptr, ptr + size);
    vector<Ciphertext> ciphertexts;
    ciphertexts.resize(size);
    for (uint32_t i = 0; i < size; i++) {
        Plaintext plaintext(parms.poly_modulus_degree());
        plaintext.set_zero();
        if (vec[i] == 1) {
            plaintext[0] = 1;
            encryptor.encrypt_symmetric(plaintext, ciphertexts[i]);
        } else {
            encryptor.encrypt_zero_symmetric(ciphertexts[i]);
        }
    }
    return serialize_ciphertexts(env, ciphertexts);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_Mbfk16IndexPirNativeUtils_generateReply(
        JNIEnv *env, jclass, jbyteArray params_bytes, jobject ciphertext_list_bytes, jobject plaintext_list_bytes,
        jintArray nvec_array) {
    EncryptionParameters params = deserialize_encryption_params(env, params_bytes);
    SEALContext context(params);
    Evaluator evaluator(context);
    auto exception = env->FindClass("java/lang/Exception");
    vector<Plaintext> database = deserialize_plaintexts_from_byte(env, plaintext_list_bytes, context);
    vector<Ciphertext> query = deserialize_ciphertexts(env, ciphertext_list_bytes, context);
    jint *ptr = env->GetIntArrayElements(nvec_array, JNI_FALSE);
    uint32_t d = env->GetArrayLength(nvec_array);
    vector<uint64_t> nvec(ptr, ptr + d);
    vector<vector<Ciphertext>> query_list(d);
    int flag = 0;
    for (int i = 0; i < d; i++) {
        query_list[i].reserve(nvec[i]);
        for (int j = 0; j < nvec[i]; j++) {
            query_list[i].push_back(query[flag++]);
        }
    }
    uint64_t product = 1;
    for (unsigned long long i : nvec) {
        product *= i;
    }
    vector<Plaintext> *cur = &database;
    vector<Plaintext> intermediate_plain; // decompose....
    auto pool = MemoryManager::GetPool();
    uint32_t expansion_ratio = compute_expansion_ratio(params);
    for (uint32_t i = 0; i < nvec.size(); i++) {
#ifdef DEBUG
        cout << "Server: " << i + 1 << "-th recursion level started " << endl;
        cout << "Server: n_i = " << nvec[i] << endl;
#endif
        if (query_list[i].size() != nvec[i]) {
            cout << " size mismatch!!! " << query_list[i].size() << ", " << nvec[i] << endl;
            env->ThrowNew(exception, "size mismatch!");
        }
        // Transform expanded query to NTT, and ...
        for (auto & jj : query_list[i]) {
            evaluator.transform_to_ntt_inplace(jj);
        }
        if (i > 0) {
            for (auto & jj : *cur) {
                evaluator.transform_to_ntt_inplace(jj,context.first_parms_id());
            }
        }
#ifdef DEBUG
        for (uint64_t k = 0; k < product; k++) {
            if ((*cur)[k].is_zero()) {
                cout << k + 1 << "/ " << product << "-th ptxt = 0 " << endl;
            }
        }
#endif
        product /= nvec[i];
        vector<Ciphertext> intermediateCtxts(product);
        Ciphertext temp;
        for (uint64_t k = 0; k < product; k++) {
            evaluator.multiply_plain(query_list[i][0], (*cur)[k],intermediateCtxts[k]);
            for (uint64_t j = 1; j < nvec[i]; j++) {
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
            for (uint64_t rr = 0; rr < product; rr++) {
                EncryptionParameters parms;
                evaluator.mod_switch_to_inplace(intermediateCtxts[rr],context.last_parms_id());
                parms = context.last_context_data()->parms();
                vector<Plaintext> plains = decompose_to_plaintexts(parms, intermediateCtxts[rr]);
                for (auto & plain : plains) {
                    intermediate_plain.emplace_back(plain);
                }
            }
            product = intermediate_plain.size(); // multiply by expansion rate.
        }
#ifdef DEBUG
        cout << "Server: " << i + 1 << "-th recursion level finished " << endl;
#endif
    }
    // This should never get here
    env->ThrowNew(exception, "generate response failed!");
    return nullptr;
}

JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_xpir_Mbfk16IndexPirNativeUtils_decryptReply(
        JNIEnv *env, jclass, jbyteArray params_byte, jbyteArray sk_byte, jobject response_list, jint d) {
    EncryptionParameters parms = deserialize_encryption_params(env, params_byte);
    SEALContext context(parms);
    auto exception = env->FindClass("java/lang/Exception");
    SecretKey secret_key = deserialize_secret_key(env, sk_byte, context);
    if (!is_metadata_valid_for(secret_key, context)) {
        env->ThrowNew(exception, "invalid secret key for this SEALContext!");
    }
    Decryptor decryptor(context, secret_key);
    parms = context.last_context_data()->parms();
    parms_id_type parms_id = context.last_parms_id();
    uint32_t exp_ratio = compute_expansion_ratio(parms);
    uint32_t recursion_level = d;
    vector<Ciphertext> temp = deserialize_ciphertexts(env, response_list, context);
    uint32_t ciphertext_size = temp[0].size();
    for (uint32_t i = 0; i < recursion_level; i++) {
#ifdef DEBUG
        cout << "Client: " << i + 1 << "/ " << recursion_level << "-th decryption layer started." << endl;
#endif
        vector<Ciphertext> newtemp;
        vector<Plaintext> tempplain;
        for (uint32_t j = 0; j < temp.size(); j++) {
            Plaintext ptxt;
            decryptor.decrypt(temp[j], ptxt);
            tempplain.push_back(ptxt);
#ifdef DEBUG
            cout << "Client: reply noise budget = " << decryptor.invariant_noise_budget(temp[j]) << endl;
            cout << "decoded (and scaled) plaintext = " << ptxt.to_string() << endl;
            cout << "recursion level : " << i << " noise budget : " << decryptor.invariant_noise_budget(temp[j]) << endl;
#endif
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
            return get_plaintext_coeffs(env, tempplain[0]);
        } else {
            tempplain.clear();
            temp = newtemp;
        }
    }
    // This should never be called
    env->ThrowNew(exception, "decode response failed!");
    return nullptr;
}