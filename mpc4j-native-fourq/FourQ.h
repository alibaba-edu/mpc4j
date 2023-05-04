/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: main header file
*
* This code is based on the paper "FourQ: four-dimensional decompositions on a 
* Q-curve over the Mersenne prime" by Craig Costello and Patrick Longa, in Advances 
* in Cryptology - ASIACRYPT, 2015.
* Preprint available at http://eprint.iacr.org/2015/565.
************************************************************************************/  

#ifndef __FOURQ_H__
#define __FOURQ_H__


// For C++
#ifdef __cplusplus
extern "C" {
#endif


#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>


// Definition of operating system
#define OS_LINUX     2
#define OS_TARGET OS_LINUX


// Definition of compiler

#define COMPILER_VC      1
#define COMPILER_GCC     2
#define COMPILER_CLANG   3

#if defined(__GNUC__)         // GNU GCC compiler
    #define COMPILER COMPILER_GCC   
#elif defined(__clang__)        // Clang compiler
    #define COMPILER COMPILER_CLANG   
#else
    #error -- "Unsupported COMPILER"
#endif


// Definition of the targeted architecture and basic data types
    
#define TARGET_AMD64        1
#define TARGET_x86          2
#define TARGET_ARM          3
#define TARGET_ARM64        4

#if defined(_AMD64_)
    #define TARGET TARGET_AMD64
    #define RADIX           64
    typedef uint64_t        digit_t;      // Unsigned 64-bit digit
    typedef int64_t         sdigit_t;     // Signed 64-bit digit
    #define NWORDS_FIELD    2             // Number of words of a field element
    #define NWORDS_ORDER    4             // Number of words of an element in Z_r 
#elif defined(_X86_)
    #define TARGET TARGET_x86
    #define RADIX           32
    typedef uint32_t        digit_t;      // Unsigned 32-bit digit
    typedef int32_t         sdigit_t;     // Signed 32-bit digit
    #define NWORDS_FIELD    4             
    #define NWORDS_ORDER    8 
#elif defined(_ARM_)
    #define TARGET TARGET_ARM
    #define RADIX           32
    typedef uint32_t        digit_t;      // Unsigned 32-bit digit
    typedef int32_t         sdigit_t;     // Signed 32-bit digit
    #define NWORDS_FIELD    4             
    #define NWORDS_ORDER    8 
#elif defined(_ARM64_)
    #define TARGET TARGET_ARM64
    #define RADIX           64
    typedef uint64_t        digit_t;      // Unsigned 64-bit digit
    typedef int64_t         sdigit_t;     // Signed 64-bit digit
    #define NWORDS_FIELD    2             
    #define NWORDS_ORDER    4              
#else
    #error -- "Unsupported ARCHITECTURE"
#endif


// Constants

#define RADIX64         64
#define NWORDS64_FIELD  2                 // Number of 64-bit words of a field element 
#define NWORDS64_ORDER  4                 // Number of 64-bit words of an element in Z_r 


// Instruction support

#define NO_SIMD_SUPPORT 0
#define AVX_SUPPORT     1
#define AVX2_SUPPORT    2

#if defined(_AVX2_)
    #define SIMD_SUPPORT AVX2_SUPPORT       // AVX2 support selection 
#elif defined(_AVX_)
    #define SIMD_SUPPORT AVX_SUPPORT        // AVX support selection 
#else
    #define SIMD_SUPPORT NO_SIMD_SUPPORT
#endif

#if defined(_ASM_)                          // Assembly support selection
    #define ASM_SUPPORT
#endif

#if defined(_GENERIC_)                      // Selection of generic, portable implementation
    #define GENERIC_IMPLEMENTATION
#endif


// Unsupported configurations
                         
#if defined(ASM_SUPPORT) && (OS_TARGET == OS_WIN)
    #error -- "Assembly is not supported on this platform"
#endif        

#if defined(ASM_SUPPORT) && defined(GENERIC_IMPLEMENTATION)
    #error -- "Unsupported configuration"
#endif        

#if (SIMD_SUPPORT != NO_SIMD_SUPPORT) && defined(GENERIC_IMPLEMENTATION)
    #error -- "Unsupported configuration"
#endif

#if (TARGET != TARGET_AMD64 && TARGET != TARGET_ARM64) && !defined(GENERIC_IMPLEMENTATION)
    #error -- "Unsupported configuration"
#endif


// Definition of complementary cryptographic functions

#define RandomBytesFunction     random_bytes    
#define CryptoHashFunction      crypto_sha512        // Use SHA-512 by default


// Basic parameters for variable-base scalar multiplication (without using endomorphisms)
#define W_VARBASE             5 
#define NBITS_ORDER_PLUS_ONE  246+1 


// Basic parameters for fixed-base scalar multiplication
#define W_FIXEDBASE       5                            // Memory requirement: 7.5KB (storage for 80 points).
#define V_FIXEDBASE       5                  


// Basic parameters for double scalar multiplication
#define WP_DOUBLEBASE     8                            // Memory requirement: 24KB (storage for 256 points).
#define WQ_DOUBLEBASE     4  
   

// FourQ's basic element definitions and point representations

typedef digit_t felm_t[NWORDS_FIELD];                  // Datatype for representing 128-bit field elements
typedef felm_t f2elm_t[2];                             // Datatype for representing quadratic extension field elements
        
typedef struct { f2elm_t x; f2elm_t y; } point_affine; // Point representation in affine coordinates.
typedef point_affine point_t[1]; 


// Definitions of the error-handling type and error codes

typedef enum {
    ECCRYPTO_SUCCESS,                          // 0x00
    ECCRYPTO_ERROR,                            // 0x01
    ECCRYPTO_ERROR_DURING_TEST,                // 0x02
    ECCRYPTO_ERROR_UNKNOWN,                    // 0x03
    ECCRYPTO_ERROR_NOT_IMPLEMENTED,            // 0x04
    ECCRYPTO_ERROR_NO_MEMORY,                  // 0x05
    ECCRYPTO_ERROR_INVALID_PARAMETER,          // 0x06
    ECCRYPTO_ERROR_SHARED_KEY,                 // 0x07
    ECCRYPTO_ERROR_SIGNATURE_VERIFICATION,     // 0x08
    ECCRYPTO_ERROR_HASH_TO_CURVE,              // 0x09
    ECCRYPTO_ERROR_END_OF_LIST
} ECCRYPTO_STATUS;

#define ECCRYPTO_STATUS_TYPE_SIZE (ECCRYPTO_ERROR_END_OF_LIST)


// Error message definitions

#define ECCRYPTO_MSG_ERROR                                  "ECCRYPTO_ERROR"
#define ECCRYPTO_MSG_SUCCESS                                "ECCRYPTO_SUCCESS"
#define ECCRYPTO_MSG_ERROR_DURING_TEST                      "ECCRYPTO_ERROR_DURING_TEST"
#define ECCRYPTO_MSG_ERROR_UNKNOWN                          "ECCRYPTO_ERROR_UNKNOWN"
#define ECCRYPTO_MSG_ERROR_NOT_IMPLEMENTED                  "ECCRYPTO_ERROR_NOT_IMPLEMENTED"
#define ECCRYPTO_MSG_ERROR_NO_MEMORY                        "ECCRYPTO_ERROR_NO_MEMORY"
#define ECCRYPTO_MSG_ERROR_INVALID_PARAMETER                "ECCRYPTO_ERROR_INVALID_PARAMETER"
#define ECCRYPTO_MSG_ERROR_SHARED_KEY                       "ECCRYPTO_ERROR_SHARED_KEY"
#define ECCRYPTO_MSG_ERROR_SIGNATURE_VERIFICATION           "ECCRYPTO_ERROR_SIGNATURE_VERIFICATION"
#define ECCRYPTO_MSG_ERROR_HASH_TO_CURVE                    "ECCRYPTO_ERROR_HASH_TO_CURVE"


#ifdef __cplusplus
}
#endif


#endif
