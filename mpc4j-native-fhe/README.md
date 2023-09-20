# `mpc4j-native-fhe`

## Introduction

`mpc4j` leverages native C/C++ codes to speed up cryptographic operations. The native codes and Java codes are interacted by the [Java Native Interface (JNI)](https://docs.oracle.com/javase/8/docs/technotes/guides/jni/) technique. We separate native C/C++ codes into two modules, namely `mpc4j-native-cool` and `mpc4j-native-fhe`. `mpc4j-native-tool` contains native codes for basic cryptographic operations, while `mpc4j-native-fhe` contains native codes for Fully Homomorphic Encryption (FHE) using [SEAL](https://github.com/microsoft/SEAL).

This is the `mpc4j-native-fhe` module that only relies on [SEAL 4.0.0](https://github.com/microsoft/SEAL/releases/tag/v4.0.0).

## Install SEAL

As shown in [readme.md](https://github.com/microsoft/SEAL/blob/main/README.md), there are multiple ways of installing Microsoft SEAL and starting to use it. The easiest way is to use a package manager to download, build, and install the library. For example, on macOS, you can use [Homebrew](https://formulae.brew.sh/formula/seal). SEAL documentation recommends compiling SEAL locally with Clang++, obtaining much better runtime performance.

> Note: Microsoft SEAL compiled with Clang++ has much better runtime performance than one compiled with GNU G++.

Here we introduce how to compile SEAL from source code with Clang++ and install it. Please prepare the source code by the following command:

```shell
git clone -b v4.0.0 https://github.com/microsoft/SEAL.git
```

### macOS (Both x86_64 and aarch64)

Run the following command to compile and install SEAL v4.0.0.

```shell
cd SEAL
mkdir build
cd build
cmake .. -DCMAKE_CXX_COMPILER=/usr/bin/clang++ -DCMAKE_C_COMPILER=/usr/bin/clang -DBUILD_SHARED_LIBS=ON -DCMAKE_BUILD_TYPE=Release -DCMAKE_CXX_FLAGS_RELEASE="-DNDEBUG -flto -O3" -DCMAKE_C_FLAGS_RELEASE="-DNDEBUG -flto -O3" -DSEAL_BUILD_BENCH=OFF -DSEAL_BUILD_EXAMPLES=OFF -DSEAL_BUILD_TESTS=OFF -DSEAL_USE_CXX17=ON -DSEAL_USE_INTRIN=ON -DSEAL_USE_MSGSL=OFF -DSEAL_USE_ZLIB=OFF -DSEAL_USE_ZSTD=OFF -DSEAL_THROW_ON_TRANSPARENT_CIPHERTEXT=ON
make
sudo make install
cd .. # return to the SEAL path
cd .. # return to the original path
```

### Ubuntu

As far as we know, you could use `apt-get` to install SEAL on Ubuntu previously. However, we cannot do that now. Here we demonstrate how to manually install SEAL on the official Ubuntu [Docker](https://www.docker.com/) image. See readme.md in `mpc4j-native-tool` on how to pull and run Ubuntu Docker image. We also successfully install SEAL on Ubuntu without Docker following the same way. Please feel free to contact us if you meet problems when installing.

Run the following command to install `git`, `clang`, and `cmake`, and download the source code of SEAL v4.0.0.

```shell
apt install git
apt install clang
apt install cmake
git clone -b v4.0.0 https://github.com/microsoft/SEAL.git
```

Then, run the following command to compile and install SEAL v4.0.0.

```shell
cd SEAL
cmake -S . -B build -DCMAKE_CXX_COMPILER=/usr/bin/clang++ -DCMAKE_C_COMPILER=/usr/bin/clang -DCMAKE_INSTALL_PREFIX=/usr/local -DBUILD_SHARED_LIBS=ON -DCMAKE_C_FLAGS_RELEASE="-O3" -DCMAKE_CXX_FLAGS_RELEASE="-O3 -march=native"
cmake --build build
cmake --install build
cd .. # return to the original path
```

Note that if you are using Docker under `aarch64` instead of `x86_64` (like MacBook M1), you may meet an error when building SEAL:

```text
clang: error: the clang compiler does not support '-march=native'
```

Simply remove `-march=native` and rerun the above commands:

```shell
cmake -S . -B build -DCMAKE_CXX_COMPILER=/usr/bin/clang++ -DCMAKE_C_COMPILER=/usr/bin/clang -DCMAKE_INSTALL_PREFIX=/usr/local -DBUILD_SHARED_LIBS=ON -DCMAKE_C_FLAGS_RELEASE="-O3" -DCMAKE_CXX_FLAGS_RELEASE="-O3"
cmake --build build
cmake --install build
```

### CentOS

We cannot directly install clang by `yum` for CentOS 7. Instead, run the following command to install clang manually. (See [How to install Clang and LLVM 3.9 on CentOS 7](https://stackoverflow.com/questions/44219158/how-to-install-clang-and-llvm-3-9-on-centos-7/48103599#48103599) for more details.)

```shell
sudo yum install centos-release-scl -y
sudo yum install llvm-toolset-7 -y
scl enable llvm-toolset-7 bash
```

Then, run the following command to compile and install SEAL v4.0.0.

```shell
cd SEAL
mkdir build
cd build
cmake .. -DCMAKE_CXX_COMPILER=/opt/rh/llvm-toolset-7/root/usr/bin/clang++ -DCMAKE_C_COMPILER=/opt/rh/llvm-toolset-7/root/usr/bin/clang -DBUILD_SHARED_LIBS=ON -DCMAKE_BUILD_TYPE=Release -DCMAKE_CXX_FLAGS_RELEASE="-DNDEBUG -flto -O3" -DCMAKE_C_FLAGS_RELEASE="-DNDEBUG -flto -O3" -DSEAL_BUILD_BENCH=OFF -DSEAL_BUILD_EXAMPLES=OFF -DSEAL_BUILD_TESTS=OFF -DSEAL_USE_CXX17=ON -DSEAL_USE_INTRIN=ON -DSEAL_USE_MSGSL=OFF -DSEAL_USE_ZLIB=ON -DSEAL_THROW_ON_TRANSPARENT_CIPHERTEXT=ON
make
sudo make install
cd .. # return to the SEAL path
cd .. # return to the original path
```

## Compile `mpc4j-native-fhe`

Please follow the procedures presented in `mcp4j-native-tool` to correctly install Java and set `JAVA_HOME`. Then, go to the path of `mpc4j-native-fhe`, run the following command to compile `mpc4j-native-fhe`.

```shell
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```