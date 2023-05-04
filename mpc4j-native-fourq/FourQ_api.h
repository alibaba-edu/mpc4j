/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: API header file
*
* This code is based on the paper "FourQ: four-dimensional decompositions on a 
* Q-curve over the Mersenne prime" by Craig Costello and Patrick Longa, in Advances 
* in Cryptology - ASIACRYPT, 2015.
* Preprint available at http://eprint.iacr.org/2015/565.
************************************************************************************/  

#ifndef __FOURQ_API_H__
#define __FOURQ_API_H__


// For C++
#ifdef __cplusplus
extern "C" {
#endif


#include "FourQ.h"


/**************** Public ECC API ****************/

// Set generator G = (x,y)
void eccset(point_t G);

// Variable-base scalar multiplication Q = k*P
bool ecc_mul(point_t P, digit_t* k, point_t Q, bool clear_cofactor);

// Fixed-base scalar multiplication Q = k*G, where G is the generator
bool ecc_mul_fixed(digit_t* k, point_t Q);

// Double scalar multiplication R = k*G + l*Q, where G is the generator
bool ecc_mul_double(digit_t* k, point_t Q, digit_t* l, point_t R);


/************* Public API for arithmetic functions modulo the curve order **************/

// Converting to Montgomery representation
void to_Montgomery(const digit_t* ma, digit_t* c);

// Converting from Montgomery to standard representation
void from_Montgomery(const digit_t* a, digit_t* mc);

// 256-bit Montgomery multiplication modulo the curve order
void Montgomery_multiply_mod_order(const digit_t* ma, const digit_t* mb, digit_t* mc);

// (Non-constant time) Montgomery inversion modulo the curve order
void Montgomery_inversion_mod_order(const digit_t* ma, digit_t* mc);

// Addition modulo the curve order, c = a+b mod order
void add_mod_order(const digit_t* a, const digit_t* b, digit_t* c);

// Subtraction modulo the curve order, c = a-b mod order
void subtract_mod_order(const digit_t* a, const digit_t* b, digit_t* c);

// Reduction modulo the order using Montgomery arithmetic internally
void modulo_order(digit_t* a, digit_t* c);


/**************** Public API for SchnorrQ ****************/

// SchnorrQ public key generation
// It produces a public key PublicKey, which is the encoding of P = s*G, where G is the generator and
// s is the output of hashing SecretKey and taking the least significant 32 bytes of the result.
// Input:  32-byte SecretKey
// Output: 32-byte PublicKey
ECCRYPTO_STATUS SchnorrQ_KeyGeneration(const unsigned char* SecretKey, unsigned char* PublicKey);

// SchnorrQ keypair generation
// It produces a private key SecretKey and computes the public key PublicKey, which is the encoding of P = s*G, 
// where G is the generator and s is the output of hashing SecretKey and taking the least significant 32 bytes of the result.
// Outputs: 32-byte SecretKey and 32-byte PublicKey
ECCRYPTO_STATUS SchnorrQ_FullKeyGeneration(unsigned char* SecretKey, unsigned char* PublicKey);

// SchnorrQ signature generation
// It produces the signature Signature of a message Message of size SizeMessage in bytes
// Inputs: 32-byte SecretKey, 32-byte PublicKey, and Message of size SizeMessage in bytes
// Output: 64-byte Signature 
ECCRYPTO_STATUS SchnorrQ_Sign(const unsigned char* SecretKey, const unsigned char* PublicKey, const unsigned char* Message, const unsigned int SizeMessage, unsigned char* Signature);

// SchnorrQ signature verification
// It verifies the signature Signature of a message Message of size SizeMessage in bytes
// Inputs: 32-byte PublicKey, 64-byte Signature, and Message of size SizeMessage in bytes
// Output: true (valid signature) or false (invalid signature)
ECCRYPTO_STATUS SchnorrQ_Verify(const unsigned char* PublicKey, const unsigned char* Message, const unsigned int SizeMessage, const unsigned char* Signature, unsigned int* valid);


/**************** Public API for co-factor ECDH key exchange with compressed, 32-byte public keys ****************/

// Compressed public key generation for key exchange
// It produces a public key PublicKey, which is the encoding of P = SecretKey*G (G is the generator).
// Input:  32-byte SecretKey
// Output: 32-byte PublicKey
ECCRYPTO_STATUS CompressedPublicKeyGeneration(const unsigned char* SecretKey, unsigned char* PublicKey);

// Keypair generation for key exchange. Public key is compressed to 32 bytes
// It produces a private key SecretKey and a public key PublicKey, which is the encoding of P = SecretKey*G (G is the generator).
// Outputs: 32-byte SecretKey and 32-byte PublicKey 
ECCRYPTO_STATUS CompressedKeyGeneration(unsigned char* SecretKey, unsigned char* PublicKey);

// Secret agreement computation for key exchange using a compressed, 32-byte public key
// The output is the y-coordinate of SecretKey*A, where A is the decoding of the public key PublicKey. 
// Inputs: 32-byte SecretKey and 32-byte PublicKey
// Output: 32-byte SharedSecret
ECCRYPTO_STATUS CompressedSecretAgreement(const unsigned char* SecretKey, const unsigned char* PublicKey, unsigned char* SharedSecret);


/**************** Public API for co-factor ECDH key exchange with uncompressed, 64-byte public keys ****************/

// Public key generation for key exchange
// It produces the public key PublicKey = SecretKey*G, where G is the generator.
// Input:  32-byte SecretKey
// Output: 64-byte PublicKey
ECCRYPTO_STATUS PublicKeyGeneration(const unsigned char* SecretKey, unsigned char* PublicKey);

// Keypair generation for key exchange
// It produces a private key SecretKey and computes the public key PublicKey = SecretKey*G, where G is the generator.
// Outputs: 32-byte SecretKey and 64-byte PublicKey 
ECCRYPTO_STATUS KeyGeneration(unsigned char* SecretKey, unsigned char* PublicKey);

// Secret agreement computation for key exchange
// The output is the y-coordinate of SecretKey*PublicKey. 
// Inputs: 32-byte SecretKey and 64-byte PublicKey
// Output: 32-byte SharedSecret
ECCRYPTO_STATUS SecretAgreement(const unsigned char* SecretKey, const unsigned char* PublicKey, unsigned char* SharedSecret);


/**************** Public API for hashing to curve, 64-byte public keys ****************/

// Hash GF(p^2) element to a curve point
// Input: GF(p^2) element
// Output: point in affine coordinates with co-factor cleared
ECCRYPTO_STATUS HashToCurve(f2elm_t r, point_t P);


#ifdef __cplusplus
}
#endif


#endif
