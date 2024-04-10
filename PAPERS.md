# Implementations of Existing Works

`mpc4j` contains some implementations of existing works. Parts of the works are listed in this documentation. We note that we rewrite some of the implementations to have a unified style. See Section References in `README.md` for more details.

## Basic Tools

### FHE

We provide an pure-Java implemeantion of BFV scheme. The implementation can be seen as a pure-Java version of [SEAL](https://github.com/microsoft/SEAL). See `mpc4j-crypto-fhe` for details.

### Filter

Besides Bloom Filter, we implement some filter algorithms in `mpc4j-common-tool`, including Cuckoo Filter and Vacuum Filter. We plan to implement Morton Filter but without any timeline.

- Wang, Minmei, and Mingxun Zhou. Vacuum Filters: More Space-efficient and Faster Replacement for Bloom and Cuckoo Filters. VLDB 2019.
- Fan, Bin, Dave G. Andersen, Michael Kaminsky, and Michael D. Mitzenmacher. Cuckoo Filter: Practically Better than Bloom.  CoNEXT 2014, ACM, pp. 75-88.

### Cuckoo Hash

We implement some Cuckoo Hashes in `mpc4j-common-tool`. See the following papers for more details.

- Pinkas, Benny, Thomas Schneider, and Michael Zohner. Scalable Private Set Intersection Based on OT Extension. ACM Transactions on Privacy and Security, 21, no. 2 (2018): 1-35.
- Demmler, Daniel, Peter Rindal, Mike Rosulek, and Ni Trieu. PIR-PSI: Scaling Private Contact Discovery. PETS 2018, 4, pp. 159-178.

### Cryptographic Tools

We implement some cryptographic tools in `mpc4j-common-tool`. We believe these tools may help users to implement other applications as well.

**Elligator Encoding for Curve25519**: An important tool for constructing simple maliciously secure PSI, see:

- Rosulek, Mike, and Ni Trieu. Compact and Malicious Private Set Intersection for Small Sets. CCS 2021, ACM, pp. 1166-1181.

**FourQ**: A high-security, high-performance elliptic curve that targets the 128-bit security level. Based on the open-source library [FourQlib](https://github.com/microsoft/FourQlib), we rewrite `MakeFile` so that now FourQ can run on MacBook. 

- Costello, Craig, and Patrick Longa. FourQ: Four-dimensional Decompositions on A-curve over the Mersenne Prime. ASIACRYPT 2015, Part I, Springer Berlin Heidelberg, pp. 214-235.

**Benes Network**: An instance of permutation network, which is a key tool for Oblivious Switching, see:

- Garimella, Gayathri, Payman Mohassel, Mike Rosulek, Saeed Sadeghian, and Jaspal Singh. Private Set Operations from Oblivious Switching. PKC 2021, Cham: Springer International Publishing, pp. 591-617.

**Polynomial Interpolation**: An important tool for many MPC protocols. We also implement batch polynomial interpolation, efficient random-point padding, and others, see:

- Pinkas, Benny, Mike Rosulek, Ni Trieu, and Avishay Yanai. SpOT-light: Lightweight Private Set Intersection from Sparse OT Extension. CRYPTO 2019, Part III, Springer International Publishing, pp. 401-431.
- Kolesnikov, Vladimir, Mike Rosulek, Ni Trieu, and Xiao Wang. Scalable Private Set Union from Symmetric-key Techniques. ASIACRYPT 2019, Part II, Cham: Springer International Publishing, pp. 636-666.

## Cryptographic Primitives

- Boldyreva, Alexandra, Nathan Chenette, Younho Lee, and Adam O’neill. Order-preserving symmetric encryption. EUROCRYPT 2009, pp. 224-241, 2009.

## Discrete Gaussian Sampler

We implement some discrete Gaussian sampling algorithms (also known as Discrete Gaussians over the Integers) in `mpc4j-common-sampler`. We learned a lot from the library [dgs](https://github.com/malb/dgs).

- Canonne, Clément L., Gautam Kamath, and Thomas Steinke. The Discrete Gaussian for Differential Privacy. NIPS 2020, 33, pp. 15676-15688.
- Micciancio, Daniele, and Michael Walter. Gaussian Sampling over the Integers: Efficient, Generic, Constant-time. CRYPTO 2017, Part II, Springer International Publishing, pp. 455-485.
- Pöppelmann, Thomas, Léo Ducas, and Tim Güneysu. Enhanced Lattice-based Signatures on Reconfigurable Hardware. CHES 2014, Springer Berlin Heidelberg, pp. 353-370.
- Ducas, Léo, Alain Durmus, Tancrède Lepoint, and Vadim Lyubashevsky. Lattice Signatures and Bimodal Gaussians. CRYPTO 2013, Part I, Springer Berlin Heidelberg, pp. 40-56.

## Oblivious Key-Value Storage

Oblivious Key-Value Storage (OKVS) is a very implement data structure abstraction used in MPC. We implement some OKVS data structures, including its instances like XePaXoS, PaXoS, and Mega Bin, in `mpc4j-common-tool`.

- Garimella, Gayathri, Benny Pinkas, Mike Rosulek, Ni Trieu, and Avishay Yanai. Oblivious Key-Value Stores and Amplification for Private Set Intersection. CRYPTO 2021, Part II, Springer International Publishing, pp. 395-425.
- Rindal, Peter, and Phillipp Schoppmann. VOLE-PSI: Fast OPRF and Circuit-PSI from Vector-OLE. EUROCRYPT 2021, Part II, Cham: Springer International Publishing, pp. 901-930.
- Pinkas, Benny, Mike Rosulek, Ni Trieu, and Avishay Yanai. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020, Part II, Cham: Springer International Publishing, pp. 739-767.
- Pinkas, Benny, Thomas Schneider, Oleksandr Tkachenko, and Avishay Yanai. Efficient Circuit-Based PSI with Linear Communication. EUROCRYPT 2019, Part III, Springer International Publishing, pp. 122-153.

## Frequency Oracle with Local Differential Privacy

We implement some Frequency Oracle (FO) with Local Differential Privacy in `mpc4j-dp-service`. We learned a lot from the library [Pure-DP](https://github.com/Samuel-Maddock/pure-LDP).

- Cormode, Graham, Samuel Maddock, and Carsten Maple. Frequency Estimation under Local Differential Privacy.  VLDB 2021, pp. 2046-2058.
- Acharya, Jayadev, Ziteng Sun, and Huanyu Zhang. Hadamard Response: Estimating Distributions Privately, Efficiently, and with Little Communication. AISTATS 2019, PMLR, pp. 1120-1129.
- Acharya, Jayadev, and Ziteng Sun. Communication Complexity in Locally Private Distribution Estimation and Heavy Hitters. ICML 2019, PMLR, pp. 51-60.
- Wang, Tianhao, and Jeremiah Blocki. Locally Differentially Private Protocols for Frequency Estimation. USENIX Security 2017.
- Differential Privacy Team, Apple. Learning with Privacy at Scale. Technique Report, 2017.
- Erlingsson, Úlfar, Vasyl Pihur, and Aleksandra Korolova. RAPPOR: Randomized Aggregatable Privacy-Preserving Ordinal Response. CCS 2014, ACM, pp. 1054-1067.

## Pseudorandom Correlation Generator

PCG allows two or more parties to securely generate long sources of useful correlated randomness via a local expansion of correlated short seeds. Correlated Oblivious Transfer (COT), Vector Oblivious Linear Evaluation (VOLE), Subfield VOLE, and multiplication triples can be seen as instances of PCG. We implement some PCG in `mpc4j-s2pc-pcg`.

### Basic Tools

We implement some basic tools used in PCG. We learned a lot from [KyberJCE](https://github.com/fisherstevenk/kyberJCE) when implementing Kyber. We learned a lot from [emp-toolkit](https://github.com/emp-toolkit) when implementing Correlated Robust Hash Function (CRHF) and Twisted CRHF (TCRHF).

- Bos, Joppe, Léo Ducas, Eike Kiltz, Tancrède Lepoint, Vadim Lyubashevsky, John M. Schanck, Peter Schwabe, Gregor Seiler, and Damien Stehlé. CRYSTALS-Kyber: A CCA-secure Module-lattice-based KEM. EuroS\&P 2018, IEEE, pp. 353-367.
- Guo, Chun, Jonathan Katz, Xiao Wang, and Yu Yu. Efficient and Secure Multiparty Computation from Fixed-key Block Ciphers.S\&P 2020, IEEE, pp. 825-841.

### Coin-tossing Protocol

Some protocols (especially protocol with malicious security) needs to invoke a (maliciously secure) coin tossing protocol for unbiased randomness generation. We implement the commonly used coin-tossing protocol that was proposed by Blum in the following paper.

- Blum, Manuel. Coin flipping by phone. In COMPCON, pp. 133-137. 1982.

The security proof is shown in the following paper.

- Lindell, Yehuda. How to simulate it: a tutorial on the simulation proof technique. Tutorials on the Foundations of Cryptography: Dedicated to Oded Goldreich (2017): 277-346.

### Base OT

There are many research results related to base OT. We adjust some results in instance implementations.

- Canetti, Ran, Pratik Sarkar, and Xiao Wang. Blazing Fast OT for Three-round UC OT Extension. PKC 2020, Part II, Springer International Publishing, pp.299-327.
- Mansy, Daniel, and Peter Rindal. Endemic Oblivious Transfer. CCS 2019, ACM, pp. 309-326.
- McQuoid, Ian, Mike Rosulek, and Lawrence Roy. Batching Base Oblivious Transfers. ASIACRYPT 2021, Part III, Springer International Publishing, pp. 281-310.
- Chou, Tung, and Claudio Orlandi. The Simplest Protocol for Oblivious Transfer. LATINCRYPT 2015, Springer International Publishing, pp. 40-58.
- Naor, Moni, and Benny Pinkas. Efficient Oblivious Transfer Protocols. SODA 2001, ACM, pp. 448-457.

### OT Extension, Silent OT, and VOLE

We implement some OT Extension (OTE) and Silent OT protocols.

- Couteau, Geoffroy, Peter Rindal, and Srinivasan Raghuraman. Silver: Silent VOLE and Oblivious Transfer from Hardness of Decoding Structured LDPC Codes. CRYPTO 2021, Part III, Cham: Springer International Publishing, pp. 502-534. (**Warning: Silver is not secure so that you need to use other silent OT. See paper "Correlated Pseudorandomness from Expand-Accumulate Codes" (CRYPTO 2022) and paper "Expand-Convolute Codes for Pseudorandom Correlation Generators from LPN" (CRYPTO 2023) for more details.**)
- Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast Extension for Correlated OT with Small Communication. CCS 2020, ACM, pp. 1607-1626.
- Orrù, Michele, Emmanuela Orsini, and Peter Scholl. Actively Secure 1-out-of-N OT Extension with Application to Private Set Intersection. CT-RSA 2017, Springer International Publishing, pp. 381-396.
- Keller, Marcel, Emmanuela Orsini, and Peter Scholl. Actively Secure OT Extension with Pptimal Overhead. CRYPTO 2015, Part I, Berlin, Heidelberg: Springer Berlin Heidelberg, pp. 724-741. Note that we fix the malicious security flaw introduced in the paper "SoftSpokenOT: Communication – Computation Tradeoffs in OT Extension (CRYPTO 2022)".
- Kolesnikov, Vladimir, and Ranjit Kumaresan. Improved OT Extension for Transferring Short Secrets. CRYPTO 2013, Part II, Springer Berlin Heidelberg, pp. 54-70.
- Asharov, Gilad, Yehuda Lindell, Thomas Schneider, and Michael Zohner. More Efficient Oblivious Transfer and Extensions for Faster Secure Computation. CCS 2013, ACM, pp. 535-548.
- Ishai, Yuval, Joe Kilian, Kobbi Nissim, and Erez Petrank. Extending Oblivious Transfers Efficiently. CRYPTO 2003, Springer Berlin Heidelberg, pp. 145-161.
- Keller, Marcel, Emmanuela Orsini, and Peter Scholl. MASCOT: Faster Malicious Arithmetic Secure Computation with Oblivious Transfer. CCS 2016, pp. 830-842.
- Weng, Chenkai, Kang Yang, Jonathan Katz, and Xiao Wang. Wolverine: Fast, Scalable, and Communication-Efficient Zero-Knowledge Proofs for Boolean and Arithmetic Circuits. S\&P 2021, IEEE, pp. 1074-1091.

### Multiplication Triple

- Rathee, Deevashwer, Thomas Schneider, and K. K. Shukla. Improved Multiplication Triple Generation over Rings via RLWE-based AHE. CANS 2019, Springer International Publishing, pp. 347-359.
- Demmler, Daniel, Thomas Schneider, and Michael Zohner. ABY - A Framework for Efficient Mixed-protocol Secure Two-party Computation. NDSS 2015.

## Private Information Retrieval

We implement some Private Information Retrieval (PIR) protocols. We note that there are parameter adjustments and re-implementations for some protocols so that the results can run in a more robust manner. All protocols are implemented based on [SEAL](https://github.com/microsoft/SEAL) (instead of [NFLlib](https://github.com/quarkslab/NFLlib)).

### Index PIR

We re-implement many index PIR schemes in `mpc4j-s2pc-pir`. We note that we learned a lot from their original implementations.

- Ahmad, Ishtiyaque, Yuntian Yang, Divyakant Agrawal, Amr El Abbadi, and Trinabh Gupta. Addra: Metadata-private Voice Communication over Fully Untrusted Infrastructure. OSDI 2021.
- Mughees, Muhammad Haris, Hao Chen, and Ling Ren. OnionPIR: Response Efficient Single-server PIR. CCS 2021, ACM, pp. 2292-2306.
- Angel, Sebastian, Hao Chen, Kim Laine, and Srinath Setty. PIR with Compressed Queries and Amortized Query Processing. S\&P 2018, IEEE, pp. 962-979.
- Melchor, Carlos Aguilar, Joris Barrier, Laurent Fousse, and Marc-Olivier Killijian. XPIR: Private Information Retrieval for Everyone. PETS 2016, pp. 155-174.
- Mughees, Muhammad Haris, and Ling Ren. Vectorized Batch Private Information Retrieval. S\&P 2023, IEEE.
- Ali, Asra, Tancrede Lepoint, Sarvar Patel, Mariana Raykova, Phillipp Schoppmann, Karn Seth, and Kevin Yeo. Communication–computation Trade-offs in PIR. USENIX Security 2021, pp. 1811-1828.
- Mahdavi, Rasoul Akhavan, and Florian Kerschbaum. Constant-weight PIR: Single-round Keyword PIR via Constant-weight Equality Operators. USENIX Security 2022, pp. 1723-1740.
- Henzinger, Alexandra, Matthew M. Hong, Henry Corrigan-Gibbs, Sarah Meiklejohn, and Vinod Vaikuntanathan. One Server for the Price of Two: Simple and Fast Single-server Private Information Retrieval. USENIX Security 2023.

### Keyword PIR and Unbalanced PSI

We implement Keyword PIR (also known as Labeled PSI) in `mpc4j-s2pc-pir` and unbalanced PSI in `mpc4j-s2pc-pso`. Here we list other labeled PSI papers, e.g., schemes proposed in CCS 2018 and CCS 2017. We do not implement them since the scheme in CCS 2021 is strictly more efficient than others. We note that we learned a lot from their original implementations. See [APSI](https://github.com/microsoft/APSI) and [6857-private-categorization](https://github.com/aleksejspopovs/6857-private-categorization) for more details.

- Cong, Kelong, Radames Cruz Moreno, Mariana Botelho da Gama, Wei Dai, Ilia Iliashenko, Kim Laine, and Michael Rosenberg. Labeled PSI from Homomorphic Encryption with Reduced Computation and Communication. CCS 2021, ACM, pp. 1135-1150.
- Chen, Hao, Zhicong Huang, Kim Laine, and Peter Rindal. Labeled PSI from Fully Homomorphic Encryption with Malicious Security. CCS 2018, ACM, pp. 1223-1237.
- Chen, Hao, Kim Laine, and Peter Rindal. Fast Private Set Intersection from Homomorphic Encryption. CCS 2017, ACM, pp. 1243-1255.
- Ahmad, Ishtiyaque, Divyakant Agrawal, Amr El Abbadi, and Trinabh Gupta. Pantheon: Private Retrieval from Public Key-Value Store. VLDB 2022, pp. 643-656.

### Client-preprocessing PIR

- Zhou, Mingxun, Andrew Park, Elaine Shi, and Wenting Zheng. Piano: Extremely Simple, Single-Server PIR with Sublinear Server Computation. S\&P 2024.
- Mughees, Muhammad Haris, I. Sun, and Ling Ren. Simple and Practical Amortized Sublinear Private Information Retrieval. Cryptology ePrint Archive (2023).

## Private Set Operation

We implement Private Set Operations in `mpc4j-s2pc-pso` and `mpc4j-s2pc-pjc`.

### Oblivious PRF and Private Set Intersection

- Raghuraman, Srinivasan, and Peter Rindal. Blazing Fast PSI from Improved OKVS and Subfield VOLE. CCS 2022, pp. 2505-2517.
- Rindal, Peter, and Phillipp Schoppmann. VOLE-PSI: Fast OPRF and Circuit-PSI from Vector-OLE. EUROCRYPT 2021, Cham: Springer International Publishing, pp. 901-930.
- Rosulek, Mike, and Ni Trieu. Compact and Malicious Private Set Intersection for Small Sets. CCS 2021, pp. 1166-1181.
- Pinkas, Benny, Mike Rosulek, Ni Trieu, and Avishay Yanai. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020, Cham: Springer International Publishing, pp. 739-767.
- Chase, Melissa, and Peihan Miao. Private Set Intersection in the Internet Setting from Lightweight Oblivious PRF. CRYPTO 2020, Part III, Springer International Publishing, pp. 34-63.
- Pinkas, Benny, Mike Rosulek, Ni Trieu, and Avishay Yanai. SpOT-light: Lightweight Private Set Intersection from Sparse OT Extension. CRYPTO 2019, Part III, Springer International Publishing, pp. 401-431.
- Resende, Amanda C. Davi, and Diego F. Aranha. Faster Unbalanced Private Set Intersection. FC 2018, Springer Berlin Heidelberg, pp. 203-221.
- Albrecht, Martin R., Christian Rechberger, Thomas Schneider, Tyge Tiessen, and Michael Zohner. Ciphers for MPC and FHE. EUROCRYPT 2015, Part I, Springer Berlin Heidelberg, pp. 430-454.
- Kolesnikov, Vladimir, Ranjit Kumaresan, Mike Rosulek, and Ni Trieu. Efficient Batched Oblivious PRF with Applications to Private Set Intersection. CCS 2016, ACM, pp. 818-829.
- Pinkas, Benny, Thomas Schneider, and Michael Zohner. Faster Private Set Intersection based on OT Extension. USENIX Security 2014, pp. 797-812.
- Dong, Changyu, Liqun Chen, and Zikai Wen. When Private Set Intersection Meets Big Data: an Efficient and Scalable Protocol. CCS 2013, pp. 789-800.
- (PSI version) Huberman, Bernardo A., Matt Franklin, and Tad Hogg. Enhancing Privacy and Trust in Electronic Communities. EC 1999, ACM, pp. 78-86.

### Circuit PSI

- Pinkas, Benny, Thomas Schneider, Oleksandr Tkachenko, and Avishay Yanai. Efficient Circuit-Based PSI with Linear Communication. EUROCRYPT 2019, Part III, Springer International Publishing, pp. 122-153.
- Chandran, Nishanth, Divya Gupta, and Akash Shah. Circuit-PSI with Linear Complexity via Relaxed Batch OPPRF. PETS 2022, no. 1, pp. 353-372.
- Son, Yongha, and Jinhyuck Jeong. PSI with computation or Circuit-PSI for Unbalanced Sets from Homomorphic Encryption.. AsiaCCS 2023, pp. 342-356. 2023.

### Private Set Union and Private ID

- Jia, Yanxue, Shi-Feng Sun, Hong-Sheng Zhou, Jiajun Du, and Dawu Gu. Shuffle-based Private Set Union: Faster and More Secure. USENIX Security 2022, pp. 2947-2964.
- Garimella, Gayathri, Payman Mohassel, Mike Rosulek, Saeed Sadeghian, and Jaspal Singh. Private Set Operations from Oblivious Switching. PKC 2021, Cham: Springer International Publishing, pp. 591-617.
- Kolesnikov, Vladimir, Mike Rosulek, Ni Trieu, and Xiao Wang. Scalable Private Set Union from Symmetric-key Techniques. ASIACRYPT 2019, Part II, Cham: Springer International Publishing, pp. 636-666.
- Buddhavarapu, Prasad, Andrew Knox, Payman Mohassel, Shubho Sengupta, Erik Taubeneck, and Vlad Vlaskin. Private Matching for Compute. Cryptology ePrint Archive, Paper 2020/599.
- Tu, Binbin, Yu Chen, Qi Liu, and Cong Zhang. Fast Unbalanced Private Set Union from Fully Homomorphic Encryption. CCS 2023, pp. 2959-2973. 2023.

### PSI Cardinality

- (PSI Cardinality version) Huberman, Bernardo A., Matt Franklin, and Tad Hogg. Enhancing Privacy and Trust in Electronic Communities. EC 1999, ACM, pp. 78-86.
- De Cristofaro, Emiliano, Paolo Gasti, and Gene Tsudik. Fast and Private Computation of Cardinality of Set Intersection and Union. CANS 2012, pp. 218-231. Springer Berlin Heidelberg, 2012.

### Other PSO

- Kamara, Seny, Payman Mohassel, Mariana Raykova, and Saeed Sadeghian. Scaling Private Set Intersection to Billion-element Sets. FC 2014, pp. 195-215. Springer Berlin Heidelberg, 2014.