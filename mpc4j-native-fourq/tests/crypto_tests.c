/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: testing code for cryptographic functions based on FourQ 
************************************************************************************/   

#include "../FourQ_api.h"
#include "../FourQ_params.h"
#include "../random/random.h"
#include "../sha512/sha512.h"
#include "test_extras.h"
#include <stdio.h>


// Benchmark and test parameters  
#if defined(GENERIC_IMPLEMENTATION)
    #define BENCH_LOOPS       100       // Number of iterations per bench
    #define TEST_LOOPS        100       // Number of iterations per test
#else 
    #define BENCH_LOOPS       10000
    #define TEST_LOOPS        1000
#endif


ECCRYPTO_STATUS SchnorrQ_test()
{ // Test the SchnorrQ digital signature scheme
    int n, passed;       
    void *msg = NULL; 
    unsigned int len, valid = false;
    unsigned char SecretKey[32], PublicKey[32], Signature[64];
    ECCRYPTO_STATUS Status = ECCRYPTO_SUCCESS;

    printf("\n--------------------------------------------------------------------------------------------------------\n\n"); 
    printf("Testing the SchnorrQ signature scheme: \n\n"); 

    passed = 1;
    for (n = 0; n < TEST_LOOPS; n++)
    {    
        // Signature key generation
        Status = SchnorrQ_FullKeyGeneration(SecretKey, PublicKey);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }  

        // Signature computation
        msg = "a";  
        len = 1;
        Status = SchnorrQ_Sign(SecretKey, PublicKey, msg, len, Signature);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }    

        // Valid signature test
        Status = SchnorrQ_Verify(PublicKey, msg, len, Signature, &valid);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }    
        if (valid == false) {
            passed = 0;
            break;
        }

        // Invalid signature test (flipping one bit of the message)
        msg = "b";  
        Status = SchnorrQ_Verify(PublicKey, msg, len, Signature, &valid);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }    
        if (valid == true) {
            passed = 0;
            break;
        }
    } 
    if (passed==1) printf("  Signature tests.................................................................. PASSED");
    else { printf("  Signature tests... FAILED"); printf("\n"); Status = ECCRYPTO_ERROR_SIGNATURE_VERIFICATION; }
    printf("\n");
    
    return Status;
}


ECCRYPTO_STATUS SchnorrQ_run()
{ // Benchmark the SchnorrQ digital signature scheme 
    int n;
    unsigned long long cycles, cycles1, cycles2;   
    void *msg = NULL;
    unsigned int len = 0, valid = false;
    unsigned char SecretKey[32], PublicKey[32], Signature[64];
    ECCRYPTO_STATUS Status = ECCRYPTO_SUCCESS;

    printf("\n--------------------------------------------------------------------------------------------------------\n\n"); 
    printf("Benchmarking the SchnorrQ signature scheme: \n\n"); 
    
    cycles = 0;
    for (n = 0; n < BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles(); 
        Status = SchnorrQ_FullKeyGeneration(SecretKey, PublicKey);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }    
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }
    printf("  SchnorrQ's key generation runs in ............................................... %8lld ", cycles/BENCH_LOOPS); print_unit;
    printf("\n");
    
    cycles = 0;
    for (n = 0; n < BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles(); 
        Status = SchnorrQ_Sign(SecretKey, PublicKey, msg, len, Signature);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }    
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }
    printf("  SchnorrQ's signing runs in ...................................................... %8lld ", cycles/BENCH_LOOPS); print_unit;
    printf("\n");
    
    cycles = 0;
    for (n = 0; n < BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles(); 
        Status = SchnorrQ_Verify(PublicKey, msg, len, Signature, &valid);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }    
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }
    printf("  SchnorrQ's verification runs in ................................................. %8lld ", cycles/BENCH_LOOPS); print_unit;
    printf("\n");
    
    return Status;
}


ECCRYPTO_STATUS compressedkex_test()
{ // Test ECDH key exchange based on FourQ
    int n, passed;
    unsigned int i;
    unsigned char SecretKeyA[32], PublicKeyA[32], SecretAgreementA[32];
    unsigned char SecretKeyB[32], PublicKeyB[32], SecretAgreementB[32];
    ECCRYPTO_STATUS Status = ECCRYPTO_SUCCESS;

    printf("\n--------------------------------------------------------------------------------------------------------\n\n");
    printf("Testing DH key exchange using compressed, 32-byte public keys: \n\n");

    passed = 1;
    for (n = 0; n < TEST_LOOPS; n++)
    {
        // Alice's keypair generation
        Status = CompressedKeyGeneration(SecretKeyA, PublicKeyA);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }
        // Bob's keypair generation
        Status = CompressedKeyGeneration(SecretKeyB, PublicKeyB);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }

        // Alice's shared secret computation
        Status = CompressedSecretAgreement(SecretKeyA, PublicKeyB, SecretAgreementA);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }
        // Bob's shared secret computation
        Status = CompressedSecretAgreement(SecretKeyB, PublicKeyA, SecretAgreementB);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }

        for (i = 0; i < 32; i++) {
            if (SecretAgreementA[i] != SecretAgreementB[i]) {
                passed = 0;
                break;
            }
        }
    }
    if (passed==1) printf("  DH key exchange tests............................................................ PASSED");
    else { printf("  DH key exchange tests... FAILED"); printf("\n"); Status = ECCRYPTO_ERROR_SHARED_KEY; }
    printf("\n");

    return Status;
}


ECCRYPTO_STATUS compressedkex_run()
{ // Benchmark ECDH key exchange based on FourQ 
    int n;
    unsigned long long cycles, cycles1, cycles2;
    unsigned char SecretKeyA[32], PublicKeyA[32], SecretAgreementA[32];
    unsigned char SecretKeyB[32], PublicKeyB[32];
    ECCRYPTO_STATUS Status = ECCRYPTO_SUCCESS;

    printf("\n--------------------------------------------------------------------------------------------------------\n\n");
    printf("Benchmarking DH key exchange using compressed, 32-byte public keys: \n\n");

    cycles = 0;
    for (n = 0; n < BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        Status = CompressedKeyGeneration(SecretKeyA, PublicKeyA);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }
        cycles2 = cpucycles();
        cycles = cycles + (cycles2 - cycles1);
    }
    printf("  Keypair generation runs in ...................................................... %8lld ", cycles/BENCH_LOOPS); print_unit;
    printf("\n");
    
    Status = CompressedKeyGeneration(SecretKeyB, PublicKeyB);
    cycles = 0;
    for (n = 0; n < BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        Status = CompressedSecretAgreement(SecretKeyA, PublicKeyB, SecretAgreementA);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }
        cycles2 = cpucycles();
        cycles = cycles + (cycles2 - cycles1);
    }
    printf("  Secret agreement runs in ........................................................ %8lld ", cycles/BENCH_LOOPS); print_unit;
    printf("\n");

    return Status;
}


ECCRYPTO_STATUS kex_test()
{ // Test ECDH key exchange based on FourQ
    int n, passed;
    unsigned int i;
    unsigned char SecretKeyA[32], PublicKeyA[64], SecretAgreementA[32];
    unsigned char SecretKeyB[32], PublicKeyB[64], SecretAgreementB[32];
    ECCRYPTO_STATUS Status = ECCRYPTO_SUCCESS;

    printf("\n--------------------------------------------------------------------------------------------------------\n\n");
    printf("Testing DH key exchange using uncompressed, 64-byte public keys: \n\n");

    passed = 1;
    for (n = 0; n < TEST_LOOPS; n++)
    {
        // Alice's keypair generation
        Status = KeyGeneration(SecretKeyA, PublicKeyA);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }
        // Bob's keypair generation
        Status = KeyGeneration(SecretKeyB, PublicKeyB);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }

        // Alice's shared secret computation
        Status = SecretAgreement(SecretKeyA, PublicKeyB, SecretAgreementA);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }
        // Bob's shared secret computation
        Status = SecretAgreement(SecretKeyB, PublicKeyA, SecretAgreementB);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }

        for (i = 0; i < 32; i++) {
            if (SecretAgreementA[i] != SecretAgreementB[i]) {
                passed = 0;
                break;
            }
        }
    }
    if (passed==1) printf("  DH key exchange tests............................................................ PASSED");
    else { printf("  DH key exchange tests... FAILED"); printf("\n"); Status = ECCRYPTO_ERROR_SHARED_KEY; }
    printf("\n");

    return Status;
}


ECCRYPTO_STATUS kex_run()
{ // Benchmark ECDH key exchange based on FourQ 
    int n;
    unsigned long long cycles, cycles1, cycles2;
    unsigned char SecretKeyA[32], PublicKeyA[64], SecretAgreementA[32];
    unsigned char SecretKeyB[32], PublicKeyB[64];
    ECCRYPTO_STATUS Status = ECCRYPTO_SUCCESS;

    printf("\n--------------------------------------------------------------------------------------------------------\n\n");
    printf("Benchmarking DH key exchange using uncompressed, 64-byte public keys: \n\n");

    cycles = 0;
    for (n = 0; n < BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        Status = KeyGeneration(SecretKeyA, PublicKeyA);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }
        cycles2 = cpucycles();
        cycles = cycles + (cycles2 - cycles1);
    }
    printf("  Keypair generation runs in ...................................................... %8lld ", cycles/BENCH_LOOPS); print_unit;
    printf("\n");

    Status = KeyGeneration(SecretKeyB, PublicKeyB);
    cycles = 0;
    for (n = 0; n < BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        Status = SecretAgreement(SecretKeyA, PublicKeyB, SecretAgreementA);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }
        cycles2 = cpucycles();
        cycles = cycles + (cycles2 - cycles1);
    }
    printf("  Secret agreement runs in ........................................................ %8lld ", cycles/BENCH_LOOPS); print_unit;
    printf("\n");

    return Status;
}


ECCRYPTO_STATUS hash2curve_test()
{ // Test hashing to FourQ
    int n, passed;
    point_t P, Q;
    point_extproj_t R;
    unsigned char Value[32], HashedValue[64];
    f2elm_t* f2elmt = (f2elm_t*)&HashedValue[0];
    ECCRYPTO_STATUS Status = ECCRYPTO_SUCCESS;

    printf("\n--------------------------------------------------------------------------------------------------------\n\n");
    printf("Testing hashing to FourQ: \n\n");

    passed = 1;
    for (n = 0; n < TEST_LOOPS; n++)
    {
        RandomBytesFunction(Value, 32);
        CryptoHashFunction(Value, 32, HashedValue);
        mod1271(((felm_t*)f2elmt)[0]);
        mod1271(((felm_t*)f2elmt)[1]);

        // Hash GF(p^2) element to curve
        Status = HashToCurve((felm_t*)f2elmt, P);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }
        hash2curve_unsafe((felm_t*)f2elmt, Q);  // Non-constant-time version for testing        
        if (fp2compare64((uint64_t*)P->x,(uint64_t*)Q->x)!=0 || fp2compare64((uint64_t*)P->y,(uint64_t*)Q->y)!=0) { passed=0; break; }

        // Check if point is on the curve
        point_setup(P, R);
        if (!ecc_point_validate(R)) { passed=0; break; }
    }
    if (passed==1) printf("  Hash to FourQ tests.............................................................. PASSED");
    else { printf("  Hash to FourQ tests... FAILED"); printf("\n"); Status = ECCRYPTO_ERROR_HASH_TO_CURVE; }
    printf("\n");

    return Status;
}


ECCRYPTO_STATUS hash2curve_run()
{ // Benchmark hashing to FourQ 
    int n;
    unsigned long long cycles, cycles1, cycles2;
    point_t P;
    unsigned char Value[32], HashedValue[64];
    f2elm_t* f2elmt = (f2elm_t*)&HashedValue[0];
    ECCRYPTO_STATUS Status = ECCRYPTO_SUCCESS;

    printf("\n--------------------------------------------------------------------------------------------------------\n\n");
    printf("Benchmarking hashing to FourQ: \n\n");

    cycles = 0;
    for (n = 0; n < BENCH_LOOPS; n++)
    {
        RandomBytesFunction(Value, 32);
        CryptoHashFunction(Value, 32, HashedValue);
        mod1271(((felm_t*)f2elmt)[0]);
        mod1271(((felm_t*)f2elmt)[1]);

        cycles1 = cpucycles();
        Status = HashToCurve((felm_t*)f2elmt, P);
        if (Status != ECCRYPTO_SUCCESS) {
            return Status;
        }
        cycles2 = cpucycles();
        cycles = cycles + (cycles2 - cycles1);
    }
    printf("  Hashing to FourQ runs in ....................................................... %8lld ", cycles/BENCH_LOOPS); print_unit;
    printf("\n");

    return Status;
}


int main()
{
    ECCRYPTO_STATUS Status = ECCRYPTO_SUCCESS;
    
    Status = SchnorrQ_test();         // Test SchnorrQ signature scheme
    if (Status != ECCRYPTO_SUCCESS) {
        printf("\n\n   Error detected: %s \n\n", FourQ_get_error_message(Status));
        return false;
    }
    Status = SchnorrQ_run();          // Benchmark SchnorrQ signature scheme
    if (Status != ECCRYPTO_SUCCESS) {
        printf("\n\n   Error detected: %s \n\n", FourQ_get_error_message(Status));
        return false;
    }

    Status = compressedkex_test();    // Test Diffie-Hellman key exchange using compressed public keys
    if (Status != ECCRYPTO_SUCCESS) {
        printf("\n\n   Error detected: %s \n\n", FourQ_get_error_message(Status));
        return false;
    }
    Status = compressedkex_run();     // Benchmark Diffie-Hellman key exchange using compressed public keys
    if (Status != ECCRYPTO_SUCCESS) {
        printf("\n\n   Error detected: %s \n\n", FourQ_get_error_message(Status));
        return false;
    }

    Status = kex_test();              // Test Diffie-Hellman key exchange using uncompressed public keys
    if (Status != ECCRYPTO_SUCCESS) {
        printf("\n\n   Error detected: %s \n\n", FourQ_get_error_message(Status));
        return false;
    }
    Status = kex_run();               // Benchmark Diffie-Hellman key exchange using uncompressed public keys
    if (Status != ECCRYPTO_SUCCESS) {
        printf("\n\n   Error detected: %s \n\n", FourQ_get_error_message(Status));
        return false;
    }
    
    Status = hash2curve_test();       // Test hash to FourQ function
    if (Status != ECCRYPTO_SUCCESS) {
        printf("\n\n   Error detected: %s \n\n", FourQ_get_error_message(Status));
        return false;
    }
    Status = hash2curve_run();        // Benchmark hash to FourQ function
    if (Status != ECCRYPTO_SUCCESS) {
        printf("\n\n   Error detected: %s \n\n", FourQ_get_error_message(Status));
        return false;
    }

    return true;
}