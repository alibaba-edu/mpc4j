# `mpc4j-native-tool`

## Introduction

`mpc4j` leverages native C/C++ codes to speed up cryptographic operations. The native codes and Java codes are interacted by the [Java Native Interface (JNI)](https://docs.oracle.com/javase/8/docs/technotes/guides/jni/) technique. We separate native C/C++ codes into two modules, namely `mpc4j-native-cool` and `mpc4j-native-fhe`. `mpc4j-native-tool` contains native codes for basic cryptographic operations, while `mpc4j-native-fhe` contains native codes for Fully Homomorphic Encryption (FHE) using [SEAL](https://github.com/microsoft/SEAL). All basic cryptographic operations in `mpc4j-native-tool` have alternative pure-Java implementations in `mpc4j` with the same functionalities and the same data representations.

`mpc4j-native-tool` relies on the following C/C++ libraries:

- [GMP](https://gmplib.org/): An efficient library for operations with arbitrary precision integers, rationals, and floating-point numbers.
- [NTL](https://libntl.org/): A high-performance, portable C++ library providing data structures and algorithms for manipulating signed, arbitrary length integers and for vectors, matrices, and polynomials over the integers and over finite fields, developed by [Victor Shoup](https://shoup.net/). Note that one can further introduce [GF2X](https://gitlab.inria.fr/gf2x/gf2x) for more efficient operations in a Galois Field. However, since the installation procedure for [GF2X](https://gitlab.inria.fr/gf2x/gf2x) is rather complicated, we use NTL by default.
- [MCL](https://github.com/herumi/mcl): A portable and fast pairing-based cryptography library. MCL also includes fast Elliptic Curve implementations. 
- [libsodium](https://doc.libsodium.org/): A modern, easy-to-use software library for encryption, decryption, signatures, password hashing, and more. libsodium includes efficient X25519 and Ed25519 implementations. 

Installing `mpc4j-native-tool` might be a bit complicated for ones who are not that familiar with Unix-like systems. As the procedures differ across platforms, here we provide installation instructions for macOS (x86_64), macOS (M1), Ubuntu, and CentOS, respectively.

We recommend creating a new dictionary (such as `~/lib`) to temporarily store all source codes and libraries before you start to install `mpc4j-native-tool`. All installation procedures assume you are under the dictionary `~/lib`.

**NOTE:** The following instructions are tested with the specified versions of the underlying libraries. Feel free to contact us if they do not work with newer versions.

## Install on MAC (x86_64)

### Necessary Tools

[Homebrew](https://brew.sh/) installs [the stuff you need](https://formulae.brew.sh/formula/) that Apple didn’t. Homebrew can be seen as `apt-get` or `yum` on macOS. Some libraries in `mpc4j-native-tool` can be installed directly using `homebrew`. You can easily install `homebrew` by following the instructions shown on the official site.

You need to install `openssl` and `cmake` for further installations by running the following command.

```shell
brew install openssl
brew install cmake
```

### GMP

GMP can be directly installed via `homebrew`: 

```shell
brew install gmp
```

GMP can also be installed via the source code. In this way, you can add `CFLAGS="-march=native -O3"` to obtain a more efficient GMP. 

Download GMP 6.2.1
```shell
wget https://gmplib.org/download/gmp/gmp-6.2.1.tar.xz
```

Install GMP.

```shell
xz -d gmp-6.2.1.tar.xz
tar -xvf gmp-6.2.1.tar
cd gmp-6.2.1
./configure CFLAGS="-march=native -O3" CXXFLAGS="-march=native -O3"
make
make check
sudo make install
cd .. # return to the original path
```

 ### NTL

Download `ntl-11.5.1.tar.gz`.

```shell
wget https://libntl.org/ntl-11.5.1.tar.gz
```

Extract source codes from `ntl-11.5.1.tar.gz`.

```shell
tar -xvzf ntl-11.5.1.tar.gz
```

Compile, check, and install NTL on the default path.

```shell
cd ntl-11.5.1
cd src
./configure SHARED=on CXXFLAGS=-O3 # Must compile NTL as a shared library
make
make check
sudo make install
cd .. # return to the ntl-11.5.1 path
cd .. # return to the original path
```

### MCL

Download [MCL v1.61](https://github.com/herumi/mcl/releases/tag/v1.61).

```shell
git clone -b v1.61 https://github.com/herumi/mcl.git
```

Compile and install MCL.

```shell
cd mcl
mkdir build
cd build
cmake ..
make
sudo make install
cd .. # return to the build path
cd .. # return to the original path
```

### libsodium

You need to install libsodium via `homebrew`: 

```shell
brew install libsodium
```

### JAVA and JAVA_HOME

First, check if you have installed Java by the following command:

```
java --version
```

If you cannot see the version information, then you may need to install Java on your own. We recommend installing Java from [oracle.com](https://www.oracle.com/java/technologies/downloads/). Recall that if you just want to run `mpc4j`, higher Java versions are OK. However, if you want to develop `mpc4j`, you may need to install JDK8.

By default, Java will be installed under the path like `/Library/Java/JavaVirtualMachines/jdk-XX.X.X.jdk/Contents/Home` or `/Users/USERNAME/Library/Java/JavaVirtualMachines/JDK_NAME/Comtents/Home`. After you find the Java installation path, run the following command to open `bash_profile`.

```shell
vim ~/.bash_profile
```

Then, add the following scripts in `bash_profile`. If you are not familiar with `vim`, just type `i` and use it just like a notebook. When you finish editing, type `:wq` and press `Enter` to confirm the modification.

```
export JAVA_HOME=YOUR_JAVA_PATH
```

Then, run the following command.

```shell
source ~/.bash_profile
```

You can verify the result by running the following command and see if the Java installation path can be correctly shown.

```
echo $JAVA_HOME
```

**NOTE:** If your terminal is `zsh` instead of `bash`, you should replace `.bash_profile` with `.zshrc` in above commands.

### Compile `mpc4j-native-tool`

Go to the path of `mpc4j-native-tool`, run the following command to compile `mpc4j-native-tool`.

```shell
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```

## Install Dependencies on MAC (M1)

### Necessary Tools

[Homebrew](https://brew.sh/) installs [the stuff you need](https://formulae.brew.sh/formula/) that Apple didn’t. Homebrew can be seen as `apt-get` or `yum` on macOS. Some libraries in `mpc4j-native-tool` can be installed directly using `homebrew`. You can easily install `homebrew` by following the instructions shown on the official site.

You need to install `openssl` and `cmake` for further installations by running the following command.

```shell
brew install openssl
brew install cmake
```

### GMP

Please **DO NOT** install GMP via `homebrew`. This is because MCL relies on GMP under aarch64. Instead, install GMP in C++ with versions higher than 6.2.1.

Download the source code of GMP 6.2.1.

```shell
wget https://gmplib.org/download/gmp/gmp-6.2.1.tar.xz
```

Install the C++ version of GMP.

```shell
xz -d gmp-6.2.1.tar.xz
tar -xvf gmp-6.2.1.tar
cd gmp-6.2.1
./configure --enable-cxx CFLAGS="-O3" CXXFLAGS="-O3"
make
make check
sudo make install
cd .. # return to the original path
```

### NTL

Download `ntl-11.5.1.tar.gz`.

```shell
wget https://libntl.org/ntl-11.5.1.tar.gz
```

Extract source codes from `ntl-11.5.1.tar.gz`.

```shell
tar -xvzf ntl-11.5.1.tar.gz
```

Compile, check, and install NTL on the default path.

```shell
cd ntl-11.5.1
cd src
./configure SHARED=on CXXFLAGS=-O3 # Must compile NTL as a shared library
make
make check
sudo make install
cd .. # return to the ntl-11.5.1 path
cd .. # return to the original path
```

### MCL

Download the source code of  [MCL v1.61](https://github.com/herumi/mcl/releases/tag/v1.61). 

```shell
git clone -b v1.61 https://github.com/herumi/mcl.git
```

MCL currently does not have specific assembly optimizations for aarch64 platforms. Therefore, we need to remove assembly language support when compiling MCL. Compile and install MCL as follows.

```shell
cd mcl
mkdir build
cd build
cmake .. -DMCL_USE_ASM=OFF # remove assembly language support
make
sudo make install
cd .. # return to the build path
cd .. # return to the original path
```

### libsodium

You need to install libsodium via `homebrew`: 

```shell
brew install libsodium
```

### JAVA and JAVA_HOME

First, check if you have installed Java by the following command:

```
java --version
```

If you cannot see the version information, then you may need to install Java on your own. We recommend installing Java from [oracle.com](https://www.oracle.com/java/technologies/downloads/). Recall that if you just want to run `mpc4j`, higher Java versions are OK. However, if you want to develop `mpc4j`, you may need to install JDK8.

By default, Java will be installed under the path like `/Library/Java/JavaVirtualMachines/jdk-XX.X.X.jdk/Contents/Home` or `/Users/USERNAME/Library/Java/JavaVirtualMachines/JDK_NAME/Comtents/Home`. After you find the Java installation path, run the following command to open `bash_profile`.

```shell
vim ~/.bash_profile
```

Then, add the following scripts in `bash_profile`. If you are not familiar with `vim`, just type `i` and use it just like a notebook. When you finish editing, type `:wq` and press `Enter` to confirm the modification.

```
export JAVA_HOME=YOUR_JAVA_PATH
```

Then, run the following command.

```shell
source ~/.bash_profile
```

You can verify the result by running the following command and see if the Java installation path can be correctly shown.

```
echo $JAVA_HOME
```

**NOTE:** If your terminal is `zsh` instead of `bash`, you should replace `.bash_profile` with `.zshrc` in above commands.


### Compile `mpc4j-native-tool`

Go to the path of `mpc4j-native-tool`, run the following command to compile `mpc4j-native-tool`.

```shell
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```

## Install Dependencies on Ubuntu

### Necessary Tools

Install `gcc`, `m4`, `make`, `cmake`, `g++`, and `openssl` by the following command.

```shell
sudo apt install gcc
sudo apt install m4
sudo apt install make
sudo apt install cmake
sudo apt-get install g++
sudo apt install openssl
```

Also, we need to add the default path `/usr/local/lib` to be the additional library path. Run the following command to open the configuration file for `ldconfig`.

```shell
sudo vim /etc/ld.so.conf
```

Then, add the following scripts in the end of `ld.so.conf`. If you are not familiar with `vim`, just type `i` and use it just like a notebook. When you finish editing, type `:wq` and press `Enter` to confirm the modification.

```text
/usr/local/lib
```

Then, run the following command.

```shell
/sbin/ldconfig
```

### GMP

Note that NTL on Ubuntu only supports [GMP v6.2.0](https://gmplib.org/download/gmp/gmp-6.2.0.tar.xz). Run the following command to download the source code for version 6.2.0.

```shell
wget https://gmplib.org/download/gmp/gmp-6.2.0.tar.xz
```

Install GMP.

```shell
xz -d gmp-6.2.0.tar.xz
tar -xvf gmp-6.2.0.tar
cd gmp-6.2.0
./configure CFLAGS="-march=native -O3" CXXFLAGS="-march=native -O3"
make
make check
sudo make install
cd .. # return to the original path
```

Install the development library for GMP.

```shell
sudo apt install libgmp-dev
```

 ### NTL

Download  `ntl-11.5.1.tar.gz`.

```shell
wget https://libntl.org/ntl-11.5.1.tar.gz
```

Extract source codes from `ntl-11.5.1.tar.gz`.

```shell
tar -xvzf ntl-11.5.1.tar.gz
```

Compile, check, and install NTL on the default path.

```shell
cd ntl-11.5.1
cd src
./configure SHARED=on CXXFLAGS=-O3 # Must compile NTL as a shared library
make
make check
sudo make install
cd .. # return to ntl-11.5.1
cd .. # return to the original path
```

### MCL

Download the source code of [MCL v1.61](https://github.com/herumi/mcl/releases/tag/v1.61). 

```shell
git clone -b v1.61 https://github.com/herumi/mcl.git
```

Compile and install MCL.

```shell
cd mcl
mkdir build
cd build
cmake ..
make
sudo make install
cd .. # return to the build path
cd .. # return to the original path
```

### libsodium

Download the latest version of libsodium. Currently, the latest version is [1.0.18](https://download.libsodium.org/libsodium/releases/libsodium-1.0.18.tar.gz).

```shell
wget https://download.libsodium.org/libsodium/releases/libsodium-1.0.18.tar.gz
```

Extract source codes from `libsodium-1.0.18.tar.gz`.

```shell
tar -xvzf libsodium-1.0.18.tar.gz
```

Compile, check, and install Libsodium on the default path.

```shell
cd libsodium-1.0.18
./configure
make
make check
sudo make install
cd .. # return to the original path
```

### JAVA and JAVA_HOME

First, check if you have installed Java by the following command:

```
java --version
```

If you cannot see the version information, then you may need to install Java on your own. We recommend installing Java from [oracle.com](https://www.oracle.com/java/technologies/downloads/). Recall that if you just want to run `mpc4j`, higher Java versions are OK. However, if you want to develop `mpc4j`, you may need to install JDK8.

On Ubuntu, Java would be installed under the path like `/usr/lib/jvm/jdk-XX.X.X`. You can find the Java installation path by the following command.

```shell
which java
```

After you find the Java installation path, run the following command to open `bashrc`.

```shell
vim ~/.bashrc
```

Then, add the following scripts in `bashrc`. If you are not familiar with `vim`, just type `i` and use it just like a notebook. When you finish editing, type `:wq` and press `Enter` to confirm the modification.

```
export JAVA_HOME=YOUR_JAVA_PATH
```

Then, run the following command.

```shell
source ~/.bashrc
```

You can verify the result by running the following command and see if the Java installation path can be correctly shown.

```
echo $JAVA_HOME
```

### Compile `mpc4j-native-tool`

Go to the path of `mpc4j-native-tool`, run the following command to compile `mpc4j-native-tool`.

```shell
export CC=/usr/bin/gcc # assign gcc
export CXX=/usr/bin/g++ # assign g++
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```

## Install Dependencies on CentOS

The installation procedures on CentOS are almost identical with Ubuntu, except that CentOS uses `yum` instead of `apt` for library management.

### Necessary Tools

Install `gcc`, `m4`, `make`, `g++`, `openssl` by the following command.

```shell
sudo yum install gcc
sudo yum install m4
sudo yum install make
sudo yum install gcc-c++ # different from Ubuntu
sudo yum install openssl
sudo yum install openssl-devel # additional installation compared with Ubuntu
```

Note that we cannot directly install `cmake` via `yum`. Instead, we need to install `cmake` from source code by running the following command.

```shell
wget https://github.com/Kitware/CMake/releases/download/v3.24.0/cmake-3.24.0.tar.gz # download a newer version. Currently the newer version is 3.24.0
tar -zxvf cmake-3.24.0.tar.gz
cd cmake-3.24.0
./bootstrap
make
make install
\cp -f ./bin/cmake ./bin/cpack ./bin/ctest /bin
cd .. # return to the original path
```

CentOS needs to install related libraries to support AES-NI by running the following commands (See [Cannot compile from source on Centos7](https://discuss.zerotier.com/t/cannot-compile-from-source-on-centos7/842) for more details).

```shell 
sudo yum install centos-release-scl
sudo yum install devtoolset-8
```

### GMP

Note that NTL on CentOS only supports [GMP v6.0.0](https://gmplib.org/download/gmp/gmp-6.0.0.tar.xz). Run the following command to download the source code for version 6.0.0.

```shell
wget https://gmplib.org/download/gmp/gmp-6.0.0.tar.xz
```

Run the following command to install GMP.

```shell
xz -d gmp-6.0.0.tar.xz
tar -xvf gmp-6.0.0.tar
cd gmp-6.0.0
./configure CFLAGS="-march=native -O3" CXXFLAGS="-march=native -O3"
make
make check
sudo make install
cd .. # return to the original path
```

You may also need to install the development library for GMP by running the following command.

```shell
sudo yum install gmp-devel # different from Ubuntu
```

 ### NTL

Download `ntl-11.5.1.tar.gz`.

```shell
wget https://libntl.org/ntl-11.5.1.tar.gz
```

Extract source codes from `ntl-11.5.1.tar.gz`.

```shell
tar -xvzf ntl-11.5.1.tar.gz
```

Compile, check, and install NTL on the default path.

```shell
cd ntl-11.5.1
cd src
./configure SHARED=on CXXFLAGS=-O3 # Must compile NTL as a shared library
make
make check
sudo make install
cd .. # return to ntl-11.5.1
cd .. # return to the original path
```

### MCL

Download the source code of [MCL v1.61](https://github.com/herumi/mcl/releases/tag/v1.61). 

```shell
git clone -b v1.61 https://github.com/herumi/mcl.git
```

Run the following command to compile and install MCL.

```shell
cd mcl
mkdir build
cd build
cmake ..
make
sudo make install
cd .. # return to the build path
cd .. # return to the original path
```

### libsodium

Download the latest version of libsodium. Currently, the latest version is [1.0.18](https://download.libsodium.org/libsodium/releases/libsodium-1.0.18.tar.gz).

```shell
wget https://download.libsodium.org/libsodium/releases/libsodium-1.0.18.tar.gz
```

Extract source codes from `libsodium-1.0.18.tar.gz`.

```shell
tar -xvzf libsodium-1.0.18.tar.gz
```

Compile, check, and install Libsodium on the default path.

```shell
cd libsodium-1.0.18
./configure
make
make check
sudo make install
cd .. # return to the original path
```

### JAVA and JAVA_HOME

You can directly install Java by the following command.

```shell
sudo yum install java-1.8.0-openjdk-devel.x86_64 # install JDK 8
```

You can find the Java installation path by the following command.

```shell
which java                      # It may show /usr/bin/java
ls -lrt /usr/bin/java           # It may show /usr/bin/java -> /etc/alternatives/java
ls -lrt /etc/alternatives/java  # It may show /etc/alternatives/java -> /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.302.b08-0.el7_9.x86_64/jre/bin/java
```

After you find the Java installation path, run the following command to open `bash_profile`.

```shell
vim ~/.bash_profile
```

Then, add the following scripts in `bash_profile`. If you are not familiar with `vim`, just type `i` and use it just like a notebook. When you finish editing, type `:wq` and press `Enter` to confirm the modification.

```
export JAVA_HOME=YOUR_JAVA_PATH
```

Then, run the following command.

```shell
source ~/.bash_profile
```

You can verify the result by running the following command and see if the Java installation path can be correctly shown.

```
echo $JAVA_HOME
```

### Compile `mpc4j-native-tool`

Go to the path of `mpc4j-native-tool`, run the following command to compile `mpc4j-native-tool`.

```shell
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```

## Helpful Commands

If `cmake` reports some libraries are not found, you can run the following command to see which library has problem.

```shell
cmake .. -LA . # there is a dot
```
