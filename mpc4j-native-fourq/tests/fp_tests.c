/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: testing code for FourQ's field arithmetic 
************************************************************************************/    

#include "../FourQ_internal.h"
#include "../FourQ_params.h"
#include "test_extras.h"
#include <stdio.h>
#include <string.h>


// Benchmark and test parameters 
#define BENCH_LOOPS       10000      // Number of iterations per bench
#define SHORT_BENCH_LOOPS 1000       // Number of iterations per bench (for expensive operations)
#define TEST_LOOPS        1000       // Number of iterations per test


bool fp2_test()
{ // Tests for the quadratic extension field arithmetic
    bool OK = true;
    int n, passed;
    f2elm_t a, b, c, d, e, f;
	digit_t ma[NWORDS_ORDER], mb[NWORDS_ORDER], mc[NWORDS_ORDER], md[NWORDS_ORDER], me[NWORDS_ORDER], mf[NWORDS_ORDER], one[NWORDS_ORDER] = {0};
	one[0] = 1;

    printf("\n--------------------------------------------------------------------------------------------------------\n\n"); 
    printf("Testing quadratic extension field arithmetic over GF((2^127-1)^2): \n\n"); 

    // GF(p^2) multiplication using p = 2^127-1
    passed = 1;
    for (n=0; n<TEST_LOOPS; n++)
    {    
        fp2random1271_test(a); fp2random1271_test(b); fp2random1271_test(c); 
        fp2random1271_test(d); fp2random1271_test(e); fp2random1271_test(f);
       
        fp2mul1271(a, b, d);                                             // d = a*b 
        mod1271(d[0]); mod1271(d[1]);
        fp2mul1271(b, a, e);                                             // e = b*a 
        mod1271(e[0]); mod1271(e[1]);
        if (fp2compare64((uint64_t*)d,(uint64_t*)e)!=0) { passed=0; break; }
        
        fp2mul1271(a, b, d); fp2mul1271(d, c, e);                        // e = (a*b)*c
        mod1271(e[0]); mod1271(e[1]);
        fp2mul1271(b, c, d); fp2mul1271(d, a, f);                        // f = a*(b*c)
        mod1271(f[0]); mod1271(f[1]);
        if (fp2compare64((uint64_t*)e,(uint64_t*)f)!=0) { passed=0; break; }
      
        fp2add1271(b, c, d); fp2mul1271(a, d, e);                        // e = a*(b+c)
        mod1271(e[0]); mod1271(e[1]);
        fp2mul1271(a, b, d); fp2mul1271(a, c, f); fp2add1271(d, f, f);   // f = a*b+a*c
        mod1271(f[0]); mod1271(f[1]);
        if (fp2compare64((uint64_t*)e,(uint64_t*)f)!=0) { passed=0; break; }
        
        fp2zero1271(b); b[0][0] = 1;
        fp2mul1271(a, b, d);                                             // d = a*1 
        mod1271(d[0]); mod1271(d[1]);                                                                     
        if (fp2compare64((uint64_t*)a,(uint64_t*)d)!=0) { passed=0; break; }
        
        fp2zero1271(b);
        fp2mul1271(a, b, d);                                             // d = a*0  
        mod1271(d[0]); mod1271(d[1]); 
        if (fp2compare64((uint64_t*)b,(uint64_t*)d)!=0) { passed=0; break; }
    }
    if (passed==1) printf("  GF(p^2) multiplication tests .................................................................... PASSED");
    else { printf("  GF(p^2) multiplication tests... FAILED"); printf("\n"); return false; }
    printf("\n");
    
    // GF(p^2) squaring using p = 2^127-1
    passed = 1;
    for (n=0; n<TEST_LOOPS; n++)
    {
        fp2random1271_test(a); fp2random1271_test(b); fp2random1271_test(c);

        fp2sqr1271(a, b);                                            // b = a^2
        fp2mul1271(a, a, c);                                         // c = a*a 
        if (fp2compare64((uint64_t*)b,(uint64_t*)c)!=0) { passed=0; break; }
        
        fp2zero1271(a);
        fp2sqr1271(a, d);                                            // d = 0^2 
        if (fp2compare64((uint64_t*)a,(uint64_t*)d)!=0) { passed=0; break; }
    }
    if (passed==1) printf("  GF(p^2) squaring tests........................................................................... PASSED");
    else { printf("  GF(p^2) squaring tests... FAILED"); printf("\n"); return false; }
    printf("\n");

    // GF(p^2) inversion using p = 2^127-1
    passed = 1;
    for (n=0; n<TEST_LOOPS; n++)
    {
        fp2random1271_test(a);  
        
        fp2zero1271(d); d[0][0] = 1;           
        fp2copy1271(a, b);                        
        fp2inv1271(a);                                
        fp2mul1271(a, b, c);                                        // c = a*a^-1 = 1
        mod1271(c[0]); mod1271(c[1]);
        if (fp2compare64((uint64_t*)c,(uint64_t*)d)!=0) { passed=0; break; }
    }
    if (passed==1) printf("  GF(p^2) inversion tests.......................................................................... PASSED");
    else { printf("  GF(p^2) inversion tests... FAILED"); printf("\n"); return false; }
    printf("\n");

	// Modular addition, modulo the order of a curve
	passed = 1;
	for (n = 0; n<TEST_LOOPS; n++)
	{
		random_order_test(ma); random_order_test(mb); random_order_test(mc);

		add_mod_order(ma, mb, md); add_mod_order(md, mc, me);       // e = (a+b)+c
		add_mod_order(mb, mc, md); add_mod_order(md, ma, mf);       // f = a+(b+c)
		if (fp2compare64((uint64_t*)me,(uint64_t*)mf)!=0) { passed=0; break; }

		add_mod_order(ma, mb, md);                                  // d = a+b 
		add_mod_order(mb, ma, me);                                  // e = b+a
		if (fp2compare64((uint64_t*)md,(uint64_t*)me)!=0) { passed=0; break; }

		memset((unsigned char*)mb, 0, 32);
		add_mod_order(ma, mb, md);                                  // d = a+0 
		if (fp2compare64((uint64_t*)ma,(uint64_t*)md)!=0) { passed=0; break; }
	}
	if (passed==1) printf("  Modular addition tests .......................................................................... PASSED");
	else { printf("  Modular addition tests... FAILED"); printf("\n"); return false; }
	printf("\n");
	
	// Montgomery multiplication modulo the order of the curve 
	passed = 1;
	for (n=0; n<TEST_LOOPS; n++)
	{
		random_order_test(ma); random_order_test(mb); random_order_test(mc);

		Montgomery_multiply_mod_order(ma, (digit_t*)&Montgomery_Rprime, md);                   // Conversion to/from Montgomery
		Montgomery_multiply_mod_order(md, one, me);                                            // Converting back to standard representation
		if (fp2compare64((uint64_t*)ma,(uint64_t*)me)!=0) { passed=0; break; }

		Montgomery_multiply_mod_order(ma, mb, md); Montgomery_multiply_mod_order(md, mc, me);  // e = (a*b)*c
		Montgomery_multiply_mod_order(mb, mc, md); Montgomery_multiply_mod_order(md, ma, mf);  // f = a*(b*c)
		if (fp2compare64((uint64_t*)me,(uint64_t*)mf)!=0) { passed=0; break; }
		
		add_mod_order(mb, mc, md); Montgomery_multiply_mod_order(ma, md, me);                  // e = a*(b+c)
		Montgomery_multiply_mod_order(ma, mb, md); Montgomery_multiply_mod_order(ma, mc, mf); 
		add_mod_order(md, mf, mf);                                                             // f = a*b+a*c
		if (fp2compare64((uint64_t*)me,(uint64_t*)mf)!=0) { passed=0; break; }

		subtract_mod_order(mb, mc, md); Montgomery_multiply_mod_order(ma, md, me);             // e = a*(b-c)
		Montgomery_multiply_mod_order(ma, mb, md); Montgomery_multiply_mod_order(ma, mc, mf);
		subtract_mod_order(md, mf, mf);                                                        // f = a*b-a*c
		if (fp2compare64((uint64_t*)me, (uint64_t*)mf) != 0) { passed = 0; break; }

		Montgomery_multiply_mod_order(ma, mb, md);                                             // d = a*b 
		Montgomery_multiply_mod_order(mb, ma, me);                                             // e = b*a 
		if (fp2compare64((uint64_t*)md,(uint64_t*)me)!=0) { passed=0; break; }

		Montgomery_multiply_mod_order(one, (digit_t*)&Montgomery_Rprime, mb);
		Montgomery_multiply_mod_order(ma, mb, md);                                             // d = a*1 
		if (fp2compare64((uint64_t*)ma,(uint64_t*)md)!=0) { passed=0; break; }

		memset((unsigned char*)mb, 0, 32);
		Montgomery_multiply_mod_order(ma, mb, md);                                             // d = a*0 
		if (fp2compare64((uint64_t*)mb,(uint64_t*)md)!=0) { passed=0; break; }
	}
	if (passed==1) printf("  Montgomery multiplication and conversion tests .................................................. PASSED");
	else { printf("  Montgomery multiplication and conversion tests... FAILED"); printf("\n"); return false; }
	printf("\n");
	
	// Montgomery inversion modulo the order of the curve 
	passed = 1;
	for (n=0; n<TEST_LOOPS; n++)
	{
		random_order_test(ma); random_order_test(mb); random_order_test(mc);

		Montgomery_multiply_mod_order(one, (digit_t*)&Montgomery_Rprime, md);
		memmove((unsigned char*)mb, (unsigned char*)ma, 32);
		Montgomery_inversion_mod_order(ma, ma);
		Montgomery_multiply_mod_order(ma, mb, mc);                                             // c = a*a^-1 
		if (fp2compare64((uint64_t*)mc,(uint64_t*)md)!=0) { passed=0; break; }
	}
	if (passed==1) printf("  Montgomery inversion tests....................................................................... PASSED");
	else { printf("  Montgomery inversion tests... FAILED"); printf("\n"); return false; }
	printf("\n");
    
    return OK;
}


bool fp2_run()
{
    bool OK = true;
    int n, i;
    unsigned long long cycles, cycles1, cycles2;
    f2elm_t a, b, c;
	digit_t ma[NWORDS_ORDER], mb[NWORDS_ORDER], mc[NWORDS_ORDER];
        
    printf("\n--------------------------------------------------------------------------------------------------------\n\n"); 
    printf("Benchmarking quadratic extension field arithmetic over GF((2^127-1)^2): \n\n"); 

    // GF(p^2) addition using p = 2^127-1
    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        fp2random1271_test(a); fp2random1271_test(b);

        cycles1 = cpucycles();
        for (i = 0; i < 1000; i++) {
            fp2add1271(a, b, c);
        }
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }
    printf("  GF(p^2) addition runs in ............... %8lld ", cycles/(BENCH_LOOPS*1000)); print_unit;
    printf("\n"); 

    // GF(p^2) subtraction using p = 2^127-1
    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        fp2random1271_test(a); fp2random1271_test(b);  
         
        cycles1 = cpucycles();
        for (i = 0; i < 1000; i++) {
            fp2sub1271(a, b, c);
        }
        cycles2 = cpucycles();

        cycles = cycles+(cycles2-cycles1);
    }
    printf("  GF(p^2) subtraction runs in ............ %8lld ", cycles/(BENCH_LOOPS*1000)); print_unit;
    printf("\n"); 

    // GF(p^2) squaring using p = 2^127-1
    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        fp2random1271_test(a); 
        
        cycles1 = cpucycles();
        for (i = 0; i < 1000; i++) {
            fp2sqr1271(a, b);
        }
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }
    printf("  GF(p^2) squaring runs in ............... %8lld ", cycles/(BENCH_LOOPS*1000)); print_unit;
    printf("\n");

    // GF(p^2) multiplication using p = 2^127-1
    cycles = 0;
    for (n=0; n<BENCH_LOOPS; n++)
    {
        fp2random1271_test(a); fp2random1271_test(b); fp2random1271_test(c);

        cycles1 = cpucycles();
        for (i = 0; i < 1000; i++) {
            fp2mul1271(a, b, c);
        }
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }
    printf("  GF(p^2) multiplication runs in ......... %8lld ", cycles/(BENCH_LOOPS*1000)); print_unit;
    printf("\n");

    // GF(p^2) inversion using p = 2^127-1
    cycles = 0;
    for (n=0; n<SHORT_BENCH_LOOPS; n++)
    {
        fp2random1271_test(a); fp2random1271_test(b); fp2random1271_test(c);  

        cycles1 = cpucycles();
        for (i = 0; i < 100; i++) {
            fp2inv1271(a);
        }
        cycles2 = cpucycles();
        cycles = cycles+(cycles2-cycles1);
    }
    printf("  GF(p^2) inversion runs in .............. %8lld ", cycles/(SHORT_BENCH_LOOPS*100)); print_unit;
    printf("\n");

	// Addition modulo the curve order
	cycles = 0;
	for (n=0; n<BENCH_LOOPS; n++)
	{
		random_order_test(ma); random_order_test(mb); 

		cycles1 = cpucycles();
		for (i = 0; i < 1000; i++) {
			add_mod_order(ma, mb, mc);
		}
		cycles2 = cpucycles();
		cycles = cycles+(cycles2-cycles1);
	}
	printf("  Addition modulo the order runs in ...... %8lld ", cycles/(BENCH_LOOPS*1000)); print_unit;
	printf("\n");

	// Subtraction modulo the curve order
	cycles = 0;
	for (n = 0; n<BENCH_LOOPS; n++)
	{
		random_order_test(ma); random_order_test(mb); 

		cycles1 = cpucycles();
		for (i = 0; i < 1000; i++) {
			subtract_mod_order(ma, mb, mc);
		}
		cycles2 = cpucycles();
		cycles = cycles+(cycles2-cycles1);
	}
	printf("  Subtraction modulo the order runs in ... %8lld ", cycles/(BENCH_LOOPS*1000)); print_unit;
	printf("\n");

	// Montgomery multiplication modulo the curve order
	cycles = 0;
	for (n = 0; n<BENCH_LOOPS; n++)
	{
		random_order_test(ma); random_order_test(mb); 

		cycles1 = cpucycles();
		for (i = 0; i < 1000; i++) {
			Montgomery_multiply_mod_order(ma, mb, mc);
		}
		cycles2 = cpucycles();
		cycles = cycles+(cycles2-cycles1);
	}
	printf("  Montgomery multiply mod order runs in .. %8lld ", cycles/(BENCH_LOOPS*1000)); print_unit;
	printf("\n");

	// Montgomery multiplication modulo the curve order
	cycles = 0;
	for (n = 0; n<SHORT_BENCH_LOOPS; n++)
	{
		random_order_test(ma);

		cycles1 = cpucycles();
		for (i = 0; i < 10; i++) {
			Montgomery_inversion_mod_order(ma, mc);
		}
		cycles2 = cpucycles();
		cycles = cycles+(cycles2-cycles1);
	}
	printf("  Montgomery inversion mod order runs in . %8lld ", cycles/(SHORT_BENCH_LOOPS*10)); print_unit;
	printf("\n");
    
    return OK;
}


int main()
{
    bool OK = true;

    OK = OK && fp2_test();     // Test quadratic extension field operations using p = 2^127-1
    OK = OK && fp2_run();      // Benchmark quadratic extension field operations using p = 2^127-1
    
    return OK;
}
