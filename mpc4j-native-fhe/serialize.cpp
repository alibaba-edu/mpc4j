#include "serialize.h"

jbyteArray serialize_encryption_parms(JNIEnv *env, const EncryptionParameters& parms) {
    std::ostringstream output;
    parms.save(output, Serialization::compr_mode_default);
    jint len = (jint) output.str().size();
    jbyteArray byte_array = env->NewByteArray(len);
    env->SetByteArrayRegion(byte_array, 0, len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return byte_array;
}

seal::EncryptionParameters deserialize_encryption_parms(JNIEnv *env, jbyteArray parms_bytes) {
    jbyte* byte_array = env->GetByteArrayElements(parms_bytes, JNI_FALSE);
    std::string str((char*) byte_array, env->GetArrayLength(parms_bytes));
    std::istringstream input(str);
    seal::EncryptionParameters parms;
    parms.load(input);
    // free
    env->ReleaseByteArrayElements(parms_bytes, byte_array, 0);
    return parms;
}

jbyteArray serialize_public_key(JNIEnv *env, const PublicKey& public_key) {
    std::ostringstream output;
    public_key.save(output, Serialization::compr_mode_default);
    jint len = (jint) output.str().size();
    jbyteArray byte_array = env->NewByteArray(len);
    env->SetByteArrayRegion(byte_array, 0, len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return byte_array;
}

jbyteArray serialize_public_key(JNIEnv *env, const Serializable<PublicKey>& public_key) {
    std::ostringstream output;
    public_key.save(output, Serialization::compr_mode_default);
    jint len = (jint) output.str().size();
    jbyteArray byte_array = env->NewByteArray(len);
    env->SetByteArrayRegion(byte_array, 0, len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return byte_array;
}

PublicKey deserialize_public_key(JNIEnv *env, jbyteArray pk_bytes, const SEALContext& context) {
    jbyte* byte_array = env->GetByteArrayElements(pk_bytes, JNI_FALSE);
    string str((char*) byte_array, env->GetArrayLength(pk_bytes));
    istringstream input(str);
    PublicKey public_key;
    public_key.load(context, input);
    // free
    env->ReleaseByteArrayElements(pk_bytes, byte_array, 0);
    return public_key;
}

jbyteArray serialize_secret_key(JNIEnv *env, const SecretKey& secret_key) {
    std::ostringstream output;
    secret_key.save(output, Serialization::compr_mode_default);
    jint len = (jint) output.str().size();
    jbyteArray byte_array = env->NewByteArray(len);
    env->SetByteArrayRegion(byte_array, 0, len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return byte_array;
}

SecretKey deserialize_secret_key(JNIEnv *env, jbyteArray sk_bytes, const SEALContext& context) {
    jbyte* byte_array = env->GetByteArrayElements(sk_bytes, JNI_FALSE);
    std::string str((char*) byte_array, env->GetArrayLength(sk_bytes));
    std::istringstream input(str);
    seal::SecretKey secret_key;
    secret_key.load(context, input);
    // free
    env->ReleaseByteArrayElements(sk_bytes, byte_array, 0);
    return secret_key;
}

jbyteArray serialize_relin_keys(JNIEnv *env, const Serializable<RelinKeys>& relin_keys) {
    std::ostringstream output;
    relin_keys.save(output, Serialization::compr_mode_default);
    jint len = (jint) output.str().size();
    jbyteArray byte_array = env->NewByteArray(len);
    env->SetByteArrayRegion(byte_array, 0, len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return byte_array;
}

RelinKeys deserialize_relin_keys(JNIEnv *env, jbyteArray relin_keys_bytes, const SEALContext& context) {
    jbyte* byte_array = env->GetByteArrayElements(relin_keys_bytes, JNI_FALSE);
    string str((char*) byte_array, env->GetArrayLength(relin_keys_bytes));
    istringstream input(str);
    RelinKeys relin_keys;
    relin_keys.load(context, input);
    // free
    env->ReleaseByteArrayElements(relin_keys_bytes, byte_array, 0);
    return relin_keys;
}

jbyteArray serialize_galois_keys(JNIEnv *env, const Serializable<GaloisKeys>& galois_keys) {
    std::ostringstream output;
    galois_keys.save(output, Serialization::compr_mode_default);
    jint len = (jint) output.str().size();
    jbyteArray byte_array = env->NewByteArray(len);
    env->SetByteArrayRegion(byte_array, 0, len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return byte_array;
}

GaloisKeys* deserialize_galois_keys(JNIEnv *env, jbyteArray galois_keys_bytes, const SEALContext& context) {
    jbyte* byte_array = env->GetByteArrayElements(galois_keys_bytes, JNI_FALSE);
    string str((char*) byte_array, env->GetArrayLength(galois_keys_bytes));
    istringstream input(str);
    auto *galois_keys = new GaloisKeys();
    galois_keys->load(context, input);
    // free
    env->ReleaseByteArrayElements(galois_keys_bytes, byte_array, 0);
    return galois_keys;
}

jbyteArray serialize_ciphertext(JNIEnv *env, const Ciphertext& ciphertext) {
    std::ostringstream output;
    ciphertext.save(output, Serialization::compr_mode_default);
    jint len = (jint) output.str().size();
    jbyteArray byte_array = env->NewByteArray(len);
    env->SetByteArrayRegion(byte_array, 0, len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return byte_array;
}

jbyteArray serialize_ciphertext(JNIEnv *env, const Serializable<Ciphertext>& ciphertext) {
    std::ostringstream output;
    ciphertext.save(output, Serialization::compr_mode_default);
    jint len = (jint) output.str().size();
    jbyteArray byte_array = env->NewByteArray(len);
    env->SetByteArrayRegion(byte_array, 0, len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return byte_array;
}

Ciphertext deserialize_ciphertext(JNIEnv *env, jbyteArray ciphertext_bytes, const SEALContext& context) {
    jbyte* byte_array = env->GetByteArrayElements(ciphertext_bytes, JNI_FALSE);
    std::string str((char*) byte_array, env->GetArrayLength(ciphertext_bytes));
    std::istringstream input(str);
    Ciphertext ciphertext;
    ciphertext.load(context, input);
    // free
    env->ReleaseByteArrayElements(ciphertext_bytes, byte_array, 0);
    return ciphertext;
}

jobject serialize_ciphertexts(JNIEnv *env, const vector<Ciphertext>& ciphertexts) {
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    for (auto & ciphertext : ciphertexts) {
        jbyteArray byte_array = serialize_ciphertext(env, ciphertext);
        env->CallBooleanMethod(list_obj, list_add, byte_array);
        env->DeleteLocalRef(byte_array);
    }
    // free
    env->DeleteLocalRef(list_jcs);
    return list_obj;
}

jobject serialize_ciphertexts(JNIEnv *env, const vector<Serializable<Ciphertext>>& ciphertexts) {
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    for (auto & ciphertext : ciphertexts) {
        jbyteArray byte_array = serialize_ciphertext(env, ciphertext);
        env->CallBooleanMethod(list_obj, list_add, byte_array);
        env->DeleteLocalRef(byte_array);
    }
    // free
    env->DeleteLocalRef(list_jcs);
    return list_obj;
}

vector<Ciphertext> deserialize_ciphertexts(JNIEnv *env, jobject ciphertext_list, const SEALContext& context) {
    jclass obj_class = env->FindClass("java/util/ArrayList");
    jmethodID get_method = env->GetMethodID(obj_class, "get", "(I)Ljava/lang/Object;");
    jmethodID size_method = env->GetMethodID(obj_class, "size", "()I");
    int size = env->CallIntMethod(ciphertext_list, size_method);
    vector<Ciphertext> result(size);
    for (uint32_t i = 0; i < size; i++) {
        auto ciphertext_bytes = (jbyteArray) env->CallObjectMethod(ciphertext_list, get_method, i);
        result[i] = deserialize_ciphertext(env, ciphertext_bytes, context);
        env->DeleteLocalRef(ciphertext_bytes);
    }
    // free
    env->DeleteLocalRef(obj_class);
    return result;
}

jbyteArray serialize_plaintext(JNIEnv *env, const Plaintext& plaintext) {
    std::ostringstream output;
    plaintext.save(output, Serialization::compr_mode_default);
    jint len = (jint) output.str().size();
    jbyteArray byte_array = env->NewByteArray(len);
    env->SetByteArrayRegion(byte_array, 0, len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return byte_array;
}

Plaintext deserialize_plaintext(JNIEnv *env, jbyteArray bytes, const SEALContext& context) {
    jbyte *byte_array = env->GetByteArrayElements(bytes, JNI_FALSE);
    std::string str((char *) byte_array, env->GetArrayLength(bytes));
    std::istringstream input(str);
    seal::Plaintext plaintext;
    plaintext.load(context, input);
    // free
    env->ReleaseByteArrayElements(bytes, byte_array, 0);
    return plaintext;
}

jobject serialize_plaintexts(JNIEnv *env, const vector<Plaintext>& plaintexts) {
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    for (auto & plaintext : plaintexts) {
        jbyteArray plaintext_bytes = serialize_plaintext(env, plaintext);
        env->CallBooleanMethod(list_obj, list_add, plaintext_bytes);
        env->DeleteLocalRef(plaintext_bytes);
    }
    // free
    env->DeleteLocalRef(list_jcs);
    return list_obj;
}

vector<Plaintext> deserialize_plaintexts(JNIEnv *env, jobjectArray array, const SEALContext& context) {
    BatchEncoder encoder(context);
    jint size = env->GetArrayLength(array);
    vector<Plaintext> plaintexts(size);
    for (jint i = 0; i < size; i++) {
        auto row = (jlongArray) env->GetObjectArrayElement(array, i);
        jlong* ptr = env->GetLongArrayElements(row, JNI_FALSE);
        vector<uint64_t> temp_vec(ptr, ptr + env->GetArrayLength(row));
        encoder.encode(temp_vec, plaintexts[i]);
        env->ReleaseLongArrayElements(row, ptr, 0);
    }
    return plaintexts;
}

vector<Plaintext> deserialize_plaintexts_array(JNIEnv *env, jobjectArray array, const SEALContext& context) {
    jint size = env->GetArrayLength(array);
    vector<Plaintext> plaintexts;
    for (jint i = 0; i < size; i++) {
        auto row = (jbyteArray) env->GetObjectArrayElement(array, i);
        plaintexts.push_back(deserialize_plaintext(env, row, context));
        env->DeleteLocalRef(row);
    }
    return plaintexts;
}

vector<Plaintext> deserialize_plaintexts(JNIEnv *env, jobject list, const SEALContext& context) {
    jclass obj_class = env->FindClass("java/util/ArrayList");
    jmethodID get_method = env->GetMethodID(obj_class, "get", "(I)Ljava/lang/Object;");
    jmethodID size_method = env->GetMethodID(obj_class, "size", "()I");
    jint size = env->CallIntMethod(list, size_method);
    vector<Plaintext> plaintexts;
    plaintexts.reserve(size);
    for (jint i = 0; i < size; i++) {
        auto plaintext_bytes = (jbyteArray) env->CallObjectMethod(list, get_method, i);
        plaintexts.push_back(deserialize_plaintext(env, plaintext_bytes, context));
        env->DeleteLocalRef(plaintext_bytes);
    }
    // free
    env->DeleteLocalRef(obj_class);
    return plaintexts;
}

Plaintext deserialize_plaintext_from_coeff(JNIEnv *env, jlongArray coeffs, const SEALContext& context) {
    BatchEncoder encoder(context);
    jint size = env->GetArrayLength(coeffs);
    jlong *ptr = env->GetLongArrayElements(coeffs, JNI_FALSE);
    vector<uint64_t> enc(ptr, ptr + size);
    Plaintext plaintext(context.first_context_data()->parms().poly_modulus_degree());
    encoder.encode(enc, plaintext);
    // free
    env->ReleaseLongArrayElements(coeffs, ptr, 0);
    return plaintext;
}

vector<Plaintext> deserialize_plaintexts_from_coeff(JNIEnv *env, jobjectArray coeffs_list, const SEALContext& context) {
    jint size = env->GetArrayLength(coeffs_list);
    vector<Plaintext> plaintexts(size);
    for (jint i = 0; i < size; i++) {
        auto coeffs = (jlongArray) env->GetObjectArrayElement(coeffs_list, i);
        plaintexts[i] = deserialize_plaintext_from_coeff(env, coeffs, context);
        env->DeleteLocalRef(coeffs);
    }
    return plaintexts;
}

vector<Plaintext> deserialize_plaintexts_from_coeff_without_batch_encode(JNIEnv *env, jobject coeff_list,
                                                                         const SEALContext& context) {
    jclass obj_class = env->FindClass("java/util/ArrayList");
    jmethodID get_method = env->GetMethodID(obj_class, "get", "(I)Ljava/lang/Object;");
    jmethodID size_method = env->GetMethodID(obj_class, "size", "()I");
    jint size = env->CallIntMethod(coeff_list, size_method);
    vector<Plaintext> plaintexts;
    plaintexts.reserve(size);
    for (jint i = 0; i < size; i++) {
        Plaintext plaintext(context.first_context_data()->parms().poly_modulus_degree());
        auto coeffs = (jlongArray) env->CallObjectMethod(coeff_list, get_method, i);
        jint len = env->GetArrayLength(coeffs);
        jlong *ptr = env->GetLongArrayElements(coeffs, JNI_FALSE);
        vector<uint64_t> vec(ptr, ptr + len);
        for (jint j = 0; j < len; j++) {
            plaintext[j] = vec[j];
        }
        plaintexts.push_back(plaintext);
        env->DeleteLocalRef(coeffs);
    }
    // free
    env->DeleteLocalRef(obj_class);
    return plaintexts;
}