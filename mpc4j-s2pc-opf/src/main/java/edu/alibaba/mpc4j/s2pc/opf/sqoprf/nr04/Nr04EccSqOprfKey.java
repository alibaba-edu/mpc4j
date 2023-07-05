package edu.alibaba.mpc4j.s2pc.opf.sqoprf.nr04;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * NR04 ECC single-query OPRF key.
 *
 * @author Qixian Zhou
 * @date 2023/4/12
 */
public class Nr04EccSqOprfKey implements SqOprfKey {
    /**
     * ECC
     */
    private final Ecc ecc;
    /**
     * key derivation function
     */
    private final Kdf kdf;
    /**
     * hash function
     */
    private final Hash hash;
    /**
     * Zp instance
     */
    private final Zp zp;
    /**
     * a_0 = (a_1^0, a_2^0, ..., a_n^0), a_i^0 is n-bit random number, n is the bit-length of input.
     * The default value of n is 128.
     */
    private final BigInteger[] a0Array;
    /**
     * a_1 = (a_1^1, a_2^1, ..., a_n^1), a_i^1 is n-bit random number, n is the bit-length of input.
     * The default value of n is 128.
     */
    private final BigInteger[] a1Array;

    Nr04EccSqOprfKey(EnvType envType, BigInteger[] a0Array, BigInteger[] a1Array) {
        // require a0.length and a1.length are all κ
        MathPreconditions.checkEqual("a0.length", "κ", a0Array.length, CommonConstants.BLOCK_BIT_LENGTH);
        MathPreconditions.checkEqual("a1.length", "κ", a1Array.length, CommonConstants.BLOCK_BIT_LENGTH);
        ecc = EccFactory.createInstance(envType);
        kdf = KdfFactory.createInstance(envType);
        hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        zp = ZpFactory.createInstance(envType, ecc.getN());
        this.a0Array = Arrays.stream(a0Array)
            .peek(a0 -> Preconditions.checkArgument(zp.validateNonZeroElement(a0)))
            .toArray(BigInteger[]::new);
        this.a1Array = Arrays.stream(a1Array)
            .peek(a1 -> Preconditions.checkArgument(zp.validateNonZeroElement(a1)))
            .toArray(BigInteger[]::new);
    }

    public BigInteger getA0Array(int index) {
        return a0Array[index];
    }

    public BigInteger getA1Array(int index) {
        return a1Array[index];
    }

    @Override
    public byte[] getPrf(byte[] input) {
        byte[] inputHash = hash.digestToBytes(input);
        boolean[] inputBinaryHash = BinaryUtils.byteArrayToBinary(inputHash, CommonConstants.BLOCK_BIT_LENGTH);
        // c = a_0^{x[0]} * a_1^{x[1]} * ... * a_n^{x[n]} mod q, where x[i] represents the i-th bit of input
        BigInteger c = BigInteger.ONE;
        for (int i = 0; i < CommonConstants.BLOCK_BIT_LENGTH; i++) {
            if (inputBinaryHash[i]) {
                c = zp.mul(c, a1Array[i]);
            } else {
                c = zp.mul(c, a0Array[i]);
            }
        }
        // g^c
        return kdf.deriveKey(ecc.encode(ecc.multiply(ecc.getG(), c), false));
    }
}
