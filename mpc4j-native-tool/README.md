# `mpc4j-native-tool`

## Introduction

`mpc4j` leverages native C/C++ codes to speed up cryptographic operations. The native codes and Java codes are interacted by the [Java Native Interface (JNI)](https://docs.oracle.com/javase/8/docs/technotes/guides/jni/) technique. We separate native C/C++ codes into two modules, namely `mpc4j-native-cool` and `mpc4j-native-fhe`. `mpc4j-native-tool` contains native codes for basic cryptographic operations, while `mpc4j-native-fhe` contains native codes for Fully Homomorphic Encryption (FHE) using [SEAL](https://github.com/microsoft/SEAL). All basic cryptographic operations in `mpc4j-native-tool` have alternative pure-Java implementations in `mpc4j` with the same functionalities and the same data representations.

`mpc4j-native-tool` relies on the following C/C++ libraries:

- [GMP](https://gmplib.org/): An efficient library for operations with arbitrary precision integers, rationals, and floating-point numbers.
- [NTL](https://libntl.org/): A high-performance, portable C++ library providing data structures and algorithms for manipulating signed, arbitrary length integers and for vectors, matrices, and polynomials over the integers and finite fields, developed by [Victor Shoup](https://shoup.net/).
- [MCL](https://github.com/herumi/mcl): A portable and fast pairing-based cryptography library. MCL also includes fast Elliptic Curve implementations. 
- [libsodium](https://doc.libsodium.org/): A modern, easy-to-use software library for encryption, decryption, signatures, password hashing, and more. libsodium includes efficient X25519 and Ed25519 implementations. 

## Compilation

Compiling `mpc4j-native-tool` might be a bit complicated for those who are not that familiar with Unix-like systems. Here we provide a brief guideline on how to compile `mpc4j-native-tool` for those familiar with Unix-like systems. If you meet any problems when trying to follow this brief introduction, or if you are a new user to Unix-like systems, see separate documentation in `/doc` on how to install dependencies (with default paths) and compile `mpc4j-native-tool` on different platforms.

- `doc/compile_mac_x86_64.md`: MacOS (`x86_64`).
- `doc/compile_mac_aarch64.md`: MacOS (`aarch64`), like MacBook M1.
- `doc/compile_ubuntu.md`: Ubuntu 20.04 (including Ubuntu and Docker on Ubuntu / MacBook `x86_64` / MacBook `aarch64`)
- `doc/compile_centos_x86_64.md`: CentOS 8 (including CentOS and Docker on CentOS / MacBook `x86_64`, failed on MacBook `aarch64`).

We do not provide a "one-time" script to automatically install all dependencies and compile `mpc4j-native-tool`. Instead, we want users to know what we need to do for the installation so that we write a step-by-step guideline.

**NOTE:** Feel free to contact us if you meet unexpected problems or dependencies do not work with newer versions.

### Dependencies

- [GMP](https://gmplib.org/): Tested on 6.2.0 / 6.2.1, depending on whether you meet problems like `GMP version check (6.2.0/6.2.1)` when installing NTL.
- [NTL](https://libntl.org): Tested on 11.5.1. You can further introduce [GF2X](https://gitlab.inria.fr/gf2x/gf2x) for more efficient operations in a Galois Field. Since the installation procedure for [GF2X](https://gitlab.inria.fr/gf2x/gf2x) is rather complicated, we use NTL by default.
- [MCL](https://github.com/herumi/mcl): Tested on 1.6.1. Feel free to contact us if you meet problems when using a higher version.
- [libsodium](https://doc.libsodium.org/): Tested on 1.0.18. Feel free to contact us if you meet problems when using a higher version.
- JDK: You must install Java Development Tool (JDK) instead of Java Runtime Environment (JRE). We recommend installing JDK from [oracle.com](https://www.oracle.com/java/technologies/downloads/). Then, you need to config the environment variable `JAVA_HOME`. When executing `echo $JAVA_HOME`, the installing path should be displayed.

### Compile `mpc4j-native-tool`

Go to the path of `mpc4j-native-tool`, and run the following command to compile `mpc4j-native-tool`.

```shell
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```

If you install GMP, NTL, libsodium, and MCL on other paths, you can execute `cmake` by specifying the dependency paths like `LIBNAME_ROOT_DIR`. For example, if you install GMP on `YOUR_GMP_PATH`, run `cmake` like the following:

```shell
cmake .. -DGMP_ROOT_DIR=YOUR_GMP_PATH
```

## Helpful Commands

If `cmake` reports some libraries are not found, you can run the following command to see which library(s) has a problem.

```shell
cmake .. -LA . # there is a dot
```
