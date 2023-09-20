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

void sample_poly_uniform(const size_t bit_width, const EncryptionParameters &parms, uint64_t *destination) {
    // Extract encryption parameters
    const auto& coeff_modulus = parms.coeff_modulus();
    size_t coeff_modulus_size = coeff_modulus.size();
    size_t coeff_count = parms.poly_modulus_degree();
    size_t coeff_count_mask = coeff_count - 1;
    size_t dest_byte_count = seal::util::mul_safe(coeff_modulus_size, coeff_count, sizeof(uint64_t));
    auto bootstrap_prng = parms.random_generator()->create();

    // Sample a public seed for generating uniform randomness
    prng_seed_type public_prng_seed;
    bootstrap_prng->generate(prng_seed_byte_count, reinterpret_cast<seal_byte *>(public_prng_seed.data()));

    // Set up a new default PRNG for expanding u from the seed sampled above
    auto ciphertext_prng = UniformRandomGeneratorFactory::DefaultFactory()->create(public_prng_seed);
    auto random_mask = static_cast<uint64_t>((1ULL<<bit_width)-1);

    // Fill the destination buffer with fresh randomness
    ciphertext_prng->generate(dest_byte_count, reinterpret_cast<seal_byte *>(destination));
    for (size_t j = 0; j < coeff_count; j++) {
        destination[j] &= random_mask;
    }
    for (size_t j = coeff_count; j < coeff_modulus_size*coeff_count; j++) {
        destination[j] = destination[j & coeff_count_mask];
    }
}

void multiply_acum(uint64_t op1, uint64_t op2, __uint128_t& product_acum) {
    product_acum = product_acum + static_cast<__uint128_t>(op1) * static_cast<__uint128_t>(op2);
}

void multiply_poly_acum(const uint64_t *ct_ptr, const uint64_t *pt_ptr, size_t size, __uint128_t *result) {
    for (int cc = 0; cc < size; cc += 32) {
        multiply_acum(ct_ptr[cc], pt_ptr[cc], result[cc]);
        multiply_acum(ct_ptr[cc + 1], pt_ptr[cc + 1], result[cc + 1]);
        multiply_acum(ct_ptr[cc + 2], pt_ptr[cc + 2], result[cc + 2]);
        multiply_acum(ct_ptr[cc + 3], pt_ptr[cc + 3], result[cc + 3]);
        multiply_acum(ct_ptr[cc + 4], pt_ptr[cc + 4], result[cc + 4]);
        multiply_acum(ct_ptr[cc + 5], pt_ptr[cc + 5], result[cc + 5]);
        multiply_acum(ct_ptr[cc + 6], pt_ptr[cc + 6], result[cc + 6]);
        multiply_acum(ct_ptr[cc + 7], pt_ptr[cc + 7], result[cc + 7]);
        multiply_acum(ct_ptr[cc + 8], pt_ptr[cc + 8], result[cc + 8]);
        multiply_acum(ct_ptr[cc + 9], pt_ptr[cc + 9], result[cc + 9]);
        multiply_acum(ct_ptr[cc + 10], pt_ptr[cc + 10], result[cc + 10]);
        multiply_acum(ct_ptr[cc + 11], pt_ptr[cc + 11], result[cc + 11]);
        multiply_acum(ct_ptr[cc + 12], pt_ptr[cc + 12], result[cc + 12]);
        multiply_acum(ct_ptr[cc + 13], pt_ptr[cc + 13], result[cc + 13]);
        multiply_acum(ct_ptr[cc + 14], pt_ptr[cc + 14], result[cc + 14]);
        multiply_acum(ct_ptr[cc + 15], pt_ptr[cc + 15], result[cc + 15]);
        multiply_acum(ct_ptr[cc + 16], pt_ptr[cc + 16], result[cc + 16]);
        multiply_acum(ct_ptr[cc + 17], pt_ptr[cc + 17], result[cc + 17]);
        multiply_acum(ct_ptr[cc + 18], pt_ptr[cc + 18], result[cc + 18]);
        multiply_acum(ct_ptr[cc + 19], pt_ptr[cc + 19], result[cc + 19]);
        multiply_acum(ct_ptr[cc + 20], pt_ptr[cc + 20], result[cc + 20]);
        multiply_acum(ct_ptr[cc + 21], pt_ptr[cc + 21], result[cc + 21]);
        multiply_acum(ct_ptr[cc + 22], pt_ptr[cc + 22], result[cc + 22]);
        multiply_acum(ct_ptr[cc + 23], pt_ptr[cc + 23], result[cc + 23]);
        multiply_acum(ct_ptr[cc + 24], pt_ptr[cc + 24], result[cc + 24]);
        multiply_acum(ct_ptr[cc + 25], pt_ptr[cc + 25], result[cc + 25]);
        multiply_acum(ct_ptr[cc + 26], pt_ptr[cc + 26], result[cc + 26]);
        multiply_acum(ct_ptr[cc + 27], pt_ptr[cc + 27], result[cc + 27]);
        multiply_acum(ct_ptr[cc + 28], pt_ptr[cc + 28], result[cc + 28]);
        multiply_acum(ct_ptr[cc + 29], pt_ptr[cc + 29], result[cc + 29]);
        multiply_acum(ct_ptr[cc + 30], pt_ptr[cc + 30], result[cc + 30]);
        multiply_acum(ct_ptr[cc + 31], pt_ptr[cc + 31], result[cc + 31]);

    }
}