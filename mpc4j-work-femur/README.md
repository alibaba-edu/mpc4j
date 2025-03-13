# FEMUR

A Flexible Framework for Fast and Secure Querying from Public Key-Value Store.

## Testing with the Source Code

### Install mpc4j Library

Install the open-source library mpc4j (v1.1.4) into your local Maven repository with the following step:

1. Clone mpc4j: `git clone https://github.com/alibaba-edu/mpc4j.git`.
2. Goto the root path of mpc4j: `cd mpc4j`.
3. Package and install: `mvn install`.

### Install SEAL Library

Follow the guideline in femur-native-fhe dictionary to compile femur-native-fhe (`femur-native-fhe/README.md`). Then, you need to assign the native library location using `-Djava.library.path`.

For the femur-rpc submodule, we implemented our algorithms by storing the database in memory. Once you have completed the above settings, you can proceed to run the unit tests to verify the correctness and stability of the algorithms.

For the femur-service submodule, we implemented our algorithms by storing the database in Redis. To run the unit tests in femur-service submodule, you need to continue with the subsequent command.

### Start Redis

Ensure that Redis is installed on your system. Then, start the redis-server before running the unit tests.

## Testing with the jar

You can generate the jar file by running mvn package in the root path of the source code. To run the project using the JAR file, execute the following command:

For the femur-rpc submodule, 

1. Run the server

   ```
   java -jar -Djava.library.path=YOUR_FEMUR_NATIVE_FHE_LIB_PATH femur-rpc-1.1.4-SNAPSHOT-jar-with-dependencies.jar femur_conf.conf server
   ```

2. Wait for the server to show it is ready and then start the client

   ```
   java -jar -Djava.library.path=YOUR_FEMUR_NATIVE_FHE_LIB_PATH femur-rpc-1.1.4-SNAPSHOT-jar-with-dependencies.jar femur_conf.conf client
   ```

For the femur-service submodule,

1. Firstly start Redis by the command redis-server, then run the server

   ```
   java -jar -Djava.library.path=YOUR_FEMUR_NATIVE_FHE_LIB_PATH femur-service-1.1.4-SNAPSHOT-jar-with-dependencies.jar femur_conf.conf server
   ```

2. Wait for the server to show it is ready and then start the client

   ```
   java -jar -Djava.library.path=YOUR_FEMUR_NATIVE_FHE_LIB_PATH femur-service-1.1.4-SNAPSHOT-jar-with-dependencies.jar femur_conf.conf client
   ```

The example config files are shown in the dictionary `resources/` of both submodules respectively.