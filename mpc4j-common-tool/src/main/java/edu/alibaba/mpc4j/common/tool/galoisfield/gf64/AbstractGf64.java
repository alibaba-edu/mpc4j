package edu.alibaba.mpc4j.common.tool.galoisfield.gf64;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.security.SecureRandom;

/**
 * abstract GF(2^64).
 *
 * @author Weiran Liu
 * @date 2023/8/28
 */
abstract class AbstractGf64 implements Gf64 {
    /**
     * l = 64 (in bit length)
     */
    private static final int L = 64;
    /**
     * l = 64 (in byte length)
     */
    protected static final int BYTE_L = 8;
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

    public AbstractGf64(EnvType envType) {
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
        return BytesUtils.xor(p, q);
    }

    @Override
    public void addi(byte[] p, final byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        // p + q is bit-wise p ⊕ q
        BytesUtils.xori(p, q);
    }

    @Override
    public byte[] neg(byte[] p) {
        assert validateElement(p);
        // -p = p
        return BytesUtils.clone(p);
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
        byte[] element = new byte[BYTE_L];
        secureRandom.nextBytes(element);
        return element;
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
        byte[] key = BytesUtils.clone(seed);
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
        return BytesUtils.equals(p, zero);
    }

    @Override
    public boolean isOne(byte[] p) {
        assert validateElement(p);
        return BytesUtils.equals(p, one);
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
        AbstractGf64 that = (AbstractGf64) o;
        // KDF and PRG can be different, all GF2K instance are the same
        return this.getL() == that.getL();
    }

    @Override
    public int hashCode() {
        return "GF64".hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (l = " + L + ")";
    }
}
