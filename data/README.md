# Datasets

We run our schemes on several public datasets to verify the correctness and effectiveness of our works. This document provides detailed information for these datasets.

## Datasets for OpBoost

### Dataset for Correctness

All datasets used for correctness are from testcases used in [Smile (Statistical Machine Intelligence and Learning Engine)](https://haifengl.github.io/). Smile is a fast and comprehensive machine learning, NLP, linear algebra, graph, interpolation, and visualization system in Java and Scala. Please visit the official website and the [GitHub](https://github.com/haifengl/smile) of Simle for more information.

We thank the sufficient unit tests for Gradient descent Boosting Decision Tree (GBDT) provided in Smile. These unit tests demonstrate many special cases in GBDT training, helping us finding many bugs and problems to make our implementation more robust.

#### Preprocess

We preprocess all datasets for correctness verifications with the following principles:

- All nominal **features** are one-hot encoded. The column name for each of the nominal values is under the format `ColumnName_NominalName`. The nominal value for the one-hot encoded column is 0 and 1.
- All nominal **labels** (when data is for classification tasks) remain unchanged.
- The dataset is in `csv` format. The first row describes the column name. The row data is separated by comma (`,`). The `NULL` data is left blank.

Take the dataset [abalone-train](https://github.com/haifengl/smile/blob/master/shell/src/universal/data/regression/abalone-train.data) as an example. We can find the dataset schema in [Abalone.java](https://github.com/haifengl/smile/blob/eb5f14eed4c0aa1474abdb1ef78f662f6dd87fea/base/src/test/java/smile/test/data/Abalone.java). The schema is printed as follows.

```text
[sex: byte nominal[F, M, I], length: double, diameter: double, height: double, whole weight: double, shucked weight: double, viscera weight: double, shell weight: double, rings: double]
```

The original dataset is as follows.

```text
M,0.455,0.365,0.095,0.514,0.2245,0.101,0.15,15
M,0.35,0.265,0.09,0.2255,0.0995,0.0485,0.07,7
F,0.53,0.42,0.135,0.677,0.2565,0.1415,0.21,9
M,0.44,0.365,0.125,0.516,0.2155,0.114,0.155,10
I,0.33,0.255,0.08,0.205,0.0895,0.0395,0.055,7
I,0.425,0.3,0.095,0.3515,0.141,0.0775,0.12,8
F,0.53,0.415,0.15,0.7775,0.237,0.1415,0.33,20
F,0.545,0.425,0.125,0.768,0.294,0.1495,0.26,16
M,0.475,0.37,0.125,0.5095,0.2165,0.1125,0.165,9
F,0.55,0.44,0.15,0.8945,0.3145,0.151,0.32,19
......
```

First, the feature `sex` in the dataset Abalone contains three nominal values: F, M, and I. We encode `sex` in the one-hot manner to have three columns `sex_F`, `sex_M`, `sex_I`. For the row that has `sex` value `F`, we let `sex_F` be 1, while setting `sex_M` and `sex_I` to be 0. Then, we add a header row describing the column name. Finally, we save the dataset in `csv` format.

Since the dataset [abalone-train](https://github.com/haifengl/smile/blob/master/shell/src/universal/data/regression/abalone-train.data) is used for the regression task, we place it in the dictionary `regression/abalone/`. The re-formatted dataset is as follows.

```text
sex_F,sex_M,sex_I,length,diameter,height,whole weight,shucked weight,viscera weight,shell weight,rings
0,1,0,0.455,0.365,0.095,0.514,0.2245,0.101,0.15,15
0,1,0,0.35,0.265,0.09,0.2255,0.0995,0.0485,0.07,7
1,0,0,0.53,0.42,0.135,0.677,0.2565,0.1415,0.21,9
0,1,0,0.44,0.365,0.125,0.516,0.2155,0.114,0.155,10
0,0,1,0.33,0.255,0.08,0.205,0.0895,0.0395,0.055,7
0,0,1,0.425,0.3,0.095,0.3515,0.141,0.0775,0.12,8
1,0,0,0.53,0.415,0.15,0.7775,0.237,0.1415,0.33,20
1,0,0,0.545,0.425,0.125,0.768,0.294,0.1495,0.26,16
0,1,0,0.475,0.37,0.125,0.5095,0.2165,0.1125,0.165,9
1,0,0,0.55,0.44,0.15,0.8945,0.3145,0.151,0.32,19
......
```

#### Regression: CPU

**This dataset is very small, and all features are numeric. This dataset is the Basic Verification Test (BVT) case.** 

The dataset is downloaded from [cpu.arff](https://github.com/haifengl/smile/blob/master/shell/src/universal/data/weka/cpu.arff). The description is contained in the original file:

```text
As used by Kilpatrick, D. & Cameron-Jones, M. (1998). Numeric prediction using instance-based learning with encoding length selection. In Progress in Connectionist-Based Information Systems. Singapore: Springer-Verlag.

Deleted "vendor" attribute to make data consistent with what we used in the data mining book.
```

The schema is as follows, in which the label is `class`.

```text
[MYCT: float, MMIN: float, MMAX: float, CACH: float, CHMIN: float, CHMAX: float, class: float]
```

#### Regression: Abalone

**This dataset contains both numeric and nominal columns.** 

The train and the test datasets are downloaded respectively from [abalone-train.data](https://github.com/haifengl/smile/blob/master/shell/src/universal/data/regression/abalone-train.data) and [abalone-test.data](https://github.com/haifengl/smile/blob/master/shell/src/universal/data/regression/abalone-test.data). The schema is as follows, in which the label is `rings`.

```text
[sex: byte nominal[F, M, I], length: double, diameter: double, height: double, whole weight: double, shucked weight: double, viscera weight: double, shell weight: double, rings: double]
```

#### Regression: AutoMPG 

**This dataset contains both numeric and nominal columns and with missing values represented by `?`.** 

In the original data file, the missing values are set as `?`. We replace `?` to blank. The nominal columns `cylinders`, `model` and `origin` contain many nominal values and these values do not start from 0.

The dataset is downloaded from [autoMpg.arff](https://github.com/haifengl/smile/blob/master/shell/src/universal/data/weka/regression/autoMpg.arff). The description is contained in the original file:

```text
% !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
%
% Identifier attribute deleted.
%
% As used by Kilpatrick, D. & Cameron-Jones, M. (1998). Numeric prediction
% using instance-based learning with encoding length selection. In Progress
% in Connectionist-Based Information Systems. Singapore: Springer-Verlag.
%
% !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
%
%
% 1. Title: Auto-Mpg Data
%
% 2. Sources:
%    (a) Origin:  This dataset was taken from the StatLib library which is
%                 maintained at Carnegie Mellon University. The dataset was
%                 used in the 1983 American Statistical Association Exposition.
%    (c) Date: July 7, 1993
%
% 3. Past Usage:
%     -  See 2b (above)
%     -  Quinlan,R. (1993). Combining Instance-Based and Model-Based Learning.
%        In Proceedings on the Tenth International Conference of Machine
%        Learning, 236-243, University of Massachusetts, Amherst. Morgan
%        Kaufmann.
%
% 4. Relevant Information:
%
%    This dataset is a slightly modified version of the dataset provided in
%    the StatLib library.  In line with the use by Ross Quinlan (1993) in
%    predicting the attribute "mpg", 8 of the original instances were removed
%    because they had unknown values for the "mpg" attribute.  The original
%    dataset is available in the file "auto-mpg.data-original".
%
%    "The data concerns city-cycle fuel consumption in miles per gallon,
%     to be predicted in terms of 3 multivalued discrete and 5 continuous
%     attributes." (Quinlan, 1993)
%
% 5. Number of Instances: 398
%
% 6. Number of Attributes: 9 including the class attribute
%
% 7. Attribute Information:
%
%     1. mpg:           continuous
%     2. cylinders:     multi-valued discrete
%     3. displacement:  continuous
%     4. horsepower:    continuous
%     5. weight:        continuous
%     6. acceleration:  continuous
%     7. model year:    multi-valued discrete
%     8. origin:        multi-valued discrete
%     9. car name:      string (unique for each instance)
%
% 8. Missing Attribute Values:  horsepower has 6 missing values
```

The schema is as follows, in which the label is `class`.

```text
[cylinders: byte nominal[8, 4, 6, 3, 5], displacement: float, horsepower: float, weight: float, acceleration: float, model: byte nominal[70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82], origin: byte nominal[1, 3, 2], class: float]
```

#### Regression: BostonHousing

**This dataset contains both numeric and nominal columns. The nominal column `CHAS` only contains two nominal values (0 and 1).**

The dataset is downloaded from [housing.arff](https://github.com/haifengl/smile/blob/master/shell/src/universal/data/weka/regression/housing.arff). The description is contained in the original file:

```text
% 1. Title: Boston Housing Data
% 
% 2. Sources:
%    (a) Origin:  This dataset was taken from the StatLib library which is
%                 maintained at Carnegie Mellon University.
%    (b) Creator:  Harrison, D. and Rubinfeld, D.L. 'Hedonic prices and the 
%                  demand for clean air', J. Environ. Economics & Management,
%                  vol.5, 81-102, 1978.
%    (c) Date: July 7, 1993
% 
% 3. Past Usage:
%    -   Used in Belsley, Kuh & Welsch, 'Regression diagnostics ...', Wiley, 
%        1980.   N.B. Various transformations are used in the table on
%        pages 244-261.
%     -  Quinlan,R. (1993). Combining Instance-Based and Model-Based Learning.
%        In Proceedings on the Tenth International Conference of Machine 
%        Learning, 236-243, University of Massachusetts, Amherst. Morgan
%        Kaufmann.
% 
% 4. Relevant Information:
% 
%    Concerns housing values in suburbs of Boston.
% 
% 5. Number of Instances: 506
% 
% 6. Number of Attributes: 13 continuous attributes (including "class"
%                          attribute "MEDV"), 1 binary-valued attribute.
% 
% 7. Attribute Information:
% 
%     1. CRIM      per capita crime rate by town
%     2. ZN        proportion of residential land zoned for lots over 
%                  25,000 sq.ft.
%     3. INDUS     proportion of non-retail business acres per town
%     4. CHAS      Charles River dummy variable (= 1 if tract bounds 
%                  river; 0 otherwise)
%     5. NOX       nitric oxides concentration (parts per 10 million)
%     6. RM        average number of rooms per dwelling
%     7. AGE       proportion of owner-occupied units built prior to 1940
%     8. DIS       weighted distances to five Boston employment centres
%     9. RAD       index of accessibility to radial highways
%     10. TAX      full-value property-tax rate per $10,000
%     11. PTRATIO  pupil-teacher ratio by town
%     12. B        1000(Bk - 0.63)^2 where Bk is the proportion of blacks 
%                  by town
%     13. LSTAT    % lower status of the population
%     14. MEDV     Median value of owner-occupied homes in $1000's
% 
% 8. Missing Attribute Values:  None.
```

The schema is as follows, in which the label is `class`.

```text
[CRIM: float, ZN: float, INDUS: float, CHAS: byte nominal[0, 1], NOX: float, RM: float, AGE: float, DIS: float, RAD: float, TAX: float, PTRATIO: float, B: float, LSTAT: float, class: float]
```

#### Regression: Kin8nm

**The dataset is relatively large (around 8,000 rows). The dataset only contains numeric columns, but some values are negative.**

The dataset is downloaded from [kin8nm.arff](https://github.com/haifengl/smile/blob/master/shell/src/universal/data/weka/regression/kin8nm.arff). The description is contained in the original file:

```text
% This is data set is concerned with the forward kinematics of an 8 link
% robot arm. Among the existing variants of this data set we have used
% the variant 8nm, which is known to be highly non-linear and medium
% noisy.
%
% Original source: DELVE repository of data. 
% Source: collection of regression datasets by Luis Torgo (ltorgo@ncc.up.pt) at
% http://www.ncc.up.pt/~ltorgo/Regression/DataSets.html
% Characteristics: 8192 cases, 9 attributes (0 nominal, 9 continuous).
```

The schema is as follows, in which the label is `y`.

```text
[theta1: double, theta2: double, theta3: double, theta4: double, theta5: double, theta6: double, theta7: double, theta8: double, y: double]
```

#### Binary Classification: Weather

**This is a small dataset with only 14 rows and all columns are nominal.**

The dataset is downloaded from [weather.nominal.arff](https://github.com/haifengl/smile/blob/master/shell/src/universal/data/weka/weather.nominal.arff). The schema is as follows, in which the label is `play`.

```text
[outlook: byte nominal[sunny, overcast, rainy], temperature: byte nominal[hot, mild, cool], humidity: byte nominal[high, normal], windy: byte nominal[TRUE, FALSE], play: byte nominal[yes, no]]
```

#### 3-Class Classification: Iris

**This dataset only contains numeric columns. The dataset is for 3-class classification.**

The dataset is downloaded from [iris.arff](https://github.com/haifengl/smile/blob/master/shell/src/universal/data/weka/iris.arff). The description is contained in the original file:

```text
% 1. Title: Iris Plants Database
% 
% 2. Sources:
%      (a) Creator: R.A. Fisher
%      (b) Donor: Michael Marshall (MARSHALL%PLU@io.arc.nasa.gov)
%      (c) Date: July, 1988
% 
% 3. Past Usage:
%    - Publications: too many to mention!!!  Here are a few.
%    1. Fisher,R.A. "The use of multiple measurements in taxonomic problems"
%       Annual Eugenics, 7, Part II, 179-188 (1936); also in "Contributions
%       to Mathematical Statistics" (John Wiley, NY, 1950).
%    2. Duda,R.O., & Hart,P.E. (1973) Pattern Classification and Scene Analysis.
%       (Q327.D83) John Wiley & Sons.  ISBN 0-471-22361-1.  See page 218.
%    3. Dasarathy, B.V. (1980) "Nosing Around the Neighborhood: A New System
%       Structure and Classification Rule for Recognition in Partially Exposed
%       Environments".  IEEE Transactions on Pattern Analysis and Machine
%       Intelligence, Vol. PAMI-2, No. 1, 67-71.
%       -- Results:
%          -- very low misclassification rates (0% for the setosa class)
%    4. Gates, G.W. (1972) "The Reduced Nearest Neighbor Rule".  IEEE 
%       Transactions on Information Theory, May 1972, 431-433.
%       -- Results:
%          -- very low misclassification rates again
%    5. See also: 1988 MLC Proceedings, 54-64.  Cheeseman et al's AUTOCLASS II
%       conceptual clustering system finds 3 classes in the data.
% 
% 4. Relevant Information:
%    --- This is perhaps the best known database to be found in the pattern
%        recognition literature.  Fisher's paper is a classic in the field
%        and is referenced frequently to this day.  (See Duda & Hart, for
%        example.)  The data set contains 3 classes of 50 instances each,
%        where each class refers to a type of iris plant.  One class is
%        linearly separable from the other 2; the latter are NOT linearly
%        separable from each other.
%    --- Predicted attribute: class of iris plant.
%    --- This is an exceedingly simple domain.
% 
% 5. Number of Instances: 150 (50 in each of three classes)
% 
% 6. Number of Attributes: 4 numeric, predictive attributes and the class
% 
% 7. Attribute Information:
%    1. sepal length in cm
%    2. sepal width in cm
%    3. petal length in cm
%    4. petal width in cm
%    5. class: 
%       -- Iris Setosa
%       -- Iris Versicolour
%       -- Iris Virginica
% 
% 8. Missing Attribute Values: None
% 
% Summary Statistics:
%  	           Min  Max   Mean    SD   Class Correlation
%    sepal length: 4.3  7.9   5.84  0.83    0.7826   
%     sepal width: 2.0  4.4   3.05  0.43   -0.4194
%    petal length: 1.0  6.9   3.76  1.76    0.9490  (high!)
%     petal width: 0.1  2.5   1.20  0.76    0.9565  (high!)
% 
% 9. Class Distribution: 33.3% for each of 3 classes.
```

The schema is as follows, in which the label is `class`.

```text
[sepallength: float, sepalwidth: float, petallength: float, petalwidth: float, class: byte nominal[Iris-setosa, Iris-versicolor, Iris-virginica]]
```
#### Multi-class Classification: Pendigits

**This dataset only contains numeric columns. The dataset is for multi-class classification.** The dataset is downloaded from [pendigits.txt](https://github.com/haifengl/smile/blob/master/shell/src/universal/data/classification/pendigits.txt). 

The schema is as follows, in which the label is `class`.

```text
[V1: double, V2: double, V3: double, V4: double, V5: double, V6: double, V7: double, V8: double, V9: double, V10: double, V11: double, V12: double, V13: double, V14: double, V15: double, V16: double, class: byte nominal[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]]
```

#### Binary Classification: BreastCancer

**This is a relatively large dataset that only contains numeric columns. The dataset is for binary classification, where the label is not the last column.** The dataset is downloaded from [breastcancer.csv](https://github.com/haifengl/smile/blob/master/shell/src/universal/data/classification/breastcancer.csv). 

The schema is as follows, in which the label is `diagnosis`.

```text
[diagnosis: byte nominal[M, B], radius_mean: double, texture_mean: double, perimeter_mean: double, area_mean: double, smoothness_mean: double, compactness_mean: double, concavity_mean: double, concave points_mean: double, symmetry_mean: double, fractal_dimension_mean: double, radius_se: double, texture_se: double, perimeter_se: double, area_se: double, smoothness_se: double, compactness_se: double, concavity_se: double, concave points_se: double, symmetry_se: double, fractal_dimension_se: double, radius_worst: double, texture_worst: double, perimeter_worst: double, area_worst: double, smoothness_worst: double, compactness_worst: double, concavity_worst: double, concave points_worst: double, symmetry_worst: double, fractal_dimension_worst: double]
```

### Datasets for Effectiveness

We introduce 4 datasets from [UCI Machine Learning Repository](https://archive.ics.uci.edu/ml/datasets.php) for the effectiveness tests. We further introduce a dataset from a real scenario for the large-scale experiment.

#### Preprocess

We also follow the principles shown below to preprocess all datasets for effectiveness:

- All nominal **features** are one-hot encoded. The column name for each of the nominal values is under the format `ColumnName_NominalName`. The nominal value for the one-hot encoded column is 0 and 1.
- All nominal **labels** (for classification tasks) remain unchanged.
- The dataset is in `csv` format. The first row describes the column name. The row data is separated by comma (`,`). The `NULL` data is left blank.

In addition, we preprocess all numerical features with different ranges into discrete values in the range of \[0, 10\] to facilitate setting privacy parameters.

#### Datasets from UCL Machine Learning Repository

- CASP (regression): [Physicochemical Properties of Protein Tertiary Structure Dataset](https://archive.ics.uci.edu/ml/datasets/Physicochemical+Properties+of+Protein+Tertiary+Structure).
- PowerPlant (regression): [Combined Cycle Power Plant Data Set](https://archive.ics.uci.edu/ml/datasets/combined+cycle+power+plant).
- Adult (binary classification): [Adult Data Set](https://archive.ics.uci.edu/ml/datasets/Adult).
- PenDigits (Multi-class classification): [Pen-Based Recognition of Handwritten Digits Data Set](https://archive.ics.uci.edu/ml/datasets/Pen-Based+Recognition+of+Handwritten+Digits).

#### Dataset from Real Scenario

We are sorry that we cannot release the dataset from the real scenario. Here we provide some basic information. 

- Task: binary classification.
- Features: 38 nominal features and 262 numerical features.
- Training: 234903 rows.
- Testing: 58727 rows.

## Datasets for PSU: Black IP

### Introduction

Private set union (PSU) enables two parties, each holding a  private set of elements, to compute the union of the two sets while revealing nothing more than the union itself. One important application of PSU is blacklist and vulnerability data aggregation. Consider that there are two organizations (i.e. the maintainers of the IP blacklists) who want to compute their IP blacklist joint list, which will help minimize vulnerabilities in their infrastructure. 

We run PSU experiments on a black IP dataset to demonstrate this PSU application. The black IP dataset is available at **[BlackIP](https://github.com/maravento/blackip)**. In our experiment, we assume the PSU sender maintains `blackip.txt` (with 3,176,636 distinct IPs), and the PSU client maintains `oldip.txt` (with 2,514,551 distinct IPs). The union result contains 3,178,512 IPs. All IPs in `blackip.txt` and `oldip.txt` are IPv4 addresses. Each IP is a 32-bit number, written in decimal digits and formatted as four 8-bit fields separated by periods. In our experiments, we uniquely represent each of these IPs by a 32-bit binary string. The dataset is located at `black_ip/blackip.txt` / `black_ip/oldip.txt`. The correlated configuration files are in `conf/psu_black_ip`.

### About BlackIP

The descriptions below are from [READMD.md](https://github.com/maravento/blackip.README.md) in the root of the BlackIP project.

**BlackIP** is a project that collects and unifies public blocklists of IP addresses, to make them compatible with [Squid](http://www.squid-cache.org/) and [IPSET](http://ipset.netfilter.org/) ([Iptables](http://www.netfilter.org/documentation/HOWTO/es/packet-filtering-HOWTO-7.html) [Netfilter](http://www.netfilter.org/))

**BlackIP** es un proyecto que recopila y unifica listas públicas de bloqueo de direcciones IPs, para hacerlas compatibles con [Squid](http://www.squid-cache.org/) e [IPSET](http://ipset.netfilter.org/) ([Iptables](http://www.netfilter.org/documentation/HOWTO/es/packet-filtering-HOWTO-7.html) [Netfilter](http://www.netfilter.org/))


#### DATA SHEET

|     ACL     | Blocked IP | File Size |
| :---------: | :--------: | :-------: |
| blackip.txt |  3176744   |  45,4 Mb  |

#### GIT CLONE

```bash
git clone https://github.com/maravento/blackip.git
```

#### CONTRIBUTIONS

We thank all those who contributed to this project. Those interested may contribute sending us new "Blocklist" links to be included in this project / Agradecemos a todos aquellos que han contribuido a este proyecto. Los interesados pueden contribuir, enviándonos enlaces de nuevas "Blocklist", para ser incluidas en este proyecto

Special thanks to: [Jhonatan Sneider](https://github.com/sney2002)

#### DONATE

BTC: 3M84UKpz8AwwPADiYGQjT9spPKCvbqm4Bc

#### BUILD

[![CreativeCommons](https://licensebuttons.net/l/by-sa/4.0/88x31.png)](http://creativecommons.org/licenses/by-sa/4.0/)
[maravento.com](http://www.maravento.com) is licensed under a [Creative Commons Reconocimiento-CompartirIgual 4.0 Internacional License](http://creativecommons.org/licenses/by-sa/4.0/).

#### OBJECTION

Due to recent arbitrary changes in computer terminology, it is necessary to clarify the meaning and connotation of the term **blacklist**, associated with this project: *In computing, a blacklist, denylist or blocklist is a basic access control mechanism that allows through all elements (email addresses, users, passwords, URLs, IP addresses, domain names, file hashes, etc.), except those explicitly mentioned. Those items on the list are denied access. The opposite is a whitelist, which means only items on the list are let through whatever gate is being used.*

Debido a los recientes cambios arbitrarios en la terminología informática, es necesario aclarar el significado y connotación del término **blacklist**, asociado a este proyecto: *En informática, una lista negra, lista de denegación o lista de bloqueo es un mecanismo básico de control de acceso que permite a través de todos los elementos (direcciones de correo electrónico, usuarios, contraseñas, URL, direcciones IP, nombres de dominio, hashes de archivos, etc.), excepto los mencionados explícitamente. Esos elementos en la lista tienen acceso denegado. Lo opuesto es una lista blanca, lo que significa que solo los elementos de la lista pueden pasar por cualquier puerta que se esté utilizando.*

Source [Wikipedia](https://en.wikipedia.org/wiki/Blacklist_(computing))

Therefore / Por tanto

**blacklist**, **blocklist**, **blackweb**, **blackip**, **whitelist**, **etc.**

are terms that have nothing to do with racial discrimination / son términos que no tienen ninguna relación con la discriminación racial

#### DISCLAIMER

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## Datasets for Streaming

We use the following three datasets for our streaming tasks:

- Synthetic dataset. This dataset is generated by randomly sampling data from a normal distribution with variance $\sigma=5$. There are $n=100,000$ values with the domain size of $d=1,000$. The dataset is located at `stream/synthetic_data.dat`.
- Retail dataset. This dataset contains the retail market basket data from an anonymous Belgian retail store with around $0.9$ million values and $16$ thousand distinct items. You can directly download the dataset from [Frequent Itemset Mining Dataset Repository](http://fimi.uantwerpen.be/data/).
- Kosarak dataset \cite{kosarak}. This dataset contains the click streams on a Hungarian website. There are around $8$ million values and $42$ thousand URLs. You can directly download the dataset from [Frequent Itemset Mining Dataset Repository](http://fimi.uantwerpen.be/data/).