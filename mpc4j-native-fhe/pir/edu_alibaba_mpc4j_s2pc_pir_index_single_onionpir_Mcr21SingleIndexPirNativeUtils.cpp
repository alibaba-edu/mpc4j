//
// Created by Liqiang Peng on 2022/11/14.
//

#include "edu_alibaba_mpc4j_s2pc_pir_index_single_onionpir_Mcr21SingleIndexPirNativeUtils.h"
#include "seal/seal.h"
#include "../utils.h"
#include "../serialize.h"
#include "../tfhe/params.h"
#include "../tfhe/tfhe.h"
#include "../index_pir.h"
#include <iomanip>

using namespace seal;
using namespace std;

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_onionpir_Mcr21SingleIndexPirNativeUtils_generateEncryptionParams(
        JNIEnv *env, jclass, jint poly_modulus_degree, jint plain_modulus_size) {
    uint64_t plain_modulus = PlainModulus::Batching(poly_modulus_degree, plain_modulus_size).value();
    EncryptionParameters parms = EncryptionParameters(scheme_type::bfv);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(plain_modulus);
    parms.set_coeff_modulus(CoeffModulus::Create(poly_modulus_degree, {60, 60, 60}));
    return serialize_encryption_parms(env, parms);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_onionpir_Mcr21SingleIndexPirNativeUtils_keyGen(
        JNIEnv *env, jclass, jbyteArray parms_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms, true, sec_level_type::none);
    KeyGenerator key_gen(context);
    const SecretKey& secret_key = key_gen.secret_key();
    Serializable<PublicKey> public_key = key_gen.create_public_key();
    Serializable<GaloisKeys> galois_keys = generate_galois_keys(context, key_gen);
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    jbyteArray pk_bytes = serialize_public_key(env, public_key);
    jbyteArray sk_bytes = serialize_secret_key(env, secret_key);
    jbyteArray galois_keys_bytes = serialize_galois_keys(env, galois_keys);
    env->CallBooleanMethod(list_obj, list_add, pk_bytes);
    env->CallBooleanMethod(list_obj, list_add, sk_bytes);
    env->CallBooleanMethod(list_obj, list_add, galois_keys_bytes);
    return list_obj;
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_onionpir_Mcr21SingleIndexPirNativeUtils_preprocessDatabase(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jobject coeff_list) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms, true, sec_level_type::none);
    vector<Plaintext> db_plaintext = deserialize_plaintexts_from_coeff_without_batch_encode(env, coeff_list, context);
    vector<vector<uint64_t *>> split_db;
    split_db.reserve(db_plaintext.size());
    uint32_t decomp_size = 2;
    uint32_t plain_base = parms.plain_modulus().bit_count() / decomp_size;
    for (auto & item : db_plaintext) {
        vector<uint64_t *> plain_decomp;
        plain_decomposition(item, context, decomp_size, plain_base, plain_decomp);
        split_db.push_back(plain_decomp);
    }
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    uint32_t coeff_count = parms.poly_modulus_degree();
    uint32_t coeff_mod_count = context.first_context_data()->parms().coeff_modulus().size();
    uint32_t size = coeff_mod_count * coeff_count;
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    for (auto & item : split_db) {
        for (uint32_t j = 0; j < decomp_size; j++) {
            jlongArray arr = env->NewLongArray((jsize) size);
            jlong *fill;
            fill = new jlong[size];
            for (uint32_t l = 0; l < size; l++) {
                fill[l] = (jlong) item[j][l];
            }
            env->SetLongArrayRegion(arr, 0, (jsize) size, fill);
            env->CallBooleanMethod(list_obj, list_add, arr);
            delete(fill);
            env->DeleteLocalRef(arr);
        }
    }
    env->DeleteLocalRef(list_jcs);
    return list_obj;
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_onionpir_Mcr21SingleIndexPirNativeUtils_encryptSecretKey(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms, true, sec_level_type::none);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    vector<Ciphertext> enc_sk;
    TFHEcipher tfhe_cipher(context, public_key);
    tfhe_cipher.encrypt(secret_key.data(), enc_sk);
    return serialize_ciphertexts(env, enc_sk);
}

[[maybe_unused]] JNIEXPORT
jobject JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_onionpir_Mcr21SingleIndexPirNativeUtils_generateQuery(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray sk_bytes, jintArray jindices,
        jintArray jnvec) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms, true, sec_level_type::none);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    uint32_t size = env->GetArrayLength(jindices);
    jint *ptr = env->GetIntArrayElements(jindices, JNI_FALSE);
    vector<uint32_t> indices(ptr, ptr + size);
    size = env->GetArrayLength(jnvec);
    ptr = env->GetIntArrayElements(jnvec, JNI_FALSE);
    vector<uint32_t> nvec(ptr, ptr + size);
    Encryptor encryptor(context, secret_key);
    auto pool = MemoryManager::GetPool();
    uint32_t coeff_count = parms.poly_modulus_degree();
    vector<Ciphertext> queries;
    queries.clear();
    uint32_t logsize, gap, dimension_size;
    uint64_t h, tt, plain_coeff;
    // handling first dimension
    dimension_size = nvec[0];
    logsize = ceil(log2(dimension_size));
    gap = ceil(coeff_count / (1 << logsize));
    uint32_t plain_decomp_size = 2;
    uint32_t plain_bits = parms.plain_modulus().bit_count();
    uint32_t plain_base =  parms.plain_modulus().bit_count() / plain_decomp_size;
    auto ptr1(seal::util::allocate_uint(2, pool));
    auto ptr2(seal::util::allocate_uint(2, pool));
    Plaintext pt(coeff_count);
    pt.set_zero();
    pt[indices[0] * gap] = 1;
    Ciphertext cipher;
    for (uint32_t i = 0; i < plain_decomp_size; i++) {
        uint32_t shift_amount = plain_bits - (i + 1) * plain_base;
        ptr2[0] = 0;
        ptr2[1] = 0;
        ptr1[0] = 1;
        ptr1[1] = 0;
        util::left_shift_uint128(ptr1.get(), (int) shift_amount, ptr2.get());
        h = seal::util::barrett_reduce_128(ptr2.get(), parms.plain_modulus().value());
        if (dimension_size > 0) {
            util::try_invert_uint_mod(dimension_size, parms.plain_modulus().value(),tt);
            h = util::multiply_uint_mod(h ,tt , parms.plain_modulus());
        }
        pt[indices[0] * gap] = h;
        encryptor.encrypt_symmetric(pt, cipher);
        queries.push_back(cipher);
    }
    if (indices.size() > 1) {
        // compressing all the remaining dimensions into one dimension of size equal to sum of remaining dimensions
        vector<uint32_t> new_indices;
        uint32_t new_dimension_size = 0;
        for (uint32_t i = 1; i < indices.size(); i++) {
            uint32_t real_index = indices[i] + new_dimension_size;
            new_indices.push_back(real_index);
            new_dimension_size = new_dimension_size + nvec[i];
        }
        pt.set_zero();
        dimension_size = new_dimension_size;
        logsize = ceil(log2(dimension_size * targetP::l_));
        gap = coeff_count / (1 << logsize);
        auto exception = env->FindClass("java/lang/Exception");
        if (gap == 0) {
            env->ThrowNew(exception, "dimension is too large!");
        }
        for (uint32_t real_index: new_indices) {
            pt[real_index * gap] = 1;
        }
        auto &coeff_modulus = context.first_context_data()->parms().coeff_modulus();
        encryptor.encrypt_zero_symmetric(cipher);
        uint32_t total_dim_with_gap = dimension_size * gap;
        uint32_t coeff_mod_count = coeff_modulus.size();
        for (uint32_t p = 0; p < targetP::l_; p++) {
            uint32_t shift_amount = targetP::digits - ((p + 1) * targetP::Bgbit);
            for (uint32_t i = 0; i < total_dim_with_gap; i++) {
                for (uint32_t j = 0; j < coeff_mod_count; j++) {
                    if ((1 << logsize) > 0) {
                        seal::util::try_invert_uint_mod(1 << logsize, coeff_modulus[j], tt);
                        plain_coeff = seal::util::multiply_uint_mod(pt.data()[i], tt, coeff_modulus[j]);
                    } else {
                        plain_coeff = pt.data()[i];
                    }
                    ptr2[0] = 0;
                    ptr2[1] = 0;
                    ptr1[0] = 1;
                    ptr1[1] = 0;
                    util::left_shift_uint128(ptr1.get(), (int) shift_amount, ptr2.get());
                    h = seal::util::barrett_reduce_128(ptr2.get(), coeff_modulus[j]);
                    h = seal::util::multiply_uint_mod(h, plain_coeff, coeff_modulus[j]);
                    uint32_t index = i + p * total_dim_with_gap + j * coeff_count;
                    cipher.data(0)[index] = seal::util::add_uint_mod(
                            cipher.data(0)[index], h, coeff_modulus[j]);
                }
            }
        }
        queries.push_back(cipher);
    }
    return serialize_ciphertexts(env, queries);
}

[[maybe_unused]] JNIEXPORT
jbyteArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_onionpir_Mcr21SingleIndexPirNativeUtils_generateReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray pk_bytes, jbyteArray galois_keys_bytes,
        jobject enc_sk_bytes, jobject query_bytes, jobject split_db_list, jintArray j_nec) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms, true, sec_level_type::none);
    Evaluator evaluator(context);
    PublicKey public_key = deserialize_public_key(env, pk_bytes, context);
    GaloisKeys* galois_keys = deserialize_galois_keys(env, galois_keys_bytes, context);
    vector<Ciphertext> query = deserialize_ciphertexts(env, query_bytes, context);
    vector<Ciphertext> enc_sk = deserialize_ciphertexts(env, enc_sk_bytes, context);
    vector<Ciphertext> first_query(2);
    first_query[0] = query[0];
    first_query[1] = query[1];
    jsize dimension_size = env->GetArrayLength(j_nec);
    jint *ptr = env->GetIntArrayElements(j_nec, JNI_FALSE);
    vector<uint32_t> nvec(ptr, ptr + dimension_size);
    uint32_t product = 1;
    for (uint32_t i : nvec) {
        product *= i;
    }
    // deserialize database
    jclass obj_class = env->FindClass("java/util/ArrayList");
    jmethodID get_method = env->GetMethodID(obj_class, "get", "(I)Ljava/lang/Object;");
    jmethodID size_method = env->GetMethodID(obj_class, "size", "()I");
    uint32_t split_db_list_size = env->CallIntMethod(split_db_list, size_method);
    vector<vector<uint64_t *>> split_db(split_db_list_size / 2);
    for (uint32_t i = 0; i < split_db_list_size; i++) {
        auto array = (jlongArray) env->CallObjectMethod(split_db_list, get_method, i);
        jlong* array_data = env->GetLongArrayElements(array, JNI_FALSE);
        split_db[i / 2].push_back((std::uint64_t *) array_data);
        env->DeleteLocalRef(array);
    }
    env->DeleteLocalRef(obj_class);
    // expand first dimension query
    vector<vector<Ciphertext>> list_enc;
    list_enc.resize(nvec[0], vector<Ciphertext>(2));
    auto list_enc_ptr = list_enc.begin();
    poc_expand_flat(list_enc_ptr, first_query, context, nvec[0], *galois_keys);
    auto exception = env->FindClass("java/lang/Exception");
    if (list_enc.size() != nvec[0]) {
        cerr << "size mismatch!" << list_enc.size() << ", " << nvec[0] << endl;
        env->ThrowNew(exception, "size mismatch!");
    }
    // handling first dimension
    vector<Ciphertext> first_dim_ctxt(product / nvec[0]);
    product /= nvec[0];
    for (uint32_t k = 0; k < product; k++) {
        first_dim_ctxt[k].resize(context, context.first_context_data()->parms_id(), 2);
        first_dim_ctxt[k] = decomp_mul(list_enc[0], split_db[k], context);
        for (uint32_t j = 1; j < nvec[0]; j++) {
            Ciphertext temp;
            temp.resize(context, context.first_context_data()->parms_id(), 2);
            temp = decomp_mul(list_enc[j], split_db[k + j * product], context);
            // Adds to first component.
            evaluator.add_inplace(first_dim_ctxt[k], temp);
        }
        evaluator.transform_from_ntt_inplace(first_dim_ctxt[k]);
    }
    if (nvec.size() > 1) {
        // expand remaining dimension queries
        TFHEcipher tfhe_cipher(context, public_key);
        vector<vector<Ciphertext>> remaining_queries;
        uint32_t new_dimension_size = 0, logsize;
        for (uint32_t i = 1; i < nvec.size(); i++) {
            new_dimension_size = new_dimension_size + nvec[i];
        }
        logsize = ceil(log2(new_dimension_size * targetP::l_));
        remaining_queries.resize(1 << logsize, vector<Ciphertext>(2 * targetP::l_));
        vector<Ciphertext> expanded_ciphers = poc_rlwe_expand(query[2], context, *galois_keys, 1 << logsize);
        for (uint32_t i = 0; i < targetP::l_; i++) {
            for (uint32_t j = 0; j < new_dimension_size; j++) {
                remaining_queries[j][i] = expanded_ciphers[j + i * new_dimension_size];
                Ciphertext res_ct(context);
                res_ct.resize(2);
                tfhe_cipher.ExternalProduct(res_ct, expanded_ciphers[j + i * new_dimension_size], enc_sk);
                res_ct.is_ntt_form() = true;
                remaining_queries[j][i + targetP::l_] = res_ct;
                evaluator.transform_to_ntt_inplace(remaining_queries[j][i]);
            }
        }
        // handling remaining dimensions
        uint32_t previous_dim = 0;
        for (uint32_t i = 1; i < nvec.size(); i++) {
            uint32_t n_i = nvec[i];
            product /= n_i;
            vector<Ciphertext> intermediate_ctxts(product);
            for (uint32_t k = 0; k < product; k++) {
                intermediate_ctxts[k].resize(context, context.first_context_data()->parms_id(), 2);
                tfhe_cipher.ExternalProduct(intermediate_ctxts[k], first_dim_ctxt[k], remaining_queries[previous_dim]);
                intermediate_ctxts[k].is_ntt_form() = true;
                for (uint32_t j = 1; j < n_i; j++) {
                    Ciphertext temp;
                    temp.resize(context, context.first_context_data()->parms_id(), 2);
                    tfhe_cipher.ExternalProduct(temp, first_dim_ctxt[k + j * product],
                                                remaining_queries[j + previous_dim]);
                    temp.is_ntt_form() = true;
                    // Adds to first component.
                    evaluator.add_inplace(intermediate_ctxts[k], temp);
                }
            }
            for (auto &intermediate_ctxt: intermediate_ctxts) {
                evaluator.transform_from_ntt_inplace(intermediate_ctxt);
            }
            first_dim_ctxt.clear();
            first_dim_ctxt = intermediate_ctxts;
            previous_dim = previous_dim + n_i;
        }
    }
    if (first_dim_ctxt.size() != 1) {
        cerr << "reply size mismatch!" << first_dim_ctxt.size() << ", " << 1 << endl;
        env->ThrowNew(exception, "reply size mismatch!");
    }
    return serialize_ciphertext(env, first_dim_ctxt[0]);
}

[[maybe_unused]] JNIEXPORT
jlongArray JNICALL Java_edu_alibaba_mpc4j_s2pc_pir_index_single_onionpir_Mcr21SingleIndexPirNativeUtils_decryptReply(
        JNIEnv *env, jclass, jbyteArray parms_bytes, jbyteArray sk_bytes, jbyteArray response_bytes) {
    EncryptionParameters parms = deserialize_encryption_parms(env, parms_bytes);
    SEALContext context(parms, true, sec_level_type::none);
    SecretKey secret_key = deserialize_secret_key(env, sk_bytes, context);
    Ciphertext response = deserialize_ciphertext(env, response_bytes, context);
    // decrypt response
    Decryptor decryptor(context, secret_key);
    uint32_t coeff_count = parms.poly_modulus_degree();
    Plaintext plaintext(coeff_count);
    decryptor.decrypt(response, plaintext);
    // get coefficients of plaintext
    jlongArray result = env->NewLongArray((jsize) coeff_count);
    jlong coeff_array[coeff_count];
    for (uint32_t i = 0; i < coeff_count; i++) {
        coeff_array[i] = (jlong) plaintext[i];
    }
    env->SetLongArrayRegion(result, 0, (jsize) coeff_count, coeff_array);
    return result;
}