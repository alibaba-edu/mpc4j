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

Serializable<GaloisKeys> generate_galois_keys(const SEALContext& context, KeyGenerator &keygen);

uint64_t invert_mod(uint64_t m, const seal::Modulus &mod);

void try_clear_irrelevant_bits(const EncryptionParameters &parms, Ciphertext &ciphertext);



template< typename T >
std::string int_to_hex( T i )
{
  std::stringstream stream;
  stream << std::hex << i;
  return stream.str();
}

#endif //MPC4J_NATIVE_FHE_UTILS_H
