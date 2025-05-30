package edu.alibaba.mpc4j.common.tool.galoisfield.gf2k;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;

import java.security.SecureRandom;

/**
 * abstract GF(2^128).
 *
 * @author Weiran Liu
 * @date 2023/2/17
 */
abstract class AbstractGf2k implements Gf2k {
    /**
     * l = 128 (in bit length)
     */
    protected static final int L = CommonConstants.BLOCK_BIT_LENGTH;
    /**
     * l = 128 (in byte length)
     */
    protected static final int BYTE_L = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * the zero element
     */
    protected final byte[] zero;
    /**
     * the identity element
     */
    protected final byte[] one;
    /**
     * the key derivation function
     */
    private final Kdf kdf;
    /**
     * the pseudo-random generator
     */
    private final Prg prg;

    public AbstractGf2k(EnvType envType) {
        zero = createZero();
        one = createOne();
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, BYTE_L);
    }

    @Override
    public int getL() {
        return L;
    }

    @Override
    public int getByteL() {
        return BYTE_L;
    }

    @Override
    public int getElementBitLength() {
        return L;
    }

    @Override
    public int getElementByteLength() {
        return BYTE_L;
    }

    @Override
    public byte[] add(final byte[] p, final byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        // p + q is bit-wise p ⊕ q
        return BlockUtils.xor(p, q);
    }

    @Override
    public void addi(byte[] p, final byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        // p + q is bit-wise p ⊕ q
        BlockUtils.xori(p, q);
    }

    @Override
    public byte[] neg(byte[] p) {
        assert validateElement(p);
        // -p = p
        return BlockUtils.clone(p);
    }

    @Override
    public void negi(byte[] p) {
        // -p = p
        assert validateElement(p);
    }

    @Override
    public byte[] sub(final byte[] p, final byte[] q) {
        // p - q = p + (-q) = p + q
        return add(p, q);
    }

    @Override
    public void subi(byte[] p, final byte[] q) {
        // p - q = p + (-q) = p + q
        addi(p, q);
    }

    @Override
    public byte[] createZero() {
        return new byte[BYTE_L];
    }

    @Override
    public byte[] createOne() {
        byte[] one = new byte[BYTE_L];
        one[one.length - 1] = 0x01;
        return one;
    }

    @Override
    public byte[] createRandom(SecureRandom secureRandom) {
        return BlockUtils.randomBlock(secureRandom);
    }

    @Override
    public byte[] createRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        return prg.extendToBytes(key);
    }

    @Override
    public byte[] createNonZeroRandom(SecureRandom secureRandom) {
        byte[] element = new byte[BYTE_L];
        while (isZero(element)) {
            secureRandom.nextBytes(element);
        }
        return element;
    }

    @Override
    public byte[] createNonZeroRandom(byte[] seed) {
        byte[] random;
        byte[] key = BlockUtils.clone(seed);
        do {
            key = kdf.deriveKey(key);
            random = createRandom(key);
        } while (isZero(random));
        return random;
    }

    @Override
    public byte[] createRangeRandom(SecureRandom secureRandom) {
        return createRandom(secureRandom);
    }

    @Override
    public byte[] createRangeRandom(byte[] seed) {
        return createRandom(seed);
    }

    @Override
    public boolean isZero(byte[] p) {
        assert validateElement(p);
        return BlockUtils.equals(p, zero);
    }

    @Override
    public boolean isOne(byte[] p) {
        assert validateElement(p);
        return BlockUtils.equals(p, one);
    }

    @Override
    public boolean validateElement(byte[] p) {
        return p.length == BYTE_L;
    }

    @Override
    public boolean validateNonZeroElement(byte[] p) {
        return !isZero(p);
    }

    @Override
    public boolean validateRangeElement(byte[] p) {
        return validateElement(p);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractGf2k that = (AbstractGf2k) o;
        // KDF and PRG can be different, all GF2K instance are the same
        return this.getL() == that.getL();
    }

    @Override
    public int hashCode() {
        return "GF2K".hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (l = " + L + ")";
    }
}
