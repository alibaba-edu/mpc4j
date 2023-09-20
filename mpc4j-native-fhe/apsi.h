/*
 * Created by pengliqiang on 2022/7/14.
 * This implementation is based on the public domain SealPIR in APSI project:
 * <p>
 * https://github.com/microsoft/APSI/blob/95ff2cbad3e523e3788a5f8e4baf4638fbf0c6c7/sender/apsi/bin_bundle.cpp
 * </p>
 */

#include <jni.h>
#include "seal/seal.h"

using namespace seal;
using namespace std;

#ifndef MPC3J_NATIVE_FHE_APSI_H
#define MPC3J_NATIVE_FHE_APSI_H

[[maybe_unused]] vector<Ciphertext> compute_encrypted_powers(const EncryptionParameters& parms, vector<Ciphertext> query, vector<vector<uint32_t>> parent_powers,
                                            vector<uint32_t> source_power_index, uint32_t ps_low_power, const RelinKeys& relin_keys);

Ciphertext polynomial_evaluation(const EncryptionParameters& parms, vector<Ciphertext> encrypted_powers, vector<Plaintext> coeff_plaintexts, const PublicKey& public_key);

Ciphertext polynomial_evaluation(const EncryptionParameters& parms, vector<Ciphertext> encrypted_powers, vector<Plaintext> coeff_plaintexts,
                                 uint32_t ps_low_power, const RelinKeys& relin_keys, const PublicKey& public_key);

Ciphertext polynomial_evaluation(const EncryptionParameters& parms, vector<Ciphertext> encrypted_powers, vector<Plaintext> coeff_plaintexts,
                                 uint32_t ps_low_power, const RelinKeys& relin_keys);

Ciphertext polynomial_evaluation(const EncryptionParameters& parms, vector<Ciphertext> encrypted_powers, vector<Plaintext> coeff_plaintexts);

Ciphertext ucpsi_polynomial_evaluation(const EncryptionParameters& parms, vector<Ciphertext> encrypted_powers,
                                       vector<Plaintext> coeff_plaintexts, const PublicKey& public_key);

Ciphertext ucpsi_polynomial_evaluation(const EncryptionParameters& parms, vector<Ciphertext> encrypted_powers,
                                       vector<Plaintext> coeff_plaintexts, uint32_t ps_low_power, const RelinKeys& relin_keys,
                                       const PublicKey& public_key);

vector<Ciphertext> ucpsi_compute_encrypted_powers(const EncryptionParameters& parms, vector<Ciphertext> query,
                                                  vector<vector<uint32_t>> parent_powers, vector<uint32_t> source_power_index,
                                                  uint32_t ps_low_power, const RelinKeys& relin_keys);

#endif //MPC3J_NATIVE_FHE_APSI_H
