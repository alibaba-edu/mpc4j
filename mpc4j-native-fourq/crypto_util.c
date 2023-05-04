/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: crypto utility functions
************************************************************************************/ 

#include "FourQ_internal.h"
#include "FourQ_params.h"
#include <string.h>

static digit_t mask4000 = (digit_t)1 << (sizeof(digit_t)*8 - 2);
static digit_t mask7fff = (digit_t)(-1) >> 1;


bool is_zero_ct(digit_t* a, unsigned int nwords)
{ // Check if multiprecision element is zero
    digit_t x;
    unsigned int i;

    x = a[0];
    for (i = 1; i < nwords; i++) {
        x |= a[i];
    }

    return (bool)(1 ^ ((x | (0-x)) >> (RADIX-1)));
}


void encode(point_t P, unsigned char* Pencoded)
{ // Encode point P
  // SECURITY NOTE: this function does not run in constant time.
    digit_t temp1 = (P->x[1][NWORDS_FIELD-1] & mask4000) << 1;
    digit_t temp2 = (P->x[0][NWORDS_FIELD-1] & mask4000) << 1;

    memmove(Pencoded, P->y, 32);
    if (is_zero_ct((digit_t*)P->x, NWORDS_FIELD) == true) {
        ((digit_t*)Pencoded)[2*NWORDS_FIELD-1] |= temp1;
    } else {
        ((digit_t*)Pencoded)[2*NWORDS_FIELD-1] |= temp2;
    }
}


ECCRYPTO_STATUS decode(const unsigned char* Pencoded, point_t P)
{ // Decode point P
  // SECURITY NOTE: this function does not run in constant time.
    felm_t r, t, t0, t1, t2, t3, t4;
    f2elm_t u, v, one = {0};
    digit_t sign_dec;
    point_extproj_t R;
    unsigned int i, sign;

    one[0][0] = 1;
    memmove((unsigned char*)P->y, Pencoded, 32);    // Decoding y-coordinate and sign
    sign = (unsigned int)(Pencoded[31] >> 7);
    P->y[1][NWORDS_FIELD-1] &= mask7fff;

    fp2sqr1271(P->y, u);
    fp2mul1271(u, (felm_t*)&PARAMETER_d, v);
    fp2sub1271(u, one, u);
    fp2add1271(v, one, v);

    fpsqr1271(v[0], t0);                            // t0 = v0^2
    fpsqr1271(v[1], t1);                            // t1 = v1^2
    fpadd1271(t0, t1, t0);                          // t0 = t0+t1   
    fpmul1271(u[0], v[0], t1);                      // t1 = u0*v0
    fpmul1271(u[1], v[1], t2);                      // t2 = u1*v1 
    fpadd1271(t1, t2, t1);                          // t1 = t1+t2  
    fpmul1271(u[1], v[0], t2);                      // t2 = u1*v0
    fpmul1271(u[0], v[1], t3);                      // t3 = u0*v1
    fpsub1271(t2, t3, t2);                          // t2 = t2-t3    
    fpsqr1271(t1, t3);                              // t3 = t1^2    
    fpsqr1271(t2, t4);                              // t4 = t2^2
    fpadd1271(t3, t4, t3);                          // t3 = t3+t4
    for (i = 0; i < 125; i++) {                     // t3 = t3^(2^125)
        fpsqr1271(t3, t3);
    }

    fpadd1271(t1, t3, t);                           // t = t1+t3
    mod1271(t);
    if (is_zero_ct(t, NWORDS_FIELD) == true) {
        fpsub1271(t1, t3, t);                       // t = t1-t3
    }
    fpadd1271(t, t, t);                             // t = 2*t            
    fpsqr1271(t0, t3);                              // t3 = t0^2      
    fpmul1271(t0, t3, t3);                          // t3 = t3*t0   
    fpmul1271(t, t3, t3);                           // t3 = t3*t
    fpexp1251(t3, r);                               // r = t3^(2^125-1)  
    fpmul1271(t0, r, t3);                           // t3 = t0*r          
    fpmul1271(t, t3, P->x[0]);                      // x0 = t*t3 
    fpsqr1271(P->x[0], t1);
    fpmul1271(t0, t1, t1);                          // t1 = t0*x0^2 
    fpdiv1271(P->x[0]);                             // x0 = x0/2         
    fpmul1271(t2, t3, P->x[1]);                     // x1 = t3*t2  

    fpsub1271(t, t1, t);
    mod1271(t);
    if (is_zero_ct(t, NWORDS_FIELD) == false) {        // If t != t1 then swap x0 and x1       
        fpcopy1271(P->x[0], t0);
        fpcopy1271(P->x[1], P->x[0]);
        fpcopy1271(t0, P->x[1]);
    }
    
    mod1271(P->x[0]);
    if (is_zero_ct((digit_t*)P->x, NWORDS_FIELD) == true) {
        sign_dec = ((digit_t*)&P->x[1])[NWORDS_FIELD-1] >> (sizeof(digit_t)*8 - 2);
    } else {
        sign_dec = ((digit_t*)&P->x[0])[NWORDS_FIELD-1] >> (sizeof(digit_t)*8 - 2);
    }

    if (sign != (unsigned int)sign_dec) {           // If sign of x-coordinate decoded != input sign bit, then negate x-coordinate
        fp2neg1271(P->x);
    }

    point_setup(P, R);
    if (ecc_point_validate(R) == false) {
        fpneg1271(R->x[1]);
        fpcopy1271(R->x[1], P->x[1]);
        if (ecc_point_validate(R) == false) {       // Final point validation
            return ECCRYPTO_ERROR;
        }
    }

    return ECCRYPTO_SUCCESS;
}


void to_Montgomery(const digit_t* ma, digit_t* c)
{ // Converting to Montgomery representation

    Montgomery_multiply_mod_order(ma, (digit_t*)&Montgomery_Rprime, c);
}


void from_Montgomery(const digit_t* a, digit_t* mc)
{ // Converting from Montgomery to standard representation
    digit_t one[NWORDS_ORDER] = {0};
    one[0] = 1;

    Montgomery_multiply_mod_order(a, one, mc);
}


void Montgomery_inversion_mod_order(const digit_t* ma, digit_t* mc)
{ // (Non-constant time) Montgomery inversion modulo the curve order using a^(-1) = a^(order-2) mod order
  // This function uses the sliding-window method
    sdigit_t i = 256;
    unsigned int j, nwords = NWORDS_ORDER;
    digit_t temp, bit = 0, count, mod2, k_EXPON = 5;       // Fixing parameter k to 5 for the sliding windows method
    digit_t modulus2[NWORDS_ORDER] = {0}, npoints = 16;
    digit_t input_a[NWORDS_ORDER];
    digit_t table[16][NWORDS_ORDER];                       // Fixing the number of precomputed elements to 16 (assuming k = 5)
    digit_t mask = (digit_t)1 << (sizeof(digit_t)*8 - 1);  // 0x800...000
    digit_t mask2 = ~((digit_t)(-1) >> k_EXPON);           // 0xF800...000, assuming k = 5

    // SECURITY NOTE: this function does not run in constant time because the modulus is assumed to be public.

    modulus2[0] = 2;
    subtract((digit_t*)&curve_order, modulus2, modulus2, nwords);       // modulus-2

    // Precomputation stage
    memmove((unsigned char*)&table[0], (unsigned char*)ma, 32);         // table[0] = ma 
    Montgomery_multiply_mod_order(ma, ma, input_a);                     // ma^2
    for (j = 0; j < npoints - 1; j++) {
        Montgomery_multiply_mod_order(table[j], input_a, table[j+1]);   // table[j+1] = table[j] * ma^2
    }

    while (bit != 1) {                                                  // Shift (modulus-2) to the left until getting first bit 1
        i--;
        temp = 0;
        for (j = 0; j < nwords; j++) {
            bit = (modulus2[j] & mask) >> (sizeof(digit_t)*8 - 1);
            modulus2[j] = (modulus2[j] << 1) | temp;
            temp = bit;
        }
    }

    // Evaluation stage
    memmove((unsigned char*)mc, (unsigned char*)ma, 32);
    bit = (modulus2[nwords-1] & mask) >> (sizeof(digit_t)*8 - 1);
    while (i > 0) {
        if (bit == 0) {                                       // Square accumulated value because bit = 0 and shift (modulus-2) one bit to the left
            Montgomery_multiply_mod_order(mc, mc, mc);        // mc = mc^2
            i--;
            for (j = (nwords - 1); j > 0; j--) {
                SHIFTL(modulus2[j], modulus2[j-1], 1, modulus2[j], RADIX);
            }
            modulus2[0] = modulus2[0] << 1;
        } else {                                              // "temp" will store the longest odd bitstring with "count" bits s.t. temp <= 2^k - 1 
            count = k_EXPON;
            temp = (modulus2[nwords-1] & mask2) >> (sizeof(digit_t)*8 - k_EXPON);  // Extracting next k bits to the left
            mod2 = temp & 1;
            while (mod2 == 0) {                               // if even then shift to the right and adjust count
                temp = (temp >> 1);
                mod2 = temp & 1;
                count--;
            }
            for (j = 0; j < count; j++) {                     // mc = mc^count
                Montgomery_multiply_mod_order(mc, mc, mc);
            }
            Montgomery_multiply_mod_order(mc, table[(temp-1) >> 1], mc);   // mc = mc * table[(temp-1)/2] 
            i = i - count;

            for (j = (nwords - 1); j > 0; j--) {              // Shift (modulus-2) "count" bits to the left
                SHIFTL(modulus2[j], modulus2[j-1], count, modulus2[j], RADIX);
            }
            modulus2[0] = modulus2[0] << count;
        }
        bit = (modulus2[nwords - 1] & mask) >> (sizeof(digit_t)*8 - 1);
    }
}


const char* FourQ_get_error_message(ECCRYPTO_STATUS Status)
{ // Output error/success message for a given ECCRYPTO_STATUS
    struct error_mapping {
        unsigned int index;
        char*        string;
    } mapping[ECCRYPTO_STATUS_TYPE_SIZE] = {
        {ECCRYPTO_ERROR, ECCRYPTO_MSG_ERROR},
        {ECCRYPTO_SUCCESS, ECCRYPTO_MSG_SUCCESS},
        {ECCRYPTO_ERROR_DURING_TEST, ECCRYPTO_MSG_ERROR_DURING_TEST},
        {ECCRYPTO_ERROR_UNKNOWN, ECCRYPTO_MSG_ERROR_UNKNOWN},
        {ECCRYPTO_ERROR_NOT_IMPLEMENTED, ECCRYPTO_MSG_ERROR_NOT_IMPLEMENTED},
        {ECCRYPTO_ERROR_NO_MEMORY, ECCRYPTO_MSG_ERROR_NO_MEMORY},
        {ECCRYPTO_ERROR_INVALID_PARAMETER, ECCRYPTO_MSG_ERROR_INVALID_PARAMETER},
        {ECCRYPTO_ERROR_SHARED_KEY, ECCRYPTO_MSG_ERROR_SHARED_KEY},
        {ECCRYPTO_ERROR_SIGNATURE_VERIFICATION, ECCRYPTO_MSG_ERROR_SIGNATURE_VERIFICATION},
        {ECCRYPTO_ERROR_HASH_TO_CURVE, ECCRYPTO_MSG_ERROR_HASH_TO_CURVE},
    };

    if (Status >= ECCRYPTO_STATUS_TYPE_SIZE || mapping[Status].string == NULL) {
        return "Unrecognized ECCRYPTO_STATUS";
    } else {
        return mapping[Status].string;
    }
};