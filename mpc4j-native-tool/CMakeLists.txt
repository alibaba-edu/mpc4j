cmake_minimum_required(VERSION 3.10)
project(mpc4j-native-tool)

set(CMAKE_CXX_STANDARD 17)
# 增加汇编指令支持（参见：https://www.zhihu.com/question/61037115）
ENABLE_LANGUAGE(ASM)
# C编译指令
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wfatal-errors -fPIC -Wno-ignored-attributes -pthread -Wall -funroll-loops")
message("Platform: ${CMAKE_SYSTEM_PROCESSOR}")
IF(${CMAKE_SYSTEM_PROCESSOR} MATCHES "(aarch64)|(arm64)")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv8-a+simd+crypto+crc")
ELSE()
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=native -mrdseed -maes -msse2 -msse4.1 -mpclmul")
ENDIF()
# 默认为Release编译
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()
message(STATUS "Build type (CMAKE_BUILD_TYPE): ${CMAKE_BUILD_TYPE}")
# 如果为debug模式，则不使用优化，增加调试编译指令
if (CMAKE_BUILD_TYPE MATCHES Debug)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O0 -ggdb")
endif()
# 如果为release模式，则使用优化
if (CMAKE_BUILD_TYPE MATCHES Release)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3")
endif()
# 设置C++编译指令
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${CMAKE_C_FLAGS}")
# 打印C/C++编译指令
message(STATUS "C Flags (CMAKE_C_FLAGS): ${CMAKE_C_FLAGS}")
message(STATUS "CXX Flags (CMAKE_CXX_FLAGS): ${CMAKE_CXX_FLAGS}")
# 设置默认路径
set(DEFAULT_DIR "/usr/local")

# 引入NTL
if(NOT DEFINED NTL_ROOT_DIR)
    set(NTL_ROOT_DIR ${DEFAULT_DIR})
endif()
find_path(NTL_INCLUDE_DIR
        NAMES NTL/ZZ.h
        HINTS ENV NTL_ROOT_DIR
        PATH_SUFFIXES include
        DOC "The directory containing the NTL include files"
        )
find_library(NTL_LIBRARY
        NAMES ntl
        HINTS ENV NTL_ROOT_DIR
        PATH_SUFFIXES lib
        DOC "Path to the NTL library"
        REQUIRED
        )
if(NTL_INCLUDE_DIR AND NTL_LIBRARY)
    set(NTL_FOUND TRUE)
endif()
if(NTL_FOUND)
    include_directories(${NTL_INCLUDE_DIRS})
    message(STATUS "NTL_INCLUDE_DIRS: ${NTL_INCLUDE_DIR}")
    message(STATUS "NTL_LIBRARY_PATH: ${NTL_LIBRARY}")
else(NTL_FOUND)
    if (NTL_FIND_REQUIRED)
        message(FATAL_ERROR "Could not find NTL")
    endif()
endif()

# 引入GMP
if(NOT DEFINED GMP_ROOT_DIR)
    set(GMP_ROOT_DIR ${DEFAULT_DIR})
endif()
find_path(GMP_INCLUDE_DIR
        NAMES gmp.h
        HINTS ENV GMP_ROOT_DIR
        PATH_SUFFIXES include
        DOC "The directory containing the GMP include files"
        )
find_library(GMP_LIBRARY
        NAMES gmp
        HINTS ENV GMP_ROOT_DIR
        PATH_SUFFIXES lib
        DOC "Path to the GMP library"
        REQUIRED
        )
if(GMP_INCLUDE_DIR AND GMP_LIBRARY)
    set(GMP_FOUND TRUE)
endif()
if(GMP_FOUND)
    include_directories(${GMP_INCLUDE_DIRS})
    message(STATUS "GMP_INCLUDE_DIRS: ${GMP_INCLUDE_DIR}")
    message(STATUS "GMP_LIBRARY_PATH: ${GMP_LIBRARY}")
else(GMP_FOUND)
    if(GMP_FIND_REQUIRED)
        message(FATAL_ERROR "Could not find GMP")
    endif()
endif()

# 引入libsodium
if(NOT DEFINED LIBSODIUM_ROOT_DIR)
    if (${CMAKE_SYSTEM_NAME} MATCHES "Darwin")
        set(LIBSODIUM_ROOT_DIR "/opt/homebrew/opt/libsodium")
    else()
        set(LIBSODIUM_ROOT_DIR ${DEFAULT_DIR})
    endif()
endif()
find_path(LIBSODIUM_INCLUDE_DIR
        NAMES sodium.h
        HINTS ENV LIBSODIUM_ROOT_DIR
        PATH_SUFFIXES include
        DOC "The directory containing the libsodium include files"
        )
find_library(LIBSODIUM_LIBRARY
        NAMES sodium
        HINTS ENV LIBSODIUM_ROOT_DIR
        PATH_SUFFIXES lib
        DOC "Path to the libsodium library"
        REQUIRED
        )
if (LIBSODIUM_INCLUDE_DIR AND LIBSODIUM_LIBRARY)
    set(LIBSODIUM_FOUND TRUE)
endif()
if(LIBSODIUM_FOUND)
    include_directories(${LIBSODIUM_INCLUDE_DIR})
    message(STATUS "LIBSODIUM_INCLUDE_DIRS: ${LIBSODIUM_INCLUDE_DIR}")
    message(STATUS "LIBSODIUM_LIBRARY_PATH: ${LIBSODIUM_LIBRARY}")
else()
    if(LIBSODIUM_FIND_REQUIRED)
        message(FATAL_ERROR "Could not find libsodium")
    endif()
endif()

# 寻找JNI路径并引入JNI目录
if(NOT DEFINED ENV{JAVA_HOME})
    # 没有找到JAVA_HOME环境变量，输出错误信息退出
    message(FATAL_ERROR "not defined environment variable:JAVA_HOME")
endif()
if (APPLE)
    set(JNI_INCLUDE_DIRS "$ENV{JAVA_HOME}/include")
    include_directories(${JNI_INCLUDE_DIRS})
    include_directories(${JNI_INCLUDE_DIRS}/darwin)
elseif(UNIX)
    set(JNI_INCLUDE_DIRS "$ENV{JAVA_HOME}/include")
    include_directories(${JNI_INCLUDE_DIRS})
    include_directories(${JNI_INCLUDE_DIRS}/linux)
endif()
message(STATUS "JNI_INCLUDE_DIRS:  ${JNI_INCLUDE_DIRS}")

# 引入openssl
if (${CMAKE_SYSTEM_NAME} MATCHES "Darwin")
    if(NOT DEFINED OPENSSL_ROOT_DIR)
        if(${CMAKE_SYSTEM_PROCESSOR} MATCHES "(aarch64)|(arm64)")
            # M1 Apple
            set(OPENSSL_ROOT_DIR "/opt/homebrew/opt/openssl")
        else()
            # Intel Apple
            set(OPENSSL_ROOT_DIR "/usr/local/opt/openssl")
        endif()
    endif()
endif()
find_package(OpenSSL REQUIRED)
if(OPENSSL_FOUND)
    include_directories(${OPENSSL_INCLUDE_DIR})
    message(STATUS "OPENSSL_INCLUDE_DIRS: ${OPENSSL_INCLUDE_DIR}")
    message(STATUS "OPENSSL_LIBRARY_PATH: ${OPENSSL_LIBRARIES}")
else()
    if(OPENSSL_FIND_REQUIRED)
        message(FATAL_ERROR "Could not find openssl")
    endif()
endif()

# 引入MCL，如果使用find_library，不知为何会找到libntl，因此这里使用绝对位置
if(NOT DEFINED MCL_ROOT_DIR)
    set(MCL_ROOT_DIR ${DEFAULT_DIR})
endif()
find_path(MCL_INCLUDE_DIR
        NAMES mcl/bn.h
        HINTS ENV MCL_ROOT_DIR
        PATH_SUFFIXES include
        DOC "The directory containing the mcl include files"
        )
set(MCL_LIBRARY ${MCL_ROOT_DIR}/lib/libmcl.a)
if (MCL_INCLUDE_DIR AND MCL_LIBRARY)
    set(MCL_FOUND TRUE)
endif()
if(MCL_FOUND)
    include_directories(${MCL_INCLUDE_DIR})
    message(STATUS "MCL_INCLUDE_DIRS: ${MCL_INCLUDE_DIR}")
    message(STATUS "MCL_LIBRARY_PATH: ${MCL_LIBRARY}")
else()
    if(MCL_FIND_REQUIRED)
        message(FATAL_ERROR "Could not find MCL")
    endif()
endif()

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
        ntl_poly/ntl_tree_zp.h
        ntl_poly/ntl_tree_zp.cpp
        ntl_poly/edu_alibaba_mpc4j_common_tool_polynomial_zp_NtlTreeZpPoly.cpp
        ntl_poly/ntl_zp_util.cpp
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
        ${MCL_LIBRARY}
)