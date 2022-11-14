/*
 * Created by pengliqiang on 2022/9/13.
 * This implementation is based on the public domain SealPIR in SealPIR project:
 * <p>
 * https://github.com/microsoft/SealPIR/blob/master/src/pir.cpp
 * </p>
 */

#ifndef MPC4J_NATIVE_FHE_INDEX_PIR_H
#define MPC4J_NATIVE_FHE_INDEX_PIR_H

#include "seal/seal.h"

using namespace std;
using namespace seal;


void compose_to_ciphertext(const EncryptionParameters& params, vector<Plaintext>::const_iterator pt_iter,
                           size_t ct_poly_count, Ciphertext &ct);

vector<Plaintext> decompose_to_plaintexts(const EncryptionParameters& params, const Ciphertext &ct);

uint32_t compute_expansion_ratio(const EncryptionParameters& params);

void compose_to_ciphertext(const EncryptionParameters& params, const vector<Plaintext> &pts, Ciphertext &ct);

#endif //MPC4J_NATIVE_FHE_INDEX_PIR_H
