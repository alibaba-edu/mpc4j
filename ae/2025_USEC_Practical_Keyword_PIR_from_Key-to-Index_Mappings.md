# Artifact Evaluation for Practical Keyword Private Information Retrieval from Key-to-Index Mappings

The submodule `mpc4j-s2pc-pir` (located in `mpc4j/mpc4j-s2pc-pir/`) contains prototype implementations of several Private Information Retrieval (PIR) schemes, including schemes described in the paper "Practical Keyword Private Information Retrieval from Key-to-Index Mappings".

## Source Code Location

- `mpc4j-s2pc-pir/src/main/java/edu/alibaba/mpc4j/s2pc/pir/cppir/ks/simple`  contains the implementations of our three constructions: KPIR$^{\mathsf{kvs}}$ (all source codes start with `SimpleNaive`), KPIR$^{\mathsf{hash}}$ (all source codes start with `SimpleBin`), KPIR$^{\mathsf{index}}$ (all source codes start with `SimplePgm`).
- `mpc4j-s2pc-pir/src/main/java/edu/alibaba/mpc4j/s2pc/pir/cppir/ks/chalamet` contains the implementation of the baseline construction ChalametPIR introduced in the paper ["Call Me By My Name: Simple, Practical Private Information Retrieval for Keyword Queries"](https://eprint.iacr.org/2024/092) (CCS 2024).
- `mpc4j-s2pc-pir/src/test/java/edu/alibaba/mpc4j/s2pc/pir/cppir/ks/` contains unit tests of all client-preprocessing keyword PIR protocols. You can run these unit tests to test the functionalities of our three constructions and the baseline construction ChalametPIR.
- `mpc4j-s2pc-pir/src/test/resources/conf_single_cp_ks_pir_example.conf`  provides an example configuration file that is used for running performance results.

## Unit Tests

We highly recommand running unit tests using [IntellJ IDEA](https://www.jetbrains.com/idea/), the leading Java IDE. You can follow **Section Development** shown in `README.md` to config, compile, and run unit tests of `mpc4j` in IntelliJ IDEA. If you are familar with Java, you can alternatively download, compile and run unit tests with following steps.

1. Clone the repository: `git clone https://github.com/alibaba-edu/mpc4j.git`.
2. Go to the root path: `cd mpc4j`.
3. Package and install: `mvn package`.
4. Run unit tests in `mpc4j-s2pc-pir/src/test/java/edu/alibaba/mpc4j/s2pc/pir/cppir/ks/`. 

We note that `mpc4j-s2pc-pir` module includes several implementations of other PIR schemes. To successfully run all unit tests, you need to follow the guideline shown in `mpc4j-native-fhe/README.md`  to compile `mpc4j-native-fhe`, and specify the native library location using `-Djava.library.path`. See  **Section Development** shown in `README.md`. The implementations of our three constructions and the baseline construction ChalametPIR are purely based on Java. It is not necessary to compile `mpc4j-native-fhe` **if you only want to test these constructions**.

## Performance Tests

To reproduce the performance results shown in the paper, you need to generate the jar file by running `mvn package`, and run it with two progresses on a single machine, or two progresses on two machines connected by the network. You also need to create a config file with suitable parameters. The template is shown in `mpc4j-s2pc-pir/src/test/resources/conf_single_cp_ks_pir_example.conf`. An example is as follows. 

```text
# server information
server_name = server
server_ip = 192.168.1.1
server_port = 9002

# client information
client_name = client
client_ip = 192.168.1.2
client_port = 9003

# append string in the output file
append_string = example

# protocol type
pto_type = SINGLE_CP_KS_PIR

# protocol config
entry_bit_length = 256
server_log_set_size = 22,22,20,18
query_num = 100
parallel = false

# SingleCpKsPir name: SIMPLE_NAIVE, SIMPLE_BIN, PGM_INDEX, or CHALAMET
single_cp_ks_pir_pto_name = PGM_INDEX
```

 ### Run Server

```
java -jar mpc4j-s2pc-pir-1.1.3-jar-with-dependencies.jar CONFIG_FILE_NAME.conf server
```

### Run Client

Please wait for the server to show it is ready and then run the client.

```
java -jar mpc4j-s2pc-pir-1.1.3-jar-with-dependencies.jar CONFIG_FILE_NAME.conf client
```

### Get Results

The performance results are reported in files ending with `.output` in the `temp` path. We note that we report the memory usage in the paper. The tool for reporting the memory usage is [jol](https://openjdk.org/projects/code-tools/jol/), whose license is not compatible with our license. Therefore, we cannot include the memory usage report in the artificate evaluation. If you are familar with Java development, it would be relatively easy to mannually include related codes into our implementation and get the memory usage report. 
