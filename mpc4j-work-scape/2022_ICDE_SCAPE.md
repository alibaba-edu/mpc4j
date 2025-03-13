# Artifact Evaluation for Scape: Scalable Collaborative Analytics System on Private Database with Malicious Security

## Source Code Location

### `mpc4j-s3pc-abb3`
- This module contains the implementation of ["ABY3: A Mixed Protocol Framework for Machine Learning"](https://eprint.iacr.org/2018/403.pdf) and ["Fast large-scale honest-majority MPC for malicious adversaries"](https://eprint.iacr.org/2018/570.pdf), where the latter one is only implemented with long input.
- `mpc4j-s3pc-abb3/src/main/java/edu/alibaba/mpc4j/s3pc/contect` contains the required correlated randomness generator and the multiplication tuple generation protocol.
- `mpc4j-s3pc-abb3/src/main/java/edu/alibaba/mpc4j/s3pc/basic` contains the core functions in three-party replicated secret sharing based computation.
  - `core`: the basic circuit for z2 and zl64 computation.
  - `conversion`: the commonly used conversion functions between arithmetic sharing and binary sharing.
  - `shuffle`: shuffle protocols on secret-shared data.

### `mpc4j-work-scape`
- This module contains the implementation of ["Scape: Scalable Collaborative Analytics System on Private Database with Malicious Security"](https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=9835540).
#### `mpc4j-work-scape/scape-s3pc-opf`
- This module contains the implementation of underlying functions in Scape.
- `./agg`: the aggregation functions for secret-shared table, the functions involve the valid flag which indicate the corresponding rows is valid or not.
- `./merge`: the merge function, which can merge two sorted shared arrays into one sorted array.
- `./permutation`: the permutation functions, which involves a shared permutation $\pi$ (in binary sharing form or arithmetic sharing form) and multiple secret-shared payload array $X$. As the result, the functions output $\pi\cdot X$ or $\pi^{-1} \cdot X$.
- `./pgsort`: the sorting protocols which can generate the permutation representing the order of the input array.
- `./soprp`: the secret-shared oblivious PRP computation protocol
- `./traversal`: the oblivious traversal protocol defined in the paper.

#### `mpc4j-work-scape/scape-s3pc-db`
- This module contains the protocols for various database operations.
- `./groupby`: the group-by protocol.
- `./orderby`: the order-by protocol.
- `./join`: the main part of the Scape, including the general join protocol (Section 4 in our paper), the Pk-Fk join protocol, and the Pk-Pk join protocol (Section 5.A in our paper).
  - `./general`: the general join protocol, which requires an upper bound of the size of the join result.
  - `./pkfk`: the Pk-Fk join protocol, including a table contains with unique join_key and a table with non-unique join_key.
  - `./pkpk`: the Pk-Pk join protocol. 
    - The function support ordered-input (with merge-compare) and disordered-input (with soprp-merge-compare).
    - We further realize the MRR20 protocol and extend it to the malicious secure version. We extend it after our paper is published, and we also release its implementation here.
- `./semijoin`: including the general semi-join and Pk-Pk semi-join protocols, which output a valid flag indicating whether the corresponding row is in the join result.
- `./tools`: some tools for DB operations.

## Unit Tests

We highly recommand running unit tests using [IntellJ IDEA](https://www.jetbrains.com/idea/), the leading Java IDE. You can follow **Section Development** shown in `README.md` to config, compile, and run unit tests of `mpc4j` in IntelliJ IDEA. If you are familar with Java, you can alternatively download, compile and run unit tests with following steps.

1. Clone the repository: `git clone https://github.com/alibaba-edu/mpc4j.git`.
2. Go to the root path: `cd mpc4j`.
3. Package and install: `mvn package`.
4. Run unit tests in `mpc4j-work-scape/scape-s3pc-opf/src/test/java/edu/alibaba/mpc4j/work/s3pc/opf/` and `mpc4j-work-scape/scape-s3pc-opf/src/test/java/edu/alibaba/mpc4j/work/s3pc/db/`. 

We note that `mpc4j-work-scape` module includes several implementations of other native schemes. To successfully run all unit tests, you need to follow the guideline shown in `mpc4j-native-tool/README.md`  to compile `mpc4j-native-tool`, and specify the native library location using `-Djava.library.path`. See  **Section Development** shown in `README.md`.

## Performance Tests

- To reproduce the performance results shown in the paper, you need to generate the jar file by running `mvn package`, and run it with two progresses on a single machine, or three progresses on three machines connected by the network. 
- You also need to create a config file with suitable parameters. The templates are shown in 
  - `mpc4j-s3pc-abb3/src/test/resources` the configure for shuffle and predicate tests
  - `mpc4j-work-scape/scape-s3pc-db/src/test/resources`. the configure for underlying functions tests
  - `mpc4j-work-scape/scape-s3pc-opf/src/test/resources`. the configure for DB operations tests

- An example configure for general join test is as follow:
```text
# first party information
first_name = first
first_ip = 127.0.0.1
first_port = 9001

# second party information
second_name = second
second_ip = 127.0.0.1
second_port = 9002

# third party information
third_name = third
third_ip = 127.0.0.1
third_port = 9003

# append string in the output file
append_string = example

# protocol type
pto_type = GENERAL_JOIN
# protocol name
join_pto_name = GENERAL_JOIN_HZF22

# protocol input configure: payload dimension, input is sorted, log of input size, log of upper bound
payload_dim = 3
is_sorted = true
log_input_size = 8,10,12
log_upper_bound = 9,12,13

# configure
# we provide the simulation mode, which doesn't require pre-generated multiplication tuples.
mt_sim_mode = true
# test the malicious secure protocols
malicious = true
# use CGH18 for multiplication verification for Zl64 data or not
use_mac = false

# the comparator type
comparator_type = TREE_COMPARATOR
```

### Run Parties
Taking the experiments for database operations as an example, we show how to run the first party.

```
java -jar -Xmx80g -Xms80g -ea -Djava.library.path=XXXXX scape-s3pc-db-1.1.4-SNAPSHOT-jar-with-dependencies.jar CONFIG_FILE_NAME.conf first
```
- Please change the path of `-Djava.library.path` with the path in your own machine.
- For the second and the third party, change the `first` to `second` and `third`, respectively.

### Get Results

The performance results are reported in files ending with `.output` in the `temp` path.

### Other evaluation

The users can refer to existing classes in `mpc4j-work-scape/scape-s3pc-db/src/test/java/edu/alibaba/mpc4j/work/scape/s3pc/opf/main` and `mpc4j-work-scape/scape-s3pc-db/src/test/java/edu/alibaba/mpc4j/work/scape/s3pc/db/main` to code the self-defined test class. Then, the users can add test classes into `ScapeOpfMain.java` or `ScapeDbMain.java`, and the program can generate the configure files as above to evaluate them.

