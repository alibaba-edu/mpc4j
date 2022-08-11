# mpc4j

## Introduction

Multi-Party Computation for Java (`mpc4j`) is an efficient and easy-to-use Secure Multi-Party Computation (MPC) library mainly written in Java.

The aim of `mpc4j` is to provide an academic library for researchers to study and develop MPC and related protocols in a unified manner. As `mpc4j` tries to provide state-of-the-art MPC implementations, researchers could leverage the library to have fair and quick comparisons between the new protocols they proposed and existing ones.


## Features

`mpc4j` has the following features:

- **Aarch64 support**: `mpc4j` can run on both `x86_64` and `aarch64`. Researchers can develop and test protocols on Macbook M1 (`aarch64`) and then run experiments on Linux OS (`x86_64`). 
- **SM series support**: In cases, developers may want to use SM series algorithms (SM2 for public-key operations, SM3 for hashing, and SM4 for block cipher operations) instead of regular algorithms (like secp256k1 for public key operations, SHA256 for hashing, and AES for block cipher operations). Also, the SM series algorithms are accepted by ISO/IES, so it may be necessary to support SM series algorithms under MPC settings. `mpc4j` leverages [Bouncy Castle](https://www.bouncycastle.org/java.html) to support SM series algorithms.

## Some Implementations of our Work

- Package `psu` in `mpc4j-s2pc-pso` contains the implementation of our paper ["Optimal Private Set Union from Multi-Query Reverse Private Membership Test"](https://eprint.iacr.org/2022/358.pdf). The configuration files are under `conf/psu` in `mpc4j-s2pc-pso`. Just run `java -jar mpc4j-s2pc-pso-X.X.X-jar-with-dependencies.jar conf_file_name.txt` separately on two platforms with direct network connections (using the network channel assigned in config files) or on two terminals in one platform (using local network 127.0.0.1).
- Module `mpc4j-sml-opboost` contains the implementation of our paper "OpBoost: A Vertical Federated Tree Boosting Framework Based on Order-Preserving Desensitization" (manuscript). The configuration files are under `conf` in `mpc4j-sml-opboost`. The paper is under review. We will release the final version when possible.
- Package `pmid` in `mpc4j-s2pc-pso` contains the implementation of our paper "Efficient Private Multiset ID Protocols and Applications to Private Multiset Operations" (manuscript). The configuration files are under `conf/pmid` in `mpc4j-s2pc-pso`. We are still updating the paper. We will release the final version when possible.

## References

`mpc4j` includes some implementation ideas and codes from the following open-source libraries.

- [blake2](https://github.com/BLAKE2/BLAKE2): Faster cryptographic hash function implementations. See `crypto/blake2` in `mpc4j-native-tool` for details.
- [blake3](https://github.com/BLAKE3-team/BLAKE3): Much faster cryptographic hash function implementations. See `crypto/blake3` in `mpc4j-native-tool` for details.
- [Rings](https://rings.readthedocs.io): An efficient lightweight library for commutative algebra. We understand how to efficiently do algebra operations from this library.
- [xgboost-predictor](https://github.com/h2oai/xgboost-predictor): Pure Java implementation of [XGBoost](https://github.com/dmlc/xgboost/) predictor for online prediction tasks. We understand the format of the XGBoost model from this library. We also introduce some codes in `mpc4j` for our privacy-preserving federated XGBoost implementation. See `mpc4j-sml-opboost` for details.
- [smile](https://github.com/haifengl/smile): A fast and comprehensive machine learning, NLP, linear algebra, graph, interpolation, and visualization system in Java and Scala. We understand many details of how to implement machine learning tasks from this library. We also introduce some codes in `mpc4j` for our privacy-preserving federated GBDT implementation. See `mpc4j-sml-opboost` for details.
- [emp-toolkit](https://github.com/emp-toolkit): Efficient bit-matrix transpose (See `bit_matrix_trans` in `mpc4j-native-tool`), AES-NI implementations (See `crypto/aes.h` in `mpc4j-native-tool`), and the implementation of the Silent OT protocol presented in the paper "Ferret : Fast Extension for coRRElated oT with Small Communication" accepted at [CCS 2020](https://eprint.iacr.org/2020/924.pdf) (See `cot` in `mpc4j-s2pc-pcg`).
- [libOTe](https://github.com/osu-crypto/libOTe): Implementations for many Oblivious Transfer (OT) protocols, especially the Silent OT protocol presented in the paper "Silver: Silent VOLE and Oblivious Transfer from Hardness of Decoding Structured LDPC Codes" accepted at [CRYPTO 2021](https://eprint.iacr.org/2021/1150.pdf) (See package `cot` in `mpc4j-s2pc-pcg`).
- [PSI-analytics](https://github.com/osu-crypto/PSI-analytics): The implementation of the protocols presented in the paper "Private Set Operations from Oblivious Switching" accepted at [PKC 2021](https://eprint.iacr.org/2021/243.pdf). We introduce its switching network implementations in `mpc4j`. See package `benes_network` in `mpc4j-native-tool` for details.
- [PSU](https://github.com/osu-crypto/PSU): The implementation of the paper "Scalable Private Set Union from Symmetric-Key Techniques" accepted at [ASIACRYPT 2019](https://eprint.iacr.org/2019/776.pdf). We introduce its fast polynomial interpolation implementations in `mpc4j`. See package `ntl_poly` in `mpc4j-native-tool` for details. The PSU implementation is in package `psu` of `mpc4j-s2pc-pso`.
- [PSU](https://github.com/dujiajun/PSU): The implementation of the paper "Shuffle-based Private Set Union: Faster and More" accepted at [USENIX Security 2022](https://eprint.iacr.org/2022/157.pdf). We introduce the idea of how to concurrently run Oblivious Switching Network (OSN) in `mpc4j`. See package `psu` in `mpc4j-s2pc-pso` for details.
- [SpOT-PSI](https://github.com/osu-crypto/SpOT-PSI): The implementation of the paper "SpOT-Light: Lightweight Private Set Intersection from Sparse OT Extension" published at [CRYPTO 2019](https://eprint.iacr.org/2019/634.pdf). We introduce many ideas for fast polynomial interpolations in `mpc4j`.
- [OPRF-PSI](https://github.com/peihanmiao/OPRF-PSI): The implementation of the paper "Private Set Intersection in the Internet Setting From Lightweight Oblivious PRF" published in [CRYPTO 2020](https://eprint.iacr.org/2020/729.pdf). We introduce its OPRF implementations in `mpc4j`. See `oprf` in `mpc4j-s2pc-pso` for details.
- [APSI](https://github.com/microsoft/APSI): The implementation of the paper "Labeled PSI from Homomorphic Encryption with Reduced Computation and Communication" published in [CCS 2021](https://eprint.iacr.org/2021/1116.pdf). For its source code, we understand how to use the Fully Homomorphic Encryption (FHE) library [SEAL](https://github.com/microsoft/SEAL). Most of the codes for Unbalanced Private Set Intersection (UPSI) are partially from ASPI. We also adapt the encoding part of [6857-private-categorization](https://github.com/aleksejspopovs/6857-private-categorization) to support arbitrary bit-length elements. See `mpc4j-native-fhe` and `upsi` in `mpc-s2pc-pso` for details.

## Acknowledge

We thank [Prof. Benny Pinkas](http://www.pinkas.net/) and [Dr. Avishay Yanai](https://www.yanai.io/) for many discussions on the implementations of Private Set Intersection (PSI) protocols. They also bring much help to our Java implementations for Oblivious Key-Value Storage (OKVS) presented in the paper "Oblivious Key-Value Stores and Amplification for Private Set Intersection" accepted at [CRYPTO 2021](https://eprint.iacr.org/2021/883.pdf). See package `okve/okvs` in `mpc4j-common-tool` for more details.

We thank [Dr. Stanislav Poslavsky](https://www.linkedin.com/in/stanislav-poslavsky-231311163) and [Prof. Benny Pinkas](http://www.pinkas.net/) for many discussions on implementations of fast polynomial interpolations when we try to implement the PSI protocol presented in the paper "SpOT-Light: Lightweight Private Set Intersection from Sparse OT Extension".

We thank [Prof. Mike Rosulek](https://web.engr.oregonstate.edu/~rosulekm/) for discussions about the implementation of Private Set Union (PSU). Their implementation for the paper "Private Set Operations from Oblivious Switching" brings much help for us to understand how to implement PSU.

We thank [Prof. Xiao Wang](https://wangxiao1254.github.io/) for discussions about fast bit-matrix transpose. From the discussion, we understand that the basic idea of fast bit-matrix transpose is from the blog [The Full SSE2 Bit Matrix Transpose Routine](https://mischasan.wordpress.com/2011/10/03/the-full-sse2-bit-matrix-transpose-routine/). He also helped me realize that there exists an efficient polynomial operation implementation in $GF(2^\kappa)$ introduced in [Intel Carry-Less Multiplication Instruction and its Usage for Computing the GCM Mode](https://www.intel.com/content/dam/develop/external/us/en/documents/clmul-wp-rev-2-02-2014-04-20.pdf). See package `galoisfield/gf2k` in `mpc4j-common-tool` for more details.

We thank [Prof. Peihan Miao](https://www.linkedin.com/in/peihan-miao-08919932/) for discussions about the implementation of the paper "Private Set Intersection in the Internet Setting From Lightweight Oblivious PRF". From the discussion, we understand there is a special case for the lightweight OPRF when $n = 1$. See package `oprf` in `mpc4j-s2pc-pso` for more details.

We thank [Prof. Yu Chen](https://yuchen1024.github.io/) for many discussions on various MPC protocols. Here we recommend his open-source library [Kunlun](https://github.com/yuchen1024/Kunlun), a modern crypto library. 

We thank [Dr. Bolin Ding](https://www.bolin-ding.com/) for many discussions on how to introduce MPC into the database field. Here we recommend his open-source library [FederatedScope](https://federatedscope.io/), an easy-to-use federated learning package.

## License

This library is licensed under the Apache License 2.0.

## Specifications

Most of the codes are in Java, except for very efficient implementations in C/C++. You need [OpenSSL](https://www.openssl.org/), [GMP](https://gmplib.org/), [NTL,](https://libntl.org/) and [MCL](https://github.com/herumi/mcl)  to compile `mpc4j-native-tool`, and [SEAL 4.0.0](https://github.com/microsoft/SEAL) to compile `mpc4j-native-fhe`. Please see READMD.md in `mpc4j-native-cool` on how to install required C/C++ libraries.

 After successfully obtaining the compiled C/C++ library (named `libmpc4j-native-tool` and `libmpc4j-native-fhe`, respectively), you need to assign the native library location when running `mpc4j` using `-Djava.library.path`.

## Tests

`mpc4j` has been tested on MAC OS x86_64, MAC OS M1, and Linux x86_64. We welcome developers to do tests on other platforms. 

## Development

`mpc4j` is developed under `JDK 8`. Specifically, `mpc4j` leverages `sun.misc.Unsafe`, which is deprecated since `JDK 9`, to do fast `byte[]` to `int[]` conversions. Therefore, you should also use `JDK 8` when you are implementing your protocols based `mpc4j`. Otherwise, you may encounter problem like "sun.misc.Unsafe is not found" when you compile the code to get the runnable `jar` file. Once obtaining the `jar` file, you can run it using any `JRE` with a higher version.

We develop `mpc4j` using [Intellij IDEA](https://www.jetbrains.com/idea/) and [CLion](https://www.jetbrains.com/clion/). After successfully compiling `mpc4j-native-tool` and `mpc4j-native-fhe` (Please see the documentation in these modules for more details on how to compile them), you need to configure IDEA with the following procedures so that IDEA can link to these native libraries.

1. Open `Run->Edit Configurations...`.
2. Open `Edit Configuration templates...`.
3. Select `JUnit`.
4. Add the following command into `VM Options`:

```text
-Djava.library.path=/YOUR_MPC4J_ABSOLUTE_PATH/mpc4j-native-tool/cmake-build-release:/YOUR_MPC4J_ABSOLUTE_PATH/mpc4j-native-fhe/cmake-build-release
```

## TODO List

- Provide more documentation.
- Translate documentation in English.
- Support Windows.

## TODO List (Impossible missions, but we will try)

- We are still adjusting our implementations on many Private Set Intersection protocols. We will soonly release the source code whenever available.
- More secure two-party computation (2PC) protocol implementations.
- More secure three-party computation (3PC) protocol implementations. Specifically, release the source code of our paper "Scape: Scalable Collaborative Analytics System on Private Database with Malicious Security" accepted at [ICDE 2022](https://ieeexplore.ieee.org/document/9835540/). 
- More differentially private algorithms and protocols, especially for our Shuffle Model implementations of our paper ["Privacy Enhancement via Dummy Points in the Shuffle Model"](https://arxiv.org/abs/2009.13738). 