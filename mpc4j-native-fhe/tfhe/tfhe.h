#pragma once
#include "params.h"
#include "rns.h"
#include "seal/ciphertext.h"
#include "seal/decryptor.h"
#include "seal/encryptor.h"
#include "seal/evaluator.h"
#include "seal/plaintext.h"
#include "seal/seal.h"
#include <vector>
namespace seal
{
    using RLWECipher = Ciphertext;
    using RGSWCipher = std::vector<RLWECipher>;
    class TFHEcipher
    {
    public:
        TFHEcipher(SEALContext &context, PublicKey &public_key) : context_(context), evaluator_(context_), public_key_(public_key),
                                                                  encryptor_(context_, public_key), rns_(context_),
                                                                  parms(context_.first_context_data()->parms())
        {
            poly_modulus_degree = parms.poly_modulus_degree();
            coeff_modulus_size = parms.coeff_modulus().size();
        }
        void encrypt(Plaintext &plain, RGSWCipher &cipher);
        void encrypt_zero(RLWECipher &cipher);
        void ExternalProduct(RLWECipher &dst, RLWECipher &src, RGSWCipher operand);

        [[maybe_unused]] void ExternalProduct_internal(util::RNSIter res_iter0, util::RNSIter res_iter1, util::RNSIter decntt_iter, util::RNSIter prod_iter0, util::RNSIter prod_iter1, RLWECipher &src, RGSWCipher operand);
    private:
        SEALContext context_;
        Evaluator evaluator_;
        PublicKey public_key_;
        Encryptor encryptor_;
        TFHERNS rns_;
        size_t poly_modulus_degree;
        size_t coeff_modulus_size;
        EncryptionParameters parms;
    };
} // namespace seal