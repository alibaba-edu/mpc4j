# CMAKE generated file: DO NOT EDIT!
# Generated by "Unix Makefiles" Generator, CMake Version 3.28

# Delete rule output on recipe failure.
.DELETE_ON_ERROR:

#=============================================================================
# Special targets provided by cmake.

# Disable implicit rules so canonical targets will work.
.SUFFIXES:

# Disable VCS-based implicit rules.
% : %,v

# Disable VCS-based implicit rules.
% : RCS/%

# Disable VCS-based implicit rules.
% : RCS/%,v

# Disable VCS-based implicit rules.
% : SCCS/s.%

# Disable VCS-based implicit rules.
% : s.%

.SUFFIXES: .hpux_make_needs_suffix_list

# Command-line flag to silence nested $(MAKE).
$(VERBOSE)MAKESILENT = -s

#Suppress display of executed commands.
$(VERBOSE).SILENT:

# A target that is always out of date.
cmake_force:
.PHONY : cmake_force

#=============================================================================
# Set environment variables for the build.

# The shell in which to execute make rules.
SHELL = /bin/sh

# The CMake executable.
CMAKE_COMMAND = /usr/bin/cmake

# The command to remove a file.
RM = /usr/bin/cmake -E rm -f

# Escaping for special characters.
EQUALS = =

# The top-level source directory on which CMake was run.
CMAKE_SOURCE_DIR = /root/mpc4j/mpc4j-native-fourq

# The top-level build directory on which CMake was run.
CMAKE_BINARY_DIR = /root/mpc4j/mpc4j-native-fourq/build

# Include any dependencies generated for this target.
include CMakeFiles/ecc_tests.dir/depend.make
# Include any dependencies generated by the compiler for this target.
include CMakeFiles/ecc_tests.dir/compiler_depend.make

# Include the progress variables for this target.
include CMakeFiles/ecc_tests.dir/progress.make

# Include the compile flags for this target's objects.
include CMakeFiles/ecc_tests.dir/flags.make

CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.o: CMakeFiles/ecc_tests.dir/flags.make
CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.o: /root/mpc4j/mpc4j-native-fourq/tests/ecc_tests.c
CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.o: CMakeFiles/ecc_tests.dir/compiler_depend.ts
	@$(CMAKE_COMMAND) -E cmake_echo_color "--switch=$(COLOR)" --green --progress-dir=/root/mpc4j/mpc4j-native-fourq/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_1) "Building C object CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.o"
	/usr/bin/cc $(C_DEFINES) $(C_INCLUDES) $(C_FLAGS) -MD -MT CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.o -MF CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.o.d -o CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.o -c /root/mpc4j/mpc4j-native-fourq/tests/ecc_tests.c

CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color "--switch=$(COLOR)" --green "Preprocessing C source to CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.i"
	/usr/bin/cc $(C_DEFINES) $(C_INCLUDES) $(C_FLAGS) -E /root/mpc4j/mpc4j-native-fourq/tests/ecc_tests.c > CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.i

CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color "--switch=$(COLOR)" --green "Compiling C source to assembly CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.s"
	/usr/bin/cc $(C_DEFINES) $(C_INCLUDES) $(C_FLAGS) -S /root/mpc4j/mpc4j-native-fourq/tests/ecc_tests.c -o CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.s

CMakeFiles/ecc_tests.dir/tests/test_extras.c.o: CMakeFiles/ecc_tests.dir/flags.make
CMakeFiles/ecc_tests.dir/tests/test_extras.c.o: /root/mpc4j/mpc4j-native-fourq/tests/test_extras.c
CMakeFiles/ecc_tests.dir/tests/test_extras.c.o: CMakeFiles/ecc_tests.dir/compiler_depend.ts
	@$(CMAKE_COMMAND) -E cmake_echo_color "--switch=$(COLOR)" --green --progress-dir=/root/mpc4j/mpc4j-native-fourq/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_2) "Building C object CMakeFiles/ecc_tests.dir/tests/test_extras.c.o"
	/usr/bin/cc $(C_DEFINES) $(C_INCLUDES) $(C_FLAGS) -MD -MT CMakeFiles/ecc_tests.dir/tests/test_extras.c.o -MF CMakeFiles/ecc_tests.dir/tests/test_extras.c.o.d -o CMakeFiles/ecc_tests.dir/tests/test_extras.c.o -c /root/mpc4j/mpc4j-native-fourq/tests/test_extras.c

CMakeFiles/ecc_tests.dir/tests/test_extras.c.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color "--switch=$(COLOR)" --green "Preprocessing C source to CMakeFiles/ecc_tests.dir/tests/test_extras.c.i"
	/usr/bin/cc $(C_DEFINES) $(C_INCLUDES) $(C_FLAGS) -E /root/mpc4j/mpc4j-native-fourq/tests/test_extras.c > CMakeFiles/ecc_tests.dir/tests/test_extras.c.i

CMakeFiles/ecc_tests.dir/tests/test_extras.c.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color "--switch=$(COLOR)" --green "Compiling C source to assembly CMakeFiles/ecc_tests.dir/tests/test_extras.c.s"
	/usr/bin/cc $(C_DEFINES) $(C_INCLUDES) $(C_FLAGS) -S /root/mpc4j/mpc4j-native-fourq/tests/test_extras.c -o CMakeFiles/ecc_tests.dir/tests/test_extras.c.s

# Object files for target ecc_tests
ecc_tests_OBJECTS = \
"CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.o" \
"CMakeFiles/ecc_tests.dir/tests/test_extras.c.o"

# External object files for target ecc_tests
ecc_tests_EXTERNAL_OBJECTS =

ecc_tests: CMakeFiles/ecc_tests.dir/tests/ecc_tests.c.o
ecc_tests: CMakeFiles/ecc_tests.dir/tests/test_extras.c.o
ecc_tests: CMakeFiles/ecc_tests.dir/build.make
ecc_tests: lib/libfourq.so
ecc_tests: CMakeFiles/ecc_tests.dir/link.txt
	@$(CMAKE_COMMAND) -E cmake_echo_color "--switch=$(COLOR)" --green --bold --progress-dir=/root/mpc4j/mpc4j-native-fourq/build/CMakeFiles --progress-num=$(CMAKE_PROGRESS_3) "Linking C executable ecc_tests"
	$(CMAKE_COMMAND) -E cmake_link_script CMakeFiles/ecc_tests.dir/link.txt --verbose=$(VERBOSE)

# Rule to build all files generated by this target.
CMakeFiles/ecc_tests.dir/build: ecc_tests
.PHONY : CMakeFiles/ecc_tests.dir/build

CMakeFiles/ecc_tests.dir/clean:
	$(CMAKE_COMMAND) -P CMakeFiles/ecc_tests.dir/cmake_clean.cmake
.PHONY : CMakeFiles/ecc_tests.dir/clean

CMakeFiles/ecc_tests.dir/depend:
	cd /root/mpc4j/mpc4j-native-fourq/build && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /root/mpc4j/mpc4j-native-fourq /root/mpc4j/mpc4j-native-fourq /root/mpc4j/mpc4j-native-fourq/build /root/mpc4j/mpc4j-native-fourq/build /root/mpc4j/mpc4j-native-fourq/build/CMakeFiles/ecc_tests.dir/DependInfo.cmake "--color=$(COLOR)"
.PHONY : CMakeFiles/ecc_tests.dir/depend
