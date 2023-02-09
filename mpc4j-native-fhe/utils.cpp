#include "utils.h"
#include <utility>
#include <seal/util/polyarithsmallmod.h>

parms_id_type get_parms_id_for_chain_idx(const SEALContext& seal_context, uint32_t chain_idx) {
    // This function returns a parms_id matching the given chain index or -- if the chain
    // index is too large -- for the largest possible parameters (first data level).
    parms_id_type parms_id = seal_context.first_parms_id();
    while (seal_context.get_context_data(parms_id)->chain_index() > chain_idx) {
        parms_id = seal_context.get_context_data(parms_id)->next_context_data()->parms_id();
    }
    return parms_id;
}

EncryptionParameters generate_encryption_parameters(scheme_type type, uint32_t poly_modulus_degree, uint64_t plain_modulus, const vector<Modulus>& coeff_modulus) {
    EncryptionParameters parms = EncryptionParameters(type);
    parms.set_poly_modulus_degree(poly_modulus_degree);
    parms.set_plain_modulus(plain_modulus);
    parms.set_coeff_modulus(coeff_modulus);
    return parms;
}

GaloisKeys generate_galois_keys(const SEALContext& context, KeyGenerator &keygen) {
    std::vector<uint32_t> galois_elts;
    auto &parms = context.first_context_data()->parms();
    uint32_t degree = parms.poly_modulus_degree();
    uint32_t logN = seal::util::get_power_of_two(degree);
    for (uint32_t i = 0; i < logN; i++) {
        galois_elts.push_back((degree + seal::util::exponentiate_uint(2, i)) / seal::util::exponentiate_uint(2, i));
    }
    GaloisKeys galois_keys;
    keygen.create_galois_keys(galois_elts, galois_keys);
    return galois_keys;
}

uint64_t invert_mod(uint64_t m, const seal::Modulus &mod) {
    uint64_t inverse = 0;
    seal::util::try_invert_uint_mod(m, mod.value(), inverse);
    return inverse;
}