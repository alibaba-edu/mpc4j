cmake_minimum_required(VERSION 3.10)
project(mpc4j-native-tool)

set(CMAKE_CXX_STANDARD 17)
# 增加汇编指令支持（参见：https://www.zhihu.com/question/61037115）
ENABLE_LANGUAGE(ASM)
# C编译指令
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -pthread -Wall -funroll-loops")
message("Platform: ${CMAKE_SYSTEM_PROCESSOR}")
IF(${CMAKE_SYSTEM_PROCESSOR} MATCHES "(aarch64)|(arm64)")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv8-a+simd+crypto+crc")
ELSE()
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=native -maes -mrdseed")
ENDIF()
# 打印C编译指令
message(STATUS "C Flags (CMAKE_C_FLAGS): ${CMAKE_C_FLAGS}")
# 设置C++编译指令
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${CMAKE_C_FLAGS} -std=c++11")
# 默认为Release编译
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif(NOT CMAKE_BUILD_TYPE)
message(STATUS "Build type (CMAKE_BUILD_TYPE): ${CMAKE_BUILD_TYPE}")
# 如果为debug模式，则不使用优化，增加调试编译指令
if (CMAKE_BUILD_TYPE MATCHES Debug)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O0 -ggdb")
endif()
# 如果为release模式，则使用优化
if (CMAKE_BUILD_TYPE MATCHES Release)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3")
endif()
# 打印C++编译指令
message(STATUS "CXX Flags (CMAKE_CXX_FLAGS): ${CMAKE_CXX_FLAGS}")
set(DEFAULT_DIR "/usr/local")

# 引入NTL
set(NTL_INCLUDE_DIR "${DEFAULT_DIR}/include/")
find_library(NTL_LIBRARY NAMES ntl)
message(STATUS "NTL_INCLUDE_DIRS:  ${NTL_INCLUDE_DIR}")
set(CMAKE_C_FLAGS "-Wall -maes -msse2 -msse4.1 -mpclmul -Wfatal-errors -pthread -fPIC -Wno-ignored-attributes -c")

# 引入GMP
set(GMP_INCLUDE_DIR "${DEFAULT_DIR}/include/")
find_library(GMP_LIBRARY NAMES gmp)
message(STATUS "GMP_INCLUDE_DIRS:  ${GMP_INCLUDE_DIR}")

# 寻找JNI路径并引入JNI目录
if (APPLE)
    if(NOT DEFINED ENV{JAVA_HOME})
        # 没有找到JAVA_HOME环境变量，输出错误信息退出
        message(FATAL_ERROR "not defined environment variable:JAVA_HOME")
    endif()
    set(JNI_INCLUDE_DIRS "$ENV{JAVA_HOME}/include")
    include_directories(${JNI_INCLUDE_DIRS})
    include_directories(${JNI_INCLUDE_DIRS}/darwin)
    message(STATUS "JNI_INCLUDE_DIRS:  ${JNI_INCLUDE_DIRS}")
ELSEIF (UNIX)
    if(NOT DEFINED ENV{JAVA_HOME})
        # 没有找到JAVA_HOME环境变量，输出错误信息退出
        message(FATAL_ERROR "not defined environment variable:JAVA_HOME")
    endif()
    set(JNI_INCLUDE_DIRS "$ENV{JAVA_HOME}/include")
    include_directories(${JNI_INCLUDE_DIRS})
    include_directories(${JNI_INCLUDE_DIRS}/linux)
    message(STATUS "JNI_INCLUDE_DIRS:  ${JNI_INCLUDE_DIRS}")
ENDIF()

# 引入openssl
if (${CMAKE_SYSTEM_NAME} MATCHES "Darwin")
    if(NOT DEFINED OPENSSL_ROOT_DIR)
        IF(${CMAKE_SYSTEM_PROCESSOR} MATCHES "(aarch64)|(arm64)")
            # M1 Apple
            set(OPENSSL_ROOT_DIR "/opt/homebrew/opt/openssl")
            message(STATUS "OPENSSL_ROOT_DIR set to default: ${OPENSSL_ROOT_DIR}")
        ELSE()
            # Intel Apple
            set(OPENSSL_ROOT_DIR "/usr/local/opt/openssl")
            message(STATUS "OPENSSL_ROOT_DIR set to default: ${OPENSSL_ROOT_DIR}")
        ENDIF()
    endif()
endif()
find_package(OpenSSL REQUIRED)
include_directories(${OPENSSL_INCLUDE_DIR})

# 引入libsodium
IF (${CMAKE_SYSTEM_NAME} MATCHES "Darwin")
    IF(NOT DEFINED LIBSODIUM_ROOT_DIR)
        set(LIBSODIUM_ROOT_DIR "/opt/homebrew/opt/libsodium")
        message(STATUS "LIBSODIUM_ROOT_DIR set to default: ${LIBSODIUM_ROOT_DIR}")
    ENDIF()
ELSE()
    set(LIBSODIUM_ROOT_DIR "/usr/local")
    message(STATUS "LIBSODIUM_ROOT_DIR set to default: ${LIBSODIUM_ROOT_DIR}")
ENDIF()
set(LIBSODIUM_INCLUDE_DIR "${LIBSODIUM_ROOT_DIR}/include/")
find_library(LIBSODIUM_LIBRARY NAMES sodium)
message(STATUS "LIBSODIUM_INCLUDE_DIR:  ${LIBSODIUM_INCLUDE_DIR}")
message(STATUS "LIBSODIUM_LIBRARY:  ${LIBSODIUM_LIBRARY}")
include_directories(${LIBSODIUM_INCLUDE_DIR})

link_directories("/usr/local/lib")

# 构建libent库
add_library(
        mpc4j-native-tool
        SHARED
        common/defines.cpp
        crypto/edu_alibaba_mpc4j_common_tool_crypto_hash_NativeSha256Hash.cpp
        crypto/edu_alibaba_mpc4j_common_tool_crypto_hash_NativeBlake2b160Hash.cpp
        crypto/edu_alibaba_mpc4j_common_tool_crypto_kdf_NativeBlake2bKdf.cpp
        crypto/blake2/sse/blake2.c
        crypto/blake2/sse/blake2bp.c
        crypto/blake2/sse/blake2xb.c
        crypto/blake2/neon/blake2b-neon.c
        crypto/blake2/neon/blake2bp.c
        crypto/blake2/neon/blake2s-neon.c
        crypto/blake2/neon/blake2sp.c
        crypto/blake2/neon/blake2xb.c
        crypto/blake2/neon/blake2xs.c
        crypto/blake2/neon/genkat-c.c
        crypto/edu_alibaba_mpc4j_common_tool_crypto_hash_NativeBlake3Hash.cpp
        crypto/edu_alibaba_mpc4j_common_tool_crypto_kdf_NativeBlake3Kdf.cpp
        crypto/blake3/blake3.c
        crypto/blake3/blake3_dispatch.c
        crypto/blake3/blake3_portable.c
        crypto/blake3/blake3_sse2_x86-64_unix.S
        crypto/blake3/blake3_sse41_x86-64_unix.S
        crypto/blake3/blake3_avx2_x86-64_unix.S
        crypto/blake3/blake3_avx512_x86-64_unix.S
        crypto/blake3/blake3_neon.c
        crypto/edu_alibaba_mpc4j_common_tool_crypto_prp_NativeAesPrp.cpp
        benes_network/edu_alibaba_mpc4j_common_tool_benes_NativeBenesNetwork.cpp
        benes_network/benes_network.cpp
        bit_matrix_trans/edu_alibaba_mpc4j_common_tool_bitmatrix_trans_NativeTransBitMatrix.cpp
        ntl_poly/edu_alibaba_mpc4j_common_tool_polynomial_gf2e_NtlNativeGf2ePoly.cpp
        ntl_poly/ntl_gf2x.cpp
        ntl_poly/edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlZpPoly.cpp
        ntl_poly/ntl_zp.cpp
        gf2k/edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_SseGf2k.cpp
        gf2k/edu_alibaba_mpc4j_common_tool_galoisfield_gf2k_NtlGf2k.cpp
        gf2e/edu_alibaba_mpc4j_common_tool_galoisfield_gf2e_NtlNativeGf2e.cpp
        ntl_poly/ntl_zp64.cpp
        ntl_poly/edu_alibaba_mpc4j_common_tool_polynomial_zp64_NtlZp64Poly.cpp
        ecc_mcl/mcl_ecc.cpp
        ecc_mcl/edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256k1MclNativeEcc.cpp
        ecc_mcl/edu_alibaba_mpc4j_common_tool_crypto_ecc_mcl_SecP256r1MclNativeEcc.cpp
        ecc_openssl/openssl_bit_iterator.hpp
        ecc_openssl/openssl_window_method.hpp
        ecc_openssl/openssl_ecc.cpp
        ecc_openssl/edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256k1OpensslNativeEcc.cpp
        ecc_openssl/edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_Sm2P256v1OpensslNativeEcc.cpp
        ecc_openssl/edu_alibaba_mpc4j_common_tool_crypto_ecc_openssl_SecP256r1OpensslNativeEcc.cpp
        ecc_sodium/edu_alibaba_mpc4j_common_tool_crypto_ecc_sodium_X25519SodiumByteMulEcc.cpp
        ecc_sodium/edu_alibaba_mpc4j_common_tool_crypto_ecc_sodium_Ed25519SodiumByteFullEcc.cpp
)

target_include_directories(
        mpc4j-native-tool
        PUBLIC
        /usr/local/include
        common
)
target_link_libraries(
        mpc4j-native-tool
        PUBLIC
        ${GF2X_LIBRARY}
        ${NTL_LIBRARY}
        ${GMP_LIBRARY}
        ${OPENSSL_CRYPTO_LIBRARY}
        ${LIBSODIUM_LIBRARY}
        libmcl.a
)