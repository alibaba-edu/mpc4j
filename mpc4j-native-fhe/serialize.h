//
// Created by pengliqiang on 2022/5/26.
//
#include <jni.h>
#include "seal/seal.h"

using namespace seal;
using namespace std;

// serialize encryption parameters
jbyteArray serialize_encryption_parms(JNIEnv *env, const EncryptionParameters& params);
// deserialize encryption parameters
EncryptionParameters deserialize_encryption_parms(JNIEnv *env, jbyteArray parms_bytes);
// serialize public key
jbyteArray serialize_public_key(JNIEnv *env, const PublicKey& public_key);
jbyteArray serialize_public_key(JNIEnv *env, const Serializable<PublicKey>& public_key);
// deserialize public key
PublicKey deserialize_public_key(JNIEnv *env, jbyteArray pk_bytes, const SEALContext& context);
// serialize secret key
jbyteArray serialize_secret_key(JNIEnv *env, const SecretKey& secret_key);
// deserialize secret key
SecretKey deserialize_secret_key(JNIEnv *env, jbyteArray sk_bytes, const SEALContext& context);
// serialize relin keys
jbyteArray serialize_relin_keys(JNIEnv *env, const Serializable<RelinKeys>& relin_keys);
// deserialize relin keys
RelinKeys deserialize_relin_keys(JNIEnv *env, jbyteArray relin_key_bytes, const SEALContext& context);
// serialize galois keys
jbyteArray serialize_galois_keys(JNIEnv *env, const Serializable<GaloisKeys>& galois_keys);
// deserialize galois keys
GaloisKeys* deserialize_galois_keys(JNIEnv *env, jbyteArray galois_key_bytes, const SEALContext& context);
// serialize ciphertext
jbyteArray serialize_ciphertext(JNIEnv *env, const Ciphertext& ciphertext);
jbyteArray serialize_ciphertext(JNIEnv *env, const Serializable<Ciphertext>& ciphertext);
// deserialize ciphertext
Ciphertext deserialize_ciphertext(JNIEnv *env, jbyteArray ciphertext_bytes, const SEALContext& context);
// serialize ciphertexts
jobject serialize_ciphertexts(JNIEnv *env, const vector<Ciphertext>& ciphertexts);
jobject serialize_ciphertexts(JNIEnv *env, const vector<Serializable<Ciphertext>>& ciphertexts);
// deserialize ciphertexts
vector<Ciphertext> deserialize_ciphertexts(JNIEnv *env, jobject ciphertext_list, const SEALContext& context);
// serialize plaintext
jbyteArray serialize_plaintext(JNIEnv *env, const Plaintext& plaintext);
// deserialize plaintext
Plaintext deserialize_plaintext(JNIEnv *env, jbyteArray bytes, const SEALContext& context);
// serialize plaintexts
jobject serialize_plaintexts(JNIEnv *env, const vector<Plaintext>& plaintexts);
// deserialize plaintexts
vector<Plaintext> deserialize_plaintexts(JNIEnv *env, jobjectArray list, const SEALContext& context);
// deserialize plaintexts
vector<Plaintext> deserialize_plaintexts(JNIEnv *env, jobject list, const SEALContext& context);
// deserialize plaintexts from byte array
vector<Plaintext> deserialize_plaintexts_array(JNIEnv *env, jobjectArray array, const SEALContext& context);
// deserialize plaintext from coefficients
Plaintext deserialize_plaintext_from_coeff(JNIEnv *env, jlongArray coeffs, const SEALContext& context);
// deserialize plaintexts from coefficients
vector<Plaintext> deserialize_plaintexts_from_coeff(JNIEnv *env, jobjectArray coeffs_list, const SEALContext& context);
// deserialize plaintext from coefficients without encode
vector<Plaintext> deserialize_plaintexts_from_coeff_without_batch_encode(JNIEnv *env, jobject coeff_list, const SEALContext& context);

