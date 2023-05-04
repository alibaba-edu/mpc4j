/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: table lookup functions
************************************************************************************/

#ifndef __TABLE_LOOKUP_H__
#define __TABLE_LOOKUP_H__


// For C++
#ifdef __cplusplus
extern "C" {
#endif


#include "FourQ_internal.h"
#if (TARGET == TARGET_AMD64) && !defined(GENERIC_IMPLEMENTATION)
    #include <immintrin.h>
#endif


void table_lookup_1x8(point_extproj_precomp_t* table, point_extproj_precomp_t P, unsigned int digit, unsigned int sign_mask)
{ // Constant-time table lookup to extract a point represented as (X+Y,Y-X,2Z,2dT) corresponding to extended twisted Edwards coordinates (X:Y:Z:T)
  // Inputs: sign_mask, digit, table containing 8 points
  // Output: P = sign*table[digit], where sign=1 if sign_mask=0xFF...FF and sign=-1 if sign_mask=0

#if (SIMD_SUPPORT == AVX2_SUPPORT)  
#if defined(ASM_SUPPORT)
    table_lookup_1x8_a(table, P, &digit, &sign_mask);
#else
    __m256i point[4], temp_point[4], full_mask; 
    unsigned int i;
    int mask;
    
    point[0] = _mm256_loadu_si256((__m256i*)table[0]->xy);              // point = table[0] 
    point[1] = _mm256_loadu_si256((__m256i*)table[0]->yx);  
    point[2] = _mm256_loadu_si256((__m256i*)table[0]->z2);  
    point[3] = _mm256_loadu_si256((__m256i*)table[0]->t2); 

    for (i = 1; i < 8; i++) 
    { 
        digit--;
        // While digit>=0 mask = 0xFF...F else mask = 0x00...0
        mask = (int)(digit >> (8*sizeof(digit)-1)) - 1;
        temp_point[0] = _mm256_loadu_si256((__m256i*)table[i]->xy);     // temp_point = table[i]
        temp_point[1] = _mm256_loadu_si256((__m256i*)table[i]->yx);
        temp_point[2] = _mm256_loadu_si256((__m256i*)table[i]->z2);
        temp_point[3] = _mm256_loadu_si256((__m256i*)table[i]->t2);
        // If mask = 0x00...0 then point = point, else if mask = 0xFF...F then point = temp_point
        full_mask = _mm256_set1_epi32(mask);
        temp_point[0] = _mm256_xor_si256(point[0], temp_point[0]);
        temp_point[1] = _mm256_xor_si256(point[1], temp_point[1]);
        temp_point[2] = _mm256_xor_si256(point[2], temp_point[2]);
        temp_point[3] = _mm256_xor_si256(point[3], temp_point[3]);
        point[0] = _mm256_xor_si256(_mm256_and_si256(temp_point[0], full_mask), point[0]);
        point[1] = _mm256_xor_si256(_mm256_and_si256(temp_point[1], full_mask), point[1]);
        point[2] = _mm256_xor_si256(_mm256_and_si256(temp_point[2], full_mask), point[2]);
        point[3] = _mm256_xor_si256(_mm256_and_si256(temp_point[3], full_mask), point[3]);
    }
                                
    temp_point[3] = _mm256_loadu_si256((__m256i*)point+3); 
    temp_point[0] = _mm256_loadu_si256((__m256i*)point+1);                   // point: x+y,y-x,2dt coordinate, temp_point: y-x,x+y,-2dt coordinate 
    temp_point[1] = _mm256_loadu_si256((__m256i*)point);
    full_mask = _mm256_set1_epi32((int)sign_mask); 
    fpneg1271((digit_t*)temp_point+12);                                      // Negate 2dt coordinate
    fpneg1271((digit_t*)temp_point+14);                                      // If sign_mask = 0 then choose negative of the point
    point[0] = _mm256_xor_si256(_mm256_and_si256(_mm256_xor_si256(point[0], temp_point[0]), full_mask), temp_point[0]);
    point[1] = _mm256_xor_si256(_mm256_and_si256(_mm256_xor_si256(point[1], temp_point[1]), full_mask), temp_point[1]);
    point[3] = _mm256_xor_si256(_mm256_and_si256(_mm256_xor_si256(point[3], temp_point[3]), full_mask), temp_point[3]);
    _mm256_storeu_si256((__m256i*)P->xy, point[0]);    
    _mm256_storeu_si256((__m256i*)P->yx, point[1]);     
    _mm256_storeu_si256((__m256i*)P->z2, point[2]);  
    _mm256_storeu_si256((__m256i*)P->t2, point[3]); 
#endif
#elif (SIMD_SUPPORT == AVX_SUPPORT)
    __m256d point[4], temp_point[4], full_mask; 
    unsigned int i;
    int mask;
    
    point[0] = _mm256_loadu_pd((double const*)table[0]->xy);                 // point = table[0] 
    point[1] = _mm256_loadu_pd((double const*)table[0]->yx);  
    point[2] = _mm256_loadu_pd((double const*)table[0]->z2);  
    point[3] = _mm256_loadu_pd((double const*)table[0]->t2); 

    for (i = 1; i < 8; i++) 
    { 
        digit--;
        // While digit>=0 mask = 0xFF...F else sign = 0x00...0
        mask = (int)(digit >> (8*sizeof(digit)-1)) - 1;
        full_mask = _mm256_set1_pd ((double)mask);
        temp_point[0] = _mm256_loadu_pd((double const*)table[i]->xy);        // temp_point = table[i]
        temp_point[1] = _mm256_loadu_pd((double const*)table[i]->yx);
        temp_point[2] = _mm256_loadu_pd((double const*)table[i]->z2);
        temp_point[3] = _mm256_loadu_pd((double const*)table[i]->t2);
        // If mask = 0x00...0 then point = point, else if mask = 0xFF...F then point = temp_point
        point[0] = _mm256_blendv_pd(point[0], temp_point[0], full_mask);     
        point[1] = _mm256_blendv_pd(point[1], temp_point[1], full_mask);  
        point[2] = _mm256_blendv_pd(point[2], temp_point[2], full_mask);  
        point[3] = _mm256_blendv_pd(point[3], temp_point[3], full_mask); 
    }
                           
    temp_point[3] = _mm256_loadu_pd((double const*)point+12); 
    temp_point[0] = _mm256_loadu_pd((double const*)point+4);                 // point: x+y,y-x,2dt coordinate, temp_point: y-x,x+y,-2dt coordinate 
    temp_point[1] = _mm256_loadu_pd((double const*)point);
    full_mask = _mm256_set1_pd((double)((int)sign_mask));
    fpneg1271((digit_t*)temp_point+12);                                      // Negate 2dt coordinate
    fpneg1271((digit_t*)temp_point+14);            
    point[0] = _mm256_blendv_pd(temp_point[0], point[0], full_mask);         // If sign_mask = 0 then choose negative of the point
    point[1] = _mm256_blendv_pd(temp_point[1], point[1], full_mask);
    point[3] = _mm256_blendv_pd(temp_point[3], point[3], full_mask); 
    _mm256_storeu_pd((double*)P->xy, point[0]);    
    _mm256_storeu_pd((double*)P->yx, point[1]);     
    _mm256_storeu_pd((double*)P->z2, point[2]);  
    _mm256_storeu_pd((double*)P->t2, point[3]); 
#else
    point_extproj_precomp_t point, temp_point;
    unsigned int i, j;
    digit_t mask;
                                  
    ecccopy_precomp(table[0], point);                                        // point = table[0]

    for (i = 1; i < 8; i++)
    {
        digit--;
        // While digit>=0 mask = 0xFF...F else sign = 0x00...0
        mask = (digit_t)(digit >> (8*sizeof(digit)-1)) - 1;
        ecccopy_precomp(table[i], temp_point);                               // temp_point = table[i] 
        // If mask = 0x00...0 then point = point, else if mask = 0xFF...F then point = temp_point
        for (j = 0; j < NWORDS_FIELD; j++) {
            point->xy[0][j] = (mask & (point->xy[0][j] ^ temp_point->xy[0][j])) ^ point->xy[0][j];
            point->xy[1][j] = (mask & (point->xy[1][j] ^ temp_point->xy[1][j])) ^ point->xy[1][j];
            point->yx[0][j] = (mask & (point->yx[0][j] ^ temp_point->yx[0][j])) ^ point->yx[0][j];
            point->yx[1][j] = (mask & (point->yx[1][j] ^ temp_point->yx[1][j])) ^ point->yx[1][j];
            point->z2[0][j] = (mask & (point->z2[0][j] ^ temp_point->z2[0][j])) ^ point->z2[0][j];
            point->z2[1][j] = (mask & (point->z2[1][j] ^ temp_point->z2[1][j])) ^ point->z2[1][j];
            point->t2[0][j] = (mask & (point->t2[0][j] ^ temp_point->t2[0][j])) ^ point->t2[0][j];
            point->t2[1][j] = (mask & (point->t2[1][j] ^ temp_point->t2[1][j])) ^ point->t2[1][j];
        }
    }
    
    fp2copy1271(point->t2, temp_point->t2);
    fp2copy1271(point->xy, temp_point->yx);                                  // point: x+y,y-x,2dt coordinate, temp_point: y-x,x+y,-2dt coordinate
    fp2copy1271(point->yx, temp_point->xy);                                   
    fpneg1271(temp_point->t2[0]);                                            // Negate 2dt coordinate
    fpneg1271(temp_point->t2[1]);             
    for (j = 0; j < NWORDS_FIELD; j++) {                                     // If sign_mask = 0 then choose negative of the point
        point->xy[0][j] = ((digit_t)((int)sign_mask) & (point->xy[0][j] ^ temp_point->xy[0][j])) ^ temp_point->xy[0][j];
        point->xy[1][j] = ((digit_t)((int)sign_mask) & (point->xy[1][j] ^ temp_point->xy[1][j])) ^ temp_point->xy[1][j];
        point->yx[0][j] = ((digit_t)((int)sign_mask) & (point->yx[0][j] ^ temp_point->yx[0][j])) ^ temp_point->yx[0][j];
        point->yx[1][j] = ((digit_t)((int)sign_mask) & (point->yx[1][j] ^ temp_point->yx[1][j])) ^ temp_point->yx[1][j];
        point->t2[0][j] = ((digit_t)((int)sign_mask) & (point->t2[0][j] ^ temp_point->t2[0][j])) ^ temp_point->t2[0][j];
        point->t2[1][j] = ((digit_t)((int)sign_mask) & (point->t2[1][j] ^ temp_point->t2[1][j])) ^ temp_point->t2[1][j];
    }                                  
    ecccopy_precomp(point, P); 
#endif
}


void table_lookup_fixed_base(point_precomp_t* table, point_precomp_t P, unsigned int digit, unsigned int sign)
{ // Constant-time table lookup to extract a point represented as (x+y,y-x,2t) corresponding to extended twisted Edwards coordinates (X:Y:Z:T) with Z=1
  // Inputs: sign, digit, table containing VPOINTS_FIXEDBASE = 2^(W_FIXEDBASE-1) points
  // Output: if sign=0 then P = table[digit], else if (sign=-1) then P = -table[digit]

#if (SIMD_SUPPORT == AVX2_SUPPORT)
    __m256i point[3], temp_point[3], full_mask; 
    unsigned int i;
    int mask;
    
    point[0] = _mm256_loadu_si256((__m256i*)table[0]->xy);                  // point = table[0] 
    point[1] = _mm256_loadu_si256((__m256i*)table[0]->yx);  
    point[2] = _mm256_loadu_si256((__m256i*)table[0]->t2); 

    for (i = 1; i < VPOINTS_FIXEDBASE; i++) 
    { 
        digit--;
        // While digit>=0 mask = 0xFF...F else sign = 0x00...0
        mask = (int)(digit >> (8*sizeof(digit)-1)) - 1;
        temp_point[0] = _mm256_loadu_si256((__m256i*)table[i]->xy);         // temp_point = table[i]
        temp_point[1] = _mm256_loadu_si256((__m256i*)table[i]->yx);
        temp_point[2] = _mm256_loadu_si256((__m256i*)table[i]->t2);
        // If mask = 0x00...0 then point = point, else if mask = 0xFF...F then point = temp_point
        full_mask = _mm256_set1_epi32(mask);
        temp_point[0] = _mm256_xor_si256(point[0], temp_point[0]);
        temp_point[1] = _mm256_xor_si256(point[1], temp_point[1]);
        temp_point[2] = _mm256_xor_si256(point[2], temp_point[2]);
        point[0] = _mm256_xor_si256(_mm256_and_si256(temp_point[0], full_mask), point[0]);
        point[1] = _mm256_xor_si256(_mm256_and_si256(temp_point[1], full_mask), point[1]);
        point[2] = _mm256_xor_si256(_mm256_and_si256(temp_point[2], full_mask), point[2]);
    }
                                
    temp_point[2] = _mm256_loadu_si256((__m256i*)point+2); 
    temp_point[0] = _mm256_loadu_si256((__m256i*)point+1);                  // point: x+y,y-x,2dt coordinate, temp_point: y-x,x+y,-2dt coordinate 
    temp_point[1] = _mm256_loadu_si256((__m256i*)point);
    full_mask = _mm256_set1_epi32((int)sign); 
    fpneg1271((digit_t*)temp_point+8);                                      // Negate 2dt coordinate
    fpneg1271((digit_t*)temp_point+10);                                     // If sign = 0xFF...F then choose negative of the point
    point[0] = _mm256_xor_si256(_mm256_and_si256(_mm256_xor_si256(point[0], temp_point[0]), full_mask), point[0]);
    point[1] = _mm256_xor_si256(_mm256_and_si256(_mm256_xor_si256(point[1], temp_point[1]), full_mask), point[1]);
    point[2] = _mm256_xor_si256(_mm256_and_si256(_mm256_xor_si256(point[2], temp_point[2]), full_mask), point[2]);
    _mm256_storeu_si256((__m256i*)P->xy, point[0]);    
    _mm256_storeu_si256((__m256i*)P->yx, point[1]);     
    _mm256_storeu_si256((__m256i*)P->t2, point[2]);
        
#elif (SIMD_SUPPORT >= AVX_SUPPORT)
    __m256d point[3], temp_point[3], full_mask; 
    unsigned int i;
    int mask;

    point[0] = _mm256_loadu_pd((double const*)table[0]->xy);                // point = table[0] 
    point[1] = _mm256_loadu_pd((double const*)table[0]->yx);  
    point[2] = _mm256_loadu_pd((double const*)table[0]->t2);  

    for (i = 1; i < VPOINTS_FIXEDBASE; i++) 
    { 
        digit--;
        // While digit>=0 mask = 0xFF...F else sign = 0x00...0
        mask = (int)(digit >> (8*sizeof(digit)-1)) - 1;
        full_mask = _mm256_set1_pd((double)mask);
        temp_point[0] = _mm256_loadu_pd((double const*)table[i]->xy);       // temp_point = table[i+1]
        temp_point[1] = _mm256_loadu_pd((double const*)table[i]->yx);
        temp_point[2] = _mm256_loadu_pd((double const*)table[i]->t2);
        // If mask = 0x00...0 then point = point, else if mask = 0xFF...F then point = temp_point
        point[0] = _mm256_blendv_pd(point[0], temp_point[0], full_mask);     
        point[1] = _mm256_blendv_pd(point[1], temp_point[1], full_mask);    
        point[2] = _mm256_blendv_pd(point[2], temp_point[2], full_mask); 
    }
                                   
    temp_point[2] = _mm256_loadu_pd((double const*)point+2*4);              // point: x+y,y-x,2dt coordinate, temp_point: y-x,x+y,-2dt coordinate
    temp_point[0] = _mm256_loadu_pd((double const*)point+1*4);  
    temp_point[1] = _mm256_loadu_pd((double const*)point);  
    full_mask = _mm256_set1_pd((double)((int)sign));     
    fpneg1271((digit_t*)temp_point+8);                                      // Negate 2dt coordinate
    fpneg1271((digit_t*)temp_point+10);                                    
    point[0] = _mm256_blendv_pd(point[0], temp_point[0], full_mask);        // If sign = 0xFF...F then choose negative of the point
    point[1] = _mm256_blendv_pd(point[1], temp_point[1], full_mask);
    point[2] = _mm256_blendv_pd(point[2], temp_point[2], full_mask);
    _mm256_storeu_pd((double*)P->xy, point[0]); 
    _mm256_storeu_pd((double*)P->yx, point[1]); 
    _mm256_storeu_pd((double*)P->t2, point[2]);
#else
    point_precomp_t point, temp_point;
    unsigned int i, j;
    digit_t mask;
                                   
    ecccopy_precomp_fixed_base(table[0], point);                             // point = table[0]

    for (i = 1; i < VPOINTS_FIXEDBASE; i++)
    {
        digit--;
        // While digit>=0 mask = 0xFF...F else sign = 0x00...0
        mask = (digit_t)(digit >> (8*sizeof(digit)-1)) - 1;
        ecccopy_precomp_fixed_base(table[i], temp_point);                    // temp_point = table[i] 
        // If mask = 0x00...0 then point = point, else if mask = 0xFF...F then point = temp_point
        for (j = 0; j < NWORDS_FIELD; j++) {
            point->xy[0][j] = (mask & (point->xy[0][j] ^ temp_point->xy[0][j])) ^ point->xy[0][j];
            point->xy[1][j] = (mask & (point->xy[1][j] ^ temp_point->xy[1][j])) ^ point->xy[1][j];
            point->yx[0][j] = (mask & (point->yx[0][j] ^ temp_point->yx[0][j])) ^ point->yx[0][j];
            point->yx[1][j] = (mask & (point->yx[1][j] ^ temp_point->yx[1][j])) ^ point->yx[1][j];
            point->t2[0][j] = (mask & (point->t2[0][j] ^ temp_point->t2[0][j])) ^ point->t2[0][j];
            point->t2[1][j] = (mask & (point->t2[1][j] ^ temp_point->t2[1][j])) ^ point->t2[1][j];
        }
    }
    
    fp2copy1271(point->t2, temp_point->t2);
    fp2copy1271(point->xy, temp_point->yx);                                  // point: x+y,y-x,2dt coordinate, temp_point: y-x,x+y,-2dt coordinate
    fp2copy1271(point->yx, temp_point->xy);                                   
    fpneg1271(temp_point->t2[0]);                                            // Negate 2dt coordinate
    fpneg1271(temp_point->t2[1]);             
    for (j = 0; j < NWORDS_FIELD; j++) {                                     // If sign = 0xFF...F then choose negative of the point
        point->xy[0][j] = ((digit_t)((int)sign) & (point->xy[0][j] ^ temp_point->xy[0][j])) ^ point->xy[0][j];
        point->xy[1][j] = ((digit_t)((int)sign) & (point->xy[1][j] ^ temp_point->xy[1][j])) ^ point->xy[1][j];
        point->yx[0][j] = ((digit_t)((int)sign) & (point->yx[0][j] ^ temp_point->yx[0][j])) ^ point->yx[0][j];
        point->yx[1][j] = ((digit_t)((int)sign) & (point->yx[1][j] ^ temp_point->yx[1][j])) ^ point->yx[1][j];
        point->t2[0][j] = ((digit_t)((int)sign) & (point->t2[0][j] ^ temp_point->t2[0][j])) ^ point->t2[0][j];
        point->t2[1][j] = ((digit_t)((int)sign) & (point->t2[1][j] ^ temp_point->t2[1][j])) ^ point->t2[1][j];
    }                                  
    ecccopy_precomp_fixed_base(point, P); 
#endif
}


#ifdef __cplusplus
}
#endif


#endif