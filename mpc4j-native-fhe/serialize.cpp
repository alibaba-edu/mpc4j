#include "serialize.h"

jobject serialize_public_key_secret_key(JNIEnv *env, const EncryptionParameters& parms,
                                        const Serializable<PublicKey>& public_key, const SecretKey& secret_key) {
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    jbyteArray parms_bytes = serialize_encryption_parms(env, parms);
    jbyteArray pk_byte = serialize_public_key(env, public_key);
    jbyteArray sk_byte = serialize_secret_key(env, secret_key);
    env->CallBooleanMethod(list_obj, list_add, parms_bytes);
    env->CallBooleanMethod(list_obj, list_add, pk_byte);
    env->CallBooleanMethod(list_obj, list_add, sk_byte);
    return list_obj;
}

jobject serialize_public_key_secret_key(JNIEnv *env, const Serializable<PublicKey>& public_key,
                                        const SecretKey& secret_key) {
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

jobject serialize_relin_public_secret_keys(JNIEnv *env, const EncryptionParameters& parms,
                                           const Serializable<RelinKeys>& relin_keys,
                                           const Serializable<PublicKey>& public_key, const SecretKey& secret_key) {
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    jbyteArray params_bytes = serialize_encryption_parms(env, parms);
    jbyteArray pk_byte = serialize_public_key(env, public_key);
    jbyteArray relin_keys_byte = serialize_relin_key(env, relin_keys);
    jbyteArray sk_byte = serialize_secret_key(env, secret_key);
    env->CallBooleanMethod(list_obj, list_add, params_bytes);
    env->CallBooleanMethod(list_obj, list_add, relin_keys_byte);
    env->CallBooleanMethod(list_obj, list_add, pk_byte);
    env->CallBooleanMethod(list_obj, list_add, sk_byte);
    return list_obj;
}

jbyteArray serialize_secret_key(JNIEnv *env, const SecretKey& secret_key) {
    std::ostringstream output;
    secret_key.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

jbyteArray serialize_relin_key(JNIEnv *env, const Serializable<RelinKeys>& relin_keys) {
    std::ostringstream output;
    relin_keys.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

jbyteArray serialize_public_key(JNIEnv *env, const Serializable<PublicKey>& public_key) {
    std::ostringstream output;
    public_key.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

jbyteArray serialize_ciphertext(JNIEnv *env, const Ciphertext& ciphertext) {
    std::ostringstream output;
    ciphertext.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

jbyteArray serialize_ciphertext(JNIEnv *env, const Serializable<Ciphertext>& ciphertext) {
    std::ostringstream output;
    ciphertext.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

jbyteArray serialize_plaintext(JNIEnv *env, const Plaintext& plaintext) {
    std::ostringstream output;
    plaintext.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

PublicKey deserialize_public_key(JNIEnv *env, jbyteArray pk_bytes, const SEALContext& context) {
    jbyte* pk_byte_data = env->GetByteArrayElements(pk_bytes, JNI_FALSE);
    std::string str((char*)pk_byte_data, env->GetArrayLength(pk_bytes));
    std::istringstream input(str);
    seal::PublicKey public_key;
    public_key.load(context, input);
    // free
    env->ReleaseByteArrayElements(pk_bytes, pk_byte_data, 0);
    return public_key;
}

RelinKeys deserialize_relin_key(JNIEnv *env, jbyteArray relin_key_bytes, const SEALContext& context) {
    jbyte* bytes = env->GetByteArrayElements(relin_key_bytes, JNI_FALSE);
    string str((char*)bytes, env->GetArrayLength(relin_key_bytes));
    istringstream input(str);
    RelinKeys relin_keys;
    relin_keys.load(context, input);
    // free
    env->ReleaseByteArrayElements(relin_key_bytes, bytes, 0);
    return relin_keys;
}

SecretKey deserialize_secret_key(JNIEnv *env, jbyteArray sk_bytes, const SEALContext& context) {
    jbyte* pk_byte_data = env->GetByteArrayElements(sk_bytes, JNI_FALSE);
    std::string str((char*)pk_byte_data, env->GetArrayLength(sk_bytes));
    std::istringstream input(str);
    seal::SecretKey secret_key;
    secret_key.load(context, input);
    // free
    env->ReleaseByteArrayElements(sk_bytes, pk_byte_data, 0);
    return secret_key;
}

Ciphertext deserialize_ciphertext(JNIEnv *env, jbyteArray bytes, const SEALContext& context) {
    jbyte* byte_data = env->GetByteArrayElements(bytes, JNI_FALSE);
    std::string str((char*)byte_data, env->GetArrayLength(bytes));
    std::istringstream input(str);
    Ciphertext ciphertext;
    ciphertext.load(context, input);
    // free
    env->ReleaseByteArrayElements(bytes, byte_data, 0);
    return ciphertext;
}

Plaintext deserialize_plaintext_from_byte(JNIEnv *env, jbyteArray bytes, const SEALContext& context) {
    jbyte *byte_data = env->GetByteArrayElements(bytes, JNI_FALSE);
    std::string str((char *) byte_data, env->GetArrayLength(bytes));
    std::istringstream input(str);
    seal::Plaintext plaintext;
    plaintext.load(context, input);
    // free
    env->ReleaseByteArrayElements(bytes, byte_data, 0);
    return plaintext;
}

Plaintext deserialize_plaintext_from_coeff(JNIEnv *env, jlongArray coeffs, const SEALContext& context) {
    BatchEncoder encoder(context);
    jsize size = env->GetArrayLength(coeffs);
    jlong *ptr = env->GetLongArrayElements(coeffs, JNI_FALSE);
    vector<uint64_t> enc(ptr, ptr + size);
    Plaintext plaintext;
    encoder.encode(enc, plaintext);
    // free
    env->ReleaseLongArrayElements(coeffs, ptr, 0);
    return plaintext;
}


vector<Ciphertext> deserialize_ciphertexts(JNIEnv *env, jobject list, const SEALContext& context) {
    jclass obj_class = env->FindClass("java/util/ArrayList");
    jmethodID get_method = env->GetMethodID(obj_class, "get", "(I)Ljava/lang/Object;");
    jmethodID size_method = env->GetMethodID(obj_class, "size", "()I");
    int size = env->CallIntMethod(list, size_method);
    vector<Ciphertext> result;
    result.reserve(size);
    for (uint32_t i = 0; i < size; i++) {
        auto bytes = (jbyteArray) env->CallObjectMethod(list, get_method, i);
        result.push_back(deserialize_ciphertext(env, bytes, context));
        env->DeleteLocalRef(bytes);
    }
    // free
    env->DeleteLocalRef(obj_class);
    return result;
}

vector<Plaintext> deserialize_plaintexts_from_byte(JNIEnv *env, jobject list, const SEALContext& context) {
    jclass obj_class = env->FindClass("java/util/ArrayList");
    jmethodID get_method = env->GetMethodID(obj_class, "get", "(I)Ljava/lang/Object;");
    jmethodID size_method = env->GetMethodID(obj_class, "size", "()I");
    int size = env->CallIntMethod(list, size_method);
    vector<Plaintext> result;
    result.reserve(size);
    for (uint32_t i = 0; i < size; i++) {
        auto bytes = (jbyteArray) env->CallObjectMethod(list, get_method, i);
        result.push_back(deserialize_plaintext_from_byte(env, bytes, context));
        env->DeleteLocalRef(bytes);
    }
    // free
    env->DeleteLocalRef(obj_class);
    return result;
}

vector<Plaintext> deserialize_plaintexts_from_coeff(JNIEnv *env, jobjectArray coeffs_list, const SEALContext& context) {
    int size = env->GetArrayLength(coeffs_list);
    vector<Plaintext> plaintexts(size);
    for (int i = 0; i < size; i++) {
        auto coeffs = (jlongArray) env->GetObjectArrayElement(coeffs_list, i);
        plaintexts[i] = deserialize_plaintext_from_coeff(env, coeffs, context);
        env->DeleteLocalRef(coeffs);
    }
    return plaintexts;
}

vector<Plaintext> deserialize_plaintext_from_coefficients(JNIEnv *env, jobject coeff_list, const SEALContext& context,
                                                          uint32_t poly_modulus_degree) {
    jclass obj_class = env->FindClass("java/util/ArrayList");
    jmethodID get_method = env->GetMethodID(obj_class, "get", "(I)Ljava/lang/Object;");
    jmethodID size_method = env->GetMethodID(obj_class, "size", "()I");
    int size = env->CallIntMethod(coeff_list, size_method);
    vector<Plaintext> result;
    result.reserve(size);
    for (uint32_t i = 0; i < size; i++) {
        Plaintext plaintext(poly_modulus_degree);
        auto coeffs = (jlongArray) env->CallObjectMethod(coeff_list, get_method, i);
        uint32_t len = env->GetArrayLength(coeffs);
        jlong *ptr = env->GetLongArrayElements(coeffs, JNI_FALSE);
        vector<uint64_t> vec(ptr, ptr + len);
        for (int j = 0; j < len; j++) {
            plaintext[j] = vec[j];
        }
        result.push_back(plaintext);
        env->DeleteLocalRef(coeffs);
    }
    // free
    env->DeleteLocalRef(obj_class);
    return result;
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

jobject serialize_plaintexts(JNIEnv *env, const vector<Plaintext>& plaintexts) {
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    for (auto & plaintext : plaintexts) {
        jbyteArray byte_array = serialize_plaintext(env, plaintext);
        env->CallBooleanMethod(list_obj, list_add, byte_array);
        env->DeleteLocalRef(byte_array);
    }
    // free
    env->DeleteLocalRef(list_jcs);
    return list_obj;
}

vector<Plaintext> deserialize_plaintexts(JNIEnv *env, jobjectArray array, const SEALContext& context) {
    BatchEncoder encoder(context);
    uint32_t size = env->GetArrayLength(array);
    vector<Plaintext> result;
    result.resize(size);
    for (int i = 0; i < size; i++) {
        auto row = (jlongArray) env->GetObjectArrayElement(array, i);
        jlong* ptr = env->GetLongArrayElements(row, JNI_FALSE);
        vector<uint64_t> temp_vec(ptr, ptr + env->GetArrayLength(row));
        encoder.encode(temp_vec, result[i]);
        env->ReleaseLongArrayElements(row, ptr, 0);
    }
    return result;
}

jbyteArray serialize_encryption_parms(JNIEnv *env, const EncryptionParameters& params) {
    std::ostringstream output;
    params.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(
            result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

seal::EncryptionParameters deserialize_encryption_params(JNIEnv *env, jbyteArray params_bytes) {
    jbyte* params_byte_data = env->GetByteArrayElements(params_bytes, JNI_FALSE);
    std::string str((char*)params_byte_data, env->GetArrayLength(params_bytes));
    std::istringstream input(str);
    seal::EncryptionParameters params;
    params.load(input);
    // free
    env->ReleaseByteArrayElements(params_bytes, params_byte_data, 0);
    return params;
}

jlongArray get_plaintext_coeffs(JNIEnv *env, Plaintext plaintext) {
    jlongArray result = env->NewLongArray((jsize) plaintext.coeff_count());
    jlong coeff_array[plaintext.coeff_count()];
    for (int i = 0; i < plaintext.coeff_count(); i++) {
        coeff_array[i] = (jlong) plaintext[i];
    }
    env->SetLongArrayRegion(result, 0, (jsize) plaintext.coeff_count(), coeff_array);
    return result;
}
