//***********************************************************************************
// FourQlib: a high-performance crypto library based on the elliptic curve FourQ
//
//   Copyright (c) Microsoft Corporation. All rights reserved.
//
// Abstract: arithmetic over GF(p^2) using x64 assembly for Linux
//***********************************************************************************

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
  push   r15
#if defined(PUSH_SET) 
  push   r12
  push   r14
  push   r13
#endif
  mov    rcx, reg_p3 

  // T0 = a0 * b0, (r11, r10, r9, r8) <- [reg_p1_0-8] * [reg_p2_0-8]
  mov    rax, [reg_p1]
  mov    r11, [reg_p2] 
  mul    r11
#if !defined(PUSH_SET) 
  push   r12
#endif
  xor    r10, r10
  mov    r8, rax
  mov    r9, rdx

  mov    r12, [reg_p2+8]
  mov    rax, [reg_p1]
  mul    r12
  add    r9, rax
#if !defined(PUSH_SET) 
  push   r14
#endif
  adc    r10, rdx

  mov    rax, [reg_p1+8] 
  mul    r11
  add    r9, rax
#if !defined(PUSH_SET) 
  push   r13
#endif
  adc    r10, rdx

  mov    rax, [reg_p1+8] 
  mul    r12
  add    r10, rax
  mov    r11, 0
  adc    r11, rdx 
  
  // T1 = a1 * b1, (r15, r14, r13, r12) <- [reg_p1_16-24] * [reg_p2_16-24]
  xor    r14, r14
  mov    rax, [reg_p1+16]
  mov    r15, [reg_p2+16] 
  mul    r15
  mov    r12, rax
  mov    rax, [reg_p2+24]
  mov    r13, rdx

  mov    rdx, [reg_p1+16]
  mul    rdx
  add    r13, rax
  mov    rax, [reg_p1+24] 
  adc    r14, rdx

  mul    r15
  add    r13, rax
  adc    r14, rdx

  mov    r15, [reg_p2+24]
  mov    rax, [reg_p1+24] 
  mul    r15
  mov    r15, 0
  add    r14, rax
  adc    r15, rdx 

  // c0 = T0 - T1 = a0*b0 - a1*b1 
  xor    rax, rax
  sub    r8, r12
  sbb    r9, r13
  sbb    r10, r14
  sbb    r11, r15
  adc    rax, 0
  
  shld   r11, r10, 1      
  shld   r10, r9, 1
  mov    r15, [reg_p2+16]
  mov    rax, [reg_p1]
  btr    r9, 63

  // T0 = a0 * b1, (r15, r14, r13, r12) <- [reg_p1_0-8] * [reg_p2_16-24]
  mul    r15
  btr    r11, 63           // Add prime if borrow=1
  sbb    r10, 0
  sbb    r11, 0
  xor    r14, r14
  mov    r12, rax
  mov    rax, [reg_p2+24]
  mov    r13, rdx

  mov    rdx, [reg_p1]
  mul    rdx
  add    r13, rax
  mov    rax, [reg_p1+8] 
  adc    r14, rdx

  mul    r15
  xor    r15, r15
  add    r13, rax
  mov    rax, [reg_p1+8] 
  adc    r14, rdx

  mul qword ptr [reg_p2+24]
  add    r8, r10
  adc    r9, r11
  add    r14, rax
  adc    r15, rdx 
  
  // Reducing and storing c0
  btr    r9, 63
  adc    r8, 0
  mov    r11, [reg_p2] 
  adc    r9, 0
  
  // T1 = a1 * b0, (r12, r11, r10, r9) <- [reg_p1_16-24] * [reg_p2_0-8]	  
  mov    rax, [reg_p1+16]
  mul    r11
  mov    [rcx], r8
  mov    [rcx+8], r9
  mov    r8, rax
  mov    r9, rdx

  mov    rax, [reg_p1+16]
  mov    rsi, [reg_p2+8]
  mul    rsi
  xor    r10, r10
  add    r9, rax
  adc    r10, rdx

  mov    rax, [reg_p1+24] 
  mul    r11
  add    r9, rax
  adc    r10, rdx

  xor    r11, r11
  mov    rax, [reg_p1+24] 
  mul    rsi
  add    r10, rax
  adc    r11, rdx 

  // c1 = T0 + T1 = a0*b1 + a1*b0 
  add    r8, r12
  adc    r9, r13
  pop    r13
  adc    r10, r14
  pop    r14
  pop    r12
  adc    r11, r15
  pop    r15

  // Reducing and storing c1
  shld   r11, r10, 1 
  shld   r10, r9, 1        
  btr    r9, 63
  btr    r11, 63
  adc    r8, r10
  adc    r9, r11
  btr    r9, 63
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
  push   r14
  
  // t0 = (r9, r8) = a0 + a1, (rcx, r14) <- a1
  mov    r8,  [reg_p1]
  mov    r14, [reg_p1+16]
  add    r8, r14
  mov    r9,  [reg_p1+8]
  mov    rcx, [reg_p1+24]
  adc    r9, rcx
           
  btr    r9, 63
  push   r12
  adc    r8, 0
  adc    r9, 0
  
  // t1 = (r11, r10) = a0 - a1
  mov    r10, [reg_p1]
  sub    r10, r14
  mov    r11, [reg_p1+8]
  sbb    r11, rcx

  btr    r11, 63
  sbb    r10, 0
  push   r13
  sbb    r11, 0
  
  //  c0 = t0 * t1 = (a0 + a1)*(a0 - a1), (rcx, r14, r13, r12) <- (r9, r8) * (r11, r10)
  xor    r14, r14
  mov    rax, r8
  mul    r10
  mov    r12, rax
  mov    rax, r11
  mov    r13, rdx

  mul    r8
  xor    rcx, rcx
  add    r13, rax
  adc    r14, rdx

  mov    rax, r9 
  mul    r10
  mov    r8, [reg_p1]
  add    r13, rax
  adc    r14, rdx

  mov    rax, r9 
  mul    r11
  mov    r9, [reg_p1+8]
  add    r14, rax
  adc    rcx, rdx

  // t2 = (r9, r8) = 2*a0
  add    r8, r8
  adc    r9, r9
  
  btr    r9, 63
  adc    r8, 0
  adc    r9, 0
  
  // Reducing and storing c0
  shld   rcx, r14, 1      
  shld   r14, r13, 1 
  btr    r13, 63
  add    r12, r14
  adc    r13, rcx
  btr    r13, 63
  adc    r12, 0
  adc    r13, 0
  mov    [reg_p2], r12
  mov    [reg_p2+8], r13
  
  //  c1 = 2a0 * a1, (rcx, r14, r11, r10) <- (r9, r8) * [reg_p1_16-24] 
  mov    rcx, [reg_p1+16] 
  mov    rax, r8
  mul    rcx
  mov    r10, rax
  mov    r11, rdx

  mov    rax, [reg_p1+24]
  xor    r14, r14
  mul    r8
  add    r11, rax
  adc    r14, rdx

  mov    rax, rcx
  mul    r9
  add    r11, rax 
  adc    r14, rdx

  mov    rax, [reg_p1+24]
  mul    r9
  xor    rcx, rcx
  add    r14, rax
  pop    r13
  adc    rcx, rdx

  // Reducing and storing c1
  shld   rcx, r14, 1      
  shld   r14, r11, 1 
  btr    r11, 63
  add    r10, r14
  pop    r12
  adc    r11, rcx
  btr    r11, 63
  adc    r10, 0
  pop    r14
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
