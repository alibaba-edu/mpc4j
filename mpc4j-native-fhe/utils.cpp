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

Serializable<GaloisKeys> generate_galois_keys(const SEALContext& context, KeyGenerator &keygen) {
    std::vector<uint32_t> galois_elts;
    auto &parms = context.first_context_data()->parms();
    uint32_t degree = parms.poly_modulus_degree();
    uint32_t logN = util::get_power_of_two(degree);
    for (uint32_t i = 0; i < logN; i++) {
        galois_elts.push_back((degree + util::exponentiate_uint(2, i)) / util::exponentiate_uint(2, i));
    }
    Serializable<GaloisKeys> galois_keys = keygen.create_galois_keys(galois_elts);
    return galois_keys;
}

uint64_t invert_mod(uint64_t m, const seal::Modulus &mod) {
    uint64_t inverse = 0;
    seal::util::try_invert_uint_mod(m, mod.value(), inverse);
    return inverse;
}

void try_clear_irrelevant_bits(const EncryptionParameters &parms, Ciphertext &ciphertext) {
    // If the parameter set has only one prime, we can compress the ciphertext by
    // setting low-order bits to zero. This effectively maxes out the noise, but that
    // doesn't matter as long as we don't use quite all noise budget.
    if (parms.coeff_modulus().size() == 1) {
        // The number of data bits we need to have left in each ciphertext coefficient
        int compr_coeff_bit_count =
                parms.plain_modulus().bit_count() +
                seal::util::get_significant_bit_count(parms.poly_modulus_degree())
                // Being pretty aggressive here
                - 1;
        int coeff_mod_bit_count = parms.coeff_modulus()[0].bit_count();
        // The number of bits to set to zero
        int irrelevant_bit_count = coeff_mod_bit_count - compr_coeff_bit_count;
        // Can compression achieve anything?
        if (irrelevant_bit_count > 0) {
            // Mask for zeroing out the irrelevant bits
            uint64_t mask = ~((uint64_t(1) << irrelevant_bit_count) - 1);
            seal_for_each_n(seal::util::iter(ciphertext), ciphertext.size(), [&](auto &&I) {
                // We only have a single RNS component so dereference once more
                seal_for_each_n(
                        *I, parms.poly_modulus_degree(), [&](auto &J) { J &= mask; });
            });
        }
    }
}