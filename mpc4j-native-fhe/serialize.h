//
// Created by pengliqiang on 2022/5/26.
//
#include <jni.h>
#include "seal/seal.h"

using namespace seal;
using namespace std;

jobject serialize_public_key_secret_key(JNIEnv *env, const EncryptionParameters& parms, const Serializable<PublicKey>& public_key, const SecretKey& secret_key);

jobject serialize_public_key_secret_key(JNIEnv *env, const Serializable<PublicKey>& public_key, const SecretKey& secret_key);

jobject serialize_relin_public_secret_keys(JNIEnv *env, const EncryptionParameters& parms, const Serializable<RelinKeys>& relin_keys, const Serializable<PublicKey>& public_key, const SecretKey& secret_key);

vector<Plaintext> deserialize_plaintexts_from_coeff(JNIEnv *env, jobjectArray coeffs_list, const SEALContext& context);

jbyteArray serialize_secret_key(JNIEnv *env, const SecretKey& secret_key);

jbyteArray serialize_public_key(JNIEnv *env, const Serializable<PublicKey>& public_key);

PublicKey deserialize_public_key(JNIEnv *env, jbyteArray pk_bytes, const SEALContext& context);

SecretKey deserialize_secret_key(JNIEnv *env, jbyteArray sk_bytes, const SEALContext& context);

jbyteArray serialize_encryption_parms(JNIEnv *env, const EncryptionParameters& params);

EncryptionParameters deserialize_encryption_params(JNIEnv *env, jbyteArray params_bytes);

jbyteArray serialize_ciphertext(JNIEnv *env, const Ciphertext& ciphertext);

Ciphertext deserialize_ciphertext(JNIEnv *env, jbyteArray bytes, const SEALContext& context);

vector<Ciphertext> deserialize_ciphertexts(JNIEnv *env, jobject list, const SEALContext& context);

jobject serialize_ciphertexts(JNIEnv *env, const vector<Ciphertext>& ciphertexts);

jbyteArray serialize_ciphertext(JNIEnv *env, const Serializable<Ciphertext>& ciphertext);

jobject serialize_ciphertexts(JNIEnv *env, const vector<Serializable<Ciphertext>>& ciphertexts);

vector<Plaintext> deserialize_plaintexts(JNIEnv *env, jobjectArray list, const SEALContext& context);

Plaintext deserialize_plaintext_from_byte(JNIEnv *env, jbyteArray bytes, const SEALContext& context);

jbyteArray serialize_plaintext(JNIEnv *env, const Plaintext& plaintext);

RelinKeys deserialize_relin_key(JNIEnv *env, jbyteArray relin_key_bytes, const SEALContext& context);

jbyteArray serialize_relin_key(JNIEnv *env, const Serializable<RelinKeys>& relin_keys);

vector<Plaintext> deserialize_plaintext_from_coefficients(JNIEnv *env, jobject coeff_list, const SEALContext& context,
                                                          uint32_t poly_modulus_degree);

jobject serialize_plaintexts(JNIEnv *env, const vector<Plaintext>& plaintexts);

vector<Plaintext> deserialize_plaintexts_from_byte(JNIEnv *env, jobject list, const SEALContext& context);

jlongArray get_plaintext_coeffs(JNIEnv *env, Plaintext plaintext);