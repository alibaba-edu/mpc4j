/*
 * Modified by Weiran Liu. Remove `main` so that we can write main in other source code
 * to do some simple tests for mpc4j-native-tool.
 *
 * BLAKE2 reference source code package - reference C implementations
 *
 * Copyright 2012, Samuel Neves <sneves@dei.uc.pt>.  You may use this under the
 * terms of the CC0, the OpenSSL Licence, or the Apache Public License 2.0, at
 * your option.  The terms of these licenses can be found at:
 *
 * - CC0 1.0 Universal : http://creativecommons.org/publicdomain/zero/1.0
 * - OpenSSL license   : https://www.openssl.org/source/license.html
 * - Apache 2.0        : http://www.apache.org/licenses/LICENSE-2.0
 *
 * More information about the BLAKE2 hash function can be found at
 * https://blake2.net.
 */
#ifdef __aarch64__
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "blake2.h"

#define STR_(x) #x
#define STR(x) STR_(x)

#define LENGTH 256

#define MAKE_KAT(name, size_prefix)                                                                \
  do {                                                                                             \
    printf("static const uint8_t " #name "_kat[BLAKE2_KAT_LENGTH][" #size_prefix                   \
           "_OUTBYTES] = \n{\n");                                                                  \
                                                                                                   \
    for (i = 0; i < LENGTH; ++i) {                                                                 \
      name(hash, size_prefix##_OUTBYTES, in, i, NULL, 0);                                          \
      printf("\t{\n\t\t");                                                                         \
                                                                                                   \
      for (j = 0; j < size_prefix##_OUTBYTES; ++j)                                                 \
        printf("0x%02X%s", hash[j],                                                                \
               (j + 1) == size_prefix##_OUTBYTES ? "\n" : j && !((j + 1) % 8) ? ",\n\t\t" : ", "); \
                                                                                                   \
      printf("\t},\n");                                                                            \
    }                                                                                              \
                                                                                                   \
    printf("};\n\n\n\n\n");                                                                        \
  } while (0)

#define MAKE_KEYED_KAT(name, size_prefix)                                                          \
  do {                                                                                             \
    printf("static const uint8_t " #name "_keyed_kat[BLAKE2_KAT_LENGTH][" #size_prefix             \
           "_OUTBYTES] = \n{\n");                                                                  \
                                                                                                   \
    for (i = 0; i < LENGTH; ++i) {                                                                 \
      name(hash, size_prefix##_OUTBYTES, in, i, key, size_prefix##_KEYBYTES);                      \
      printf("\t{\n\t\t");                                                                         \
                                                                                                   \
      for (j = 0; j < size_prefix##_OUTBYTES; ++j)                                                 \
        printf("0x%02X%s", hash[j],                                                                \
               (j + 1) == size_prefix##_OUTBYTES ? "\n" : j && !((j + 1) % 8) ? ",\n\t\t" : ", "); \
                                                                                                   \
      printf("\t},\n");                                                                            \
    }                                                                                              \
                                                                                                   \
    printf("};\n\n\n\n\n");                                                                        \
  } while (0)

#define MAKE_XOF_KAT(name)                                                                         \
  do {                                                                                             \
    printf("static const uint8_t " #name "_kat[BLAKE2_KAT_LENGTH][BLAKE2_KAT_LENGTH] = \n{\n");    \
                                                                                                   \
    for (i = 1; i <= LENGTH; ++i) {                                                                \
      name(hash, i, in, LENGTH, NULL, 0);                                                          \
      printf("\t{\n\t\t");                                                                         \
                                                                                                   \
      for (j = 0; j < i; ++j)                                                                      \
        printf("0x%02X%s", hash[j],                                                                \
               (j + 1) == LENGTH ? "\n" : j && !((j + 1) % 8) ? ",\n\t\t" : ", ");                 \
                                                                                                   \
      for (j = i; j < LENGTH; ++j)                                                                 \
        printf("0x00%s", (j + 1) == LENGTH ? "\n" : j && !((j + 1) % 8) ? ",\n\t\t" : ", ");       \
                                                                                                   \
      printf("\t},\n");                                                                            \
    }                                                                                              \
                                                                                                   \
    printf("};\n\n\n\n\n");                                                                        \
  } while (0)

#define MAKE_XOF_KEYED_KAT(name, size_prefix)                                                      \
  do {                                                                                             \
    printf("static const uint8_t " #name                                                           \
           "_keyed_kat[BLAKE2_KAT_LENGTH][BLAKE2_KAT_LENGTH] = \n{\n");                            \
                                                                                                   \
    for (i = 1; i <= LENGTH; ++i) {                                                                \
      name(hash, i, in, LENGTH, key, size_prefix##_KEYBYTES);                                      \
      printf("\t{\n\t\t");                                                                         \
                                                                                                   \
      for (j = 0; j < i; ++j)                                                                      \
        printf("0x%02X%s", hash[j],                                                                \
               (j + 1) == LENGTH ? "\n" : j && !((j + 1) % 8) ? ",\n\t\t" : ", ");                 \
                                                                                                   \
      for (j = i; j < LENGTH; ++j)                                                                 \
        printf("0x00%s", (j + 1) == LENGTH ? "\n" : j && !((j + 1) % 8) ? ",\n\t\t" : ", ");       \
                                                                                                   \
      printf("\t},\n");                                                                            \
    }                                                                                              \
                                                                                                   \
    printf("};\n\n\n\n\n");                                                                        \
  } while (0)
#endif