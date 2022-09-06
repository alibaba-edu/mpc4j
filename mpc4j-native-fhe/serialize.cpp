//
// Created by pengliqiang on 2022/5/26.
//

#include "serialize.h"

jbyteArray serialize_secret_key(JNIEnv *env, const SecretKey& secret_key) {
    std::ostringstream output;
    secret_key.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

jbyteArray serialize_relin_key(JNIEnv *env, const Serializable<RelinKeys>& relin_keys) {
    std::ostringstream output;
    relin_keys.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

jbyteArray serialize_public_key(JNIEnv *env, const Serializable<PublicKey>& public_key) {
    std::ostringstream output;
    public_key.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

jbyteArray serialize_ciphertext(JNIEnv *env, const Ciphertext& ciphertext) {
    std::ostringstream output;
    ciphertext.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

jbyteArray serialize_ciphertext(JNIEnv *env, const Serializable<Ciphertext>& ciphertext) {
    std::ostringstream output;
    ciphertext.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
    return result;
}

PublicKey deserialize_public_key(JNIEnv *env, jbyteArray pk_bytes, const SEALContext& context) {
    jbyte* pk_byte_data = env->GetByteArrayElements(pk_bytes, JNI_FALSE);
    std::string str((char*)pk_byte_data, env->GetArrayLength(pk_bytes));
    std::istringstream input(str);
    seal::PublicKey public_key;
    public_key.load(context, input);
    if (!is_metadata_valid_for(public_key, context)) {
        cerr << "invalid public key for this SEALContext" << endl;
        exit(-1);
    }

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
    if (!is_metadata_valid_for(relin_keys, context)) {
        cerr << "invalid relinearization key for this SEALContext" << endl;
        exit(-1);
    }

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
    if (!is_metadata_valid_for(secret_key, context)) {
        cerr << "invalid secret key for this SEALContext" << endl;
        exit(-1);
    }

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
    if (!is_metadata_valid_for(ciphertext, context)) {
        cerr << "invalid ciphertext for this SEALContext" << endl;
        exit(-1);
    }

    // free
    env->ReleaseByteArrayElements(bytes, byte_data, 0);
    return ciphertext;
}

Plaintext deserialize_plaintext(JNIEnv *env, jbyteArray bytes, const SEALContext& context) {
    jbyte *ptxt_byte_data = env->GetByteArrayElements(bytes, JNI_FALSE);
    std::string str((char *) ptxt_byte_data, env->GetArrayLength(bytes));
    std::istringstream input(str);
    seal::Plaintext plaintext;
    plaintext.load(context, input);
    if (!is_metadata_valid_for(plaintext, context)) {
        cerr << "invalid plaintext for this SEALContext" << endl;
        exit(-1);
    }

    // free
    env->ReleaseByteArrayElements(bytes, ptxt_byte_data, 0);
    return plaintext;
}

vector<Ciphertext> deserialize_ciphertexts(JNIEnv *env, jobject list, const SEALContext& context) {
    jclass obj_class = env->FindClass("java/util/ArrayList");
    if (obj_class == nullptr) {
        std::cout << "ArrayList not found !" << std::endl;
        return static_cast<std::vector<seal::Ciphertext>>(0);

    }
    jmethodID get_method = env->GetMethodID(obj_class, "get", "(I)Ljava/lang/Object;");
    jmethodID size_method = env->GetMethodID(obj_class, "size", "()I");
    if (get_method == nullptr || size_method == nullptr) {
        std::cout << "'get' or 'size' method not found !" << std::endl;
        return static_cast<std::vector<seal::Ciphertext>>(0);
    }
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

jobject serialize_ciphertexts(JNIEnv *env, const vector<Ciphertext>& ciphertexts) {
    // 获取ArrayList类引用
    jclass list_jcs = env->FindClass("java/util/ArrayList");
    if (list_jcs == nullptr) {
        std::cout << "ArrayList not found !" << std::endl;
        return nullptr;
    }
    // 获取ArrayList构造函数id
    jmethodID list_init = env->GetMethodID(list_jcs, "<init>", "()V");
    // 创建一个ArrayList对象
    jobject list_obj = env->NewObject(list_jcs, list_init, "");
    // 获取ArrayList对象的add()的methodID
    jmethodID list_add = env->GetMethodID(list_jcs, "add", "(Ljava/lang/Object;)Z");
    if (list_add == nullptr || list_init == nullptr) {
        std::cout << "'init' or 'add' method not found !" << std::endl;
        return nullptr;
    }
    // 明文多项式转化为字节数组
    for (auto & ciphertext : ciphertexts) {
        jbyteArray byte_array = serialize_ciphertext(env, ciphertext);
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

jbyteArray serialize_encryption_params(JNIEnv *env, const EncryptionParameters& params) {
    std::ostringstream output;
    params.save(output, Serialization::compr_mode_default);
    uint32_t len = output.str().size();
    jbyteArray result = env->NewByteArray((jsize) len);
    env->SetByteArrayRegion(result, 0, (jsize) len, reinterpret_cast<const jbyte *>(output.str().c_str()));
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
