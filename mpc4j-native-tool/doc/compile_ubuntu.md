## Compile on Ubuntu

The following guidelines have been tested on Ubuntu 20.04 and [Docker](https://www.docker.com/) on Unbuntu 20.04 / MAC (`x86_64`) / MAC (`aarch64`) with the official Ubuntu image.

We recommend creating a new dictionary (such as `~/libs`) to temporarily store all source codes and libraries before installing `mpc4j-native-tool`. All installation procedures assume you are under the dictionary `~/libs`.

### Ubuntu Docker image

You can run the following commands if you want to use Ubuntu with a Docker image. First, pull the latest Ubuntu Docker image.

```shell
Docker pull ubuntu
```

Then, run the Ubuntu Docker image.

```shell
docker run -it ubuntu
```

Next, update the `apt` command.

```shell
apt update
```

**Note**: If you use Ubuntu Docker image to install `mpc4j-native-tool`, execute all commands without `sudo`.

### Necessary Tools

Install `gcc`, `m4`, `make`, `cmake`, `g++`, `openssl` (and its development tool), `wget`, `xz-util`, and `vim` by the following command.

```shell
sudo apt install gcc
sudo apt install m4
sudo apt install make
sudo apt install cmake
sudo apt install g++
sudo apt install openssl
sudo apt install libssl-dev
sudo apt install wget
sudo apt install xz-utils
sudo apt install vim
```

Also, we need to add the default path `/usr/local/lib` to be the additional library path. Run the following command to open the configuration file for `ldconfig`.

```shell
sudo vim /etc/ld.so.conf
```

Then, add the following scripts at the end of `ld.so.conf`. If you are not familiar with `vim`, just type `i` and use it just like a notebook. When you finish editing, type `:wq` and press `Enter` to confirm the modification.

```text
/usr/local/lib
```

Then, run the following command.

```shell
/sbin/ldconfig
```

### GMP

Note that different Ubuntu platforms need different GMP version for compiling NTL. For example, our Ubuntu platform only supports [GMP v6.2.0](https://gmplib.org/download/gmp/gmp-6.2.0.tar.xz). However, when using the Ubuntu Docker image, [GMP v6.2.1](https://gmplib.org/download/gmp/gmp-6.2.1.tar.xz) is needed. When you compile NTL, and you meet problems like:

```text
GMP version check (6.2.0/6.2.1)
```

you need to uninstall the inconsistent GMP version (by executing `make uninstall`) in the GMP source code dictionary and compile and install the correct GMP version. In the following, we write `gmp-6.2.X` to represent different GMP versions. Please replace `X` with the correct version number when executing the commands.

Run the following command to download the source code of GMP.

```shell
wget https://gmplib.org/download/gmp/gmp-6.2.X.tar.xz
```

Install GMP.

```shell
xz -d gmp-6.2.X.tar.xz
tar -xvf gmp-6.2.X.tar
cd gmp-6.2.X
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

### JDK and JAVA_HOME

Since we need to compile `mpc4j-native-tool` with the help of `jni.h`, you need to install Java Development Tool (JDK) instead of Java Runtime Environment (JRE). JDK 17 (or later) is needed for development. We recommend installing JDK from [oracle.com](https://www.oracle.com/java/technologies/downloads/). You can also install JDK via the following command:

```shell
sudo apt install default-jdk
```

On Ubuntu, Java would be installed under the path like `/usr/lib/jvm/jdk-XX.X.X`. You can find the Java installation path by the following command.

```shell
whereis java
```

It may show a lot of java paths. Find the java path that contains `jdk`, like `/usr/lib/jvm/jdk-17.0.2/bin/java`. This mains the Java installation path is `/usr/lib/jvm/jdk-17.0.2`. After you find the Java installation path, run the following command to open `bashrc`.

```shell
vim ~/.bashrc
```

Then, add the following scripts in `bashrc`. If you are not familiar with `vim`, just type `i` and use it just like a notebook. When you finish editing, type `:wq` and press `Enter` to confirm the modification.

```shell
export JAVA_HOME=YOUR_JAVA_PATH
```

Then, run the following command.

```shell
source ~/.bashrc
```

You can verify the result by running the following command and see if the Java installation path can be correctly shown.

```shell
echo $JAVA_HOME
```
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
export CC=/usr/bin/gcc # assign gcc
export CXX=/usr/bin/g++ # assign g++
mkdir cmake-build-release
cd cmake-build-release
cmake ..
make
```
