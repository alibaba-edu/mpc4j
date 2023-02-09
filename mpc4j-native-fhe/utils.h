//
// Created by pengliqiang on 2022/9/8.
//

#ifndef MPC4J_NATIVE_FHE_UTILS_H
#define MPC4J_NATIVE_FHE_UTILS_H

#include <iomanip>
#include "seal/seal.h"

using namespace seal;
using namespace std;


parms_id_type get_parms_id_for_chain_idx(const SEALContext& seal_context, uint32_t chain_idx);

EncryptionParameters generate_encryption_parameters(scheme_type type, uint32_t poly_modulus_degree, uint64_t plain_modulus,
                                                    const vector<Modulus>& coeff_modulus);

GaloisKeys generate_galois_keys(const SEALContext& context, KeyGenerator &keygen);

uint64_t invert_mod(uint64_t m, const seal::Modulus &mod);

#endif //MPC4J_NATIVE_FHE_UTILS_H
