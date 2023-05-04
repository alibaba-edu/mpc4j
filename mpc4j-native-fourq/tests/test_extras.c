/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: utility functions for tests
************************************************************************************/  

#include "../FourQ_internal.h"
#include "../FourQ_params.h"
#include "test_extras.h"
#if (OS_TARGET == OS_WIN)
    #include <windows.h>
    #include <intrin.h>
#endif
#if (OS_TARGET == OS_LINUX) && (TARGET == TARGET_ARM || TARGET == TARGET_ARM64)
    #include <time.h>
#endif
#include <stdlib.h>
#include <string.h>


int64_t cpucycles(void)
{ // Access system counter for benchmarking
#if (OS_TARGET == OS_WIN) && (TARGET == TARGET_AMD64 || TARGET == TARGET_x86)
    return __rdtsc();
#elif (OS_TARGET == OS_WIN) && (TARGET == TARGET_ARM)
    return __rdpmccntr64();
#elif (OS_TARGET == OS_LINUX) && (TARGET == TARGET_AMD64 || TARGET == TARGET_x86)
    unsigned int hi, lo;

    asm volatile ("rdtsc\n\t" : "=a" (lo), "=d"(hi));
    return ((int64_t)lo) | (((int64_t)hi) << 32);
#elif (OS_TARGET == OS_LINUX) && (TARGET == TARGET_ARM || TARGET == TARGET_ARM64)
    struct timespec time;

    clock_gettime(CLOCK_REALTIME, &time);
    return (int64_t)(time.tv_sec*1e9 + time.tv_nsec);
#else
    return 0;            
#endif
}


int fp2compare64(uint64_t* a, uint64_t* b)
{ // Comparing uint64_t digits of two quadratic extension field elements, ai=bi? : (0) equal, (1) unequal
  // NOTE: this function does not have constant-time execution. TO BE USED FOR TESTING ONLY.
    unsigned int i;

    for (i = 0; i < (2*NWORDS64_FIELD); i++) {
        if (a[i] != b[i]) return 1;
    }
    
    return 0; 
}


void random_scalar_test(uint64_t* a)
{ // Generating a pseudo-random scalar value in [0, 2^256-1] 
  // NOTE: distribution is not fully uniform. TO BE USED FOR TESTING ONLY.
    unsigned char* string = (unsigned char*)&a[0];
    unsigned int i;

    for (i = 0; i < (sizeof(uint64_t)*NWORDS64_ORDER); i++) {
        string[i] = (unsigned char)rand();             
    }
}


void fp2random1271_test(f2elm_t a)
{ // Generating a pseudo-random GF(p^2) element a+b*i, where a,b in [0, 2^127-1] 
  // NOTE: distribution is not fully uniform. TO BE USED FOR TESTING ONLY.
    digit_t mask_7fff = (digit_t)-1 >> 1;

    random_scalar_test((uint64_t*)&a[0]);
    a[0][NWORDS_FIELD - 1] &= mask_7fff;
    a[1][NWORDS_FIELD - 1] &= mask_7fff;
}


void random_order_test(digit_t* a)
{ // Generating a pseudo-random element in [0, order-1] 
  // SECURITY NOTE: distribution is not fully uniform. TO BE USED FOR TESTING ONLY.
    int i;
    unsigned char* string = (unsigned char*)a;

    for (i = 0; i < 31; i++) {
        string[i] = (unsigned char)rand();               // Obtain 246-bit number
    }
    string[30] &= 0x3F;
    string[31] = 0;
    subtract_mod_order(a, (digit_t*)&curve_order, a);

    return;
}


bool verify_mLSB_recoding(uint64_t* scalar, int* digits)
{ // Verification of the mLSB-set's recoding algorithm used in fixed-base scalar multiplication 
    unsigned int j, l = L_FIXEDBASE, d = D_FIXEDBASE;
    uint64_t temp, temp2, carry, borrow, generated_scalar[NWORDS64_ORDER] = {0};
    int i, digit;

    for (i = (l-1); i >= 0; i--)
    {
        // Shift generated scalar to the left by 1 (multiply by 2)
        temp = ((generated_scalar[0] >> (RADIX64-1)) & 1) ;
        generated_scalar[0] = generated_scalar[0] << 1;

        for (j = 1; j < NWORDS64_ORDER; j++) {
            temp2 = ((generated_scalar[j] >> (RADIX64-1)) & 1) ;
            generated_scalar[j] = (generated_scalar[j] << 1) | temp;
            temp = temp2;
        }
     
        // generated scalar + digit_i
        if (i < (int)d) {
            digit = digits[i] | 1;
            if (digit >= 0) {
                generated_scalar[0] = generated_scalar[0] + digit;
                carry = (generated_scalar[0] < (unsigned int)digit);
                for (j = 1; j < NWORDS64_ORDER; j++)
                {
                    generated_scalar[j] = generated_scalar[j] + carry;    
                    carry = (generated_scalar[j] < carry);
                }
            } else {
                borrow = 0;
                temp = (uint64_t)(-digit);
                for (j = 0; j < NWORDS64_ORDER; j++)
                {
                    temp2 = generated_scalar[j] - temp;
                    carry = (generated_scalar[j] < temp);
                    generated_scalar[j] = temp2 - borrow;
                    borrow = carry || (temp2 < borrow);
                    temp = 0;
                }
            } 
        } else {
            digit = digits[i]*(digits[i-(i/d)*d] | 1);
            if (digit >= 0) {
                generated_scalar[0] = generated_scalar[0] + digit;
                carry = (generated_scalar[0] < (unsigned int)digit);
                for (j = 1; j < NWORDS64_ORDER; j++)
                {
                    generated_scalar[j] = generated_scalar[j] + carry;    
                    carry = (generated_scalar[j] < carry);
                }
            } else {
                borrow = 0;
                temp = (uint64_t)(-digit);
                for (j = 0; j < NWORDS64_ORDER; j++)
                {
                    temp2 = generated_scalar[j] - temp;
                    carry = (generated_scalar[j] < temp);
                    generated_scalar[j] = temp2 - borrow;
                    borrow = carry || (temp2 < borrow);
                    temp = 0;
                }
            } 
        }
    }

    for (j = 0; j < NWORDS64_ORDER; j++)
    {
        if (scalar[j] != generated_scalar[j]) 
            return false;
    }

    return true;
}
            

static inline bool fpeq1271_unsafe(felm_t in1, felm_t in2)
{
    return memcmp(in1, in2, sizeof(felm_t)) == 0;
}


void hash2curve_unsafe(f2elm_t r, point_t out)
{ //  (Unsafe, non-constant-time version of) hash to curve function for testing
    digit_t *r0 = (digit_t*)r[0], *r1 = (digit_t*)r[1];
    felm_t t0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18;
    felm_t one = {0};
    one[0] = 1;

    digit_t* x0 = (digit_t*)out->x[0];
    digit_t* x1 = (digit_t*)out->x[1];
    digit_t* y0 = (digit_t*)out->y[0];
    digit_t* y1 = (digit_t*)out->y[1];

    fpadd1271(r0, r1, t0);  
    fpsub1271(r0, r1, t1);       
    fpmul1271(t0, t1, t0);       
    fpmul1271(r0, r1, t1);       
    fpadd1271(t1, t1, t1);  
    fpadd1271(t1, t1, t2);  
    fpadd1271(t0, t2, t2);  
    fpadd1271(t0, t0, t0);  
    fpsub1271(t0, t1, t3);      
    fpadd1271(t3, one, t0);  
    fpmul1271(A0, t0, t4);       
    fpmul1271(A1, t2, t1);       
    fpsub1271(t1, t4, t4);     
    fpmul1271(A1, t0, t5);       
    fpmul1271(A0, t2, t1);       
    fpadd1271(t1, t5, t1); 
    fpadd1271(t0, t2, t5); 
    fpsub1271(t0, t2, t6); 
    fpmul1271(t5, t6, t6);       
    fpmul1271(t2, t0, t5);       
    fpadd1271(t5, t5, t5);  
    fpmul1271(con1, t3, t7);       
    fpsub1271(t6, t7, t8);      
    fpmul1271(con2, t2, t7);       
    fpadd1271(t7, t8, t8);  
    fpmul1271(con1, t2, t7);      
    fpsub1271(t5, t7, t9);     
    fpmul1271(con2, t3, t7);       
    fpsub1271(t9, t7, t9);   
    fpmul1271(t4, t8, t5);       
    fpmul1271(t1, t9, t7);       
    fpadd1271(t5, t7, t7); 
    fpmul1271(t4, t9, t5);       
    fpmul1271(t1, t8, t10);       
    fpsub1271(t5, t10, t10); 
    fpsqr1271(t7, t5);           
    fpsqr1271(t10, t7);           
    fpadd1271(t5, t7, t5); 
    fpexp1251(t5, t7);          
    fpsqr1271(t7, t7); 
    fpmul1271(t5, t7, t7);        
    fpcopy1271(A0, t8);
    fpcopy1271(A1, t9);
    fpneg1271(t8);  
    fpneg1271(t9);  
    fpadd1271(A0, t4, t5);  
    fpsub1271(A1, t1, t11);   

    if (fpeq1271_unsafe(t7, one)) {
        fpcopy1271(t8, t3);
        fpcopy1271(t9, t10);
    } else {
        fpcopy1271(t5, t3);
        fpcopy1271(t11, t10);
    }

    fpmul1271(t0, t3, t5);     
    fpmul1271(t2, t10, t8);     
    fpsub1271(t5, t8, t8);    
    fpmul1271(t2, t3, t5);     
    fpmul1271(t0, t10, t9);     
    fpadd1271(t5, t9, t9);   
    fpadd1271(t3, t10, t5);   
    fpsub1271(t3, t10, t11);    
    fpmul1271(t5, t11, t5);     
    fpmul1271(t3, t10, t11);     
    fpadd1271(t11, t11, t11);   
    fpmul1271(t3, t4, t12);     
    fpmul1271(t1, t10, t13);     
    fpadd1271(t12, t13, t13);   
    fpmul1271(t4, t10, t14);     
    fpmul1271(t1, t3, t12);     
    fpsub1271(t14, t12, t12);    
    fpsub1271(t5, t13, t5);    
    fpsub1271(t11, t12, t11);    
    fpadd1271(t5, t6, t5);  
    fpmul1271(t0, t2, t6);     
    fpadd1271(t6, t6, t6);   
    fpadd1271(t11, t6, t11);   
    fpmul1271(t5, t8, t6);     
    fpmul1271(t9, t11, t12);     
    fpsub1271(t6, t12, t6);    
    fpmul1271(t5, t9, t12);     
    fpmul1271(t8, t11, t8);     
    fpadd1271(t12, t8, t12);   
    fpadd1271(t6, t6, t6);  
    fpadd1271(t6, t6, t6);     
    fpadd1271(t6, t6, t6);     
    fpadd1271(t6, t6, t6);   
    fpadd1271(t12, t12, t12);   
    fpadd1271(t12, t12, t12);  
    fpadd1271(t12, t12, t12);  
    fpadd1271(t12, t12, t12);  
    fpadd1271(t0, t3, t14);   
    fpadd1271(t14, t14, t14);  
    fpadd1271(t2, t10, t8);   
    fpadd1271(t8, t8, t8);   
    fpmul1271(t6, t14, t4);     
    fpmul1271(t8, t12, t1);     
    fpsub1271(t4, t1, t4);    
    fpmul1271(t12, t14, t9);     
    fpmul1271(t6, t8, t1);     
    fpadd1271(t1, t9, t1);   
    fpsqr1271(t12, t5);     
    fpsqr1271(t6, t9);     
    fpadd1271(t5, t9, t9);   
    fpsqr1271(t1, t5);     
    fpsqr1271(t4, t11);     
    fpadd1271(t11, t5, t11);   
    fpsqr1271(t11, t5);     
    fpmul1271(t5, t9, t5);     
    fpexp1251(t5, t7);          
    fpsqr1271(t7, t13);    
    fpsqr1271(t13, t13);    
    fpmul1271(t11, t13, t13);     
    fpmul1271(t9, t13, t13);     
    fpmul1271(t5, t13, t13);     
    fpmul1271(t13, t7, t7);     
    fpmul1271(t5, t7, t7);     
    fpadd1271(t6, t7, t5);   
    fpdiv1271(t5);
    fpexp1251(t5, t9);          
    fpsqr1271(t9, t11);     
    fpsqr1271(t11, t11);    
    fpmul1271(t5, t11, t11);     
    fpmul1271(t5, t9, t9);     
    fpmul1271(t11, t12, t11);     
    fpsqr1271(t9, t7);
    fpadd1271(one, one, t15);   
    fpcopy1271(t11, t16);
    fpcopy1271(t15, x0);
    fpneg1271(x0);

    if (fpeq1271_unsafe(t5, t7)) {
        fpcopy1271(t15, t17);
        fpcopy1271(t16, t18);
    } else {
        fpcopy1271(t16, t17);
        fpcopy1271(x0, t18);
    }

    fpadd1271(t13, t13, t13);     
    fpsub1271(t3, t0, y0);    
    fpsub1271(t10, t2, y1);    
    fpmul1271(y0, t6, t16);    
    fpmul1271(y1, t12, t15);    
    fpsub1271(t16, t15, t15);    
    fpmul1271(y0, t12, y0);    
    fpmul1271(t6, y1, t16);    
    fpadd1271(t16, y0, t16);     
    fpmul1271(t15, t4, x0);    
    fpmul1271(t1, t16, y0);    
    fpadd1271(x0, y0, y0);     
    fpmul1271(t4, t16, y1);    
    fpmul1271(t1, t15, x0);    
    fpsub1271(y1, x0, y1);    
    fpmul1271(y0, t13, y0);    
    fpmul1271(y1, t13, y1);   
    fpmul1271(b0, t3, t15);    
    fpmul1271(b1, t10, x0);    
    fpsub1271(t15, x0, t15);    
    fpmul1271(b0, t10, t16);    
    fpmul1271(b1, t3, x0);    
    fpadd1271(t16, x0, t16);     
    fpmul1271(t15, t4, t5);    
    fpmul1271(t1, t16, x0);   
    fpadd1271(x0, t5, x0);     
    fpmul1271(t4, t16, x1);    
    fpmul1271(t1, t15, t5);    
    fpsub1271(x1, t5, x1);    
    fpmul1271(x0, t0, t5);    
    fpmul1271(x1, t2, t15);    
    fpsub1271(t5, t15, t15);    
    fpmul1271(x1, t0, t5);    
    fpmul1271(x0, t2, t16);    
    fpadd1271(t5, t16, t16);     
    fpmul1271(t15, t14, t5);   
    fpmul1271(t16, t8, x0);    
    fpsub1271(t5, x0, x0);    
    fpmul1271(t15, t8, t5);    
    fpmul1271(t16, t14, x1);    
    fpadd1271(x1, t5, x1);     
    fpmul1271(x0, t17, t5);    
    fpmul1271(x1, t18, t15);    
    fpsub1271(t5, t15, t15);    
    fpmul1271(t17, x1, t5);    
    fpmul1271(t18, x0, t16);    
    fpadd1271(t16, t5, t16);     
    fpmul1271(t13, t9, t13);    
    fpmul1271(t15, t13, x0);   
    fpmul1271(t16, t13, x1);

    // Clear cofactor
    point_extproj_t P;
    point_setup(out, P);
    cofactor_clearing(P);
    eccnorm(P, out);
}
