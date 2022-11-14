#include "utils.h"
#include <utility>

parms_id_type get_parms_id_for_chain_idx(const SEALContext& seal_context, size_t chain_idx) {
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

EncryptionParameters generate_encryption_parameters(scheme_type type, uint32_t poly_modulus_degree, uint64_t plain_modulus) {
    return generate_encryption_parameters(type, poly_modulus_degree, plain_modulus, CoeffModulus::BFVDefault(poly_modulus_degree, sec_level_type::tc128));
}

EncryptionParameters generate_encryption_parameters(scheme_type type, uint32_t poly_modulus_degree, uint64_t plain_modulus, vector<int> bit_sizes) {
    return generate_encryption_parameters(type, poly_modulus_degree, plain_modulus, CoeffModulus::Create(poly_modulus_degree, std::move(bit_sizes)));
}

