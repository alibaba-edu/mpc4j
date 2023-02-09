//
// Created by pengliqiang on 2023/1/17.
//
#include "edu_alibaba_mpc4j_s2pc_pir_index_sealpir_Acls18IndexPirNativeUtils.h"
#include "seal/seal.h"
#include "../utils.h"
#include "../serialize.h"
#include <iomanip>
#include "../index_pir.h"

using namespace seal;
using namespace std;

// #define DEBUG

JNIEXPORT jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_sealpir_Acls18IndexPirNativeUtils_generateSealContext(
        JNIEnv *env, jclass, jint poly_modulus_degree, jlong plain_modulus) {
    EncryptionParameters parms = generate_encryption_parameters(scheme_type::bfv, poly_modulus_degree, plain_modulus,
                                                                CoeffModulus::BFVDefault(poly_modulus_degree,
                                                                                         sec_level_type::tc128));
    return serialize_encryption_parms(env, parms);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_sealpir_Acls18IndexPirNativeUtils_keyGen(
        JNIEnv *env, jclass, jbyteArray parms_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    KeyGenerator key_gen(context);
    const SecretKey& secret_key = key_gen.secret_key();
    PublicKey public_key;
    key_gen.create_public_key(public_key);
    GaloisKeys galois_keys = generate_galois_keys(context, key_gen);
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

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_sealpir_Acls18IndexPirNativeUtils_nttTransform(
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

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_sealpir_Acls18IndexPirNativeUtils_generateQuery(
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
    vector<Ciphertext> result;
    uint32_t coeff_count = parms.poly_modulus_degree();
    Plaintext pt(coeff_count);
    for (uint32_t i = 0; i < indices.size(); i++) {
        uint32_t num_ptxts = ceil((nvec[i] + 0.0) / coeff_count);
#ifdef DEBUG
        cout << "Client: number of ctxts needed for query = " << num_ptxts << endl;
#endif
        for (uint32_t j = 0; j < num_ptxts; j++) {
            pt.set_zero();
            if (indices[i] >= coeff_count * j && indices[i] <= coeff_count * (j + 1)) {
                uint64_t real_index = indices[i] - coeff_count * j;
                uint64_t n_i = nvec[i];
                uint64_t total = coeff_count;
                if (j == num_ptxts - 1) {
                    total = n_i % coeff_count;
                    if (total == 0) {
                        total = coeff_count;
                    }
                }
                uint64_t log_total = ceil(log2(total));
#ifdef DEBUG
                cout << "Client: Inverting " << pow(2, log_total) << endl;
#endif
                pt[real_index] = invert_mod((uint64_t) pow(2, log_total), parms.plain_modulus());
            }
            Ciphertext dest;
            encryptor.encrypt_symmetric(pt, dest);
            result.push_back(dest);
        }
    }
    return serialize_ciphertexts(env, result);
}

JNIEXPORT jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_sealpir_Acls18IndexPirNativeUtils_generateReply(
        JNIEnv * env, jclass, jbyteArray parms_bytes, jbyteArray galois_keys_bytes, jobject ciphertexts_list,
        jobject plaintexts_list, jintArray nvec_array) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    Evaluator evaluator(context);
    GaloisKeys galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    auto exception = env->FindClass("java/lang/Exception");
    vector<Plaintext> database = deserialize_plaintexts(env, plaintexts_list, context);
    vector<Ciphertext> query_list = deserialize_ciphertexts(env, ciphertexts_list, context);
    jint *ptr = env->GetIntArrayElements(nvec_array, JNI_FALSE);
    uint32_t d = env->GetArrayLength(nvec_array);
    vector<uint32_t> nvec(ptr, ptr + d);
    vector<vector<Ciphertext>> query(d);
    uint32_t coeff_count = parms.poly_modulus_degree();
    uint32_t index = 0;
    for (uint32_t i = 0; i < d; i++) {
        uint32_t num_ptxts = ceil((nvec[i] + 0.0) / coeff_count);
        for (uint32_t j = 0; j < num_ptxts; j++) {
            query[i].push_back(query_list[index++]);
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
#ifdef DEBUG
        cout << "Server: " << i + 1 << "-th recursion level started " << endl;
        cout << "Server: n_i = " << nvec[i] << endl;
#endif
        vector<Ciphertext> expanded_query;
        for (uint32_t j = 0; j < query[i].size(); j++) {
            uint64_t total = coeff_count;
            if (j == query[i].size() - 1) {
                total = nvec[i] % coeff_count;
                if (total == 0) {
                    total = coeff_count;
                }
            }
#ifdef DEBUG
            cout << "-- expanding one query ctxt into " << total << " ctxts " << endl;
#endif
            vector<Ciphertext> expanded_query_part = expand_query(parms, query[i][j], galois_keys, total);
            // poc_rlwe_expand(query[i][j], context, galois_keys, total);
            expanded_query.insert(
                    expanded_query.end(),
                    std::make_move_iterator(expanded_query_part.begin()),
                    std::make_move_iterator(expanded_query_part.end()));
            expanded_query_part.clear();
        }
#ifdef DEBUG
        cout << "Server: expansion done " << endl;
#endif
        if (expanded_query.size() != nvec[i]) {
            env->ThrowNew(exception, "size mismatch!");
        }
        for (auto & jj : expanded_query) {
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
            evaluator.multiply_plain(expanded_query[0], (*cur)[k],intermediateCtxts[k]);
            for (uint32_t j = 1; j < nvec[i]; j++) {
                evaluator.multiply_plain(expanded_query[j], (*cur)[k + j * product], temp);
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
#ifdef DEBUG
        cout << "Server: " << i + 1 << "-th recursion level finished " << endl;
#endif
    }
    // This should never get here
    env->ThrowNew(exception, "generate response failed!");
    return nullptr;
}

JNIEXPORT jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_sealpir_Acls18IndexPirNativeUtils_decryptReply(
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

JNIEXPORT jint JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_sealpir_Acls18IndexPirNativeUtils_expansionRatio(
        JNIEnv *env, jclass, jbyteArray parms_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms);
    return (jint) compute_expansion_ratio(context.first_context_data()->parms());
}