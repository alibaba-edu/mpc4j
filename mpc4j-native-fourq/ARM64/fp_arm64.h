/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: modular arithmetic and other low-level operations for x64 platforms
************************************************************************************/

#ifndef __FP_ARM64_H__
#define __FP_ARM64_H__


// For C++
#ifdef __cplusplus
extern "C" {
#endif


#include "../table_lookup.h"
#include "../FourQ_params.h"


const uint128_t prime1271 = ((uint128_t)1 << 127) - 1;
#define mask63 0x7FFFFFFFFFFFFFFF


void mod1271(felm_t a)
{ // Modular correction, a = a mod (2^127-1)    
    uint128_t* r = (uint128_t*)&a[0];

    *r = *r - prime1271;
    *r = *r + (((uint128_t)0 - (*r >> 127)) & prime1271);
}


__inline void fpcopy1271(felm_t a, felm_t c)
{ // Copy of a field element, c = a
    c[0] = a[0];
    c[1] = a[1];
}


static __inline void fpzero1271(felm_t a)
{ // Zeroing a field element, a = 0
    a[0] = 0;
    a[1] = 0;
}


__inline void fpadd1271(felm_t a, felm_t b, felm_t c)
{ // Field addition, c = a+b mod (2^127-1)
    uint128_t* r = (uint128_t*)&a[0];
    uint128_t* s = (uint128_t*)&b[0];
    uint128_t* t = (uint128_t*)&c[0];

    *t = *r + *s;
    *t += (*t >> 127);
    *t &= prime1271;
}


__inline void fpsub1271(felm_t a, felm_t b, felm_t c)
{ // Field subtraction, c = a-b mod (2^127-1)
    uint128_t* r = (uint128_t*)&a[0];
    uint128_t* s = (uint128_t*)&b[0];
    uint128_t* t = (uint128_t*)&c[0];

    *t = *r - *s;
    *t -= (*t >> 127);
    *t &= prime1271;
} 


void fpneg1271(felm_t a)
{ // Field negation, a = -a mod (2^127-1)
    uint128_t* r = (uint128_t*)&a[0];

    *r = prime1271 - *r;
}


__inline void fpmul1271(felm_t a, felm_t b, felm_t c)
{ // Field multiplication, c = a*b mod (2^127-1)
    uint128_t tt1, tt2, tt3 = {0};
    
    tt1 = (uint128_t)a[0]*b[0];
    tt2 = (uint128_t)a[0]*b[1] + (uint128_t)a[1]*b[0] + (uint64_t)(tt1 >> 64);
    tt3 = (uint128_t)a[1]*(b[1]*2) + ((uint128_t)tt2 >> 63);
    tt1 = (uint64_t)tt1 | ((uint128_t)((uint64_t)tt2 & mask63) << 64);
    tt1 += tt3;
    tt1 = (tt1 >> 127) + (tt1 & prime1271); 
    c[0] = (uint64_t)tt1;
    c[1] = (uint64_t)(tt1 >> 64);
}


void fpsqr1271(felm_t a, felm_t c)
{ // Field squaring, c = a^2 mod (2^127-1)
    uint128_t tt1, tt2, tt3 = {0};
  
    tt1 = (uint128_t)a[0]*a[0];
    tt2 = (uint128_t)a[0]*(a[1]*2) + (uint64_t)(tt1 >> 64);
    tt3 = (uint128_t)a[1]*(a[1]*2) + ((uint128_t)tt2 >> 63);
    tt1 = (uint64_t)tt1 | ((uint128_t)((uint64_t)tt2 & mask63) << 64);
    tt1 += tt3;
    tt1 = (tt1 >> 127) + (tt1 & prime1271); 
    c[0] = (uint64_t)tt1;
    c[1] = (uint64_t)(tt1 >> 64);
}


__inline void fpexp1251(felm_t a, felm_t af)
{ // Exponentiation over GF(p), af = a^(125-1)
    int i;
    felm_t t1, t2, t3, t4, t5;

    fpsqr1271(a, t2);                              
    fpmul1271(a, t2, t2); 
    fpsqr1271(t2, t3);  
    fpsqr1271(t3, t3);                          
    fpmul1271(t2, t3, t3);
    fpsqr1271(t3, t4);  
    fpsqr1271(t4, t4);   
    fpsqr1271(t4, t4);  
    fpsqr1271(t4, t4);                         
    fpmul1271(t3, t4, t4);  
    fpsqr1271(t4, t5);
    for (i=0; i<7; i++) fpsqr1271(t5, t5);                      
    fpmul1271(t4, t5, t5); 
    fpsqr1271(t5, t2); 
    for (i=0; i<15; i++) fpsqr1271(t2, t2);                    
    fpmul1271(t5, t2, t2); 
    fpsqr1271(t2, t1); 
    for (i=0; i<31; i++) fpsqr1271(t1, t1);                         
    fpmul1271(t2, t1, t1); 
    for (i=0; i<32; i++) fpsqr1271(t1, t1);    
    fpmul1271(t1, t2, t1); 
    for (i=0; i<16; i++) fpsqr1271(t1, t1);                         
    fpmul1271(t5, t1, t1);    
    for (i=0; i<8; i++) fpsqr1271(t1, t1);                           
    fpmul1271(t4, t1, t1);    
    for (i=0; i<4; i++) fpsqr1271(t1, t1);                          
    fpmul1271(t3, t1, t1);    
    fpsqr1271(t1, t1);                           
    fpmul1271(a, t1, af);
}


void fpinv1271(felm_t a)
{ // Field inversion, af = a^-1 = a^(p-2) mod p
  // Hardcoded for p = 2^127-1
    felm_t t;

    fpexp1251(a, t);    
    fpsqr1271(t, t);     
    fpsqr1271(t, t);                             
    fpmul1271(a, t, a); 
}


static __inline void multiply(const digit_t* a, const digit_t* b, digit_t* c)
{ // Schoolbook multiprecision multiply, c = a*b   
    unsigned int i, j;
    digit_t u, v, UV[2];
    unsigned char carry = 0;

     for (i = 0; i < (2*NWORDS_ORDER); i++) c[i] = 0;

     for (i = 0; i < NWORDS_ORDER; i++) {
          u = 0;
          for (j = 0; j < NWORDS_ORDER; j++) {
               MUL(a[i], b[j], UV+1, UV[0]); 
               ADDC(0, UV[0], u, carry, v); 
               u = UV[1] + carry;
               ADDC(0, c[i+j], v, carry, v); 
               u = u + carry;
               c[i+j] = v;
          }
          c[NWORDS_ORDER+i] = u;
     }
}


static __inline unsigned char add(const digit_t* a, const digit_t* b, digit_t* c, const unsigned int nwords)
{ // Multiprecision addition, c = a+b. Returns the carry bit 
    unsigned int i;
    unsigned char carry = 0;

    for (i = 0; i < nwords; i++) {
        ADDC(carry, a[i], b[i], carry, c[i]);
    }
    
    return carry;
}


unsigned char subtract(const digit_t* a, const digit_t* b, digit_t* c, const unsigned int nwords)
{ // Multiprecision subtraction, c = a-b. Returns the borrow bit 
    unsigned int i;
    unsigned char borrow = 0;

    for (i = 0; i < nwords; i++) {
        SUBC(borrow, a[i], b[i], borrow, c[i]);
    }

    return borrow;
}   


void subtract_mod_order(const digit_t* a, const digit_t* b, digit_t* c)
{ // Subtraction modulo the curve order, c = a-b mod order
    digit_t mask, carry = 0;
	digit_t* order = (digit_t*)curve_order;
    unsigned int i, bout;

    bout = subtract(a, b, c, NWORDS_ORDER);            // (bout, c) = a - b
    mask = 0 - (digit_t)bout;                          // if bout = 0 then mask = 0x00..0, else if bout = 1 then mask = 0xFF..F

    for (i = 0; i < NWORDS_ORDER; i++) {               // c = c + (mask & order)
        ADDC(carry, c[i], mask & order[i], carry, c[i]);
    }
}


void add_mod_order(const digit_t* a, const digit_t* b, digit_t* c)
{ // Addition modulo the curve order, c = a+b mod order

	add(a, b, c, NWORDS_ORDER);                        // c = a + b
	subtract_mod_order(c, (digit_t*)&curve_order, c);  // if c >= order then c = c - order
}


void Montgomery_multiply_mod_order(const digit_t* ma, const digit_t* mb, digit_t* mc)
{ // 256-bit Montgomery multiplication modulo the curve order, mc = ma*mb*r' mod order, where ma,mb,mc in [0, order-1]
  // ma, mb and mc are assumed to be in Montgomery representation
  // The Montgomery constant r' = -r^(-1) mod 2^(log_2(r)) is the global value "Montgomery_rprime", where r is the order   
    unsigned int i;
    digit_t mask, P[2*NWORDS_ORDER], Q[2*NWORDS_ORDER], temp[2*NWORDS_ORDER];
	digit_t* order = (digit_t*)curve_order;
    unsigned char cout = 0, bout = 0;           

    multiply(ma, mb, P);                               // P = ma * mb
    multiply(P, (digit_t*)&Montgomery_rprime, Q);      // Q = P * r' mod 2^(log_2(r))
    multiply(Q, (digit_t*)&curve_order, temp);         // temp = Q * r
    cout = add(P, temp, temp, 2*NWORDS_ORDER);         // (cout, temp) = P + Q * r     

    for (i = 0; i < NWORDS_ORDER; i++) {               // (cout, mc) = (P + Q * r)/2^(log_2(r))
        mc[i] = temp[NWORDS_ORDER + i];
    }

    // Final, constant-time subtraction     
    bout = subtract(mc, (digit_t*)&curve_order, mc, NWORDS_ORDER);    // (cout, mc) = (cout, mc) - r
    mask = (digit_t)(cout - bout);                     // if (cout, mc) >= 0 then mask = 0x00..0, else if (cout, mc) < 0 then mask = 0xFF..F

    for (i = 0; i < NWORDS_ORDER; i++) {               // temp = mask & r
        temp[i] = (order[i] & mask);
    }
    add(mc, temp, mc, NWORDS_ORDER);                   //  mc = mc + (mask & r)

    return;
}


void modulo_order(digit_t* a, digit_t* c)
{ // Reduction modulo the order using Montgomery arithmetic
  // ma = a*Montgomery_Rprime mod r, where a,ma in [0, r-1], a,ma,r < 2^256
  // c = ma*1*Montgomery_Rprime^(-1) mod r, where ma,c in [0, r-1], ma,c,r < 2^256
    digit_t ma[NWORDS_ORDER], one[NWORDS_ORDER] = {0};
    
    one[0] = 1;
	Montgomery_multiply_mod_order(a, (digit_t*)&Montgomery_Rprime, ma);
	Montgomery_multiply_mod_order(ma, one, c);
}


void conversion_to_odd(digit_t* k, digit_t* k_odd)
{// Convert scalar to odd if even using the prime subgroup order r
    digit_t i, mask;
	digit_t* order = (digit_t*)curve_order;
    unsigned char carry = 0;

    mask = ~(0 - (k[0] & 1));     

    for (i = 0; i < NWORDS_ORDER; i++) {  // If (k is odd) then k_odd = k else k_odd = k + r 
        ADDC(carry, order[i] & mask, k[i], carry, k_odd[i]);
    }
}


void fpdiv1271(felm_t a)
{ // Field division by two, c = a/2 mod p
     digit_t mask, temp[2];
     unsigned char carry;

     mask = (0 - (1 & a[0]));
     ADDC(0,     a[0], mask, carry, temp[0]);
     ADDC(carry, a[1], (mask >> 1), carry, temp[1]);
     SHIFTR(temp[1], temp[0], 1, a[0], RADIX);
     a[1] = (temp[1] >> 1);
}


void fp2div1271(f2elm_t a)
{ // GF(p^2) division by two c = a/2 mod p
     digit_t mask, temp[2];
     unsigned char carry;

     mask = (0 - (1 & a[0][0]));
     ADDC(0,     a[0][0], mask, carry, temp[0]);
     ADDC(carry, a[0][1], (mask >> 1), carry, temp[1]);
     SHIFTR(temp[1], temp[0], 1, a[0][0], RADIX);
     a[0][1] = (temp[1] >> 1);
     
     mask = (0 - (1 & a[1][0]));
     ADDC(0,     a[1][0], mask, carry, temp[0]);
     ADDC(carry, a[1][1], (mask >> 1), carry, temp[1]);
     SHIFTR(temp[1], temp[0], 1, a[1][0], RADIX);
     a[1][1] = (temp[1] >> 1);
}


#ifdef __cplusplus
}
#endif


#endif
