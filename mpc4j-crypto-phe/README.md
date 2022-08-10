# Introduction

The module `mpc4j-crypto-phe` is a Java library for Partially Homomorphic Encryption (PHE). Parts of implementations (specifically, plaintext representations) are from [Javallier](https://github.com/n1analytics/javallier), with modifications to additionally support other schemes and optimizations.

# Schemes and Optimizations

Currently, the implementations include the following schemes:

- \[OU98\] Tatsuaki Okamoto, and Shigenori Uchiyama. A new public-key cryptosystem as secure as factoring. EUROCRYPT 1998, Springer, Berlin, Heidelberg, pp. 308-318.
- \[Pai99\] Pascal Paillier. Public-key cryptosystems based on composite degree residuosity classes. EUROCRYPT 1999, Springer, Berlin, Heidelberg, pp. 223-238.

We also introduce the following optimizations:

- \[CNP99\] J-S. Coron, David Naccache, and Pascal Paillier. Accelerating Okamoto-Uchiyama public-key cryptosystem. Electronics Letters, 1999, 35(4), pp. 291-292.
- \[CGH+01\] Dario Catalano, Rosario Gennaro, Nick Howgrave-Graham, and Phong Q. Nguyen. Paillier's cryptosystem revisited. CCS 2001, ACM, pp. 206-214.
- \[DJN10\] Ivan Damgård, Mads Jurik, and Jesper Buus Nielsen. A generalization of Paillier’s public-key system with applications to electronic voting. International Journal of Information Security, 2010, 9(6), pp. 371-385.
- \[MHL21\] Huanyu Ma, Shuai Han, and Hao Lei. Optimized Paillier’s Cryptosystem with Fast Encryption and Decryption. ACSAC 2021, pp. 106-118.

Note that we **do not introduce all optimizations**, especially for the ones using some special primes in key generation. The experiments show that finding these special primes is very slow. Although it is reasonable to assume that key generation can be run ahead of time, such a long-time key generation is not good enough for applications in practice.

# About Javallier

The descriptions below are from [READMD.md](https://github.com/n1analytics/javallier/blob/master/README.md) in Javallier.

## Javallier

A Java library for [Paillier partially homomorphic encryption](https://en.wikipedia.org/wiki/Paillier_cryptosystem) based on [python-paillier](https://github.com/NICTA/python-paillier). 

The homomorphic properties of the paillier cryptosystem are:

- Encrypted numbers can be multiplied by a non encrypted scalar.
- Encrypted numbers can be added together. 
- Encrypted numbers can be added to non encrypted scalars.

## Release

Releases will be signed by [Brian Thorne](https://keybase.io/hardbyte) with the PGP key
[22AD F3BF C183 47DE](https://pgp.mit.edu/pks/lookup?op=vindex&search=0x22ADF3BFC18347DE).

## Limitation

Adding two encrypted numbers where the exponents differ wildly may result in overflow in the `EncryptedNumber` (changed to `PheCiphertext` in `mpc4j-crypto-phe`) domain. The addition result can be successfully decrypted and decoded but the computation result is incorrect. Current implementation does not detect such overflow. 