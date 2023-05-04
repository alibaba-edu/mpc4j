/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: testing code for FourQ's curve arithmetic 
************************************************************************************/

#include "../FourQ_api.h"
#include "../FourQ_params.h"
#include "../FourQ_tables.h"
#include "test_extras.h"
#include <stdio.h>


// Benchmark and test parameters  
#if defined(GENERIC_IMPLEMENTATION)
    #define BENCH_LOOPS       10000      // Number of iterations per bench
    #define SHORT_BENCH_LOOPS 1000       // Number of iterations per bench (for expensive operations)
#else
    #define BENCH_LOOPS       100000
    #define SHORT_BENCH_LOOPS 10000
#endif
#define TEST_LOOPS            1000       // Number of iterations per test


bool ecc_test()
{
    bool clear_cofactor, OK = true;
    unsigned int n;
    int passed;
    point_t A;
    point_extproj_t P; // 真正运算的坐标类型
    point_extproj_precomp_t Q; // 在上面的基础上进行预计算的类型
    f2elm_t t1; // 有限域元素
    uint64_t scalar[4], res_x[4], res_y[4];


    printf("\n--------------------------------------------------------------------------------------------------------\n\n");
    printf("Testing FourQ's curve arithmetic: \n\n");

    // Point doubling
    passed = 1;
    eccset(A); // A = G
    point_setup(A, P); // convert point_t to point_extproj_t

    for (n=0; n<TEST_LOOPS; n++)
    {
        eccdouble(P);                      // 2*P
    } // double 的计算都是在 point_extproj_t 这个坐标系下进行的

    // 把 point_extproj_t 变换回 point_t
    // 并 处理到有限域中
    eccnorm(P, A);
    mod1271(A->x[0]); mod1271(A->x[1]);    // Fully reduced P
    mod1271(A->y[0]); mod1271(A->y[1]);

    // Result
    // 坐标x,y 本质上就是 uint64_t[4] , 表示 a + bi 
    // 前2个值表示 a, 后两个值表示 b
    res_x[0] = 0xC9099C54855859D6; res_x[1] = 0x2C3FD8822C82270F; res_x[2] = 0xA7B3F6E2043E8E68; res_x[3] = 0x4DA5B9E83AA7A1B2;
    res_y[0] = 0x3EE089F0EB49AA14; res_y[1] = 0x2001EB3A57688396; res_y[2] = 0x1FEE5617A7E954CD; res_y[3] = 0x0FFDB0D761421F50;

    if (fp2compare64((uint64_t*)A->x, res_x)!=0 || fp2compare64((uint64_t*)A->y, res_y)!=0) passed=0;
    if (passed==1) printf("  Point doubling tests .................................................................... PASSED");
    else { printf("  Point doubling tests ... FAILED"); printf("\n"); return false; }
    printf("\n");

    // Point addition
    eccset(A); // G
    point_setup(A, P); // convert point_t (x, y) to point_extproj_t (X, Y, Z, Ta, Tb)
    // 可以发现 真正运算的坐标系都是 point_extproj_t

    // 没看太懂 前面的每一行都在做什么计算呢？
    // 难道说 eccadd 需要完成前面的步骤，才能够正确的调用？
    for (n=0; n<TEST_LOOPS; n++)
    {   
        // -x^2 + y^2 = 1 + dx^2y^2
        // t1 = d
        fp2copy1271((felm_t*)&PARAMETER_d, t1); 
        fp2mul1271(t1, P->ta, t1);         // d*ta
        fp2add1271(t1, t1, t1);            // 2*d*ta
        fp2mul1271(t1, P->tb, Q->t2);      // 2*d*t
        fp2add1271(P->x, P->y, Q->xy);     // x+y    
        fp2sub1271(P->y, P->x, Q->yx);     // y-x
        fp2copy1271(P->z, Q->z2);
        fp2add1271(Q->z2, Q->z2, Q->z2);   // 2*z
        // 前面的每一行都在干什么呢？
        eccadd(Q, P);                      // 2*P
    }
    // 转换回 point_t 并 验证计算正确性
    eccnorm(P, A);
    mod1271(A->x[0]); mod1271(A->x[1]);    // Fully reduced P
    mod1271(A->y[0]); mod1271(A->y[1]);

    // Result
    res_x[0] = 0xC9099C54855859D6; res_x[1] = 0x2C3FD8822C82270F; res_x[2] = 0xA7B3F6E2043E8E68; res_x[3] = 0x4DA5B9E83AA7A1B2;
    res_y[0] = 0x3EE089F0EB49AA14; res_y[1] = 0x2001EB3A57688396; res_y[2] = 0x1FEE5617A7E954CD; res_y[3] = 0x0FFDB0D761421F50;

    if (fp2compare64((uint64_t*)A->x, res_x)!=0 || fp2compare64((uint64_t*)A->y, res_y)!=0) passed=0;

    eccset(A);
    point_setup(A, P);
    fp2copy1271((felm_t*)&PARAMETER_d, t1);
    fp2mul1271(t1, P->x, t1);              // d*x
    fp2add1271(t1, t1, t1);                // 2*d*x
    fp2mul1271(t1, P->y, Q->t2);           // 2*d*t
    fp2add1271(P->x, P->y, Q->xy);         // x+y    
    fp2sub1271(P->y, P->x, Q->yx);         // y-x
    fp2zero1271(Q->z2); *Q->z2[0] = 2;     // 2*z
    eccdouble(P);                          // P = 2P 

    for (n=0; n<TEST_LOOPS; n++)
    {
        eccadd(Q, P);                      // P = P+Q
    }
    eccnorm(P, A);
    mod1271(A->x[0]); mod1271(A->x[1]);    // Fully reduced P
    mod1271(A->y[0]); mod1271(A->y[1]);

    // Result
    res_x[0] = 0x6480B1EF0A151DB0; res_x[1] = 0x3E243958590C4D90; res_x[2] = 0xAA270F644A65D473; res_x[3] = 0x5327AF7D84238CD0;
    res_y[0] = 0x5E06003D73C43EB1; res_y[1] = 0x3EF69A49CB7E0237; res_y[2] = 0x4E752648AC2EF0AB; res_y[3] = 0x293EB1E26DD23B4E;

    if (fp2compare64((uint64_t*)A->x, res_x)!=0 || fp2compare64((uint64_t*)A->y, res_y)!=0) passed=0;

    if (passed==1) printf("  Point addition tests .................................................................... PASSED");
    else { printf("  Point addition tests ... FAILED"); printf("\n"); return false; }
    printf("\n");

#if (USE_ENDO == true)
    // Psi endomorphism
    eccset(A); 
    point_setup(A, P);

    for (n=0; n<TEST_LOOPS; n++)
    {
        ecc_psi(P);                        // P = Psi(P)
    }    
    eccnorm(P, A);
    mod1271(A->x[0]); mod1271(A->x[1]);    // Fully reduced P
    mod1271(A->y[0]); mod1271(A->y[1]);   

    // Result
    res_x[0] = 0xD8F3C8C24A2BC7E2; res_x[1] = 0x75AF54EDB41A2B93; res_x[2] = 0x4DE2466701F009A9; res_x[3] = 0x065249F9EDE0C798;
    res_y[0] = 0x1C6E119ADD608104; res_y[1] = 0x06DBB85BFFB7C21E; res_y[2] = 0xFD234D6C4CFA3EC1; res_y[3] = 0x060A30903424BF13;

    if (fp2compare64((uint64_t*)A->x, res_x)!=0 || fp2compare64((uint64_t*)A->y, res_y)!=0) passed=0;

    if (passed==1) printf("  Psi endomorphism tests .................................................................. PASSED");
    else { printf("  Psi endomorphism tests ... FAILED"); printf("\n"); return false; }
    printf("\n");
   
    // Phi endomorphism
    {        
    eccset(A); 
    point_setup(A, P);

    for (n=0; n<TEST_LOOPS; n++)
    {
        ecc_phi(P);                        // P = Phi(P)
        eccnorm(P, A);
        point_setup(A, P);
    }    
    mod1271(A->x[0]); mod1271(A->x[1]);    // Fully reduced P
    mod1271(A->y[0]); mod1271(A->y[1]); 

    // Result
    res_x[0] = 0xD5B5A3061287DB16; res_x[1] = 0x5550AAB9E7A620EE; res_x[2] = 0xEC321E6CF33610FC; res_x[3] = 0x3E61EBB9A1CB0210;
    res_y[0] = 0x7E2851D5A8E83FB9; res_y[1] = 0x5474BF8EC55603AE; res_y[2] = 0xA5077613491788D5; res_y[3] = 0x5476093DBF8BF6BF;

    if (fp2compare64((uint64_t*)A->x, res_x)!=0 || fp2compare64((uint64_t*)A->y, res_y)!=0) passed=0;
    if (passed==1) printf("  Phi endomorphism tests .................................................................. PASSED");
    else { printf("  Phi endomorphism tests ... FAILED"); printf("\n"); return false; }
    printf("\n");
    
    // Scalar decomposition and recoding
    {        
    uint64_t acc1, acc2, acc3, acc4, scalars[4];
    unsigned int digits[65], sign_masks[65];
    uint64_t k[4];
    int i;

    for (n=0; n<TEST_LOOPS*10; n++)
    {
        random_scalar_test(k);
        decompose(k, scalars);  
        fp2copy1271((felm_t*)scalars, (felm_t*)scalar);
        recode(scalars, digits, sign_masks); 

        acc1 = acc2 = acc3 = acc4 = 0; 

        for (i = 64; i >= 0; i--)
        {
            acc1 = 2*acc1; acc2 = 2*acc2; acc3 = 2*acc3; acc4 = 2*acc4; 
            if (sign_masks[i] == (unsigned int)-1) {
                acc1 += 1;
                acc2 += (digits[i] & 1);
                acc3 += ((digits[i] >> 1) & 1);
                acc4 += ((digits[i] >> 2) & 1);
            } else if (sign_masks[i] == 0) {
                acc1 -= 1;
                acc2 -= (digits[i] & 1);
                acc3 -= ((digits[i] >> 1) & 1);
                acc4 -= ((digits[i] >> 2) & 1);
            }
        }   
        if (scalar[0] != acc1 || scalar[1] != acc2  || scalar[2] != acc3 || scalar[3] != acc4) { passed=0; break; }
    }
    
    if (passed==1) printf("  Recoding and decomposition tests ........................................................ PASSED");
    else { printf("  Recoding and decomposition tests ... FAILED"); printf("\n"); return false; }
    printf("\n");
    }
    }
#endif

    // Scalar multiplication
    eccset(A);
    clear_cofactor = false;
    scalar[0] = 0x3AD457AB55456230; scalar[1] = 0x3A8B3C2C6FD86E0C; scalar[2] = 0x7E38F7C9CFBB9166; scalar[3] = 0x0028FD6CBDA458F0;

    for (n=0; n<TEST_LOOPS; n++)
    {
        scalar[1] = scalar[2];
        scalar[2] += scalar[0];

        ecc_mul(A, (digit_t*)scalar, A, clear_cofactor);
    }

    res_x[0] = 0xDFD2B477BD494BEF; res_x[1] = 0x257C122BBFC94A1B; res_x[2] = 0x769593547237C459; res_x[3] = 0x469BF80CB5B11F01;
    res_y[0] = 0x281C5067996F3344; res_y[1] = 0x0901B3817C0E936C; res_y[2] = 0x4FE8C429915F1245; res_y[3] = 0x570B948EACACE210;
    if (fp2compare64((uint64_t*)A->x, res_x)!=0 || fp2compare64((uint64_t*)A->y, res_y)!=0) passed=0;


    eccset(A);
    clear_cofactor = true;
    scalar[0] = 0x3AD457AB55456230; scalar[1] = 0x3A8B3C2C6FD86E0C; scalar[2] = 0x7E38F7C9CFBB9166; scalar[3] = 0x0028FD6CBDA458F0;

    for (n=0; n<TEST_LOOPS; n++)
    {
        scalar[1] = scalar[2];
        scalar[2] += scalar[0];

        ecc_mul(A, (digit_t*)scalar, A, clear_cofactor);
    }

    res_x[0] = 0x85CF54A3BEE3FD23; res_x[1] = 0x7A7EC43976FAAD92; res_x[2] = 0x7697567B785E2327; res_x[3] = 0x4CBDAB448B1539F2;
    res_y[0] = 0xE9193B41CDDF94D0; res_y[1] = 0x5AA6C859ECC810D5; res_y[2] = 0xAA876E760AA8B331; res_y[3] = 0x320C53F02230094A;
    if (fp2compare64((uint64_t*)A->x, res_x)!=0 || fp2compare64((uint64_t*)A->y, res_y)!=0) passed=0;

    if (passed==1) printf("  Scalar multiplication tests ............................................................. PASSED");
    else { printf("  Scalar multiplication tests ... FAILED"); printf("\n"); return false; }
    printf("\n");

    {
    point_t AA, B, C;
    unsigned int j, w, v, e, d;
    uint64_t k[4];
    unsigned int digits_fixed[NBITS_ORDER_PLUS_ONE+(W_FIXEDBASE*V_FIXEDBASE)-1] = {0};

    // Scalar recoding using the mLSB-set representation
    w = W_FIXEDBASE;
    v = V_FIXEDBASE;
    e = E_FIXEDBASE;
    d = D_FIXEDBASE;

    for (n=0; n<TEST_LOOPS; n++)
    {
        random_scalar_test(scalar);
        modulo_order((digit_t*)scalar, (digit_t*)scalar);    // k = k mod (order)
        conversion_to_odd((digit_t*)scalar, (digit_t*)k);
        for (j = 0; j < NWORDS64_ORDER; j++) scalar[j] = k[j];
        mLSB_set_recode(k, digits_fixed);

        if (verify_mLSB_recoding(scalar, (int*)digits_fixed)==false) { passed=0; break; }
    }

    if (passed==1) printf("  mLSB-set recoding tests ................................................................. PASSED");
    else { printf("  mLSB-set recoding tests ... FAILED"); printf("\n"); return false; }
    printf("\n");

    // Fixed-base scalar multiplication
    eccset(AA);

    for (n=0; n<TEST_LOOPS; n++)
    {
        random_scalar_test(scalar);
        ecc_mul_fixed((digit_t*)scalar, B);
        ecc_mul(AA, (digit_t*)scalar, C, false);

        if (fp2compare64((uint64_t*)B->x,(uint64_t*)C->x)!=0 || fp2compare64((uint64_t*)B->y,(uint64_t*)C->y)!=0) { passed=0; break; }
    }

    if (passed==1) printf("  Fixed-base scalar multiplication tests .................................................. PASSED");
    else { printf("  Fixed-base scalar multiplication tests ... FAILED"); printf("\n"); return false; }
    printf("\n");
    }

    {
    point_t PP, QQ, RR, UU, TT;
    point_extproj_precomp_t AA;
    point_extproj_t BB;
    uint64_t k[4], l[4], kk[4];

    // Double scalar multiplication
    eccset(QQ);
    eccset(PP);

    for (n=0; n<TEST_LOOPS; n++)
    {
        random_scalar_test(kk);
        ecc_mul(QQ, (digit_t*)kk, QQ, false);
        random_scalar_test(k);
        random_scalar_test(l);
        ecc_mul_double((digit_t*)k, QQ, (digit_t*)l, RR);
        ecc_mul(PP, (digit_t*)k, UU, false);
        ecc_mul(QQ, (digit_t*)l, TT, false);

        fp2add1271(UU->x, UU->y, AA->xy);
        fp2sub1271(UU->y, UU->x, AA->yx);
        fp2mul1271(UU->x, UU->y, AA->t2);
        fp2add1271(AA->t2, AA->t2, AA->t2);
        fp2mul1271(AA->t2, (felm_t*)&PARAMETER_d, AA->t2);
        fp2zero1271(AA->z2); AA->z2[0][0] = 2;
        point_setup(TT, BB);

        eccadd(AA, BB);
        eccnorm(BB, UU);

        if (fp2compare64((uint64_t*)UU->x,(uint64_t*)RR->x)!=0 || fp2compare64((uint64_t*)UU->y,(uint64_t*)RR->y)!=0) { passed=0; break; }
    }

    if (passed==1) printf("  Double scalar multiplication tests ...................................................... PASSED");
    else { printf("  Double scalar multiplication tests ... FAILED"); printf("\n"); return false; }
    printf("\n");
    }

    return OK;
}


bool ecc_run()
{
    bool OK = true;
    unsigned int n;
    unsigned long long cycles, cycles1, cycles2;
    point_t A, B;
    point_extproj_t P;
    point_extproj_precomp_t Q, Table[8];
    f2elm_t t1;
    uint64_t scalar[4];

    printf("\n--------------------------------------------------------------------------------------------------------\n\n");
    printf("Benchmarking FourQ's curve arithmetic \n\n");

    // Point doubling (twisted Edwards a=-1)
    eccset(A);
    point_setup(A, P);

    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        eccdouble(P);
        eccdouble(P);
        eccdouble(P);
        eccdouble(P);
        eccdouble(P);
        eccdouble(P);
        eccdouble(P);
        eccdouble(P);
        eccdouble(P);
        eccdouble(P);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }
    printf("  Point doubling runs in ...                                       %8lld ", cycles/(BENCH_LOOPS*10)); print_unit;
    printf("\n");

    // Point addition (twisted Edwards a=-1)
    eccset(A);
    point_setup(A, P);
    fp2copy1271((felm_t*)&PARAMETER_d, t1);
    fp2mul1271(t1, P->x, t1);              // d*x
    fp2add1271(t1, t1, t1);                // 2*d*x
    fp2mul1271(t1, P->y, Q->t2);           // 2*d*t
    fp2add1271(P->x, P->y, Q->xy);         // x+y    
    fp2sub1271(P->y, P->x, Q->yx);         // y-x
    fp2zero1271(Q->z2); *Q->z2[0] = 2;     // 2*z
    eccdouble(P);                          // P = 2P 

    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        eccadd(Q, P);
        eccadd(Q, P);
        eccadd(Q, P);
        eccadd(Q, P);
        eccadd(Q, P);
        eccadd(Q, P);
        eccadd(Q, P);
        eccadd(Q, P);
        eccadd(Q, P);
        eccadd(Q, P);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }
    printf("  Point addition runs in ...                                       %8lld ", cycles/(BENCH_LOOPS*10)); print_unit;
    printf("\n");

#if (USE_ENDO == true)
    // Psi endomorphism
    eccset(A);
    point_setup(A, P);

    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        ecc_psi(P);
        ecc_psi(P);
        ecc_psi(P);
        ecc_psi(P);
        ecc_psi(P);
        ecc_psi(P);
        ecc_psi(P);
        ecc_psi(P);
        ecc_psi(P);
        ecc_psi(P);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }
    printf("  Psi mapping runs in ...                                          %8lld ", cycles/(BENCH_LOOPS*10)); print_unit;
    printf("\n");
   
    // Phi endomorphism
    eccset(A);
    point_setup(A, P);

    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        ecc_phi(P);
        ecc_phi(P);
        ecc_phi(P);
        ecc_phi(P);
        ecc_phi(P);
        ecc_phi(P);
        ecc_phi(P);
        ecc_phi(P);
        ecc_phi(P);
        ecc_phi(P);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }
    printf("  Phi mapping runs in ...                                          %8lld ", cycles/(BENCH_LOOPS*10)); print_unit;
    printf("\n");
   
    // Scalar decomposition
    {
    uint64_t scalars[4];
    random_scalar_test(scalar); 
    
    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        decompose(scalar, scalars);
        decompose(scalar, scalars);
        decompose(scalar, scalars);
        decompose(scalar, scalars);
        decompose(scalar, scalars);
        decompose(scalar, scalars);
        decompose(scalar, scalars);
        decompose(scalar, scalars);
        decompose(scalar, scalars);
        decompose(scalar, scalars);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }
    
    printf("  Scalar decomposition runs in ...                                 %8lld ", cycles/(BENCH_LOOPS*10)); print_unit;
    printf("\n");
    }

    // Scalar recoding
    {
    unsigned int digits[65], sign_masks[65];
    random_scalar_test(scalar); 

    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        recode(scalar, digits, sign_masks);
        recode(scalar, digits, sign_masks);
        recode(scalar, digits, sign_masks);
        recode(scalar, digits, sign_masks);
        recode(scalar, digits, sign_masks);
        recode(scalar, digits, sign_masks);
        recode(scalar, digits, sign_masks);
        recode(scalar, digits, sign_masks);
        recode(scalar, digits, sign_masks);
        recode(scalar, digits, sign_masks);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }
    
    printf("  Scalar recoding runs in ...                                      %8lld ", cycles/(BENCH_LOOPS*10)); print_unit;
    printf("\n");  
    }
#endif

    // Precomputation
    eccset(A);
    point_setup(A, P);

    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        ecc_precomp(P, Table);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }

    printf("  Precomputation runs in ...                                       %8lld ", cycles/BENCH_LOOPS); print_unit;
    printf("\n");

    // Table lookup
    eccset(A);
    point_setup(A, P);
    ecc_precomp(P, Table);

    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        table_lookup_1x8(Table, Q, 0, 0);
        table_lookup_1x8(Table, Q, 1, (unsigned int)-1);
        table_lookup_1x8(Table, Q, 2, 0);
        table_lookup_1x8(Table, Q, 3, (unsigned int)-1);
        table_lookup_1x8(Table, Q, 4, 0);
        table_lookup_1x8(Table, Q, 5, (unsigned int)-1);
        table_lookup_1x8(Table, Q, 6, 0);
        table_lookup_1x8(Table, Q, 7, (unsigned int)-1);
        table_lookup_1x8(Table, Q, 0, 0);
        table_lookup_1x8(Table, Q, 1, (unsigned int)-1);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }

    printf("  Table lookup runs in ...                                         %8lld ", cycles/(BENCH_LOOPS*10)); print_unit;
    printf("\n");

    // Scalar multiplication
    random_scalar_test(scalar);

    for (n=0; n<SHORT_BENCH_LOOPS; n++)
    {
        eccset(A);
        ecc_mul(A, (digit_t*)scalar, B, false);
    }
    cycles = 0;
    for (n=0; n<SHORT_BENCH_LOOPS; n++)
    {
        eccset(A);
        cycles1 = cpucycles();
        ecc_mul(A, (digit_t*)scalar, B, false);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }

    printf("  Scalar multiplication (without clearing cofactor) runs in ...    %8lld ", cycles/SHORT_BENCH_LOOPS); print_unit;
    printf("\n");

    random_scalar_test(scalar);

    for (n=0; n<SHORT_BENCH_LOOPS; n++)
    {
        eccset(A);
        ecc_mul(A, (digit_t*)scalar, B, true);
    }
    cycles = 0;
    for (n=0; n<SHORT_BENCH_LOOPS; n++)
    {
        eccset(A);
        cycles1 = cpucycles();
        ecc_mul(A, (digit_t*)scalar, B, true);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }

    printf("  Scalar multiplication (including clearing cofactor) runs in ...  %8lld ", cycles/SHORT_BENCH_LOOPS); print_unit;
    printf("\n");

    {
    point_precomp_t T;
    unsigned int digits_fixed[256+(W_FIXEDBASE*V_FIXEDBASE)-1] = {0};

    // Reduction modulo the order
    random_scalar_test(scalar);

    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        modulo_order((digit_t*)scalar, (digit_t*)scalar);
        modulo_order((digit_t*)scalar, (digit_t*)scalar);
        modulo_order((digit_t*)scalar, (digit_t*)scalar);
        modulo_order((digit_t*)scalar, (digit_t*)scalar);
        modulo_order((digit_t*)scalar, (digit_t*)scalar);
        modulo_order((digit_t*)scalar, (digit_t*)scalar);
        modulo_order((digit_t*)scalar, (digit_t*)scalar);
        modulo_order((digit_t*)scalar, (digit_t*)scalar);
        modulo_order((digit_t*)scalar, (digit_t*)scalar);
        modulo_order((digit_t*)scalar, (digit_t*)scalar);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }

    printf("  Reduction modulo the order runs in ...                           %8lld ", cycles/(BENCH_LOOPS*10)); print_unit;
    printf("\n");

    // Scalar recoding using the mLSB-set representation
    random_scalar_test(scalar);

    cycles = 0;
    for (n=0; n<SHORT_BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        mLSB_set_recode(scalar, digits_fixed);
        mLSB_set_recode(scalar, digits_fixed);
        mLSB_set_recode(scalar, digits_fixed);
        mLSB_set_recode(scalar, digits_fixed);
        mLSB_set_recode(scalar, digits_fixed);
        mLSB_set_recode(scalar, digits_fixed);
        mLSB_set_recode(scalar, digits_fixed);
        mLSB_set_recode(scalar, digits_fixed);
        mLSB_set_recode(scalar, digits_fixed);
        mLSB_set_recode(scalar, digits_fixed);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }

    printf("  Fixed-base recoding runs in ...                                  %8lld ", cycles/(SHORT_BENCH_LOOPS*10)); print_unit;
    printf("\n");

    // Table lookup for fixed-base scalar multiplication
    eccset(A);

    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        cycles1 = cpucycles();
        table_lookup_fixed_base((point_precomp_t*)&FIXED_BASE_TABLE, T, 1, 0);
        table_lookup_fixed_base((point_precomp_t*)&FIXED_BASE_TABLE, T, 2, (unsigned int)-1);
        table_lookup_fixed_base((point_precomp_t*)&FIXED_BASE_TABLE, T, 1, 0);
        table_lookup_fixed_base((point_precomp_t*)&FIXED_BASE_TABLE, T, 2, (unsigned int)-1);
        table_lookup_fixed_base((point_precomp_t*)&FIXED_BASE_TABLE, T, 1, 0);
        table_lookup_fixed_base((point_precomp_t*)&FIXED_BASE_TABLE, T, 2, (unsigned int)-1);
        table_lookup_fixed_base((point_precomp_t*)&FIXED_BASE_TABLE, T, 1, 0);
        table_lookup_fixed_base((point_precomp_t*)&FIXED_BASE_TABLE, T, 2, (unsigned int)-1);
        table_lookup_fixed_base((point_precomp_t*)&FIXED_BASE_TABLE, T, 1, 0);
        table_lookup_fixed_base((point_precomp_t*)&FIXED_BASE_TABLE, T, 2, (unsigned int)-1);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }

    printf("  Fixed-base table lookup runs in ...                              %8lld ", cycles/(BENCH_LOOPS*10)); print_unit;
    printf("\n");

    // Fixed-base scalar multiplication
    eccset(A);

    cycles = 0;
    for (n=0; n<SHORT_BENCH_LOOPS; n++)
    {
        random_scalar_test(scalar);
        cycles1 = cpucycles();
        ecc_mul_fixed((digit_t*)scalar, B);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }

    printf("  Fixed-base scalar mul runs in ...                                %8lld cycles with w=%d and v=%d", cycles/SHORT_BENCH_LOOPS, W_FIXEDBASE, V_FIXEDBASE);
    printf("\n");
    }

    {
    point_t PP, QQ, RR;
    uint64_t k[4], l[4], kk[4];

    // Double scalar multiplication
    eccset(QQ);
    eccset(PP);
    random_scalar_test(kk);
    ecc_mul(QQ, (digit_t*)kk, QQ, false);

    cycles = 0;
    for (n=0; n<SHORT_BENCH_LOOPS; n++)
    {
        random_scalar_test(k);
        random_scalar_test(l);
        cycles1 = cpucycles();
        ecc_mul_double((digit_t*)k, QQ, (digit_t*)l, RR);
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }

    printf("  Double scalar mul runs in ...                                    %8lld cycles with wP=%d and wQ=%d", cycles/SHORT_BENCH_LOOPS, WP_DOUBLEBASE, WQ_DOUBLEBASE);
    printf("\n");
    }

    return OK;
}


int main()
{
    bool OK = true;

    OK = OK && ecc_test();         // Test FourQ's curve functions
    OK = OK && ecc_run();          // Benchmark FourQ's curve functions

    return OK;
}
