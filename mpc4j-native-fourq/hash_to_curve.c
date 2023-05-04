/**********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: hash to FourQ
***********************************************************************************/ 

#include "FourQ_internal.h"
#include "FourQ_params.h"


static digit_t fpeq1271(digit_t* a, digit_t* b)
{ // Constant-time comparison of two field elements, ai=bi? : (0) equal, (-1) unequal
    digit_t c = 0;

    for (unsigned int i = 0; i < NWORDS_FIELD; i++)
        c |= a[i] ^ b[i];
    
    return (digit_t)((-(sdigit_t)(c >> 1) | -(sdigit_t)(c & 1)) >> (8*sizeof(digit_t) - 1)); 
}   


static void fpselect(digit_t* a, digit_t* b, digit_t* c, digit_t selector)
{ // Constant-time selection of field elements
  // If selector = 0 do c <- a, else if selector =-1 do c <- b

    for (unsigned int i = 0; i < NWORDS_FIELD; i++)
        c[i] = (selector & (a[i] ^ b[i])) ^ a[i]; 
}


ECCRYPTO_STATUS HashToCurve(f2elm_t r, point_t out)
{
    digit_t *r0 = (digit_t*)r[0], *r1 = (digit_t*)r[1];
    felm_t t0, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16;
    felm_t one = {0};
    one[0] = 1;

    digit_t* x0 = (digit_t*)out->x[0];
    digit_t* x1 = (digit_t*)out->x[1];
    digit_t* y0 = (digit_t*)out->y[0];
    digit_t* y1 = (digit_t*)out->y[1];
    digit_t selector;

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
    
    selector = fpeq1271(t7, one);
    fpselect(t8, t5, t3, selector);
    fpselect(t9, t11, t10, selector);

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
    
    selector = fpeq1271(t5, t7);
    fpselect(t15, t16, t7, selector);
    fpselect(t16, x0, t11, selector);

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
    fpmul1271(x0, t7, t5);    
    fpmul1271(x1, t11, t15);    
    fpsub1271(t5, t15, t15);    
    fpmul1271(t7, x1, t5);    
    fpmul1271(t11, x0, t16);    
    fpadd1271(t16, t5, t16);     
    fpmul1271(t13, t9, t13);    
    fpmul1271(t15, t13, x0);   
    fpmul1271(t16, t13, x1);

    // Clear cofactor
    point_extproj_t P;
    point_setup(out, P);
    cofactor_clearing(P);
    eccnorm(P, out);

    return ECCRYPTO_SUCCESS;
}
