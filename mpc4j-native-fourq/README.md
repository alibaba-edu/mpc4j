<!--
 * @Description: 
 * @Author: Qixian Zhou
 * @Date: 2023-03-30 22:20:29
-->

# Introduction

FourQ is a high-security, high-performance elliptic curve that targets the 128-bit security level. [FourQlib](https://github.com/microsoft/FourQlib) implements FourQ elliptic curve and cryptographic functions. 

However, FourQlib officially only provides support on Linux and Windows, and does not support MacOS. In order to be able to use FourQlib in MacOS, we conducted some explorations.

# Modification

We copied the source code of [FourQ_64bit_and_portable](https://github.com/microsoft/FourQlib/tree/master/FourQ_64bit_and_portable) in FourQlib. In order to be able to compile successfully, we have modified some content and provide a new `CMakeLists.txt`, which is not officially provided by FourQlib. The main modifications are as follows.

1. Change `#include <malloc.h>` in `schnorrq.c` to `#include<stdlib.h>`. This is to solve compilation errors on MacOS.
2. We modified the `enum ECCRYPTO_STATUS` in `FourQ.h`. We swapped the order of `ECCRYPTO_ERROR` and `ECCRYPTO_SUCCESS` so that `ECCRYPTO_SUCCESS` corresponds to `0x00`, and `ECCRYPTO_ERROR` corresponds to `0x01`. This allows us to build unit tests in `cmake` for `FourQlib` since in `cmake` all tests are passed only when returning `0x00`.

# Compile FourQlib

We have verified that FourQlib can be successfully compiled and installed on Linux, MacOS (Amd64), and MacOS (Arm64) according to the following steps.

```
cd mpc4j-native-fourq
mkdir build
cd build
cmake .. 
make 
make test
sudo make install
```