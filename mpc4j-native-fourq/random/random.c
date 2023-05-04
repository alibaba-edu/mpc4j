/***********************************************************************************
* FourQlib: a high-performance crypto library based on the elliptic curve FourQ
*
*    Copyright (c) Microsoft Corporation. All rights reserved.
*
* Abstract: pseudo-random function
************************************************************************************/ 

#include "random.h"
#include <stdlib.h>
#include <stdbool.h>
#if defined(__WINDOWS__)
    #include <windows.h>
    #include <bcrypt.h>
    #define RTL_GENRANDOM "SystemFunction036"
    NTSTATUS last_bcrypt_error = 0;
#elif defined(__LINUX__)
    #include <unistd.h>
    #include <fcntl.h>
    static int lock = -1;
#endif


static __inline void delay(unsigned int count)
{
    while (count--) {}
}


int random_bytes(unsigned char* random_array, unsigned int nbytes)
{ // Generation of "nbytes" of random values

#if defined(__WINDOWS__)    
    if (BCRYPT_SUCCESS(last_bcrypt_error)) {
        NTSTATUS status = BCryptGenRandom(NULL, random_array, nbytes, BCRYPT_USE_SYSTEM_PREFERRED_RNG);

        if (BCRYPT_SUCCESS(status)) {
            return true;
        }
        last_bcrypt_error = status;
    }

    HMODULE hAdvApi = LoadLibraryA("ADVAPI32.DLL");
    if (!hAdvApi) {
        return false;
    }

    BOOLEAN(APIENTRY * RtlGenRandom)(void*, ULONG) = (BOOLEAN(APIENTRY*)(void*, ULONG))GetProcAddress(hAdvApi, RTL_GENRANDOM);

    BOOLEAN genrand_result = FALSE;
    if (RtlGenRandom) {
        genrand_result = RtlGenRandom(random_array, nbytes);
    }

    FreeLibrary(hAdvApi);

    if (!genrand_result) {
        return false;
    }

#elif defined(__LINUX__)
    int r, n = nbytes, count = 0;
    
    if (lock == -1) {
        do {
            lock = open("/dev/urandom", O_RDONLY);
            if (lock == -1) {
                delay(0xFFFFF);
            }
        } while (lock == -1);
    }

    while (n > 0) {
        do {
            r = read(lock, random_array+count, n);
            if (r == -1) {
                delay(0xFFFF);
            }
        } while (r == -1);
        count += r;
        n -= r;
    }
#endif

    return true;
}