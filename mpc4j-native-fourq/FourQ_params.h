/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: FourQ's curve parameters
*
* This code is based on the paper "FourQ: four-dimensional decompositions on a 
* Q-curve over the Mersenne prime" by Craig Costello and Patrick Longa, in Advances 
* in Cryptology - ASIACRYPT, 2015.
* Preprint available at http://eprint.iacr.org/2015/565.
************************************************************************************/ 

#pragma once
#ifndef __FOURQ_PARAMS_H__
#define __FOURQ_PARAMS_H__

#include "FourQ_internal.h"


// Encoding of field elements, elements over Z_r and elements over GF(p^2):
// -----------------------------------------------------------------------
// Elements over GF(p) and Z_r are encoded with the least significant digit located in the leftmost position (i.e., little endian format). 
// Elements (a+b*i) over GF(p^2), where a and b are defined over GF(p), are encoded as a||b, with a in the least significant position.

static const uint64_t PARAMETER_d[4]       = { 0x0000000000000142, 0x00000000000000E4, 0xB3821488F1FC0C8D, 0x5E472F846657E0FC };
static const uint64_t GENERATOR_x[4]       = { 0x286592AD7B3833AA, 0x1A3472237C2FB305, 0x96869FB360AC77F6, 0x1E1F553F2878AA9C };
static const uint64_t GENERATOR_y[4]       = { 0xB924A2462BCBB287, 0x0E3FEE9BA120785A, 0x49A7C344844C8B5C, 0x6E1C4AF8630E0242 };
static const uint64_t curve_order[4]       = { 0x2FB2540EC7768CE7, 0xDFBD004DFE0F7999, 0xF05397829CBC14E5, 0x0029CBC14E5E0A72 };
static const uint64_t Montgomery_Rprime[4] = { 0xC81DB8795FF3D621, 0x173EA5AAEA6B387D, 0x3D01B7C72136F61C, 0x0006A5F16AC8F9D3 };
static const uint64_t Montgomery_rprime[4] = { 0xE12FE5F079BC3929, 0xD75E78B8D1FCDCF3, 0xBCE409ED76B5DB21, 0xF32702FDAFC1C074 };


// Constants for hash to FourQ function

#if (RADIX == 32)
    static felm_t con1 = { 6651107, 0, 4290264256, 2147483647 };
    static felm_t con2 = { 1725590130, 1719979744, 2225079900, 707200452 };
    static felm_t b0 = { 3738038324, 2664081113, 587564626, 1252475115 };
    static felm_t b1 = { 17, 0, 4294967284, 2147483647 };
    static felm_t A0 = { 1289, 0, 4294966384, 2147483647 };
    static felm_t A1 = { 1007904792, 2866591091, 4136083791, 1668973403 };
#elif (RADIX == 64)
    static felm_t con1 = { 6651107ULL, 9223372036850072768ULL };
    static felm_t con2 = { 7387256751988042354ULL, 3037402815281497692ULL };
    static felm_t b0 = { 11442141257964318772ULL, 5379339658566403666ULL };
    static felm_t b1 = { 17ULL, 9223372036854775796ULL };
    static felm_t A0 = { 1289ULL, 9223372036854774896ULL };
    static felm_t A1 = { 12311914987857864728ULL, 7168186187914912079ULL };
#endif

#endif