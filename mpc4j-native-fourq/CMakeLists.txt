cmake_minimum_required(VERSION 3.10)
project(fourq_lib VERSION 3.1)


ENABLE_LANGUAGE(ASM)
# C编译指令
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wfatal-errors -fPIC -Wno-ignored-attributes -pthread -Wall -funroll-loops")
message("Platform: ${CMAKE_SYSTEM_PROCESSOR}")

# 不同平台使用不同的编译指令 和文件 AMD64/ ARM64/
IF (${CMAKE_SYSTEM_PROCESSOR} MATCHES "(aarch64)|(arm64)")

    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=armv8-a+simd+crypto+crc -D _ARM64_")
    file(GLOB_RECURSE PLATFORM_SOURCES "ARM64/*.c")
ELSE ()
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -march=native -mrdseed -maes -msse2 -msse4.1 -mpclmul -D _AMD64_")
    # file(GLOB_RECURSE PLATFORM_SOURCES "AMD64/*.c" "AMD64/*.S" "AMD64/*.s")
    file(GLOB_RECURSE PLATFORM_SOURCES "AMD64/*.c")
ENDIF ()

# 默认为 Release 编译
if (NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif (NOT CMAKE_BUILD_TYPE)

message(STATUS "Build type (CMAKE_BUILD_TYPE): ${CMAKE_BUILD_TYPE}")

# 如果是Release 编译 则使用优化 
if (CMAKE_BUILD_TYPE MATCHES Release)
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -D __LINUX__") # 这里是在 前面的 CMAKE_C_FLAGS 后面追加新的编译命令 , 注意是 O3 不是 03
endif ()

# 添加待编译的文件 这里是把当前目录下所有的
# file(GLOB_RECURSE FOURQ_SOURCES "*.c")
file(GLOB_RECURSE FOURQ_HEADERS "*.h") # 所有的头文件放在一起

# Set the library output directory
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/lib)

add_library(
        fourq
        SHARED
        ${PLATFORM_SOURCES}
        eccp2.c
        eccp2_core.c
        eccp2_no_endo.c
        hash_to_curve.c
        kex.c
        schnorrq.c
        crypto_util.c
        random/random.c
        sha512/sha512.c
)

# enable test
enable_testing()
# add crypto_tests
add_executable(
        crypto_tests
        tests/crypto_tests.c
        tests/test_extras.c
)
target_link_libraries(
        crypto_tests
        fourq
)
add_test(crypto_tests crypto_tests)
# add ecc_tests
add_executable(
        ecc_tests
        tests/ecc_tests.c
        tests/test_extras.c
)
target_link_libraries(
        ecc_tests
        fourq
)
add_test(ecc_tests ecc_tests)
# ecc_tests return bool = true if passed
set_property(TEST ecc_tests PROPERTY WILL_FAIL true)
# add fp_tests
add_executable(
        fp_tests
        tests/fp_tests.c
        tests/test_extras.c
)
target_link_libraries(
        fp_tests
        fourq
)
add_test(fp_tests fp_tests)
# fp_tests return bool = true if passed
set_property(TEST fp_tests PROPERTY WILL_FAIL true)

# Set the include directories
# target_include_directories(fourq_shared PUBLIC ${CMAKE_CURRENT_BINARY_DIR}/include)
include(GNUInstallDirs)
# 安装动态库文件和头文件 到系统目录
# 执行 make install 会默认安装到 /usr/local/ 下对应的目录
install(TARGETS fourq DESTINATION ${CMAKE_INSTALL_LIBDIR})
# note that you must include(GNUInstallDirs) , then you can use this ${CMAKE_INSTALL_INCLUDEDIR}.
install(FILES ${FOURQ_HEADERS} DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}) 

