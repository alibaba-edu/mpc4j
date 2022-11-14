## Compile on MAC (`aarch64`)

We recommend creating a new dictionary (such as `~/libs`) to temporarily store all source codes and libraries before installing `mpc4j-native-tool`. All installation procedures assume you are under the dictionary `~/libs`.

### Necessary Tools

[Homebrew](https://brew.sh/) installs [the stuff you need](https://formulae.brew.sh/formula/) that Apple doesn’t. Homebrew can be seen as `apt-get` or `yum` on macOS. Some libraries in `mpc4j-native-tool` can be installed directly using `homebrew`. You can easily install `homebrew` by following the instructions on the official site.

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

### JDK and JAVA_HOME

Since we need to compile `mpc4j-native-tool` with the help of `jni.h`, you need to install Java Development Tool (JDK) instead of Java Runtime Environment (JRE). We recommend installing JDK from [oracle.com](https://www.oracle.com/java/technologies/downloads/).

By default, Java will be installed under the path like `/Library/Java/JavaVirtualMachines/jdk-XX.X.X.jdk/Contents/Home` or `/Users/USERNAME/Library/Java/JavaVirtualMachines/JDK_NAME/Contents/Home`. After you find the Java installation path, run the following command to open `bash_profile`.

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

**NOTE:** If your terminal is `zsh` instead of `bash`, you should replace `.bash_profile` with `.zshrc` in the above commands.


### Compile `mpc4j-native-tool`

Go to the path of `mpc4j-native-tool`, and run the following command to compile `mpc4j-native-tool`.

```shell
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```

## 