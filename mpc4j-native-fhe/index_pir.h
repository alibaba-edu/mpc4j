/*
 * Created by pengliqiang on 2022/9/13.
 */

#ifndef MPC4J_NATIVE_FHE_INDEX_PIR_H
#define MPC4J_NATIVE_FHE_INDEX_PIR_H

#include "seal/seal.h"
#include "tfhe/tfhe.h"
using namespace std;
using namespace seal;


void compose_to_ciphertext(const EncryptionParameters& parms, vector<Plaintext>::const_iterator pt_iter,
                           uint32_t ct_poly_count, Ciphertext &ct);

vector<Plaintext> decompose_to_plaintexts(const EncryptionParameters& parms, const Ciphertext &ct);

uint32_t compute_expansion_ratio(const EncryptionParameters& parms);

void compose_to_ciphertext(const EncryptionParameters& parms, const vector<Plaintext> &pts, Ciphertext &ct);

Ciphertext decomp_mul(vector<Ciphertext> ct_decomp, vector<uint64_t *> pt_decomp, const SEALContext& context);

void poc_expand_flat(vector<vector<Ciphertext>>::iterator &result, vector<Ciphertext> &packed_swap_bits,
                     const SEALContext& context, uint32_t size, seal::GaloisKeys &galois_keys);

vector<Ciphertext> poc_rlwe_expand(const Ciphertext& packed_query, const SEALContext& context, const seal::GaloisKeys& galois_keys, uint32_t size);

void multiply_power_of_X(const Ciphertext &encrypted, Ciphertext &destination,
                         uint32_t index, const SEALContext& context);

void poc_decompose_array(uint64_t *value, uint32_t count, std::vector<Modulus> coeff_modulus, uint32_t coeff_mod_count);

void plain_decomposition(Plaintext &pt, const SEALContext &context, uint32_t decomp_size, uint32_t base_bit,
                         vector<uint64_t *> &plain_decomp);

vector<Ciphertext> expand_query(const EncryptionParameters& parms, const Ciphertext &encrypted,
                                const GaloisKeys& galois_keys, uint32_t m);

uint32_t get_next_power_of_two(uint32_t number);

uint32_t get_number_of_bits(uint64_t number);

Ciphertext get_sum(vector<Ciphertext> &query, Evaluator& evaluator, GaloisKeys &gal_keys, vector<Plaintext> &encoded_db,
                   uint32_t start, uint32_t end);

#endif //MPC4J_NATIVE_FHE_INDEX_PIR_H
