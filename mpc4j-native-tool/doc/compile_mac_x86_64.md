## Compile on MAC (`x86_64`)

We recommend creating a new dictionary (such as `~/libs`) to temporarily store all source codes and libraries before installing `mpc4j-native-tool`. All installation procedures assume you are under the dictionary `~/libs`.

### Necessary Tools

[Homebrew](https://brew.sh/) installs [the stuff you need](https://formulae.brew.sh/formula/) that Apple doesn’t. Homebrew can be seen as `apt-get` or `yum` on macOS. Some libraries in `mpc4j-native-tool` can be installed directly using `homebrew`. You can easily install `homebrew` by following the instructions on the official site.

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

### libsodium

You need to install libsodium via `homebrew`: 

```shell
brew install libsodium
```

### JDK and JAVA_HOME

Since we need to compile `mpc4j-native-tool` with the help of `jni.h`, you need to install Java Development Tool (JDK) instead of Java Runtime Environment (JRE). JDK 17 (or later) is needed for development. We recommend installing JDK from [oracle.com](https://www.oracle.com/java/technologies/downloads/).

By default, Java will be installed under the path like `/Library/Java/JavaVirtualMachines/jdk-XX.X.X.jdk/Contents/Home` or `/Users/USERNAME/Library/Java/JavaVirtualMachines/JDK_NAME/Contents/Home`. After you find the Java installation path, run the following command to open `bash_profile`.

```shell
vim ~/.bash_profile
```

Then, add the following scripts in `bash_profile`. If you are not familiar with `vim`, just type `i` and use it just like a notebook. When you finish editing, type `:wq` and press `Enter` to confirm the modification.

```shell
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

### Compile and Install `mpc4j-native-fourq`

Go to the path of `mpc4j-native-fourq`, and run the following command to compile and install `mpc4j-native-fourq`.

```shell
mkdir build
cd build
cmake .. 
make 
make test
sudo make install
```

### Compile `mpc4j-native-tool`

Go to the path of `mpc4j-native-tool`, and run the following command to compile `mpc4j-native-tool`.

```shell
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```
