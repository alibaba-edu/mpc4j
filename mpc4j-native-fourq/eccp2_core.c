/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: core GF(p^2) and ECC operations over GF(p^2)
*
* This code is based on the paper "FourQ: four-dimensional decompositions on a 
* Q-curve over the Mersenne prime" by Craig Costello and Patrick Longa, in Advances 
* in Cryptology - ASIACRYPT, 2015.
* Preprint available at http://eprint.iacr.org/2015/565.
************************************************************************************/ 

#include "FourQ_internal.h"
#include "FourQ_params.h"
#include "FourQ_tables.h"
#if defined(GENERIC_IMPLEMENTATION)
    #include "generic/fp.h"
#elif (TARGET == TARGET_AMD64)
    #include "AMD64/fp_x64.h"
#elif (TARGET == TARGET_ARM64)
    #include "ARM64/fp_arm64.h"
#endif


/***********************************************/
/************* GF(p^2) FUNCTIONS ***************/

void fp2copy1271(f2elm_t a, f2elm_t c)
{// Copy of a GF(p^2) element, c = a
    fpcopy1271(a[0], c[0]);
    fpcopy1271(a[1], c[1]);
}


void fp2zero1271(f2elm_t a)
{// Zeroing a GF(p^2) element, a = 0
    fpzero1271(a[0]);
    fpzero1271(a[1]);
}


void fp2neg1271(f2elm_t a)
{// GF(p^2) negation, a = -a in GF((2^127-1)^2)
    fpneg1271(a[0]);
    fpneg1271(a[1]);
}


void fp2sqr1271(f2elm_t a, f2elm_t c)
{// GF(p^2) squaring, c = a^2 in GF((2^127-1)^2)

#ifdef ASM_SUPPORT
    fp2sqr1271_a(a, c);
#else
    felm_t t1, t2, t3;

    fpadd1271(a[0], a[1], t1);           // t1 = a0+a1 
    fpsub1271(a[0], a[1], t2);           // t2 = a0-a1
    fpmul1271(a[0], a[1], t3);           // t3 = a0*a1
    fpmul1271(t1, t2, c[0]);             // c0 = (a0+a1)(a0-a1)
    fpadd1271(t3, t3, c[1]);             // c1 = 2a0*a1
#ifdef TEMP_ZEROING
    clear_words((void*)t1, sizeof(felm_t)/sizeof(unsigned int));
    clear_words((void*)t2, sizeof(felm_t)/sizeof(unsigned int));
    clear_words((void*)t3, sizeof(felm_t)/sizeof(unsigned int));
#endif
#endif
}


void fp2mul1271(f2elm_t a, f2elm_t b, f2elm_t c)
{// GF(p^2) multiplication, c = a*b in GF((2^127-1)^2)

#if defined(ASM_SUPPORT)        
    fp2mul1271_a(a, b, c);
#else
    felm_t t1, t2, t3, t4;
    
    fpmul1271(a[0], b[0], t1);          // t1 = a0*b0
    fpmul1271(a[1], b[1], t2);          // t2 = a1*b1
    fpadd1271(a[0], a[1], t3);          // t3 = a0+a1
    fpadd1271(b[0], b[1], t4);          // t4 = b0+b1
    fpsub1271(t1, t2, c[0]);            // c[0] = a0*b0 - a1*b1
    fpmul1271(t3, t4, t3);              // t3 = (a0+a1)*(b0+b1)
    fpsub1271(t3, t1, t3);              // t3 = (a0+a1)*(b0+b1) - a0*b0
    fpsub1271(t3, t2, c[1]);            // c[1] = (a0+a1)*(b0+b1) - a0*b0 - a1*b1    
#ifdef TEMP_ZEROING
    clear_words((void*)t1, sizeof(felm_t)/sizeof(unsigned int));
    clear_words((void*)t2, sizeof(felm_t)/sizeof(unsigned int));
    clear_words((void*)t3, sizeof(felm_t)/sizeof(unsigned int));
    clear_words((void*)t4, sizeof(felm_t)/sizeof(unsigned int));
#endif
#endif
}


__inline void fp2add1271(f2elm_t a, f2elm_t b, f2elm_t c)
{// GF(p^2) addition, c = a+b in GF((2^127-1)^2)
    fpadd1271(a[0], b[0], c[0]);
    fpadd1271(a[1], b[1], c[1]);
}


__inline void fp2sub1271(f2elm_t a, f2elm_t b, f2elm_t c)
{// GF(p^2) subtraction, c = a-b in GF((2^127-1)^2) 
    fpsub1271(a[0], b[0], c[0]);
    fpsub1271(a[1], b[1], c[1]);
}


static __inline void fp2addsub1271(f2elm_t a, f2elm_t b, f2elm_t c)
{// GF(p^2) addition followed by subtraction, c = 2a-b in GF((2^127-1)^2)
    
#ifdef ASM_SUPPORT
    fp2addsub1271_a(a, b, c);
#else
    fp2add1271(a, a, a);
    fp2sub1271(a, b, c);
#endif 
}


void fp2inv1271(f2elm_t a)
{// GF(p^2) inversion, a = (a0-i*a1)/(a0^2+a1^2)
    f2elm_t t1;

    fpsqr1271(a[0], t1[0]);             // t10 = a0^2
    fpsqr1271(a[1], t1[1]);             // t11 = a1^2
    fpadd1271(t1[0], t1[1], t1[0]);     // t10 = a0^2+a1^2
    fpinv1271(t1[0]);                   // t10 = (a0^2+a1^2)^-1
    fpneg1271(a[1]);                    // a = a0-i*a1
    fpmul1271(a[0], t1[0], a[0]);
    fpmul1271(a[1], t1[0], a[1]);       // a = (a0-i*a1)*(a0^2+a1^2)^-1
#ifdef TEMP_ZEROING
    clear_words((void*)t1, sizeof(f2elm_t)/sizeof(unsigned int));
#endif
}


void clear_words(void* mem, unsigned int nwords)
{ // Clear integer-size digits from memory. "nwords" indicates the number of integer digits to be zeroed.
  // This function uses the volatile type qualifier to inform the compiler not to optimize out the memory clearing.
  // It has been tested with MSVS 2013 and GNU GCC 4.6.3, 4.7.3, 4.8.2 and 4.8.4. Users are responsible for verifying correctness with different compilers.  
  // See "Compliant Solution (C99)" at https://www.securecoding.cert.org/confluence/display/c/MSC06-C.+Beware+of+compiler+optimizations 
    unsigned int i;
    volatile unsigned int *v = mem; 

    for (i = 0; i < nwords; i++)
        v[i] = 0;
}


/***********************************************/
/**********  CURVE/SCALAR FUNCTIONS  ***********/

void eccset(point_t P)
{ // Set generator  
  // Output: P = (x,y)
    
    fp2copy1271((felm_t*)&GENERATOR_x, P->x);    // X1
    fp2copy1271((felm_t*)&GENERATOR_y, P->y);    // Y1
}


void eccnorm(point_extproj_t P, point_t Q)
{ // Normalize a projective point (X1:Y1:Z1), including full reduction
  // Input: P = (X1:Y1:Z1) in twisted Edwards coordinates    
  // Output: Q = (X1/Z1,Y1/Z1), corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates
    
    fp2inv1271(P->z);                      // Z1 = Z1^-1
    fp2mul1271(P->x, P->z, Q->x);          // X1 = X1/Z1
    fp2mul1271(P->y, P->z, Q->y);          // Y1 = Y1/Z1
    mod1271(Q->x[0]); mod1271(Q->x[1]); 
    mod1271(Q->y[0]); mod1271(Q->y[1]); 
}


__inline void R1_to_R2(point_extproj_t P, point_extproj_precomp_t Q) 
{ // Conversion from representation (X,Y,Z,Ta,Tb) to (X+Y,Y-X,2Z,2dT), where T = Ta*Tb
  // Input:  P = (X1,Y1,Z1,Ta,Tb), where T1 = Ta*Tb, corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates
  // Output: Q = (X1+Y1,Y1-X1,2Z1,2dT1) corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates
    
    fp2add1271(P->ta, P->ta, Q->t2);                  // T = 2*Ta
    fp2add1271(P->x, P->y, Q->xy);                    // QX = X+Y
    fp2sub1271(P->y, P->x, Q->yx);                    // QY = Y-X 
    fp2mul1271(Q->t2, P->tb, Q->t2);                  // T = 2*T
    fp2add1271(P->z, P->z, Q->z2);                    // QZ = 2*Z
    fp2mul1271(Q->t2, (felm_t*)&PARAMETER_d, Q->t2); // QT = 2d*T
}


__inline void R1_to_R3(point_extproj_t P, point_extproj_precomp_t Q)      
{ // Conversion from representation (X,Y,Z,Ta,Tb) to (X+Y,Y-X,Z,T), where T = Ta*Tb 
  // Input:  P = (X1,Y1,Z1,Ta,Tb), where T1 = Ta*Tb, corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates
  // Output: Q = (X1+Y1,Y1-X1,Z1,T1) corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates 
    
    fp2add1271(P->x, P->y, Q->xy);         // XQ = (X1+Y1) 
    fp2sub1271(P->y, P->x, Q->yx);         // YQ = (Y1-X1) 
    fp2mul1271(P->ta, P->tb, Q->t2);       // TQ = T1
    fp2copy1271(P->z, Q->z2);              // ZQ = Z1 
}


void R2_to_R4(point_extproj_precomp_t P, point_extproj_t Q)      
{ // Conversion from representation (X+Y,Y-X,2Z,2dT) to (2X,2Y,2Z,2dT) 
  // Input:  P = (X1+Y1,Y1-X1,2Z1,2dT1) corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates
  // Output: Q = (2X1,2Y1,2Z1) corresponding to (X1:Y1:Z1) in twisted Edwards coordinates 
    
    fp2sub1271(P->xy, P->yx, Q->x);        // XQ = 2*X1
    fp2add1271(P->xy, P->yx, Q->y);        // YQ = 2*Y1
    fp2copy1271(P->z2, Q->z);              // ZQ = 2*Z1
}


__inline void eccdouble(point_extproj_t P)
{ // Point doubling 2P
  // Input: P = (X1:Y1:Z1) in twisted Edwards coordinates
  // Output: 2P = (Xfinal,Yfinal,Zfinal,Tafinal,Tbfinal), where Tfinal = Tafinal*Tbfinal,
  //         corresponding to (Xfinal:Yfinal:Zfinal:Tfinal) in extended twisted Edwards coordinates
    f2elm_t t1, t2;  

    fp2sqr1271(P->x, t1);                  // t1 = X1^2
    fp2sqr1271(P->y, t2);                  // t2 = Y1^2
    fp2add1271(P->x, P->y, P->x);          // t3 = X1+Y1
    fp2add1271(t1, t2, P->tb);             // Tbfinal = X1^2+Y1^2      
    fp2sub1271(t2, t1, t1);                // t1 = Y1^2-X1^2      
    fp2sqr1271(P->x, P->ta);               // Ta = (X1+Y1)^2 
    fp2sqr1271(P->z, t2);                  // t2 = Z1^2  
    fp2sub1271(P->ta, P->tb, P->ta);       // Tafinal = 2X1*Y1 = (X1+Y1)^2-(X1^2+Y1^2)  
    fp2addsub1271(t2, t1, t2);             // t2 = 2Z1^2-(Y1^2-X1^2) 
    fp2mul1271(t1, P->tb, P->y);           // Yfinal = (X1^2+Y1^2)(Y1^2-X1^2)  
    fp2mul1271(t2, P->ta, P->x);           // Xfinal = 2X1*Y1*[2Z1^2-(Y1^2-X1^2)]
    fp2mul1271(t1, t2, P->z);              // Zfinal = (Y1^2-X1^2)[2Z1^2-(Y1^2-X1^2)]
#ifdef TEMP_ZEROING
    clear_words((void*)t1, sizeof(f2elm_t)/sizeof(unsigned int));
    clear_words((void*)t2, sizeof(f2elm_t)/sizeof(unsigned int));
#endif
}


__inline void eccadd_core(point_extproj_precomp_t P, point_extproj_precomp_t Q, point_extproj_t R)      
{ // Basic point addition R = P+Q or R = P+P
  // Inputs: P = (X1+Y1,Y1-X1,2Z1,2dT1) corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates
  //         Q = (X2+Y2,Y2-X2,Z2,T2) corresponding to (X2:Y2:Z2:T2) in extended twisted Edwards coordinates    
  // Output: R = (Xfinal,Yfinal,Zfinal,Tafinal,Tbfinal), where Tfinal = Tafinal*Tbfinal,
  //         corresponding to (Xfinal:Yfinal:Zfinal:Tfinal) in extended twisted Edwards coordinates
    f2elm_t t1, t2; 
          
    fp2mul1271(P->t2, Q->t2, R->z);        // Z = 2dT1*T2 
    fp2mul1271(P->z2, Q->z2, t1);          // t1 = 2Z1*Z2  
    fp2mul1271(P->xy, Q->xy, R->x);        // X = (X1+Y1)(X2+Y2) 
    fp2mul1271(P->yx, Q->yx, R->y);        // Y = (Y1-X1)(Y2-X2) 
    fp2sub1271(t1, R->z, t2);              // t2 = theta
    fp2add1271(t1, R->z, t1);              // t1 = alpha
    fp2sub1271(R->x, R->y, R->tb);         // Tbfinal = beta
    fp2add1271(R->x, R->y, R->ta);         // Tafinal = omega
    fp2mul1271(R->tb, t2, R->x);           // Xfinal = beta*theta
    fp2mul1271(t1, t2, R->z);              // Zfinal = theta*alpha
    fp2mul1271(R->ta, t1, R->y);           // Yfinal = alpha*omega
#ifdef TEMP_ZEROING
    clear_words((void*)t1, sizeof(f2elm_t)/sizeof(unsigned int));
    clear_words((void*)t2, sizeof(f2elm_t)/sizeof(unsigned int));
#endif
}


__inline void eccadd(point_extproj_precomp_t Q, point_extproj_t P)      
{ // Complete point addition P = P+Q or P = P+P
  // Inputs: P = (X1,Y1,Z1,Ta,Tb), where T1 = Ta*Tb, corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates
  //         Q = (X2+Y2,Y2-X2,2Z2,2dT2) corresponding to (X2:Y2:Z2:T2) in extended twisted Edwards coordinates   
  // Output: P = (Xfinal,Yfinal,Zfinal,Tafinal,Tbfinal), where Tfinal = Tafinal*Tbfinal, 
  //         corresponding to (Xfinal:Yfinal:Zfinal:Tfinal) in extended twisted Edwards coordinates
    point_extproj_precomp_t R;
    
    R1_to_R3(P, R);                        // R = (X1+Y1,Y1-Z1,Z1,T1)
    eccadd_core(Q, R, P);                  // P = (X2+Y2,Y2-X2,2Z2,2dT2) + (X1+Y1,Y1-Z1,Z1,T1)

#ifdef TEMP_ZEROING
    clear_words((void*)R, sizeof(point_extproj_precomp_t)/sizeof(unsigned int));
#endif
}


__inline void point_setup(point_t P, point_extproj_t Q)
{ // Point conversion to representation (X,Y,Z,Ta,Tb) 
  // Input: P = (x,y) in affine coordinates
  // Output: P = (X,Y,1,Ta,Tb), where Ta=X, Tb=Y and T=Ta*Tb, corresponding to (X:Y:Z:T) in extended twisted Edwards coordinates

    fp2copy1271(P->x, Q->x);
    fp2copy1271(P->y, Q->y);
    fp2copy1271(Q->x, Q->ta);              // Ta = X1
    fp2copy1271(Q->y, Q->tb);              // Tb = Y1
    fp2zero1271(Q->z); Q->z[0][0]=1;       // Z1 = 1
}


__inline bool ecc_point_validate(point_extproj_t P)
{ // Point validation: check if point lies on the curve
  // Input: P = (x,y) in affine coordinates, where x, y in [0, 2^127-1]. 
  // Output: TRUE (1) if point lies on the curve E: -x^2+y^2-1-dx^2*y^2 = 0, FALSE (0) otherwise.
  // SECURITY NOTE: this function does not run in constant time (input point P is assumed to be public).
    f2elm_t t1, t2, t3;

    fp2sqr1271(P->y, t1);  
    fp2sqr1271(P->x, t2);
    fp2sub1271(t1, t2, t3);                     // -x^2 + y^2 
    fp2mul1271(t1, t2, t1);                     // x^2*y^2
    fp2mul1271((felm_t*)&PARAMETER_d, t1, t2);  // dx^2*y^2
    fp2zero1271(t1);  t1[0][0] = 1;             // t1 = 1
    fp2add1271(t2, t1, t2);                     // 1 + dx^2*y^2
    fp2sub1271(t3, t2, t1);                     // -x^2 + y^2 - 1 - dx^2*y^2
    
#if defined(GENERIC_IMPLEMENTATION)
    { unsigned int i, j;
    mod1271(t1[0]);
    mod1271(t1[1]);

    for (i = 0; i < 2; i++) {
        for (j = 0; j < NWORDS_FIELD; j++) {
            if (t1[i][j] != 0) return false;
        }
    }

    return true; }
#else
    return ((is_digit_zero_ct(t1[0][0] | t1[0][1]) || is_digit_zero_ct((t1[0][0]+1) | (t1[0][1]+1))) &
            (is_digit_zero_ct(t1[1][0] | t1[1][1]) || is_digit_zero_ct((t1[1][0]+1) | (t1[1][1]+1))));
#endif
}


static __inline void R5_to_R1(point_precomp_t P, point_extproj_t Q)      
{ // Conversion from representation (x+y,y-x,2dt) to (X,Y,Z,Ta,Tb) 
  // Input:  P = (x1+y1,y1-x1,2dt1) corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates, where Z1=1
  // Output: Q = (x1,y1,z1,x1,y1), where z1=1, corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates 
    
    fp2sub1271(P->xy, P->yx, Q->x);        // 2*x1
    fp2add1271(P->xy, P->yx, Q->y);        // 2*y1
    fp2div1271(Q->x);                      // XQ = x1
    fp2div1271(Q->y);                      // YQ = y1 
    fp2zero1271(Q->z); Q->z[0][0]=1;       // ZQ = 1
    fp2copy1271(Q->x, Q->ta);              // TaQ = x1
    fp2copy1271(Q->y, Q->tb);              // TbQ = y1
}


static __inline void eccmadd(point_precomp_t Q, point_extproj_t P)
{ // Mixed point addition P = P+Q or P = P+P
  // Inputs: P = (X1,Y1,Z1,Ta,Tb), where T1 = Ta*Tb, corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates
  //         Q = (x2+y2,y2-x2,2dt2) corresponding to (X2:Y2:Z2:T2) in extended twisted Edwards coordinates, where Z2=1  
  // Output: P = (Xfinal,Yfinal,Zfinal,Tafinal,Tbfinal), where Tfinal = Tafinal*Tbfinal, 
  //         corresponding to (Xfinal:Yfinal:Zfinal:Tfinal) in extended twisted Edwards coordinates
    f2elm_t t1, t2;
    
    fp2mul1271(P->ta, P->tb, P->ta);        // Ta = T1
    fp2add1271(P->z, P->z, t1);             // t1 = 2Z1        
    fp2mul1271(P->ta, Q->t2, P->ta);        // Ta = 2dT1*t2 
    fp2add1271(P->x, P->y, P->z);           // Z = (X1+Y1) 
    fp2sub1271(P->y, P->x, P->tb);          // Tb = (Y1-X1)
    fp2sub1271(t1, P->ta, t2);              // t2 = theta
    fp2add1271(t1, P->ta, t1);              // t1 = alpha
    fp2mul1271(Q->xy, P->z, P->ta);         // Ta = (X1+Y1)(x2+y2)
    fp2mul1271(Q->yx, P->tb, P->x);         // X = (Y1-X1)(y2-x2)
    fp2mul1271(t1, t2, P->z);               // Zfinal = theta*alpha
    fp2sub1271(P->ta, P->x, P->tb);         // Tbfinal = beta
    fp2add1271(P->ta, P->x, P->ta);         // Tafinal = omega
    fp2mul1271(P->tb, t2, P->x);            // Xfinal = beta*theta
    fp2mul1271(P->ta, t1, P->y);            // Yfinal = alpha*omega
#ifdef TEMP_ZEROING
    clear_words((void*)t1, sizeof(f2elm_t)/sizeof(unsigned int));
    clear_words((void*)t2, sizeof(f2elm_t)/sizeof(unsigned int));
#endif
}


void eccmadd_ni(point_precomp_t Q, point_extproj_t P)
{
    eccmadd(Q, P);
}


bool ecc_mul_fixed(digit_t* k, point_t Q)
{ // Fixed-base scalar multiplication Q = k*G, where G is the generator. FIXED_BASE_TABLE stores v*2^(w-1) = 80 multiples of G.
  // Inputs: scalar "k" in [0, 2^256-1].
  // Output: Q = k*G in affine coordinates (x,y).
  // The function is based on the modified LSB-set comb method, which converts the scalar to an odd signed representation
  // with (bitlength(order)+w*v) digits.
    unsigned int j, w = W_FIXEDBASE, v = V_FIXEDBASE, d = D_FIXEDBASE, e = E_FIXEDBASE;
    unsigned int digit = 0, digits[NBITS_ORDER_PLUS_ONE+(W_FIXEDBASE*V_FIXEDBASE)-1] = {0}; 
    digit_t temp[NWORDS_ORDER];
    point_extproj_t R;
    point_precomp_t S;
    int i, ii;

	modulo_order(k, temp);                                      // temp = k mod (order) 
	conversion_to_odd(temp, temp);                              // Converting scalar to odd using the prime subgroup order
	mLSB_set_recode((uint64_t*)temp, digits);                   // Scalar recoding

    // Extracting initial digit 
    digit = digits[w*d-1];
    for (i = (int)((w-1)*d-1); i >= (int)(2*d-1); i = i-d)           
    {
        digit = 2*digit + digits[i];
    }
    // Initialize R = (x+y,y-x,2dt) with a point from the table
	table_lookup_fixed_base(((point_precomp_t*)&FIXED_BASE_TABLE)+(v-1)*(1 << (w-1)), S, digit, digits[d-1]);
    R5_to_R1(S, R);                                             // Converting to representation (X:Y:1:Ta:Tb)

    for (j = 0; j < (v-1); j++)
    {
        digit = digits[w*d-(j+1)*e-1];
        for (i = (int)((w-1)*d-(j+1)*e-1); i >= (int)(2*d-(j+1)*e-1); i = i-d)           
        {
            digit = 2*digit + digits[i];
        }
        // Extract point in (x+y,y-x,2dt) representation
        table_lookup_fixed_base(((point_precomp_t*)&FIXED_BASE_TABLE)+(v-j-2)*(1 << (w-1)), S, digit, digits[d-(j+1)*e-1]);
        eccmadd(S, R);                                          // R = R+S using representations (X,Y,Z,Ta,Tb) <- (X,Y,Z,Ta,Tb) + (x+y,y-x,2dt) 
    }

    for (ii = (e-2); ii >= 0; ii--)
    {
        eccdouble(R);                                           // R = 2*R using representations (X,Y,Z,Ta,Tb) <- 2*(X,Y,Z)
        for (j = 0; j < v; j++)
        {
            digit = digits[w*d-j*e+ii-e];
            for (i = (int)((w-1)*d-j*e+ii-e); i >= (int)(2*d-j*e+ii-e); i = i-d)           
            {
                digit = 2*digit + digits[i];
            }
            // Extract point in (x+y,y-x,2dt) representation
            table_lookup_fixed_base(((point_precomp_t*)&FIXED_BASE_TABLE)+(v-j-1)*(1 << (w-1)), S, digit, digits[d-j*e+ii-e]);
            eccmadd(S, R);                                      // R = R+S using representations (X,Y,Z,Ta,Tb) <- (X,Y,Z,Ta,Tb) + (x+y,y-x,2dt)
        }        
    }     
    eccnorm(R, Q);                                              // Conversion to affine coordinates (x,y) and modular correction. 
    
#ifdef TEMP_ZEROING
    clear_words((void*)digits, NBITS_ORDER_PLUS_ONE+(W_FIXEDBASE*V_FIXEDBASE)-1);
    clear_words((void*)S, sizeof(point_precomp_t)/sizeof(unsigned int));
#endif
    return true;
}


void mLSB_set_recode(uint64_t* scalar, unsigned int *digits)
{ // Computes the modified LSB-set representation of a scalar
  // Inputs: scalar in [0, order-1], where the order of FourQ's subgroup is 246 bits.
  // Output: digits, where the first "d" values (from index 0 to (d-1)) store the signs for the recoded values using the convention: -1 (negative), 0 (positive), and
  //         the remaining values (from index d to (l-1)) store the recoded values in mLSB-set representation, excluding their sign, 
  //         where l = d*w and d = ceil(bitlength(order)/(w*v))*v. The values v and w are fixed and must be in the range [1, 10] (see FourQ.h); they determine the size 
  //         of the precomputed table "FIXED_BASE_TABLE" used by ecc_mul_fixed(). 
    unsigned int i, j, d = D_FIXEDBASE, l = L_FIXEDBASE;
    uint64_t temp, carry;
    
    digits[d-1] = 0;

    // Shift scalar to the right by 1   
    for (j = 0; j < (NWORDS64_ORDER-1); j++) {
        SHIFTR(scalar[j+1], scalar[j], 1, scalar[j], RADIX64);
    }
    scalar[NWORDS64_ORDER-1] >>= 1;

    for (i = 0; i < (d-1); i++)
    {
        digits[i] = (unsigned int)((scalar[0] & 1) - 1);  // Convention for the "sign" row: 
                                                          // if scalar_(i+1) = 0 then digit_i = -1 (negative), else if scalar_(i+1) = 1 then digit_i = 0 (positive)
        // Shift scalar to the right by 1   
        for (j = 0; j < (NWORDS64_ORDER-1); j++) {
            SHIFTR(scalar[j+1], scalar[j], 1, scalar[j], RADIX64);
        }
        scalar[NWORDS64_ORDER-1] >>= 1;
    } 

    for (i = d; i < l; i++)
    {
        digits[i] = (unsigned int)(scalar[0] & 1);        // digits_i = k mod 2. Sign is determined by the "sign" row

        // Shift scalar to the right by 1  
        for (j = 0; j < (NWORDS64_ORDER-1); j++) {
            SHIFTR(scalar[j+1], scalar[j], 1, scalar[j], RADIX64);
        }
        scalar[NWORDS64_ORDER-1] >>= 1;

        temp = (0 - digits[i-(i/d)*d]) & digits[i];       // if (digits_i=0 \/ 1) then temp = 0, else if (digits_i=-1) then temp = 1 
            
        // floor(scalar/2) + temp
        scalar[0] = scalar[0] + temp;
        carry = (temp & (uint64_t)is_digit_zero_ct((digit_t)scalar[0]));       // carry = (scalar[0] < temp);
        for (j = 1; j < NWORDS64_ORDER; j++)
        {
            scalar[j] = scalar[j] + carry; 
            carry = (carry & (uint64_t)is_digit_zero_ct((digit_t)scalar[j]));  // carry = (scalar[j] < temp);
        }
    } 
    return;              
}


static __inline void eccneg_extproj_precomp(point_extproj_precomp_t P, point_extproj_precomp_t Q)
{ // Point negation
  // Input : point P in coordinates (X+Y,Y-X,2Z,2dT)
  // Output: point Q = -P = (Y-X,X+Y,2Z,-2dT)
    fp2copy1271(P->t2, Q->t2);
    fp2copy1271(P->xy, Q->yx);
    fp2copy1271(P->yx, Q->xy);
    fp2copy1271(P->z2, Q->z2);
    fp2neg1271(Q->t2);
}


static __inline void eccneg_precomp(point_precomp_t P, point_precomp_t Q)
{ // Point negation
  // Input : point P in coordinates (x+y,y-x,2dt)
  // Output: point Q = -P = (y-x,x+y,-2dt)
    fp2copy1271(P->t2, Q->t2);
    fp2copy1271(P->xy, Q->yx);
    fp2copy1271(P->yx, Q->xy);
    fp2neg1271(Q->t2);
}


bool ecc_mul_double(digit_t* k, point_t Q, digit_t* l, point_t R)
{ // Double scalar multiplication R = k*G + l*Q, where the G is the generator. Uses DOUBLE_SCALAR_TABLE, which contains multiples of G, Phi(G), Psi(G) and Phi(Psi(G)).
  // Inputs: point Q in affine coordinates,
  //         scalars "k" and "l" in [0, 2^256-1].
  // Output: R = k*G + l*Q in affine coordinates (x,y).
  // The function uses wNAF with interleaving.
            
    // SECURITY NOTE: this function is intended for a non-constant-time operation such as signature verification. 

#if (USE_ENDO == true)
    unsigned int position;
    int i, digits_k1[65] = {0}, digits_k2[65] = {0}, digits_k3[65] = {0}, digits_k4[65] = {0};
    int digits_l1[65] = {0}, digits_l2[65] = {0}, digits_l3[65] = {0}, digits_l4[65] = {0};
	point_precomp_t V;
    point_extproj_t Q1, Q2, Q3, Q4, T; 
    point_extproj_precomp_t U, Q_table1[NPOINTS_DOUBLEMUL_WQ], Q_table2[NPOINTS_DOUBLEMUL_WQ], Q_table3[NPOINTS_DOUBLEMUL_WQ], Q_table4[NPOINTS_DOUBLEMUL_WQ];
    uint64_t k_scalars[4], l_scalars[4];
    
    point_setup(Q, Q1);                                        // Convert to representation (X,Y,1,Ta,Tb)
    
    if (ecc_point_validate(Q1) == false) {                     // Check if point lies on the curve
        return false;
    }
    
    // Computing endomorphisms over point Q
    ecccopy(Q1, Q2);
    ecc_phi(Q2);
    ecccopy(Q1, Q3);    
    ecc_psi(Q3); 
    ecccopy(Q2, Q4); 
    ecc_psi(Q4);  
    
    decompose((uint64_t*)k, k_scalars);                        // Scalar decomposition
    decompose((uint64_t*)l, l_scalars);  
    wNAF_recode(k_scalars[0], WP_DOUBLEBASE, digits_k1);       // Scalar recoding
    wNAF_recode(k_scalars[1], WP_DOUBLEBASE, digits_k2);
    wNAF_recode(k_scalars[2], WP_DOUBLEBASE, digits_k3);
    wNAF_recode(k_scalars[3], WP_DOUBLEBASE, digits_k4);
    wNAF_recode(l_scalars[0], WQ_DOUBLEBASE, digits_l1);      
    wNAF_recode(l_scalars[1], WQ_DOUBLEBASE, digits_l2);
    wNAF_recode(l_scalars[2], WQ_DOUBLEBASE, digits_l3);
    wNAF_recode(l_scalars[3], WQ_DOUBLEBASE, digits_l4);
    ecc_precomp_double(Q1, Q_table1, NPOINTS_DOUBLEMUL_WQ);    // Precomputation
    ecc_precomp_double(Q2, Q_table2, NPOINTS_DOUBLEMUL_WQ); 
    ecc_precomp_double(Q3, Q_table3, NPOINTS_DOUBLEMUL_WQ); 
    ecc_precomp_double(Q4, Q_table4, NPOINTS_DOUBLEMUL_WQ); 

    fp2zero1271(T->x);                                         // Initialize T as the neutral point (0:1:1)
    fp2zero1271(T->y); T->y[0][0] = 1; 
    fp2zero1271(T->z); T->z[0][0] = 1;     

    for (i = 64; i >= 0; i--)
    {   
        eccdouble(T);                                          // Double (X_T,Y_T,Z_T,Ta_T,Tb_T) = 2(X_T,Y_T,Z_T,Ta_T,Tb_T)
        if (digits_l1[i] < 0) {
            position = (-digits_l1[i])/2;                      
            eccneg_extproj_precomp(Q_table1[position], U);     // Load and negate U = (X_U,Y_U,Z_U,Td_U) <- -(X+Y,Y-X,2Z,2dT) from a point in the precomputed table 
            eccadd(U, T);                                      // T = T+U = (X_T,Y_T,Z_T,Ta_T,Tb_T) = (X_T,Y_T,Z_T,Ta_T,Tb_T) + (X_U,Y_U,Z_U,Td_U) 
        } else if (digits_l1[i] > 0) {            
            position = (digits_l1[i])/2;                       // Take U = (X_U,Y_U,Z_U,Td_U) <- (X+Y,Y-X,2Z,2dT) from a point in the precomputed table
            eccadd(Q_table1[position], T);                     // T = T+U = (X_T,Y_T,Z_T,Ta_T,Tb_T) = (X_T,Y_T,Z_T,Ta_T,Tb_T) + (X_U,Y_U,Z_U,Td_U) 
        }                                          
        if (digits_l2[i] < 0) {
            position = (-digits_l2[i])/2;                      
            eccneg_extproj_precomp(Q_table2[position], U);      
            eccadd(U, T);                                
        } else if (digits_l2[i] > 0) {            
            position = (digits_l2[i])/2;                       
            eccadd(Q_table2[position], T);               
        }                                        
        if (digits_l3[i] < 0) {
            position = (-digits_l3[i])/2;                      
            eccneg_extproj_precomp(Q_table3[position], U);      
            eccadd(U, T);                                
        } else if (digits_l3[i] > 0) {            
            position = (digits_l3[i])/2;                       
            eccadd(Q_table3[position], T);               
        }                                        
        if (digits_l4[i] < 0) {
            position = (-digits_l4[i])/2;                      
            eccneg_extproj_precomp(Q_table4[position], U);      
            eccadd(U, T);                                
        } else if (digits_l4[i] > 0) {            
            position = (digits_l4[i])/2;                       
            eccadd(Q_table4[position], T);               
        }

        if (digits_k1[i] < 0) {
            position = (-digits_k1[i])/2;                      
            eccneg_precomp(((point_precomp_t*)&DOUBLE_SCALAR_TABLE)[position], V);    // Load and negate V = (X_V,Y_V,Z_V,Td_V) <- -(x+y,y-x,2dt) from a point in the precomputed table 
            eccmadd(V, T);                                                            // T = T+V = (X_T,Y_T,Z_T,Ta_T,Tb_T) = (X_T,Y_T,Z_T,Ta_T,Tb_T) + (X_V,Y_V,Z_V,Td_V) 
        } else if (digits_k1[i] > 0) {            
            position = (digits_k1[i])/2;                                              // Take V = (X_V,Y_V,Z_V,Td_V) <- (x+y,y-x,2dt) from a point in the precomputed table
            eccmadd(((point_precomp_t*)&DOUBLE_SCALAR_TABLE)[position], T);           // T = T+V = (X_T,Y_T,Z_T,Ta_T,Tb_T) = (X_T,Y_T,Z_T,Ta_T,Tb_T) + (X_V,Y_V,Z_V,Td_V) 
        }
        if (digits_k2[i] < 0) {
            position = (-digits_k2[i])/2;                      
            eccneg_precomp(((point_precomp_t*)&DOUBLE_SCALAR_TABLE)[NPOINTS_DOUBLEMUL_WP+position], V);
            eccmadd(V, T);                              
        } else if (digits_k2[i] > 0) {            
            position = (digits_k2[i])/2;                       
            eccmadd(((point_precomp_t*)&DOUBLE_SCALAR_TABLE)[NPOINTS_DOUBLEMUL_WP+position], T);
        }
        if (digits_k3[i] < 0) {
            position = (-digits_k3[i])/2;                      
            eccneg_precomp(((point_precomp_t*)&DOUBLE_SCALAR_TABLE)[2*NPOINTS_DOUBLEMUL_WP+position], V);
            eccmadd(V, T);                              
        } else if (digits_k3[i] > 0) {            
            position = (digits_k3[i])/2;                       
            eccmadd(((point_precomp_t*)&DOUBLE_SCALAR_TABLE)[2*NPOINTS_DOUBLEMUL_WP+position], T);
        }
        if (digits_k4[i] < 0) {
            position = (-digits_k4[i])/2;                      
            eccneg_precomp(((point_precomp_t*)&DOUBLE_SCALAR_TABLE)[3*NPOINTS_DOUBLEMUL_WP+position], V);
            eccmadd(V, T);                              
        } else if (digits_k4[i] > 0) {            
            position = (digits_k4[i])/2;                       
            eccmadd(((point_precomp_t*)&DOUBLE_SCALAR_TABLE)[3*NPOINTS_DOUBLEMUL_WP+position], T);
        }
    }

#else
    point_t A;
    point_extproj_t T;
    point_extproj_precomp_t S;

    if (ecc_mul(Q, l, A, false) == false) {
        return false;
    }
    point_setup(A, T);
    R1_to_R2(T, S);

    ecc_mul_fixed(k, A);
    point_setup(A, T);
    eccadd(S, T);
#endif
    eccnorm(T, R);                                             // Output R = (x,y)
    
    return true;
}


void ecc_precomp_double(point_extproj_t P, point_extproj_precomp_t* Table, unsigned int npoints)
{ // Generation of the precomputation table used internally by the double scalar multiplication function ecc_mul_double().  
  // Inputs: point P in representation (X,Y,Z,Ta,Tb),
  //         Table with storage for npoints, 
  //         number of points "npoints".
  // Output: Table containing multiples of the base point P using representation (X+Y,Y-X,2Z,2dT).
    point_extproj_t Q;
    point_extproj_precomp_t PP;
    unsigned int i; 
    
    R1_to_R2(P, Table[0]);                     // Precomputed point Table[0] = P in coordinates (X+Y,Y-X,2Z,2dT)
    eccdouble(P);                              // A = 2*P in (X,Y,Z,Ta,Tb)
    R1_to_R3(P, PP);                           // Converting from (X,Y,Z,Ta,Tb) to (X+Y,Y-X,Z,T) 
    
    for (i = 1; i < npoints; i++) {
        eccadd_core(Table[i-1], PP, Q);        // Table[i] = Table[i-1]+2P using the representations (X,Y,Z,Ta,Tb) <- (X+Y,Y-X,2Z,2dT) + (X+Y,Y-X,Z,T)
        R1_to_R2(Q, Table[i]);                 // Converting from (X,Y,Z,Ta,Tb) to (X+Y,Y-X,2Z,2dT)
    }
    
    return;
}


void wNAF_recode(uint64_t scalar, unsigned int w, int* digits)
{ // Computes wNAF recoding of a scalar, where digits are in set {0,+-1,+-3,...,+-(2^(w-1)-1)}
    unsigned int i;
    int digit, index = 0; 
    int val1 = (int)(1 << (w-1)) - 1;                  // 2^(w-1) - 1
    int val2 = (int)(1 << w);                          // 2^w;
    uint64_t k = scalar, mask = (uint64_t)val2 - 1;    // 2^w - 1 

    while (k != 0)
    {
        digit = (int)(k & 1); 

        if (digit == 0) {                         
            k >>= 1;                 // Shift scalar to the right by 1
            digits[index] = 0;
        } else {
            digit = (int)(k & mask); 
            k >>= w;                 // Shift scalar to the right by w            

            if (digit > val1) {
                digit -= val2; 
            }
            if (digit < 0) {         // scalar + 1
                k += 1;
            }
            digits[index] = digit; 
                       
            if (k != 0) {            // Check if scalar != 0
                for (i = 0; i < (w-1); i++) 
                {     
                    index++; 
                    digits[index] = 0;
                }
            }
        }
        index++;
    } 
    return;
}
