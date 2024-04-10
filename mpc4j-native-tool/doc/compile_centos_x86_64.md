## Compile on CentOS (`x86_64`)

The following guidelines have been tested both on CentOS 8, and [Docker](https://www.docker.com/) on Unbuntu 20.04 / MAC (`x86_64`) with the official CentOS 8 image. The installation procedures on CentOS are almost identical to Ubuntu, except that CentOS uses `yum` instead of `apt` for library management.

**Note**: We cannot install dependencies on CentOS with Docker under `aarch64` (e.g., MacBook M1) with the official CentOS 8 image. The reason is that CentOS does not provide `devtoolset-8` for `aarch64`. A candidate solution is to manually install all libraries related to [`devtoolset-8 (aarch64)`](https://centos.pkgs.org/7/centos-sclo-rh-aarch64/devtoolset-8-8.1-1.el7.aarch64.rpm.html) using `rpm`. 

We recommend creating a new dictionary (such as `~/libs`) to temporarily store all source codes and libraries before installing `mpc4j-native-tool`. All installation procedures assume you are under the dictionary `~/libs`.

### CentOS Docker image

You can run the following commands if you want to use CentOS with Docker image. First, pull the latest CentOS Docker image.

```shell
Docker pull centos
```

Then, run the CentOS Docker image.

```shell
docker run -it centos
```

Although `yum` has been installed in the CentOS Docker image, we need to update the repo url for successfully installing all libraries. First, we need to change the default mirror link (See [Error: Failed to download metadata for repo](https://blog.csdn.net/weixin_43252521/article/details/124409151) for details).

```shell
cd /etc/yum.repos.d/
sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-*
sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-*
yum makecache
```

Then, install `wget`, and run the following commands to change the repo url for `yum`.

```shell
yum install wget
wget -O /etc/yum.repos.d/CentOS-Base.repo https://mirrors.aliyun.com/repo/Centos-vault-8.5.2111.repo
wget -O /etc/yum.repos.d/CentOS-8.repo https://mirrors.aliyun.com/repo/Centos-8.repo
yum makecache
```

**Note**: If you use CentOS Docker image to install `mpc4j-native-tool`, just execute all commands without `sudo`.

### Necessary Tools

Install `gcc`, `m4`, `make`, `g++`, `openssl`, `perl`, `git`, and `vim` by the following commands.

```shell
sudo yum install gcc
sudo yum install m4
sudo yum install make
sudo yum install gcc-c++ # different from Ubuntu
sudo yum install openssl
sudo yum install openssl-devel # additional installation compared with Ubuntu
sudo yum install perl
sudo yum install git
sudo yum install vim
```

Note that we cannot directly install `cmake` via `yum`. Instead, we need to install `cmake` from the source code by running the following command.

```shell
wget https://github.com/Kitware/CMake/releases/download/v3.24.0/cmake-3.24.0.tar.gz # download a newer version. Currently, the newer version is 3.24.0
tar -zxvf cmake-3.24.0.tar.gz
cd cmake-3.24.0
./bootstrap
make
make install
\cp -f ./bin/cmake ./bin/cpack ./bin/ctest /bin
cd .. # return to the original path
```

CentOS 7 needs to install related libraries to support AES-NI by running the following commands (See [Cannot compile from source on Centos7](https://discuss.zerotier.com/t/cannot-compile-from-source-on-centos7/842) for more details).

```shell
sudo yum install centos-release-scl
sudo yum install devtoolset-8
```

### GMP

As far as we know, CentOS comes with its own GMP library. CentOS 7 contains [GMP v6.0.0](https://gmplib.org/download/gmp/gmp-6.0.0.tar.xz), and CentOS 8, contains [GMP v6.1.2](https://gmplib.org/download/gmp/gmp-6.1.2.tar.xz). Therefore, we need to install the same version of GMP in the corresponding version of Centos. We can download the latest version of GMP and replace the GMP that comes with Centos.

Run the following command to download the source code for version 6.x.x.

```shell
wget https://gmplib.org/download/gmp/gmp-6.x.x.tar.xz
```

Run the following command to install GMP.

```shell
xz -d gmp-6.x.x.tar.xz
tar -xvf gmp-6.x.x.tar
cd gmp-6.x.x
./configure CFLAGS="-march=native -O3" CXXFLAGS="-march=native -O3"
make
make check
sudo make install
cd .. # return to the original path
```

**Optimal**: You can replace the GMP of CentOS through the following command.

```shell
cp /usr/local/lib/libgmp.so /usr/lib64/ #The first path in this command is the installation path of gmp
cp /usr/local/lib/libgmp.so.10 /usr/lib64/
cp /usr/local/lib/libgmp.so.10.4.1 /usr/lib64/ # the name may be different, depending on which version of gmp you install.
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

Since we need to compile `mpc4j-native-tool` with the help of `jni.h`, you need to install Java Development Tool (JDK) instead of Java Runtime Environment (JRE).  JDK 17 (or later) is needed for development. We recommend installing JDK from [oracle.com](https://www.oracle.com/java/technologies/downloads/). You can directly install JDK by the following command.

```shell
yum search java # You can get a candidate version of Java that you can install
sudo yum install java-17-openjdk # install JDK (the name with "devel"), here we install JDK 17
```

You can find the Java installation path by the following command.

```shell
whereis java
```

It may show a lot of java paths. Find the java path that contains `jdk`, like `/usr/lib/jvm/jdk-17.0.2/bin/java`. This mains the Java installation path is `/usr/lib/jvm/jdk-17.0.2`. After you find the Java installation path, run the following command to open `bash_profile`.

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