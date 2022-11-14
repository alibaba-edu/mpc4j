# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## \[1.0.5\]

### Added

- `mpc4j-common-tool`
  - Polynomial: We add batched polynomial implementation algorithms (both for Java and C/C++) introduced in the CRYPTO 2019 paper [SpOT-Light: Lightweight Private Set Intersection from Sparse OT Extension](https://eprint.iacr.org/2019/634).
  - Ecc: We now support pure-Java [Ristretto](https://ristretto.group/) curve. We also support pure-Java Elliagtor encoding/decoding introduced in the CCS 2021 paper [Compact and Malicious Private Set Intersection for Small Sets](https://eprint.iacr.org/2021/1159).
- `mpc4j-common-rpc`
  - We add the interface `PtoFactory` and make protocol factory classes implement `PtoFactory`.
  - We add `setEnvType()` into the interface `SecurePtoConfig`. All protocol config can support `setEnvType()` so that we can switch `EnvType.STANDARD` to others in a unified way.
- `mpc4j-native-fhe`
  - We merged all native tools in one utils class for all protocols.
- `mpc4j-s2pc-pcg`
  - Multiplication Triple in Zp64: Introduce Multiplication Triple Generation (MTG) under Zp64 in `mpc4j-s2pc-pcg`.
- `mpc4j-s2pc-pir`
  - Index PIR: We implemented XPIR proposed in the PETS 2022 paper [XPIR : Private Information Retrieval for Everyone](https://petsymposium.org/2016/files/papers/XPIR___Private_Information_Retrieval_for_Everyone.pdf).
- `mpc4j-s2pc-pso`
  - `psu`
    - Now `Main` supports unbalanced PSU inputs.
    - Now `Main` supports BlackIP tests, recommended by anonymous USENIX Security 2023 reviewers.

### Changed

- Documentations
  - We update documentations for how to install and run `mpc4j`. Now, the documentation contains installing `mpc4j` in Ubuntu and CentOS Docker images both for `aarch64` and `x86_64`.
- `mpc4j-common-tool`
  - We revise the code for `SparseBitMatrix`. Now the code is easier to understand.

### Fixed

- `mpc4j-common-rpc`
  - Fix issue \#5.
- `mpc4j-native-tool`
  - We thank anonymous USENIX Security 2023 Artifact Evaluation (AE) reviewers for many suggestions for `mpc4j-native-tool`. These suggestions help us fix many memory leakage problems. Also, the comments help us remove many duplicate codes. Specifically, we replace constant-size heap allocations (e.g., `auto *p = new uint8_t[]`) with stack allocations (e.g., `uint8_t p[]`). We fixed many memory leakage bugs in our C/C++ implementations.
  - We update `CMakeList.txt` so that one can successfully compile `mpc4j-native-tool` in Ubuntu and CentOS Docker images both for `aarch64` and `x86_64`.


## \[1.0.4\]

### Added

- `mpc4j-common-tool`
  - ByteEcc: Add scalar validation for X25519. Add libsodium support for both X25519 and Ed25519.
  - Kyber: Add post-quantum secure public key encryption scheme Kyber. The implementation is modified from [KyberJCK](https://github.com/fisherstevenk/kyberJCE).
- `mpc4j-s2pc-pcg`
  - Multiplication Triple in Zl: Introduce Multiplication Triple Generation (MTG) under Zl in `mpc4j-s2pc-pcg`.
  - Kyber Base-OT: Introduce Kyber Base-OT schemes.
- `mpc4j-s2pc-pso`
  - mqRPMT: Introduce mqRPMT.
  - Facebook PID: Introduce the Facebook PID scheme based on X25519.
  - PSI: Introduce EC-DH-PSI and KKRT16-PSI.
  
### Changed

- `mpc4j-s2pc-pcg`
  - $2^l$-out-of-1 homomorphic oblivious transfer: We change $2^l$-out-of-1 homomorphic oblivious transfer to core $2^l$-out-of-1 oblivious transfer. In this way, $2^l$-out-of-1 oblivious transfer implementations have the same style with 2-out-of-1 oblivious transfer implementations.

### Remove

- `mpc4j-common-tool`
  - `byte[]` -> `int[]`: More tests show that the ByteBuffer conversion is as fast as unsafe conversion. We remove the unsafe conversion method. Now, developer can use `mpc4j` on any JDK with version 1.8 or later (instead of only 1.8).
- `mpc4j-s2pc-pcg`
  - n-out-of-1 oblivious transfer: We remove n-out-of-1 oblivious transfer since it seems useless in the current framework.

## \[1.0.3\]

### Added

- CHANGELOG: We add CHANGELOG.md to write any changes during our development.
- UNSAFE: We find that `byte[]` to `int[]` conversion dominates the cost for Silent OT. We add `unsafeByteArrayToIntArray` in `IntUtils`, and introduce such a method in our Silent OT implementation.
- Ecc in OpenSSL: Ecc now supports OpenSSL. This means that we now have C/C++ SM2 implementation in `mpc4j`.
- ByteEcc: We add `ByteMulEcc` and `ByteFullEcc` interface and its Ed25519 and X25519 implementations. The performance report shows that Ed25519 and X25519 are more efficient than the standard Ecc implementations but with some limitations. For example, X25519 only supports multiplication with specific scalars.
- PropertiesUtils: We add `PropertiesUtils` in `mpc4j-common-tool` for ease of using `Properties`. In addition, we refine `main` in `mpc4j-s2pc-pso` and `mpc4j-sml-opboost`. 

### Changed

- Fixed-Point Multiplication in ECC: In `mpc4j-common-tool`, we introduce the Window Method for ECC Fixed-Point Multiplication implemented in MCL into our pure-Java implementation, replacing the pre-computation techniques provided by Bouncy Castle. The efficiency results show that our new implementation is about 10x faster than the original one.
- Multiplication Triple: In `mpc4j-s2pc-pcg`, we merge Boolean Triple Generation (BTG) packages into Multiplication Triple Generation (MTG) packages and rename `booleanTriple` to `Z2Triple`, since BTG is a special case of MTG under the Z2 Field.
- Distributed Punctured PRF: In `mpc4j-s2pc-pcg`, we define a new protocol named Distributed Punctured PRF (DPPRF), and move all related implementations into DPRRF. This helps remove repeating codes when using DPPRF to implement subfield VOLE, including Silent OT and $GF(2^{\kappa})$-(sub)VOLE.
- PMID in `mpc4j-s2pc-pso` supports multiset inputs for both parties. We further refine implementations for PMID protocols.

### Removed

- Single Sparse-Point COT (`sspcot`): We remove `sspcot` in our Silent OT implementation since there is no usage in `mpc4j`. We recommend developers use `mspcot` instead.
- Z2-VOLE: We find that Z2-VOLE is not secure and has no usage. We remove it from `mpc4j`.

### Fixed

- Ecc multiple init: We find a bug that if we first init the first native Ecc, then init the second native Ecc, and finally use the first one, an error would arise. This is because we call `native.init` in the constructor, and the later constructor would overlap the previous status. We fix this bug by refining our ECC implementation.
- APSI: There would be some unknown error when using `try_clear_irrelevant_bits` (provided by [the original APSI implementation](https://github.com/microsoft/APSI/blob/main/sender/apsi/bin_bundle.cpp)) to reduce communication costs. The error occurs with relatively low probability, around 0.8% in total tries. We remove it from our APSI implementation to ensure 100% correctness.
- APSI: Add JNI memory release functions in `mpc4j-native-fhe/upsi/serialize.cpp`.

## Reminder

### Guiding Principles

- Changelogs are for humans, not machines.
- There should be an entry for every single version.
- The same types of changes should be grouped.
- Versions and sections should be linkable.
- The latest version comes first.
- The release date of each version is displayed.
- Mention whether you follow [Semantic Versioning](https://semver.org/).

### Types of Changes

- `Added` for new features.
- `Changed` for changes in existing functionality.
- `Deprecated` for soon-to-be removed features.
- `Removed` for now removed features.
- `Fixed` for any bug fixes.
- `Security` in case of vulnerabilities.