#ifndef __RANDOM_H__
#define __RANDOM_H__


// For C++
#ifdef __cplusplus
extern "C" {
#endif


// Generate random bytes and output the result to random_array
int random_bytes(unsigned char* random_array, unsigned int nbytes);


#ifdef __cplusplus
}
#endif


#endif
