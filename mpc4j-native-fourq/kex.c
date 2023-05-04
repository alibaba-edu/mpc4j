/********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: Diffie-Hellman key exchange based on FourQ
*           option 1: co-factor ecdh using compressed 32-byte public keys,
*           (see https://datatracker.ietf.org/doc/draft-ladd-cfrg-4q/).         
*           option 2: co-factor ecdh using uncompressed, 64-byte public keys. 
*********************************************************************************/

#include "FourQ_internal.h"
#include "random/random.h"
#include <string.h>


static __inline bool is_neutral_point(point_t P)
{ // Is P the neutral point (0,1)?
  // SECURITY NOTE: this function does not run in constant time (input point P is assumed to be public).
  
    if (is_zero_ct((digit_t*)P->x, 2*NWORDS_FIELD) && is_zero_ct(&((digit_t*)P->y)[1], 2*NWORDS_FIELD-1) && is_digit_zero_ct(P->y[0][0] - 1)) {  
		return true;
    }
    return false;
}


/*************** ECDH USING COMPRESSED, 32-BYTE PUBLIC KEYS ***************/

ECCRYPTO_STATUS CompressedPublicKeyGeneration(const unsigned char* SecretKey, unsigned char* PublicKey)
{ // Compressed public key generation for key exchange
  // It produces a public key PublicKey, which is the encoding of P = SecretKey*G (G is the generator).
  // Input:  32-byte SecretKey
  // Output: 32-byte PublicKey
    point_t P;
    
    ecc_mul_fixed((digit_t*)SecretKey, P);  // Compute public key                                       
	encode(P, PublicKey);                   // Encode public key

    return ECCRYPTO_SUCCESS;
}


ECCRYPTO_STATUS CompressedKeyGeneration(unsigned char* SecretKey, unsigned char* PublicKey)
{ // Keypair generation for key exchange. Public key is compressed to 32 bytes
  // It produces a private key SecretKey and a public key PublicKey, which is the encoding of P = SecretKey*G (G is the generator).
  // Outputs: 32-byte SecretKey and 32-byte PublicKey 
    ECCRYPTO_STATUS Status = ECCRYPTO_ERROR_UNKNOWN;

	Status = RandomBytesFunction(SecretKey, 32);
	if (Status != ECCRYPTO_SUCCESS) {
		goto cleanup;
	}
  
    Status = CompressedPublicKeyGeneration(SecretKey, PublicKey);
    if (Status != ECCRYPTO_SUCCESS) {
        goto cleanup;
    }

    return ECCRYPTO_SUCCESS;

cleanup:
    clear_words((unsigned int*)SecretKey, 256/(sizeof(unsigned int)*8));
    clear_words((unsigned int*)PublicKey, 256/(sizeof(unsigned int)*8));

    return Status;
}


ECCRYPTO_STATUS CompressedSecretAgreement(const unsigned char* SecretKey, const unsigned char* PublicKey, unsigned char* SharedSecret)
{ // Secret agreement computation for key exchange using a compressed, 32-byte public key
  // The output is the y-coordinate of SecretKey*A, where A is the decoding of the public key PublicKey.   
  // Inputs: 32-byte SecretKey and 32-byte PublicKey
  // Output: 32-byte SharedSecret
    point_t A;
    ECCRYPTO_STATUS Status = ECCRYPTO_ERROR_UNKNOWN;

    if ((PublicKey[15] & 0x80) != 0) {  // Is bit128(PublicKey) = 0?
		Status = ECCRYPTO_ERROR_INVALID_PARAMETER;
		goto cleanup;
    }

	Status = decode(PublicKey, A);    // Also verifies that A is on the curve. If it is not, it fails
	if (Status != ECCRYPTO_SUCCESS) {
		goto cleanup;
	}
         
    Status = ecc_mul(A, (digit_t*)SecretKey, A, true);
	if (Status != ECCRYPTO_SUCCESS) {
		goto cleanup;
	}

    if (is_neutral_point(A)) {  // Is output = neutral point (0,1)?
		Status = ECCRYPTO_ERROR_SHARED_KEY;
		goto cleanup;
    }
  
	memmove(SharedSecret, (unsigned char*)A->y, 32);

	return ECCRYPTO_SUCCESS;
    
cleanup:
    clear_words((unsigned int*)SharedSecret, 256/(sizeof(unsigned int)*8));
    
    return Status;
}


/*************** ECDH USING UNCOMPRESSED PUBLIC KEYS ***************/

ECCRYPTO_STATUS PublicKeyGeneration(const unsigned char* SecretKey, unsigned char* PublicKey)
{ // Public key generation for key exchange
  // It produces the public key PublicKey = SecretKey*G, where G is the generator.
  // Input:  32-byte SecretKey
  // Output: 64-byte PublicKey

	ecc_mul_fixed((digit_t*)SecretKey, (point_affine*)PublicKey);  // Compute public key

	return ECCRYPTO_SUCCESS;
}


ECCRYPTO_STATUS KeyGeneration(unsigned char* SecretKey, unsigned char* PublicKey)
{ // Keypair generation for key exchange
  // It produces a private key SecretKey and computes the public key PublicKey = SecretKey*G, where G is the generator.
  // Outputs: 32-byte SecretKey and 64-byte PublicKey 
	ECCRYPTO_STATUS Status = ECCRYPTO_ERROR_UNKNOWN;

	Status = RandomBytesFunction(SecretKey, 32);
	if (Status != ECCRYPTO_SUCCESS) {
		goto cleanup;
	}

	Status = PublicKeyGeneration(SecretKey, PublicKey);
	if (Status != ECCRYPTO_SUCCESS) {
		goto cleanup;
	}

	return ECCRYPTO_SUCCESS;

cleanup:
	clear_words((unsigned int*)SecretKey, 256/(sizeof(unsigned int)*8));
	clear_words((unsigned int*)PublicKey, 512/(sizeof(unsigned int)*8));

	return Status;
}


ECCRYPTO_STATUS SecretAgreement(const unsigned char* SecretKey, const unsigned char* PublicKey, unsigned char* SharedSecret)
{ // Secret agreement computation for key exchange
  // The output is the y-coordinate of SecretKey*PublicKey. 
  // Inputs: 32-byte SecretKey and 64-byte PublicKey
  // Output: 32-byte SharedSecret
	point_t A;
	ECCRYPTO_STATUS Status = ECCRYPTO_ERROR_UNKNOWN;

    if (((PublicKey[15] & 0x80) != 0) || ((PublicKey[31] & 0x80) != 0) || ((PublicKey[47] & 0x80) != 0) || ((PublicKey[63] & 0x80) != 0)) {  // Are PublicKey_x[i] and PublicKey_y[i] < 2^127?
		Status = ECCRYPTO_ERROR_INVALID_PARAMETER;
		goto cleanup;
    }

	Status = ecc_mul((point_affine*)PublicKey, (digit_t*)SecretKey, A, true);  // Also verifies that PublicKey is a point on the curve. If it is not, it fails
	if (Status != ECCRYPTO_SUCCESS) {
		goto cleanup;
	}

    if (is_neutral_point(A)) {  // Is output = neutral point (0,1)?
		Status = ECCRYPTO_ERROR_SHARED_KEY;
		goto cleanup;
    }
  
	memmove(SharedSecret, (unsigned char*)A->y, 32);

	return ECCRYPTO_SUCCESS;

cleanup:
	clear_words((unsigned int*)SharedSecret, 256/(sizeof(unsigned int)*8));

	return Status;
}