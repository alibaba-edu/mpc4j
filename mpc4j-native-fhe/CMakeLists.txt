cmake_minimum_required(VERSION 3.10)
project(mpc4j_native_fhe)

# Import Microsoft SEAL
find_package(SEAL REQUIRED)
if(NOT SEAL_FOUND)
    message(FATAL_ERROR "Microsoft SEAL: not found")
else()
    message(STATUS "Microsoft SEAL: ${SEAL_DIR}")
endif()

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


# 构建mpc4j_native_fhe库
add_library(
        mpc4j-native-fhe
        SHARED
        serialize.cpp
        apsi.cpp
        polynomials.cpp
        utils.cpp
        index_pir.cpp
        random.cpp
        tfhe/tfhe.cpp
        tfhe/util.cpp
        tfhe/util.h
        upso/edu_alibaba_mpc4j_s2pc_upso_upsi_cmg21_Cmg21UpsiNativeUtils.cpp
        upso/edu_alibaba_mpc4j_s2pc_upso_ucpsi_sj23_peqt_Sj23PeqtUcpsiNativeUtils.cpp
        upso/edu_alibaba_mpc4j_s2pc_upso_ucpsi_sj23_pdsm_Sj23PdsmUcpsiNativeUtils.cpp
        pir/edu_alibaba_mpc4j_work_psipir_Lpzl24BatchPirNativeUtils.cpp
        upso/edu_alibaba_mpc4j_s2pc_upso_upsu_tcl23_Tcl23UpsuNativeUtils.cpp
        pir/std/edu_alibaba_mpc4j_s2pc_pir_stdpir_index_xpir_XpirStdIdxPirNativeUtils.cpp
        pir/std/edu_alibaba_mpc4j_s2pc_pir_stdpir_index_seal_SealStdIdxPirNativeUtils.cpp
        pir/std/edu_alibaba_mpc4j_s2pc_pir_stdpir_index_mul_MulStdIdxPirNativeUtils.cpp
        pir/std/edu_alibaba_mpc4j_s2pc_pir_stdpir_index_onion_OnionStdIdxPirNativeUtils.cpp
        pir/std/edu_alibaba_mpc4j_s2pc_pir_stdpir_index_vectorized_VectorizedStdIdxPirNativeUtils.cpp
        pir/std/edu_alibaba_mpc4j_s2pc_pir_stdpir_index_cw_CwStdIdxPirNativeUtils.cpp
        pir/std/edu_alibaba_mpc4j_s2pc_pir_stdpir_index_fast_FastStdIdxPirNativeUtils.cpp
        pir/std/edu_alibaba_mpc4j_s2pc_pir_stdpir_ks_labelpsi_LabelpsiStdKsPirNativeUtils.cpp
        pir/std/edu_alibaba_mpc4j_s2pc_pir_stdpir_kw_pantheon_PantheonStdKwPirNativeUtils.cpp
)
# 链接SEAL库
if(TARGET SEAL::seal)
    target_link_libraries(mpc4j-native-fhe PUBLIC SEAL::seal)
elseif(TARGET SEAL::seal_shared)
    target_link_libraries(mpc4j-native-fhe PUBLIC SEAL::seal_shared)
else()
    message(FATAL_ERROR "Cannot find target SEAL::seal or SEAL::seal_shared")
endif()