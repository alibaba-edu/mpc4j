/*
 * Created by anonymous on 2022/9/13.
 */

#ifndef FEMUR_NATIVE_FHE_INDEX_PIR_H
#define FEMUR_NATIVE_FHE_INDEX_PIR_H

#include "seal/seal.h"
using namespace std;
using namespace seal;


void compose_to_ciphertext(const EncryptionParameters& parms, vector<Plaintext>::const_iterator pt_iter,
                           uint32_t ct_poly_count, Ciphertext &ct);

vector<Plaintext> decompose_to_plaintexts(const EncryptionParameters& parms, const Ciphertext &ct);

uint32_t compute_expansion_ratio(const EncryptionParameters& parms);

void compose_to_ciphertext(const EncryptionParameters& parms, const vector<Plaintext> &pts, Ciphertext &ct);

void multiply_power_of_X(const Ciphertext &encrypted, Ciphertext &destination,
                         uint32_t index, const SEALContext& context);

vector<Ciphertext> expand_query(const EncryptionParameters& parms, const Ciphertext &encrypted,
                                const GaloisKeys& galois_keys, uint32_t m);

Serializable<GaloisKeys> generate_galois_keys(const SEALContext& context, KeyGenerator &keygen);

uint64_t invert_mod(uint64_t m, const seal::Modulus &mod);

#endif //FEMUR_NATIVE_FHE_INDEX_PIR_H
