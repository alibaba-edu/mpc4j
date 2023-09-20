/*
 * @Description: 
 * @Author: Qixian Zhou
 * @Date: 2023-05-28 19:35:10
 */
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

// For MulPIR, reference: https://github.com/OpenMined/PIR/blob/master/pir/cpp/server.cpp#L148
vector<Ciphertext> new_expand_query(const EncryptionParameters& parms, const std::vector<Ciphertext>& cts, uint32_t total_items,
                                    const GaloisKeys& galois_keys);

// For MulPIR, reference: https://github.com/OpenMined/PIR/blob/master/pir/cpp/server.cpp#L106
vector<Ciphertext> new_single_expand_query(const EncryptionParameters& parms, const Ciphertext& ct, uint32_t num_items,
                                           const GaloisKeys& galois_keys);

// For MulPIR, reference:  https://github.com/OpenMined/PIR/blob/master/pir/cpp/database.cpp#L170
vector<Ciphertext> multiply_mulpir(const EncryptionParameters& parms, const RelinKeys* relin_keys, const vector<Plaintext>& database,
                                   uint32_t database_it, vector<Ciphertext>& selection_vector, uint32_t selection_vector_it,
                                   vector<int32_t>& dimensions, uint32_t depth);

// convert h ciphertexts to length-m ciphers vector, m is the codeword bit length
vector<Ciphertext> mk22_expand_input_ciphers(const EncryptionParameters& parms, const GaloisKeys& galois_keys,  vector<Ciphertext>& input_ciphers, uint64_t num_input_ciphers, uint64_t num_bits);

// convert single Ciphertext to 2^c Ciphertext 
vector<Ciphertext> mk22_expand_procedure(const EncryptionParameters& parms, const GaloisKeys& galois_keys, const Ciphertext &input_cipher, uint64_t used_slots);

Ciphertext mk22_folklore_eq(Evaluator& evaluator, const RelinKeys* relin_keys, uint32_t codeword_bit_length,
                            vector<Ciphertext>& encrypted_query, vector<uint32_t>& single_pt_index_codeword);

Ciphertext mk22_constant_weight_eq(Evaluator& evaluator, const RelinKeys* relin_keys, uint32_t codeword_bit_length,
                                   uint32_t hamming_weight, vector<Ciphertext>& encrypted_query,
                                   vector<uint32_t>& single_pt_index_codeword);

Ciphertext mk22_generate_selection_bit(Evaluator& evaluator,
                                       const RelinKeys* relin_keys,
                                       uint32_t codeword_bit_length,
                                       uint32_t hamming_weight,
                                       uint32_t eq_type,
                                       vector<Ciphertext>& encrypted_query,
                                       vector<uint32_t>& single_pt_index_codeword);

void mk22_generate_selection_vector(Evaluator& evaluator, const RelinKeys* relin_keys, uint32_t codeword_bit_length,
                                    uint32_t hamming_weight, uint32_t eq_type, vector<Ciphertext>& expanded_query,
                                    vector<vector<uint32_t>>& pt_index_codewords_list, vector<Ciphertext>& selection_vector);

Ciphertext mk22_faster_inner_product(Evaluator& evaluator, vector<Ciphertext>& selection_vector, vector<Plaintext>& database);

vector<Ciphertext> merge_response(const SEALContext& context, const GaloisKeys& galois_keys, vector<Ciphertext> response,
                                  int32_t num_slots_per_entry, uint32_t first_two_dimension_size);

#endif //MPC4J_NATIVE_FHE_INDEX_PIR_H
