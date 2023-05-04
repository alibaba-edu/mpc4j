#ifndef __SHA512_H__
#define __SHA512_H__


// For C++
#ifdef __cplusplus
extern "C" {
#endif


// Hashing using SHA-512. Output is 64 bytes long
int crypto_sha512(const unsigned char *in, unsigned long long inlen, unsigned char *out);


#ifdef __cplusplus
}
#endif


#endif
