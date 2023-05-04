package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * abstract GF(2^l).
 *
 * @author Weiran Liu
 * @date 2022/8/7
 */
abstract class AbstractGf2e implements Gf2e {
    /**
     * the element bit length
     */
    protected final int l;
    /**
     * the element byte length
     */
    protected final int byteL;
    /**
     * the finite field
     */
    protected final FiniteField<UnivariatePolynomialZp64> finiteField;
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
    /**
     * special case for GF(2^k)
     */
    protected final Gf2k gf2k;

    AbstractGf2e(EnvType envType, int l) {
        assert l > 0;
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        finiteField = Gf2eManager.getFiniteField(l);
        zero = createZero();
        one = createOne();
        kdf = KdfFactory.createInstance(envType);
        prg = PrgFactory.createInstance(envType, byteL);
        gf2k = Gf2kFactory.createInstance(envType);
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int getByteL() {
        return byteL;
    }

    @Override
    public int getElementBitLength() {
        return l;
    }

    @Override
    public int getElementByteLength() {
        return byteL;
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
        return new byte[byteL];
    }

    @Override
    public byte[] createOne() {
        byte[] one = new byte[byteL];
        one[one.length - 1] = (byte) 0x01;
        return one;
    }

    @Override
    public byte[] createRandom(SecureRandom secureRandom) {
        return BytesUtils.randomByteArray(byteL, l, secureRandom);
    }

    @Override
    public byte[] createRandom(byte[] seed) {
        byte[] key = kdf.deriveKey(seed);
        byte[] element = prg.extendToBytes(key);
        BytesUtils.reduceByteArray(element, l);
        return element;
    }

    @Override
    public byte[] createNonZeroRandom(SecureRandom secureRandom) {
        byte[] element;
        do {
            element = createRandom(secureRandom);
        } while (isZero(element));
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
        return Arrays.equals(p, zero);
    }

    @Override
    public boolean isOne(byte[] p) {
        assert validateElement(p);
        return Arrays.equals(p, one);
    }

    @Override
    public boolean validateElement(byte[] p) {
        return BytesUtils.isFixedReduceByteArray(p, byteL, l);
    }

    @Override
    public boolean validateNonZeroElement(byte[] p) {
        return BytesUtils.isFixedReduceByteArray(p, byteL, l) && !isZero(p);
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
        AbstractGf2e that = (AbstractGf2e) o;
        // KDF and PRG can be different
        return this.finiteField.equals(that.finiteField);
    }

    @Override
    public int hashCode() {
        return finiteField.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (" + finiteField.toString() + ")";
    }
}
