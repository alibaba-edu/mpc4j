//
// Created by pengliqiang on 2022/5/26.
//
#include <jni.h>
#include "seal/seal.h"

using namespace seal;
using namespace std;

jbyteArray serialize_secret_key(JNIEnv *env, const SecretKey& secret_key);

jbyteArray serialize_public_key(JNIEnv *env, const Serializable<PublicKey>& public_key);

PublicKey deserialize_public_key(JNIEnv *env, jbyteArray pk_bytes, const SEALContext& context);

SecretKey deserialize_secret_key(JNIEnv *env, jbyteArray sk_bytes, const SEALContext& context);

jbyteArray serialize_encryption_params(JNIEnv *env, const EncryptionParameters& params);

EncryptionParameters deserialize_encryption_params(JNIEnv *env, jbyteArray params_bytes);

jbyteArray serialize_ciphertext(JNIEnv *env, const Ciphertext& ciphertext);

Ciphertext deserialize_ciphertext(JNIEnv *env, jbyteArray bytes, const SEALContext& context);

vector<Ciphertext> deserialize_ciphertexts(JNIEnv *env, jobject list, const SEALContext& context);

jobject serialize_ciphertexts(JNIEnv *env, const vector<Ciphertext>& ciphertexts);

jbyteArray serialize_ciphertext(JNIEnv *env, const Serializable<Ciphertext>& ciphertext);

vector<Plaintext> deserialize_plaintexts(JNIEnv *env, jobjectArray list, const SEALContext& context);

Plaintext deserialize_plaintext(JNIEnv *env, jbyteArray bytes, const SEALContext& context);

RelinKeys deserialize_relin_key(JNIEnv *env, jbyteArray relin_key_bytes, const SEALContext& context);

jbyteArray serialize_relin_key(JNIEnv *env, const Serializable<RelinKeys>& relin_keys);


