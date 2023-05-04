/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: ECC operations over GF(p^2) exploiting endomorphisms
*
* This code is based on the paper "FourQ: four-dimensional decompositions on a 
* Q-curve over the Mersenne prime" by Craig Costello and Patrick Longa, in Advances 
* in Cryptology - ASIACRYPT, 2015.
* Preprint available at http://eprint.iacr.org/2015/565.
************************************************************************************/

#include "FourQ_internal.h"


#if (USE_ENDO == true)

// Fixed GF(p^2) constants for the endomorphisms 
static uint64_t ctau1[4]     = {0x74DCD57CEBCE74C3, 0x1964DE2C3AFAD20C, 0x12, 0x0C};         
static uint64_t ctaudual1[4] = {0x9ECAA6D9DECDF034, 0x4AA740EB23058652, 0x11, 0x7FFFFFFFFFFFFFF4};
static uint64_t cphi0[4] = {0xFFFFFFFFFFFFFFF7, 0x05, 0x4F65536CEF66F81A, 0x2553A0759182C329};
static uint64_t cphi1[4] = {0x07, 0x05, 0x334D90E9E28296F9, 0x62C8CAA0C50C62CF};
static uint64_t cphi2[4] = {0x15, 0x0F, 0x2C2CB7154F1DF391, 0x78DF262B6C9B5C98};
static uint64_t cphi3[4] = {0x03, 0x02, 0x92440457A7962EA4, 0x5084C6491D76342A};
static uint64_t cphi4[4] = {0x03, 0x03, 0xA1098C923AEC6855, 0x12440457A7962EA4};
static uint64_t cphi5[4] = {0x0F, 0x0A, 0x669B21D3C5052DF3, 0x459195418A18C59E};
static uint64_t cphi6[4] = {0x18, 0x12, 0xCD3643A78A0A5BE7, 0x0B232A8314318B3C};
static uint64_t cphi7[4] = {0x23, 0x18, 0x66C183035F48781A, 0x3963BC1C99E2EA1A};
static uint64_t cphi8[4] = {0xF0, 0xAA, 0x44E251582B5D0EF0, 0x1F529F860316CBE5};
static uint64_t cphi9[4] = {0xBEF, 0x870, 0x14D3E48976E2505, 0xFD52E9CFE00375B};
static uint64_t cpsi1[4] = {0xEDF07F4767E346EF, 0x2AF99E9A83D54A02, 0x13A, 0xDE};
static uint64_t cpsi2[4] = {0x143, 0xE4, 0x4C7DEB770E03F372, 0x21B8D07B99A81F03};
static uint64_t cpsi3[4] = {0x09, 0x06, 0x3A6E6ABE75E73A61, 0x4CB26F161D7D6906};
static uint64_t cpsi4[4] = {0xFFFFFFFFFFFFFFF6, 0x7FFFFFFFFFFFFFF9, 0xC59195418A18C59E, 0x334D90E9E28296F9};

// Fixed integer constants for the decomposition
// Close "offset" vector
static uint64_t c1  = {0x72482C5251A4559C};
static uint64_t c2  = {0x59F95B0ADD276F6C};
static uint64_t c3  = {0x7DD2D17C4625FA78};
static uint64_t c4  = {0x6BC57DEF56CE8877};
// Optimal basis vectors 
static uint64_t b11 = {0x0906FF27E0A0A196};   
static uint64_t b12 = {0x1363E862C22A2DA0};                                              
static uint64_t b13 = {0x07426031ECC8030F};                                              
static uint64_t b14 = {0x084F739986B9E651};   
static uint64_t b21 = {0x1D495BEA84FCC2D4};
static uint64_t b24 = {0x25DBC5BC8DD167D0};
static uint64_t b31 = {0x17ABAD1D231F0302};
static uint64_t b32 = {0x02C4211AE388DA51};
static uint64_t b33 = {0x2E4D21C98927C49F};
static uint64_t b34 = {0x0A9E6F44C02ECD97};
static uint64_t b41 = {0x136E340A9108C83F};
static uint64_t b42 = {0x3122DF2DC3E0FF32};
static uint64_t b43 = {0x068A49F02AA8A9B5};
static uint64_t b44 = {0x18D5087896DE0AEA};
// Precomputed integers for fast-Babai rounding
static uint64_t ell1[4] = {0x259686E09D1A7D4F, 0xF75682ACE6A6BD66, 0xFC5BB5C5EA2BE5DF, 0x07};
static uint64_t ell2[4] = {0xD1BA1D84DD627AFB, 0x2BD235580F468D8D, 0x8FD4B04CAA6C0F8A, 0x03};
static uint64_t ell3[4] = {0x9B291A33678C203C, 0xC42BD6C965DCA902, 0xD038BF8D0BFFBAF6, 0x00};
static uint64_t ell4[4] = {0x12E5666B77E7FDC0, 0x81CBDC3714983D82, 0x1B073877A22D8410, 0x03};


/***********************************************/
/**********  CURVE/SCALAR FUNCTIONS  ***********/

static __inline void ecc_tau(point_extproj_t P)
{ // Apply tau mapping to a point, P = tau(P)
  // Input: P = (X1:Y1:Z1) on E in twisted Edwards coordinates
  // Output: P = (Xfinal:Yfinal:Zfinal) on Ehat in twisted Edwards coordinates
    f2elm_t t0, t1; 

    fp2sqr1271(P->x, t0);                     // t0 = X1^2
    fp2sqr1271(P->y, t1);                     // t1 = Y1^2
    fp2mul1271(P->x, P->y, P->x);             // X = X1*Y1
    fp2sqr1271(P->z, P->y);                   // Y = Z1^2
    fp2add1271(t0, t1, P->z);                 // Z = X1^2+Y1^2
    fp2sub1271(t1, t0, t0);                   // t0 = Y1^2-X1^2
    fp2add1271(P->y, P->y, P->y);             // Y = 2*Z1^2
    fp2mul1271(P->x, t0, P->x);               // X = X1*Y1*(Y1^2-X1^2)
    fp2sub1271(P->y, t0, P->y);               // Y = 2*Z1^2-(Y1^2-X1^2)
    fp2mul1271(P->x, (felm_t*)&ctau1, P->x);  // Xfinal = X*ctau1
    fp2mul1271(P->y, P->z, P->y);             // Yfinal = Y*Z
    fp2mul1271(P->z, t0, P->z);               // Zfinal = t0*Z
}


static __inline void ecc_tau_dual(point_extproj_t P)
{ // Apply tau_dual mapping to a point, P = tau_dual(P)
  // Input: P = (X1:Y1:Z1) on Ehat in twisted Edwards coordinates
  // Output: P = (Xfinal,Yfinal,Zfinal,Tafinal,Tbfinal) on E, where Tfinal = Tafinal*Tbfinal,
  //         corresponding to (Xfinal:Yfinal:Zfinal:Tfinal) in extended twisted Edwards coordinates
    f2elm_t t0, t1;

    fp2sqr1271(P->x, t0);                          // t0 = X1^2
    fp2sqr1271(P->z, P->ta);                       // Ta = Z1^2
    fp2sqr1271(P->y, t1);                          // t1 = Y1^2
    fp2add1271(P->ta, P->ta, P->z);                // Z = 2*Z1^2
    fp2sub1271(t1, t0, P->ta);                     // Tafinal = Y1^2-X1^2
    fp2add1271(t0, t1, t0);                        // t0 = X1^2+Y1^2
    fp2mul1271(P->x, P->y, P->x);                  // X = X1*Y1
    fp2sub1271(P->z, P->ta, P->z);                 // Z = 2*Z1^2-(Y1^2-X1^2)
    fp2mul1271(P->x, (felm_t*)&ctaudual1, P->tb);  // Tbfinal = ctaudual1*X1*X1
    fp2mul1271(P->z, P->ta, P->y);                 // Yfinal = Z*Tafinal
    fp2mul1271(P->tb, t0, P->x);                   // Xfinal = Tbfinal*t0
    fp2mul1271(P->z, t0, P->z);                    // Zfinal = Z*t0
}


static __inline void ecc_delphidel(point_extproj_t P)
{ // Apply delta_phi_delta mapping to a point, P = delta(phi_W(delta_inv(P))), 
  // where phi_W is the endomorphism on the Weierstrass form.
  // Input: P = (X1:Y1:Z1) on Ehat in twisted Edwards coordinates
  // Output: P = (Xfinal:Yfinal:Zfinal) on Ehat in twisted Edwards coordinates
    f2elm_t t0, t1, t2, t3, t4, t5, t6; 

    fp2sqr1271(P->z, t4);                          // t4 = Z1^2
    fp2mul1271(P->y, P->z, t3);                    // t3 = Y1*Z1
    fp2mul1271(t4, (felm_t*)&cphi4, t0);           // t0 = cphi4*t4
    fp2sqr1271(P->y, t2);                          // t2 = Y1^2
    fp2add1271(t0, t2, t0);                        // t0 = t0+t2
    fp2mul1271(t3, (felm_t*)&cphi3, t1);           // t1 = cphi3*t3
    fp2sub1271(t0, t1, t5);                        // t5 = t0-t1
    fp2add1271(t0, t1, t0);                        // t0 = t0+t1
    fp2mul1271(t0, P->z, t0);                      // t0 = t0*Z1
    fp2mul1271(t3, (felm_t*)&cphi1, t1);           // t1 = cphi1*t3
    fp2mul1271(t0, t5, t0);                        // t0 = t0*t5
    fp2mul1271(t4, (felm_t*)&cphi2, t5);           // t5 = cphi2*t4
    fp2add1271(t2, t5, t5);                        // t5 = t2+t5
    fp2sub1271(t1, t5, t6);                        // t6 = t1-t5
    fp2add1271(t1, t5, t1);                        // t1 = t1+t5
    fp2mul1271(t6, t1, t6);                        // t6 = t1*t6
    fp2mul1271(t6, (felm_t*)&cphi0, t6);           // t6 = cphi0*t6
    fp2mul1271(P->x, t6, P->x);                    // X = X1*t6
    fp2sqr1271(t2, t6);                            // t6 = t2^2
    fp2sqr1271(t3, t2);                            // t2 = t3^2
    fp2sqr1271(t4, t3);                            // t3 = t4^2
    fp2mul1271(t2, (felm_t*)&cphi8, t1);           // t1 = cphi8*t2
    fp2mul1271(t3, (felm_t*)&cphi9, t5);           // t5 = cphi9*t3
    fp2add1271(t1, t6, t1);                        // t1 = t1+t6
    fp2mul1271(t2, (felm_t*)&cphi6, t2);           // t2 = cphi6*t2
    fp2mul1271(t3, (felm_t*)&cphi7, t3);           // t3 = cphi7*t3
    fp2add1271(t1, t5, t1);                        // t1 = t1+t5
    fp2add1271(t2, t3, t2);                        // t2 = t2+t3
    fp2mul1271(t1, P->y, t1);                      // t1 = Y1*t1
    fp2add1271(t6, t2, P->y);                      // Y = t6+t2
    fp2mul1271(P->x, t1, P->x);                    // X = X*t1
    fp2mul1271(P->y, (felm_t*)&cphi5, P->y);       // Y = cphi5*Y
    fpneg1271(P->x[1]);                            // Xfinal = X^p
    fp2mul1271(P->y, P->z, P->y);                  // Y = Y*Z1
    fp2mul1271(t0, t1, P->z);                      // Z = t0*t1
    fp2mul1271(P->y, t0, P->y);                    // Y = Y*t0
    fpneg1271(P->z[1]);                            // Zfinal = Z^p
    fpneg1271(P->y[1]);                            // Yfinal = Y^p
}


static __inline void ecc_delpsidel(point_extproj_t P)
{ // Apply delta_psi_delta mapping to a point, P = delta(psi_W(delta_inv(P))), 
  // where psi_W is the endomorphism on the Weierstrass form.
  // Input: P = (X1:Y1:Z1) on Ehat in twisted Edwards coordinates
  // Output: P = (Xfinal:Yfinal:Zfinal) on Ehat in twisted Edwards coordinates
    f2elm_t t0, t1, t2; 

    fpneg1271(P->x[1]);                            // X = X1^p
    fpneg1271(P->z[1]);                            // Z = Z1^p
    fpneg1271(P->y[1]);                            // Y = Y1^p
    fp2sqr1271(P->z, t2);                          // t2 = Z1^p^2
    fp2sqr1271(P->x, t0);                          // t0 = X1^p^2
    fp2mul1271(P->x, t2, P->x);                    // X = X1^p*Z1^p^2
    fp2mul1271(t2, (felm_t*)&cpsi2, P->z);         // Z = cpsi2*Z1^p^2
    fp2mul1271(t2, (felm_t*)&cpsi3, t1);           // t1 = cpsi3*Z1^p^2
    fp2mul1271(t2, (felm_t*)&cpsi4, t2);           // t2 = cpsi4*Z1^p^2
    fp2add1271(t0, P->z, P->z);                    // Z = X1^p^2 + cpsi2*Z1^p^2
    fp2add1271(t0, t2, t2);                        // t2 = X1^p^2 + cpsi4*Z1^p^2
    fp2add1271(t0, t1, t1);                        // t1 = X1^p^2 + cpsi3*Z1^p^2
    fp2neg1271(t2);                                // t2 = -(X1^p^2 + cpsi4*Z1^p^2)
    fp2mul1271(P->z, P->y, P->z);                  // Z = Y1^p*(X1^p^2 + cpsi2*Z1^p^2)
    fp2mul1271(P->x, t2, P->x);                    // X = -X1^p*Z1^p^2*(X1^p^2 + cpsi4*Z1^p^2)
    fp2mul1271(t1, P->z, P->y);                    // Yfinal = t1*Z
    fp2mul1271(P->x, (felm_t*)&cpsi1, P->x);       // Xfinal = cpsi1*X
    fp2mul1271(P->z, t2, P->z);                    // Zfinal = Z*t2
}


void ecc_psi(point_extproj_t P)
{ // Apply psi mapping to a point, P = psi(P)
  // Input: P = (X1:Y1:Z1) on E in twisted Edwards coordinates
  // Output: P = (Xfinal,Yfinal,Zfinal,Tafinal,Tbfinal) on E, where Tfinal = Tafinal*Tbfinal,
  //         corresponding to (Xfinal:Yfinal:Zfinal:Tfinal) in extended twisted Edwards coordinates

    ecc_tau(P);                            
    ecc_delpsidel(P);                      		
    ecc_tau_dual(P);                        
}


void ecc_phi(point_extproj_t P)
{ // Apply phi mapping to a point, P = phi(P)
  // Input: P = (X1:Y1:Z1) on E in twisted Edwards coordinates
  // Output: P = (Xfinal,Yfinal,Zfinal,Tafinal,Tbfinal) on E, where Tfinal = Tafinal*Tbfinal,
  //         corresponding to (Xfinal:Yfinal:Zfinal:Tfinal) in extended twisted Edwards coordinates

    ecc_tau(P);                            
    ecc_delphidel(P);                      		
    ecc_tau_dual(P);  
}


static __inline void mul_truncate(uint64_t* s, uint64_t* C, uint64_t* out)       
{ // 256-bit multiplication with truncation for the scalar decomposition
  // Outputs 64-bit value "out" = (uint64_t)((s * C) >> 256).
    uint128_t tt1, tt2;
    unsigned int carry1;

#if defined(GENERIC_IMPLEMENTATION) || defined(SCALAR_INTRIN_SUPPORT)
    unsigned int carry2;
    uint64_t temp;

    MUL128(s[0], C[0], tt2);   
    tt2[0] = tt2[1];
    tt2[1] = 0;
    MUL128(s[1], C[0], tt1); 
    ADD128(tt1, tt2, tt1);
    MUL128(s[0], C[1], tt2); 
    ADC128(tt1, tt2, carry1, tt1);
    tt1[0] = tt1[1];
    tt1[1] = (uint64_t)(carry1);
    MUL128(s[2], C[0], tt2); 
    ADD128(tt1, tt2, tt1);
    MUL128(s[0], C[2], tt2); 
    ADC128(tt1, tt2, carry1, tt1);
    MUL128(s[1], C[1], tt2); 
    ADC128(tt1, tt2, carry2, tt1);
    tt1[0] = tt1[1];
    tt1[1] = (uint64_t)carry1 + (uint64_t)carry2;
    MUL128(s[0], C[3], tt2); 
    ADD128(tt1, tt2, tt1);
    MUL128(s[3], C[0], tt2); 
    ADC128(tt1, tt2, carry1, tt1);
    MUL128(s[1], C[2], tt2); 
    ADC128(tt1, tt2, carry2, tt1);
    temp = (uint64_t)carry1 + (uint64_t)carry2;
    MUL128(s[2], C[1], tt2); 
    ADC128(tt1, tt2, carry2, tt1);
    tt1[0] = tt1[1];
    tt1[1] = temp + (uint64_t)carry2;
    MUL128(s[1], C[3], tt2); 
    ADD128(tt1, tt2, tt1);
    MUL128(s[3], C[1], tt2); 
    ADD128(tt1, tt2, tt1);
    MUL128(s[2], C[2], tt2); 
    ADD128(tt1, tt2, tt1);
    *out = tt1[0];
#ifdef TEMP_ZEROING
    clear_words((void*)tt1, sizeof(uint128_t)/sizeof(unsigned int));
    clear_words((void*)tt2, sizeof(uint128_t)/sizeof(unsigned int));
    clear_words((void*)&temp, sizeof(uint64_t)/sizeof(unsigned int));
#endif
    
#elif defined(UINT128_SUPPORT)
    uint128_t tt3, tt4;

    tt2 = (uint128_t)s[0]*C[0];
    tt1 = (uint128_t)s[1]*C[0] + (uint64_t)(tt2 >> 64);
    tt2 = (uint128_t)s[0]*C[1];
    carry1 = (unsigned int)(((uint128_t)((uint64_t)tt1) + (uint128_t)((uint64_t)tt2)) >> 64);
    tt1 = (uint128_t)(tt1 >> 64) + (uint128_t)(tt2 >> 64) + (uint64_t)carry1;
    tt1 += (uint128_t)s[2]*C[0];
    tt2 = (uint128_t)s[0]*C[2];
    tt3 = (uint128_t)s[1]*C[1];
    carry1 = (unsigned int)(((uint128_t)((uint64_t)tt1) + (uint128_t)((uint64_t)tt2) + (uint128_t)((uint64_t)tt3)) >> 64);
    tt1 = (uint128_t)(tt1 >> 64) + (uint128_t)(tt2 >> 64) + (uint128_t)(tt3 >> 64) + (uint64_t)carry1;
    tt1 += (uint128_t)s[0]*C[3];
    tt2 = (uint128_t)s[3]*C[0]; 
    tt3 = (uint128_t)s[1]*C[2]; 
    tt4 = (uint128_t)s[2]*C[1];
    carry1 = (unsigned int)(((uint128_t)((uint64_t)tt1) + (uint128_t)((uint64_t)tt2) + (uint128_t)((uint64_t)tt3) + (uint128_t)((uint64_t)tt4)) >> 64);
    tt1 = (uint128_t)(tt1 >> 64) + (uint128_t)(tt2 >> 64) + (uint128_t)(tt3 >> 64) + (uint128_t)(tt4 >> 64) + (uint64_t)carry1;
    tt1 += (uint128_t)s[1]*C[3] + (uint128_t)s[3]*C[1] + (uint128_t)s[2]*C[2];
    *out = (uint64_t)tt1;
#ifdef TEMP_ZEROING
    clear_words((void*)&tt1, sizeof(uint128_t)/sizeof(unsigned int));
    clear_words((void*)&tt2, sizeof(uint128_t)/sizeof(unsigned int));
    clear_words((void*)&tt3, sizeof(uint128_t)/sizeof(unsigned int));
    clear_words((void*)&tt4, sizeof(uint128_t)/sizeof(unsigned int));
#endif
#endif
}


void decompose(uint64_t* k, uint64_t* scalars)
{ // Scalar decomposition for the variable-base scalar multiplication
  // Input: scalar in the range [0, 2^256-1].
  // Output: 4 64-bit sub-scalars. 
    uint64_t a1, a2, a3, a4, temp, mask;

#if (TARGET == TARGET_x86) && (COMPILER == COMPILER_VC)
    uint128_t t1, t2, t3, t4;

    mul_truncate(k, ell1, &a1);
    mul_truncate(k, ell2, &a2);
    mul_truncate(k, ell3, &a3);
    mul_truncate(k, ell4, &a4);

    MUL128(a1, b11, t1); MUL128(a2, b21, t2); MUL128(a3, b31, t3); MUL128(a4, b41, t4);
    temp = k[0] - t1[0] - t2[0] - t3[0] - t4[0] + c1;
    mask = ~(0 - (temp & 1));      // If temp is even then mask = 0xFF...FF, else mask = 0
    
    scalars[0] = temp + (mask & b41);
    MUL128(a1, b12, t1); MUL128(a3, b32, t2); MUL128(a4, b42, t3); 
    scalars[1] = t1[0] + (uint64_t)a2 - t2[0] - t3[0] + c2 + (mask & b42);
    MUL128(a3, b33, t1); MUL128(a1, b13, t2); MUL128(a4, b43, t3); 
    scalars[2] = t1[0] - t2[0] - (uint64_t)a2 + t3[0] + c3 - (mask & b43);
    MUL128(a1, b14, t1); MUL128(a2, b24, t2); MUL128(a3, b34, t3); MUL128(a4, b44, t4); 
    scalars[3] = t1[0] - t2[0] - t3[0] + t4[0] + c4 - (mask & b44);
#else 
    mul_truncate(k, ell1, &a1);
    mul_truncate(k, ell2, &a2);
    mul_truncate(k, ell3, &a3);
    mul_truncate(k, ell4, &a4);

    temp = k[0] - (uint64_t)a1*b11 - (uint64_t)a2*b21 - (uint64_t)a3*b31 - (uint64_t)a4*b41 + c1;
    mask = ~(0 - (temp & 1));      // If temp is even then mask = 0xFF...FF, else mask = 0
    
    scalars[0] = temp + (mask & b41);
    scalars[1] = (uint64_t)a1*b12 + (uint64_t)a2     - (uint64_t)a3*b32 - (uint64_t)a4*b42 + c2 + (mask & b42);
    scalars[2] = (uint64_t)a3*b33 - (uint64_t)a1*b13 - (uint64_t)a2     + (uint64_t)a4*b43 + c3 - (mask & b43);
    scalars[3] = (uint64_t)a1*b14 - (uint64_t)a2*b24 - (uint64_t)a3*b34 + (uint64_t)a4*b44 + c4 - (mask & b44);
#endif

#ifdef TEMP_ZEROING
    clear_words((void*)&a1, sizeof(uint64_t)/sizeof(unsigned int));
    clear_words((void*)&a2, sizeof(uint64_t)/sizeof(unsigned int));
    clear_words((void*)&a3, sizeof(uint64_t)/sizeof(unsigned int));
    clear_words((void*)&a4, sizeof(uint64_t)/sizeof(unsigned int));
    clear_words((void*)&temp, sizeof(uint64_t)/sizeof(unsigned int));
    clear_words((void*)&mask, sizeof(uint64_t)/sizeof(unsigned int));
#endif
}


void ecc_precomp(point_extproj_t P, point_extproj_precomp_t *T)
{ // Generation of the precomputation table used by the variable-base scalar multiplication ecc_mul().
  // Input: P = (X1,Y1,Z1,Ta,Tb), where T1 = Ta*Tb, corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates
  // Output: table T containing 8 points: P, P+phi(P), P+psi(P), P+phi(P)+psi(P), P+psi(phi(P)), P+phi(P)+psi(phi(P)), P+psi(P)+psi(phi(P)), P+phi(P)+psi(P)+psi(phi(P))
  // Precomputed points use the representation (X+Y,Y-X,2Z,2dT) corresponding to (X:Y:Z:T) in extended twisted Edwards coordinates
    point_extproj_precomp_t Q, R, S;
    point_extproj_t PP;                    

    // Generating Q = phi(P) = (XQ+YQ,YQ-XQ,ZQ,TQ)
    ecccopy(P, PP);
    ecc_phi(PP);
    R1_to_R3(PP, Q);                       // Converting from (X,Y,Z,Ta,Tb) to (X+Y,Y-X,Z,T) 

    // Generating S = psi(Q) = (XS+YS,YS-XS,ZS,TS)
    ecc_psi(PP);  
    R1_to_R3(PP, S);                       // Converting from (X,Y,Z,Ta,Tb) to (X+Y,Y-X,Z,T) 

    // Generating T[0] = P = (XP+YP,YP-XP,2ZP,2dTP) 
    R1_to_R2(P, T[0]);                     // Converting from (X,Y,Z,Ta,Tb) to (X+Y,Y-X,2Z,2dT)

    // Generating R = psi(P) = (XR+YR,YR-XR,ZR,TR)
    ecc_psi(P); 
    R1_to_R3(P, R);                        // Converting from (X,Y,Z,Ta,Tb) to (X+Y,Y-X,Z,T)  

    eccadd_core(T[0], Q, PP);              // T[1] = P+Q using the representations (X,Y,Z,Ta,Tb) <- (X+Y,Y-X,2Z,2dT) + (X+Y,Y-X,Z,T)
    R1_to_R2(PP, T[1]);                    // Converting from (X,Y,Z,Ta,Tb) to (X+Y,Y-X,2Z,2dT)
    eccadd_core(T[0], R, PP);              // T[2] = P+R 
    R1_to_R2(PP, T[2]);
    eccadd_core(T[1], R, PP);              // T[3] = P+Q+R 
    R1_to_R2(PP, T[3]);
    eccadd_core(T[0], S, PP);              // T[4] = P+S 
    R1_to_R2(PP, T[4]);
    eccadd_core(T[1], S, PP);              // T[5] = P+Q+S 
    R1_to_R2(PP, T[5]);
    eccadd_core(T[2], S, PP);              // T[6] = P+R+S 
    R1_to_R2(PP, T[6]);
    eccadd_core(T[3], S, PP);              // T[7] = P+Q+R+S 
    R1_to_R2(PP, T[7]);              
}


void recode(uint64_t* scalars, unsigned int* digits, unsigned int* sign_masks)
{ // Recoding sub-scalars for use in the variable-base scalar multiplication. See Algorithm 1 in "Efficient and Secure Methods for GLV-Based Scalar 
  // Multiplication and their Implementation on GLV-GLS Curves (Extended Version)", A. Faz-Hernandez, P. Longa, and A.H. Sanchez, in Journal
  // of Cryptographic Engineering, Vol. 5(1), 2015.
  // Input: 4 64-bit sub-scalars passed through "scalars", which are obtained after calling decompose().
  // Outputs: "digits" array with 65 nonzero entries. Each entry is in the range [0, 7], corresponding to one entry in the precomputed table.
  //          "sign_masks" array with 65 entries storing the signs for their corresponding digits in "digits". 
  //          Notation: if the corresponding digit > 0 then sign_mask = 0xFF...FF, else if digit < 0 then sign_mask = 0.
    unsigned int i, bit, bit0, carry;
    sign_masks[64] = (unsigned int)-1; 

    for (i = 0; i < 64; i++)
    {
        scalars[0] >>= 1;
        bit0 = (unsigned int)scalars[0] & 1;
        sign_masks[i] = 0 - bit0;

        bit = (unsigned int)scalars[1] & 1;
        carry = (bit0 | bit) ^ bit0; 
        scalars[1] = (scalars[1] >> 1) + (uint64_t)carry; 
        digits[i] = bit;

        bit = (unsigned int)scalars[2] & 1;
        carry = (bit0 | bit) ^ bit0; 
        scalars[2] = (scalars[2] >> 1) + (uint64_t)carry; 
        digits[i] += (bit << 1);

        bit = (unsigned int)scalars[3] & 1;
        carry = (bit0 | bit) ^ bit0; 
        scalars[3] = (scalars[3] >> 1) + (uint64_t)carry; 
        digits[i] += (bit << 2);
    }
    digits[64] = (unsigned int)(scalars[1] + (scalars[2] << 1) + (scalars[3] << 2));
}


bool ecc_mul(point_t P, digit_t* k, point_t Q, bool clear_cofactor)
{ // Variable-base scalar multiplication Q = k*P using a 4-dimensional decomposition
  // Inputs: scalar "k" in [0, 2^256-1],
  //         point P = (x,y) in affine coordinates,
  //         clear_cofactor = 1 (TRUE) or 0 (FALSE) whether cofactor clearing is required or not, respectively.
  // Output: Q = k*P in affine coordinates (x,y).
  // This function performs point validation and (if selected) cofactor clearing.
    point_extproj_t R;
    point_extproj_precomp_t S, Table[8];
    uint64_t scalars[NWORDS64_ORDER];
    unsigned int digits[65], sign_masks[65];
    int i;

    point_setup(P, R);                                        // Convert to representation (X,Y,1,Ta,Tb)
    decompose((uint64_t*)k, scalars);                         // Scalar decomposition
    
    if (ecc_point_validate(R) == false) {                     // Check if point lies on the curve
        return false;
    }
    
    if (clear_cofactor == true) {
        cofactor_clearing(R);
    }
    recode(scalars, digits, sign_masks);                      // Scalar recoding
    ecc_precomp(R, Table);                                    // Precomputation
    table_lookup_1x8(Table, S, digits[64], sign_masks[64]);   // Extract initial point in (X+Y,Y-X,2Z,2dT) representation
    R2_to_R4(S, R);                                           // Conversion to representation (2X,2Y,2Z)
    
    for (i = 63; i >= 0; i--)
    {
        table_lookup_1x8(Table, S, digits[i], sign_masks[i]); // Extract point S in (X+Y,Y-X,2Z,2dT) representation
        eccdouble(R);                                         // P = 2*P using representations (X,Y,Z,Ta,Tb) <- 2*(X,Y,Z)
        eccadd(S, R);                                         // P = P+S using representations (X,Y,Z,Ta,Tb) <- (X,Y,Z,Ta,Tb) + (X+Y,Y-X,2Z,2dT)
    }
    eccnorm(R, Q);                                            // Conversion to affine coordinates (x,y) and modular correction. 
    
#ifdef TEMP_ZEROING
    clear_words((void*)digits, 65);
    clear_words((void*)sign_masks, 65);
    clear_words((void*)S, sizeof(point_extproj_precomp_t)/sizeof(unsigned int));
#endif
    return true;
}


void cofactor_clearing(point_extproj_t P)
{ // Co-factor clearing
  // Input: P = (X1,Y1,Z1,Ta,Tb), where T1 = Ta*Tb, corresponding to (X1:Y1:Z1:T1) in extended twisted Edwards coordinates
  // Output: P = 392*P = (Xfinal,Yfinal,Zfinal,Tafinal,Tbfinal), where Tfinal = Tafinal*Tbfinal,
  //         corresponding to (Xfinal:Yfinal:Zfinal:Tfinal) in extended twisted Edwards coordinates
    point_extproj_precomp_t Q;
     
    R1_to_R2(P, Q);                      // Converting from (X,Y,Z,Ta,Tb) to (X+Y,Y-X,2Z,2dT)
    eccdouble(P);                        // P = 2*P using representations (X,Y,Z,Ta,Tb) <- 2*(X,Y,Z)
    eccadd(Q, P);                        // P = P+Q using representations (X,Y,Z,Ta,Tb) <- (X,Y,Z,Ta,Tb) + (X+Y,Y-X,2Z,2dT)
    eccdouble(P);
    eccdouble(P);
    eccdouble(P);
    eccdouble(P);
    eccadd(Q, P);
    eccdouble(P);
    eccdouble(P);
    eccdouble(P);
}

#endif
