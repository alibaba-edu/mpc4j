# OpBoost

## Introduction

This is the implementation of our paper "OpBoost: A Vertical Federated Tree Boosting Framework Based on Order-Preserving Desensitization" (manuscript).

## How to Run

### Configuration File

Before running the performance tests, you need to first create two configuration files, one for the host party and the other for the slave party. The following properties should be included in the configuration files:

- `task_type`: candidates are `REG_OP_GRAD_BOOST`, `REG_OP_XG_BOOST`, `CLS_OP_GRAD_BOOST`, `CLS_OP_XG_BOOST`.
- `host_name`, `host_ip`, `host_port`: Host party information.
- `slave_name`, `slave_ip`, `slave_port`: Slave party information.
- `own_name`: must be the assigned value of `host_name` or `slave_name`.
- `dataset_name`, `train_dataset_path`, `test_dataset_path`: dataset information. Train/test datasets must be in `csv` format with a header in the first row and the delimiter `,`.
- `tree_num`, `max_depth`, `shrinkage`: training parameters.
- `column_names`: column names, split by `,`. The number of names must match the number of columns in datasets.
- `column_types`: `N` for a nominal column, `I` for an integer column, `D` for a float/double column, and `C` for a classification column (only in classification).
- `formula`: label column name.
- `class_types`: (only in classification) class types.
- `total_round`: test round.
- `party_columns`: 1 for the slave party, 0 for the host party. Label column must belong to the host. 
- `ldp_columns`: 1 for LDP, 0 for nothing.
- `epsilon`: $\epsilon$ values in the test, split by `,`.
- `theta`: $\theta$ values in the test,  split by `,`.
- `alpha`: $alpha$ values in the test, split by`,`.

### Example: PowerPlant (Regression)

See `conf_opboost_reg_powerplant_host.txt` and `conf_opboost_reg_powerplant_slave.txt` in `conf` as examples. Here we attach the configuration files and translate comments into English. The configuration file for the host party is as follows:

```text
# Task Type, REG_OP_GRAD_BOOST / REG_OP_XG_BOOST
task_type = REG_OP_GRAD_BOOST
# task_type = REG_OP_XG_BOOST

# Host party information, here we use local network
host_name = host
host_ip = 127.0.0.1
host_port = 9000
# Slave party information, here we use local network
slave_name = slave
slave_ip = 127.0.0.1
slave_port = 9001
# Own party
own_name = host

# dataset information
dataset_name = powerplant
train_dataset_path = data/regression/powerplant/powerplant10_train.csv
test_dataset_path = data/regression/powerplant/powerplant10_test.csv
# column name
column_names = AT,V,AP,RH,PE
# column types, I for integer column, D for float/double column
column_types = I,I,I,I,D
# label column name
formula = PE

# test round
total_round = 10

# the number of iterations (trees)
tree_num = 80
# the maximum depth of the tree
max_depth = 3
# the shrinkage parameter in (0, 1] controls the learning rate of procedure.
shrinkage = 0.1

# columns belong to which parties, 0 for the host party, 1 for the slave party.
party_columns = 1,1,1,1,0
# whether add LDP noice, 1 for yes, 0 for no.
ldp_columns = 1,1,1,1,0

# ε, float number
epsilon = 0.01,0.02,0.04,0.08,0.16,0.32,0.64,1.28,2.56,5.12
# θ (only for LocalMap and AdjMap), int number
theta = 2,4
# α (only for AdjMap), float number
alpha = 0.4,0.6,0.8,1,2,5,10
```

The configuration file for the slave party is almost identical, except `own_name = slave`.

### Example: PenDigits (Classification)

See `conf_opboost_cls_digits_host.txt` and `conf_opboost_cls_digits_slave.txt` in `conf` as examples. Here we attach the configuration files and translate comments into English. The configuration file for the host party is as follows:

```text
# Task Type, CLS_OP_GRAD_BOOST / CLS_OP_XG_BOOST
task_type = CLS_OP_GRAD_BOOST
# task_type = CLS_OP_XG_BOOST

# Host party information, here we use local network
host_name = host
host_ip = 127.0.0.1
host_port = 9000
# Slave party information, here we use local network
slave_name = slave
slave_ip = 127.0.0.1
slave_port = 9001
# Own party
own_name = host

# dataset information
dataset_name = pen_digits
train_dataset_path = data/classification/pen_digits/pen_digits10_train.csv
test_dataset_path = data/classification/pen_digits/pen_digits10_test.csv
# column name
column_names = F1,F2,F3,F4,F5,F6,F7,F8,F9,F10,F11,F12,F13,F14,F15,F16,class
# column types, I for integer column, C for label column.
column_types = I,I,I,I,I,I,I,I,I,I,I,I,I,I,I,I,C
# label column name
formula = class
# label class type, support String class type
class_types = 0,1,2,3,4,5,6,7,8,9

# test round
total_round = 10

# the number of iterations (trees)
tree_num = 80
# the maximum depth of the tree
max_depth = 3
# the shrinkage parameter in (0, 1] controls the learning rate of procedure.
shrinkage = 0.1

# columns belong to which parties, 0 for the host party, 1 for the slave party.
party_columns = 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0
# whether add LDP noice, 1 for yes, 0 for no.
ldp_columns = 1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0

# ε, float number
epsilon = 0.01,0.02,0.04,0.08,0.16,0.32,0.64,1.28,2.56,5.12
# θ (only for LocalMap and AdjMap), int number
theta = 2,4
# α (only for AdjMap), float number
alpha = 0.4,0.6,0.8,1,2,5,10
```

The configuration file for the slave party is almost identical, except `own_name = slave`.

### Other Examples

See other files in `conf` for performance test configurations used in the paper. See README.md in `mpc4j/data` for detailed information on where our test data come from.

### Run

After having configuration files, you can start to do the test by just running `jar` with configuration files on two platforms or on two terminals from one platform. 

Assume

1. You compile and get the `jar` file with the name `mpc4j-sml-opboost-X.X.X-jar-with-dependencies.jar`. 
2. You write two valid configuration files `conf_XXX_host.txt` for the host party and `conf_XXX_slave.txt` for the slave party. 
3. You have the train dataset `train.csv` and the test dataset `test.csv` with proper `csv` format.

Then:

1. Put all these files into the same location.
2. In  `conf_XXX_host.txt` and `conf_XXX_slave.txt`, set `train_dataset_path = train.csv` and `test_dataset_path = test.csv`.
3. Separately run the following commands on distinct terminals:

```shell
java -jar mpc4j-sml-opboost-X.X.X-jar-with-dependencies.jar conf_XXX_host.txt
java -jar mpc4j-sml-opboost-X.X.X-jar-with-dependencies.jar conf_XXX_slave.txt
```

Finally:

1. The terminals would show log information.
2. You would get two additional files. One (name ends with 0) is the result in the host party. The other (name ends with 1) is the result for the slave party.

## XGBoost4j on Apple Silicon M1

[XGBoost4j](https://xgboost.readthedocs.io/en/stable/jvm/index.html)是著名决策树机器学习库[XGBoost](https://xgboost.readthedocs.io/en/stable/index.html)的Java/Scala封装版本，允许开发人员应用Java语言调用XGBoost机器学习算法，实现模型的训练和预测。XGBoost4j包含Linux、MacOS、Windows的x86_64环境支持。经过实验，XGBoost4j不支持arrch64环境，这意味着XGBoost4j无法在MacBook Pro M1等环境下应用。

本问题最初由Martin Treurnicht于2021年12月9日在XGBoost4j的[GitHub网站](https://github.com/dmlc/xgboost)中通过提交Issue的方式指出。在问题[XGBoost4j missing dylib for apple sillicon m1 \#7501](https://github.com/dmlc/xgboost/issues/7501)下，Martin Treurnicht指出：

> Getting the following error when trying to run our model tests.
> ```text
> java.lang.ExceptionInInitializerError
>	at ml.dmlc.xgboost4j.java.Booster.init(Booster.java:694)
>	at ml.dmlc.xgboost4j.java.Booster.<init>(Booster.java:51)
>	at ml.dmlc.xgboost4j.java.Booster.loadModel(Booster.java:80)
>	at ml.dmlc.xgboost4j.java.XGBoost.loadModel(XGBoost.java:77)
> ```
> Seems like the root cause at the bottom of the stacktrace is a missing dylib:
> ```text
> Caused by: java.io.FileNotFoundException: File /lib/macos/aarch64/libxgboost4j.dylib was not found inside JAR.
>   at ml.dmlc.xgboost4j.java.NativeLibLoader.createTempFileFromResource(NativeLibLoader.java:233)
>   at ml.dmlc.xgboost4j.java.NativeLibLoader.loadLibraryFromJar(NativeLibLoader.java:176)
>   at ml.dmlc.xgboost4j.java.NativeLibLoader.initXGBoost(NativeLibLoader.java:130)
>   at ml.dmlc.xgboost4j.java.XGBoostJNI.<clinit>(XGBoostJNI.java:34)
>   ... 88 more
> ```

Adam Pocock在回复中指出：

> You can compile xgboost4j from source on an M1 machine and it works. I did this last year when they came out on my personal M1 MBP, but without build resources to make the binaries it's hard to deploy.

这意味着可以在M1下通过源代码编译XGBoost4j，从而在aarch64下成功安装XGBoost4j。本文总结了M1下XGBoost4j的安装过程，供参考。

### 本机环境

macOS Monterey (12.1)

- MacBook Pro (16英寸，2021年)
- 芯片：Apple M1 Pro
- 内存：16GB

### 安装`homebrew`

MacOS的`homebrew`对应的是Ubuntu下面的`apt-get`命令，CentOS下面的`yum`命令，是MacOS下非常好用的软件库安装命令。[Homebrew官方网站](https://brew.sh/)介绍了如何安装`homebrew`。直接执行下述命令即可完成安装。

```shell
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

上述命令执行效率可能比较慢，需要等待比较长的实践。

### 安装`libomp`

根据XGBoost4j官方网站中的[Building From Source](https://xgboost.readthedocs.io/en/stable/build.html)介绍，在MacOS下编译XGBoost4j需要安装`libomp`。安装好`homebrew`后，直接执行下述指令即可完成安装。

```shell
brew install libomp
```

### 安装Python3

参考[Can not save Pipeline model in Spark Apache 3.2.0 with xgboost4j-spark_2.12-1.3.1 -1.4.1 - 1.5; they are not compatible with Spark 3.2.0](https://github.com/dmlc/xgboost/issues/7384)，编译XGBoost4j需要使用Python3。如果使用Python2，则后续编译会报错：

```text
File "create_jni.py", line 125 run(f'"{sys.executable}" mapfeat.py') ^ SyntaxError: invalid syntax [ERROR] Command execution failed.
```

安装好`homebrew`后，直接执行下述指令即可完成Python3的安装。

```shell
brew install python3
```

安装完毕后，执行下述指令，查看Python是否为Python3：

```shell
python --version
```

如果不为Python3，参考[brew install doesn't link python3](https://stackoverflow.com/questions/51885394/brew-install-doesnt-link-python3)，执行下述指令打开shell配置文件：

```shell
vim ~/.bash_profile
```

添加下述语句：

```shell
export PATH=/usr/local/opt/python/libexec/bin:$PATH
```

再执行`python --version`，即可看到Python已切换为Python3。

### 下载并编译源代码

XGBoost4j官方网站中的[Building From Source](https://xgboost.readthedocs.io/en/stable/build.html)页面介绍了如何下载源代码并编译。然而，此页面下载的源代码版本过新。我们需要下载已经官方发布的正式版本，否则非aarch64环境也需要源代码编译。查找[MVN Repository](https://mvnrepository.com/artifact/ml.dmlc/xgboost4j)可知目前官方发布的最新版本为1.5.2。执行下述命令，下载1.5.1版本的代码。

> 经过尝试，1.5.1版本代码编译得到的是1.5.2版本。

```shell
git clone --recursive -b release-1.5.1 https://github.com/dmlc/xgboost
```

此源代码不能使用JDK 8编译，否则会报下述错误，原因参考[NoSuchMethodError on JDK8 with Clojure](https://github.com/lmdbjava/lmdbjava/issues/116)。

```text
java.lang.NoSuchMethodError: java.nio.ByteBuffer.clear()Ljava/nio/ByteBuffer;
```

此源代码也不能使用JDK 17编译，否则会报下述错误。

```text
/packages cannot be represented as URI
```

如前所述，如果使用Python2编译，会报下述错误。

```text
File "create_jni.py", line 125 run(f'"{sys.executable}" mapfeat.py') ^ SyntaxError: invalid syntax [ERROR] Command execution failed.
```

经过测试，JDK 11可以编译通过。为此，我们通过IntelliJ IDEA下载并安装了`azul-11.0.14.1`版本的JDK 11，此版本支持aarch64环境。使用IntelliJ IDEA下载并安装的`azul-11.0.14.1`路径为`/Users/XXX/Library/Java/JavaVirtualMachines/azul-11.0.14.1/Contents/Home/`，其中XXX为MacOS的用户名。 我们需要将JAVA_HOME临时替换为JDK 11的路径，执行下述命令：

```shell
export JAVA_HOME=/Users/XXX/Library/Java/JavaVirtualMachines/azul-11.0.14.1/Contents/Home/
```

执行完毕后，尝试运行`java -version`，应可看到Java版本被临时切换为JDK 11。

```text
openjdk version "11.0.14.1" 2022-02-08 LTS
OpenJDK Runtime Environment Zulu11.54+25-CA (build 11.0.14.1+1-LTS)
OpenJDK 64-Bit Server VM Zulu11.54+25-CA (build 11.0.14.1+1-LTS, mixed mode)
```

根据官方网站描述执行下述命令，即可编译并成功安装支持aarch64的XGBoost4j。

```shell
mvn -DskipTests=true install
```

### 同时支持x86_64和aarch64的动态库

本机编译成功得到XGBoost4j的jar包中只包含支持aarch64的动态库文件，位于jar包中的`lib/macos/aarch64`路径中。这意味着，如果将整个工程打包成一个jar包并直接迁移到x86_64平台上，是无法运行的。

为此，我们将x86_64的动态库合并到了本机编译成功的jar包中。其中，`lib/linux/x86_64`存放了Linux的x86_64平台所需的动态库、`lib/macos/x86_64`存放了MacOS的x86_64平台所需的动态库、`lib/windows/x86_64`存放了Windows的x86_64平台所需的动态库。此jar包存放在`mpc4j/mpc4j-sml-opboost/lib`路径下，可直接获取并使用。

### XGBoost4j模型序列化问题

编译XGBoost4j的2.0.0-SNAPSHOT版本并使用后发现，2.0.0之后的版本将有三种可能的模型序列化方法：Universal Binary JSON、JSON以及未来将要被废弃的Binary Format。区分使用何种序列化方式的方法是，在序列化时将文件名后缀指定为`ubj`（Universal Binary JSON，参见[Universal Binary JSON Specification](https://ubjson.org/)）、`json`（JSON）或`deprecated`（Binary Format）。如果文件后缀名不为上述三项，则截至2.0.0版本，序列化默认仍然使用Binary Format。[xgboost Release 2.0.0-dev](https://buildmedia.readthedocs.org/media/pdf/xgboost/latest/xgboost.pdf)的第1.4.2节"Introduction to Model IO"介绍了2.0.0版本模型序列化的基本信息。

注意，如果使用2.0.0版本，且序列化文件后缀名不为`ubj`、`json`或`deprecated`，则会给出警告。此警告由[c_api.cc](https://github.com/dmlc/xgboost/blob/master/src/c_api/c_api.cc)的第935行输出，警告内容如下：

```text
Saving into deprecated binary model format, please consider using `json` or `ubj`. Model format will default to JSON in XGBoost 2.2 if not specified.
```

建议无论使用何种版本的XGBoost4j，序列化时都明确指定文件名后缀为`ubj`、`json`或`deprecated`，避免未来可能出现的兼容性问题。

`mpc4j-sml-opboost`使用[xgboost-predictor](https://github.com/h2oai/xgboost-predictor)通过Java读取序列化后的XGBoost模型。然而，xgboost-predictor目前仅支持Binary Format模型读取。因此，如果想使用XGBoost4j训练并序列化模型，随后使用xgboost-predictor读取并使用模型，则序列化时必须使用Binary Format格式，即序列化时将文件名后缀指定为`deprecated`。