//***********************************************************************************
// FourQlib: a high-performance crypto library based on the elliptic curve FourQ
//
//   Copyright (c) Microsoft Corporation. All rights reserved.
//
// Abstract: arithmetic over GF(p^2) using x64 assembly for Linux with AVX2 support
//***********************************************************************************

#include "consts.s"

.intel_syntax noprefix 

// Registers that are used for parameter passing:
#define reg_p1  rdi
#define reg_p2  rsi
#define reg_p3  rdx
#define reg_p4  rcx


.text
//**************************************************************************
//  Quadratic extension field multiplication using lazy reduction
//  Based on schoolbook method
//  Operation: c [reg_p3] = a [reg_p1] * b [reg_p2] in GF(p^2), p = 2^127-1
//  NOTE: only a=c is allowed for fp2mul1271_a(a, b, c)
//************************************************************************** 
.global fp2mul1271_a
fp2mul1271_a:
  mov    rcx, reg_p3 

  // T0 = a0 * b0, (r11, r10, r9, r8) <- [reg_p1_0-8] * [reg_p2_0-8]
  mov    rdx, [reg_p2]	
  mulx   r9, r8, [reg_p1]
  mulx   rax, r10, [reg_p1+8] 
  push   r15
  push   r14
  add    r9, r10
  mov    rdx, [reg_p2+8]
  mulx   r11, r10, [reg_p1+8]
  push   r13
  adc    r10, rax
  push   r12
  mulx   rax, rdx, [reg_p1]
  adc    r11, 0
  add    r9, rdx

  // T1 = a1 * b1, (r15, r14, r13, r12) <- [reg_p1_16-24] * [reg_p2_16-24]
  mov    rdx, [reg_p2+16]
  mulx   r13, r12, [reg_p1+16]
  adc    r10, rax
  mulx   rax, r14, [reg_p1+24] 
  adc    r11, 0  
  mov    rdx, [reg_p2+24]
  add    r13, r14
  mulx   r15, r14, [reg_p1+24]
  adc    r14, rax
  adc    r15, 0
  mulx   rax, rdx, [reg_p1+16] 
  add    r13, rdx
  adc    r14, rax
  adc    r15, 0  
  
  // c0 = T0 - T1 = a0*b0 - a1*b1 
  xor    rax, rax
  sub    r8, r12
  sbb    r9, r13
  sbb    r10, r14
  sbb    r11, r15
  
  shld   r11, r10, 1      
  shld   r10, r9, 1
  mov    rdx, [reg_p2+16]
  btr    r9, 63

  // T0 = a0 * b1, (r15, r14, r13, r12) <- [reg_p1_0-8] * [reg_p2_16-24]
  mulx   r13, r12, [reg_p1]
  btr    r11, 63           // Add prime if borrow=1
  sbb    r10, 0
  sbb    r11, 0
  mulx   rax, r14, [reg_p1+8] 
  add    r13, r14
  mov    rdx, [reg_p2+24]
  mulx   r15, r14, [reg_p1+8]
  adc    r14, rax
  adc    r15, 0
  mulx   rax, rdx, [reg_p1] 
  add    r13, rdx
  adc    r14, rax
  adc    r15, 0  

  // Reducing and storing c0
  add    r10, r8
  adc    r11, r9
  btr    r11, 63
  adc    r10, 0
  adc    r11, 0

  // T1 = a1 * b0, (r12, r11, r10, r9) <- [reg_p1_16-24] * [reg_p2_0-8]	  
  mov    rdx, [reg_p2]
  mulx   r9, r8, [reg_p1+16]
  mov    [rcx], r10
  mulx   rax, r10, [reg_p1+24] 
  mov    [rcx+8], r11
  add    r9, r10
  mov    rdx, [reg_p2+8]
  mulx   r11, r10, [reg_p1+24]
  adc    r10, rax
  adc    r11, 0
  mulx   rax, rdx, [reg_p1+16]
  add    r9, rdx
  adc    r10, rax
  adc    r11, 0  

  // c1 = T0 + T1 = a0*b1 + a1*b0 
  add    r8, r12
  pop    r12
  adc    r9, r13
  pop    r13
  adc    r10, r14
  pop    r14
  adc    r11, r15

  // Reducing and storing c1
  shld   r11, r10, 1 
  shld   r10, r9, 1
  btr    r9, 63
  btr    r11, 63
  adc    r8, r10
  adc    r9, r11  
  btr    r9, 63
  pop    r15
  adc    r8, 0
  adc    r9, 0
  mov    [rcx+16], r8
  mov    [rcx+24], r9
  ret


//***********************************************************************
//  Quadratic extension field squaring
//  Operation: c [reg_p2] = a^2 [reg_p1] in GF(p^2), p = 2^127-1
//  NOTE: a=c is not allowed for fp2sqr1271_a(a, c)
//*********************************************************************** 
.global fp2sqr1271_a
fp2sqr1271_a:

  // t0 = (r9, r8) = a0 + a1, (rcx, r14) <- a1
  mov    r10,  [reg_p1]
  push   r14
  mov    r14, [reg_p1+16]
  sub    r10, r14
  mov    r11,  [reg_p1+8]
  mov    rcx, [reg_p1+24]
  sbb    r11, rcx

  push   r13
  btr    r11, 63
  push   r12
  sbb    r10, 0
  
  // t1 = (r11, r10) = a0 - a1
  mov    rdx, r10
  mov    r8, [reg_p1]
  add    r8, r14
  mov    r9, [reg_p1+8]
  adc    r9, rcx

  //  c0 = t0 * t1 = (a0 + a1)*(a0 - a1), (rcx, r14, r13, r12) <- (r9, r8) * (r11, r10)
  mulx   r13, r12, r8
  sbb    r11, 0
  mulx   rax, r14, r9 
  mov    rdx, r11
  add    r13, r14
  mulx   rcx, r14, r9
  mov    r9, [reg_p1+8]
  adc    r14, rax
  adc    rcx, 0
  mulx   rax, rdx, r8 
  mov    r8, [reg_p1]
  add    r13, rdx
  adc    r14, rax
  adc    rcx, 0  

  // t2 = (r9, r8) = 2*a0
  add    r8, r8
  adc    r9, r9
  
  // Reducing and storing c0
  shld   rcx, r14, 1   
  shld   r14, r13, 1
  btr    r13, 63 
  btr    rcx, 63             
  adc    r12, r14
  adc    r13, rcx
  btr    r13, 63
  adc    r12, 0
  adc    r13, 0
  mov    [reg_p2], r12
  mov    [reg_p2+8], r13

  //  c1 = 2a0 * a1, (rcx, r14, r11, r10) <- (r9, r8) * [reg_p1_16-24] 
  mov    rdx, [reg_p1+16]
  mulx   r11, r10, r8
  pop    r12
  mulx   rax, r14, r9 
  pop    r13
  add    r11, r14
  mov    rdx, [reg_p1+24]
  mulx   rcx, r14, r9
  adc    r14, rax
  adc    rcx, 0
  mulx   rax, rdx, r8 
  add    r11, rdx
  adc    r14, rax
  adc    rcx, 0  
  
  // Reducing and storing c1
  shld   rcx, r14, 1  
  shld   r14, r11, 1
  btr    r11, 63
  btr    rcx, 63
  adc    r10, r14
  adc    r11, rcx
  btr    r11, 63
  pop    r14
  adc    r10, 0
  adc    r11, 0
  mov    [reg_p2+16], r10
  mov    [reg_p2+24], r11
  ret


//***************************************************************************
//  Quadratic extension field addition/subtraction
//  Operation: c [reg_p3] = 2*a [reg_p1] - b [reg_p2] in GF(p^2), p = 2^127-1
//*************************************************************************** 
.global fp2addsub1271_a
fp2addsub1271_a:
  mov    r8, [reg_p1]
  mov    r9, [reg_p1+8]
  add    r8, r8
  adc    r9, r9  
  btr    r9, 63
  adc    r8, 0
  adc    r9, 0
  
  mov    r10, [reg_p2]
  sub    r8, r10
  mov    r10, [reg_p2+8]
  sbb    r9, r10  
  btr    r9, 63
  sbb    r8, 0
  mov    [reg_p3], r8
  sbb    r9, 0
  mov    [reg_p3+8], r9

  mov    r8, [reg_p1+16]
  mov    r9, [reg_p1+24]
  add    r8, r8
  adc    r9, r9  
  btr    r9, 63
  adc    r8, 0
  adc    r9, 0
  
  mov    r10, [reg_p2+16]
  sub    r8, r10
  mov    r10, [reg_p2+24]
  sbb    r9, r10  
  btr    r9, 63
  sbb    r8, 0
  mov    [reg_p3+16], r8
  sbb    r9, 0
  mov    [reg_p3+24], r9
  ret


//***********************************************************************************************
//  Constant-time table lookup to extract a point
// Inputs: sign_mask, digit, table containing 8 points
// Output: P = sign*table[digit], where sign=1 if sign_mask=0xFF...FF and sign=-1 if sign_mask=0
//*********************************************************************************************** 
.global table_lookup_1x8_a
table_lookup_1x8_a: 
  vpbroadcastd ymm4, DWORD PTR [reg_p3]
  vpbroadcastd ymm14, DWORD PTR [reg_p4]
  vmovdqu      ymm5, [ONEx8+rip]      
  vmovdqu      ymm11, [TWOx8+rip]      
  vmovdqu      ymm0, YMMWORD PTR [reg_p1] 
  vmovdqu      ymm1, YMMWORD PTR [reg_p1+32] 
  vmovdqu      ymm2, YMMWORD PTR [reg_p1+64] 
  vmovdqu      ymm3, YMMWORD PTR [reg_p1+96] 
  vmovdqu      ymm10, ymm4
  
// While digit>=0 mask = 0x00...0 else mask = 0xFF...F
// If mask = 0xFF...F then point = point, else if mask = 0x00...0 then point = temp_point
  vpsubd       ymm4, ymm4, ymm5
  vpsubd       ymm10, ymm10, ymm11
  vmovdqu      ymm6, YMMWORD PTR [reg_p1+128] 
  vmovdqu      ymm7, YMMWORD PTR [reg_p1+160] 
  vmovdqu      ymm8, YMMWORD PTR [reg_p1+192] 
  vmovdqu      ymm9, YMMWORD PTR [reg_p1+224] 
  vpsrad       ymm15, ymm4, 31  
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9
  vpand        ymm0, ymm0, ymm15 
  vpand        ymm1, ymm1, ymm15 
  vpand        ymm2, ymm2, ymm15 
  vpand        ymm3, ymm3, ymm15  
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9    
  
  vmovdqu      ymm6, YMMWORD PTR [reg_p1+256] 
  vmovdqu      ymm7, YMMWORD PTR [reg_p1+288] 
  vmovdqu      ymm8, YMMWORD PTR [reg_p1+320] 
  vmovdqu      ymm9, YMMWORD PTR [reg_p1+352] 
  vpsrad       ymm15, ymm10, 31   
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9
  vpand        ymm0, ymm0, ymm15 
  vpand        ymm1, ymm1, ymm15 
  vpand        ymm2, ymm2, ymm15 
  vpand        ymm3, ymm3, ymm15  
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9    
   
  vpsubd       ymm4, ymm10, ymm5
  vpsubd       ymm10, ymm10, ymm11
  vmovdqu      ymm6, YMMWORD PTR [reg_p1+384] 
  vmovdqu      ymm7, YMMWORD PTR [reg_p1+416] 
  vmovdqu      ymm8, YMMWORD PTR [reg_p1+448] 
  vmovdqu      ymm9, YMMWORD PTR [reg_p1+480] 
  vpsrad       ymm15, ymm4, 31 
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9
  vpand        ymm0, ymm0, ymm15 
  vpand        ymm1, ymm1, ymm15 
  vpand        ymm2, ymm2, ymm15 
  vpand        ymm3, ymm3, ymm15  
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9    
  
  vmovdqu      ymm6, YMMWORD PTR [reg_p1+512] 
  vmovdqu      ymm7, YMMWORD PTR [reg_p1+544] 
  vmovdqu      ymm8, YMMWORD PTR [reg_p1+576] 
  vmovdqu      ymm9, YMMWORD PTR [reg_p1+608]
  vpsrad       ymm15, ymm10, 31  
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9
  vpand        ymm0, ymm0, ymm15 
  vpand        ymm1, ymm1, ymm15 
  vpand        ymm2, ymm2, ymm15 
  vpand        ymm3, ymm3, ymm15  
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9    
  
  vpsubd       ymm4, ymm10, ymm5
  vpsubd       ymm10, ymm10, ymm11
  vmovdqu      ymm6, YMMWORD PTR [reg_p1+640] 
  vmovdqu      ymm7, YMMWORD PTR [reg_p1+672] 
  vmovdqu      ymm8, YMMWORD PTR [reg_p1+704] 
  vmovdqu      ymm9, YMMWORD PTR [reg_p1+736]
  vpsrad       ymm15, ymm4, 31  
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9
  vpand        ymm0, ymm0, ymm15 
  vpand        ymm1, ymm1, ymm15 
  vpand        ymm2, ymm2, ymm15 
  vpand        ymm3, ymm3, ymm15  
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9    
  
  vmovdqu      ymm6, YMMWORD PTR [reg_p1+768] 
  vmovdqu      ymm7, YMMWORD PTR [reg_p1+800] 
  vmovdqu      ymm8, YMMWORD PTR [reg_p1+832] 
  vmovdqu      ymm9, YMMWORD PTR [reg_p1+864]
  vpsrad       ymm15, ymm10, 31  
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9
  vpand        ymm0, ymm0, ymm15 
  vpand        ymm1, ymm1, ymm15 
  vpand        ymm2, ymm2, ymm15 
  vpand        ymm3, ymm3, ymm15  
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9    
  
  vpsubd       ymm4, ymm10, ymm5
  vmovdqu      ymm6, YMMWORD PTR [reg_p1+896] 
  vmovdqu      ymm7, YMMWORD PTR [reg_p1+928] 
  vmovdqu      ymm8, YMMWORD PTR [reg_p1+960] 
  vmovdqu      ymm9, YMMWORD PTR [reg_p1+992] 
  vpsrad       ymm15, ymm4, 31  
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9
  vpand        ymm0, ymm0, ymm15 
  vpand        ymm1, ymm1, ymm15 
  vpand        ymm2, ymm2, ymm15 
  vpand        ymm3, ymm3, ymm15  
  vpxor        ymm0, ymm0, ymm6  
  vpxor        ymm1, ymm1, ymm7  
  vpxor        ymm2, ymm2, ymm8  
  vpxor        ymm3, ymm3, ymm9 
   
// point: x+y,y-x,2dt, temp_point: y-x,x+y,-2dt coordinate 
// If sign_mask = 0 then choose negative of the point
  vmovdqu      ymm5, [PRIME1271+rip] 
  vmovdqu      ymm6, ymm0
  vpsubq       ymm7, ymm5, ymm3                // Negate 2dt coordinate
  vpxor        ymm10, ymm0, ymm1
  vpand        ymm10, ymm10, ymm14 
  vpxor        ymm0, ymm1, ymm10  
  vpxor        ymm10, ymm6, ymm1
  vpand        ymm10, ymm10, ymm14 
  vpxor        ymm1, ymm6, ymm10 
  vpblendvb    ymm3, ymm7, ymm3, ymm14    
    
  vmovdqu      YMMWORD PTR [reg_p2], ymm0
  vmovdqu      YMMWORD PTR [reg_p2+32], ymm1
  vmovdqu      YMMWORD PTR [reg_p2+64], ymm2
  vmovdqu      YMMWORD PTR [reg_p2+96], ymm3
  ret
