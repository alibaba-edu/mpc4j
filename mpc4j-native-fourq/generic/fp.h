/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: portable modular arithmetic and other low-level operations
************************************************************************************/

#ifndef __FP_H__
#define __FP_H__


// For C++
#ifdef __cplusplus
extern "C" {
#endif


#include "../table_lookup.h"
#include "../FourQ_params.h"

const digit_t mask_7fff = (digit_t)(-1) >> 1;
const digit_t prime1271_0 = (digit_t)(-1);
#define prime1271_1 mask_7fff


void digit_x_digit(digit_t a, digit_t b, digit_t* c)
{ // Digit multiplication, digit * digit -> 2-digit result    
    register digit_t al, ah, bl, bh, temp;
    digit_t albl, albh, ahbl, ahbh, res1, res2, res3, carry;
    digit_t mask_low = (digit_t)(-1) >> (sizeof(digit_t)*4), mask_high = (digit_t)(-1) << (sizeof(digit_t)*4);

    al = a & mask_low;                        // Low part
    ah = a >> (sizeof(digit_t) * 4);          // High part
    bl = b & mask_low;
    bh = b >> (sizeof(digit_t) * 4);

    albl = al*bl;
    albh = al*bh;
    ahbl = ah*bl;
    ahbh = ah*bh;
    c[0] = albl & mask_low;                   // C00

    res1 = albl >> (sizeof(digit_t) * 4);
    res2 = ahbl & mask_low;
    res3 = albh & mask_low;  
    temp = res1 + res2 + res3;
    carry = temp >> (sizeof(digit_t) * 4);
    c[0] ^= temp << (sizeof(digit_t) * 4);    // C01   

    res1 = ahbl >> (sizeof(digit_t) * 4);
    res2 = albh >> (sizeof(digit_t) * 4);
    res3 = ahbh & mask_low;
    temp = res1 + res2 + res3 + carry;
    c[1] = temp & mask_low;                   // C10 
    carry = temp & mask_high; 
    c[1] ^= (ahbh & mask_high) + carry;       // C11
}


__inline void fpcopy1271(felm_t a, felm_t c)
{ // Copy of a field element, c = a
    unsigned int i;

    for (i = 0; i < NWORDS_FIELD; i++)
        c[i] = a[i];
}


static __inline void fpzero1271(felm_t a)
{ // Zeroing a field element, a = 0
    unsigned int i;

    for (i = 0; i < NWORDS_FIELD; i++)
        a[i] = 0;
}


__inline void fpadd1271(felm_t a, felm_t b, felm_t c)
{ // Field addition, c = a+b mod p  
    unsigned int i;    
    unsigned int carry = 0;
    
    for (i = 0; i < NWORDS_FIELD; i++) {
        ADDC(carry, a[i], b[i], carry, c[i]); 
    }
    carry = (unsigned int)(c[NWORDS_FIELD-1] >> (RADIX-1));
    c[NWORDS_FIELD-1] &= mask_7fff; 
    for (i = 0; i < NWORDS_FIELD; i++) {
        ADDC(carry, c[i], 0, carry, c[i]); 
    }
}


__inline void fpsub1271(felm_t a, felm_t b, felm_t c)
{ // Field subtraction, c = a-b mod p  
    unsigned int i;
    unsigned int borrow = 0;
    
    for (i = 0; i < NWORDS_FIELD; i++) {
        SUBC(borrow, a[i], b[i], borrow, c[i]); 
    }
    borrow = (unsigned int)(c[NWORDS_FIELD-1] >> (RADIX-1));
    c[NWORDS_FIELD-1] &= mask_7fff; 
    for (i = 0; i < NWORDS_FIELD; i++) {
        SUBC(borrow, c[i], 0, borrow, c[i]); 
    }
}


__inline void fpneg1271(felm_t a)
{ // Field negation, a = -a mod p
    unsigned int i;
    unsigned int borrow = 0;
    
    for (i = 0; i < (NWORDS_FIELD-1); i++) {
        SUBC(borrow, prime1271_0, a[i], borrow, a[i]); 
    }
    a[NWORDS_FIELD-1] = prime1271_1 - a[NWORDS_FIELD-1];
}


void fpmul1271(felm_t a, felm_t b, felm_t c)
{ // Field multiplication using schoolbook method, c = a*b mod p  
    unsigned int i, j;
    digit_t u, v, UV[2], temp, bit_mask;
    digit_t t[2*NWORDS_FIELD] = {0};
    unsigned int carry = 0;
    
    for (i = 0; i < NWORDS_FIELD; i++) {
         u = 0;
         for (j = 0; j < NWORDS_FIELD; j++) {
              MUL(a[i], b[j], UV+1, UV[0]); 
              ADDC(0, UV[0], u, carry, v); 
              u = UV[1] + carry;
              ADDC(0, t[i+j], v, carry, v); 
              u = u + carry;
              t[i+j] = v;
         }
         t[NWORDS_FIELD+i] = u;
    }
    bit_mask = (t[NWORDS_FIELD-1] >> (RADIX-1));
    t[NWORDS_FIELD-1] &= mask_7fff; 
    carry = 0;
    for (i = 0; i < NWORDS_FIELD; i++) {
        temp = (t[NWORDS_FIELD+i] >> (RADIX-1));
        t[NWORDS_FIELD+i] = (t[NWORDS_FIELD+i] << 1) + bit_mask;
        bit_mask = temp; 
        ADDC(carry, t[i], t[NWORDS_FIELD+i], carry, t[i]); 
    }
    carry = (unsigned int)(t[NWORDS_FIELD-1] >> (RADIX-1));
    t[NWORDS_FIELD-1] &= mask_7fff; 
    for (i = 0; i < NWORDS_FIELD; i++) {
        ADDC(carry, t[i], 0, carry, c[i]); 
    }
}


void fpsqr1271(felm_t a, felm_t c)
{ // Field squaring using schoolbook method, c = a^2 mod p  
    
    fpmul1271(a, a, c);
}


void mod1271(felm_t a)
{ // Modular correction, a = a mod (2^127-1)  
    digit_t mask;
    unsigned int i;
    unsigned int borrow = 0;
    
    for (i = 0; i < (NWORDS_FIELD-1); i++) {
        SUBC(borrow, a[i], prime1271_0, borrow, a[i]); 
    }
    SUBC(borrow, a[NWORDS_FIELD-1], prime1271_1, borrow, a[NWORDS_FIELD-1]); 

    mask = 0 - (digit_t)borrow;    // If result < 0 then mask = 0xFF...F else sign = 0x00...0
    borrow = 0;
    for (i = 0; i < (NWORDS_FIELD-1); i++) {
        ADDC(borrow, a[i], mask, borrow, a[i]); 
    }
    ADDC(borrow, a[NWORDS_FIELD-1], (mask >> 1), borrow, a[NWORDS_FIELD-1]); 
}


void mp_mul(const digit_t* a, const digit_t* b, digit_t* c, const unsigned int nwords)
{ // Schoolbook multiprecision multiply, c = a*b   
    unsigned int i, j;
    digit_t u, v, UV[2];
    unsigned int carry = 0;

     for (i = 0; i < (2*nwords); i++) c[i] = 0;

     for (i = 0; i < nwords; i++) {
          u = 0;
          for (j = 0; j < nwords; j++) {
               MUL(a[i], b[j], UV+1, UV[0]); 
               ADDC(0, UV[0], u, carry, v); 
               u = UV[1] + carry;
               ADDC(0, c[i+j], v, carry, v); 
               u = u + carry;
               c[i+j] = v;
          }
          c[nwords+i] = u;
     }
}


unsigned int mp_add(digit_t* a, digit_t* b, digit_t* c, unsigned int nwords)
{ // Multiprecision addition, c = a+b, where lng(a) = lng(b) = nwords. Returns the carry bit 
    unsigned int i, carry = 0;

    for (i = 0; i < nwords; i++) {
        ADDC(carry, a[i], b[i], carry, c[i]);
    }
    
    return carry;
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

    mp_mul(a, b, c, NWORDS_ORDER);
}


static __inline unsigned int add(const digit_t* a, const digit_t* b, digit_t* c, const unsigned int nwords)
{ // Multiprecision addition, c = a+b, where lng(a) = lng(b) = nwords. Returns the carry bit 
    
    return mp_add((digit_t*)a, (digit_t*)b, c, (unsigned int)nwords);
}


unsigned int subtract(const digit_t* a, const digit_t* b, digit_t* c, const unsigned int nwords)
{ // Multiprecision subtraction, c = a-b, where lng(a) = lng(b) = nwords. Returns the borrow bit 
    unsigned int i;
    unsigned int borrow = 0;

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
	digit_t mask, P[2 * NWORDS_ORDER], Q[2 * NWORDS_ORDER], temp[2 * NWORDS_ORDER];
	digit_t* order = (digit_t*)curve_order;
	unsigned int cout = 0, bout = 0;

	multiply(ma, mb, P);                               // P = ma * mb
	multiply(P, (digit_t*)&Montgomery_rprime, Q);      // Q = P * r' mod 2^(log_2(r))
	multiply(Q, (digit_t*)&curve_order, temp);         // temp = Q * r
	cout = add(P, temp, temp, 2 * NWORDS_ORDER);         // (cout, temp) = P + Q * r     

	for (i = 0; i < NWORDS_ORDER; i++) {               // (cout, mc) = (P + Q * r)/2^(log_2(r))
		mc[i] = temp[NWORDS_ORDER + i];
	}

	// Final, constant-time subtraction     
	bout = subtract(mc, (digit_t*)&curve_order, mc, NWORDS_ORDER);    // (cout, mc) = (cout, mc) - r
	mask = (digit_t)cout - (digit_t)bout;              // if (cout, mc) >= 0 then mask = 0x00..0, else if (cout, mc) < 0 then mask = 0xFF..F

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
	digit_t ma[NWORDS_ORDER], one[NWORDS_ORDER] = { 0 };

	one[0] = 1;
	Montgomery_multiply_mod_order(a, (digit_t*)&Montgomery_Rprime, ma);
	Montgomery_multiply_mod_order(ma, one, c);
}


void conversion_to_odd(digit_t* k, digit_t* k_odd)
{// Convert scalar to odd if even using the prime subgroup order r
	digit_t i, mask;
	digit_t* order = (digit_t*)curve_order;
	unsigned int carry = 0;

	mask = ~(0 - (k[0] & 1));

	for (i = 0; i < NWORDS_ORDER; i++) {  // If (k is odd) then k_odd = k else k_odd = k + r 
		ADDC(carry, order[i] & mask, k[i], carry, k_odd[i]);
	}
}


__inline void fpdiv1271(felm_t a)
{ // Field division by two, c = a/2 mod p
    digit_t mask;
    unsigned int carry = 0;
    unsigned int i;

    mask = 0 - (a[0] & 1);  // if a is odd then mask = 0xFF...FF, else mask = 0
    
    for (i = 0; i < (NWORDS_FIELD-1); i++) {
        ADDC(carry, mask, a[i], carry, a[i]);
    }
    ADDC(carry, (mask >> 1), a[NWORDS_FIELD-1], carry, a[NWORDS_FIELD-1]);

    for (i = 0; i < (NWORDS_FIELD-1); i++) {
        SHIFTR(a[i+1], a[i], 1, a[i], RADIX);
    }
    a[NWORDS_FIELD-1] = (a[NWORDS_FIELD-1] >> 1);
}


void fp2div1271(f2elm_t a)
{ // GF(p^2) division by two c = a/2 mod p
    fpdiv1271(a[0]);
    fpdiv1271(a[1]);
}


#ifdef __cplusplus
}
#endif


#endif
