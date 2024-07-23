package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.BytesField;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * Direct GF(2^l), where the mix multiplication is done by directly treating a subfield element as a field element.
 *
 * @author Weiran Liu
 * @date 2024/6/11
 */
public class Dgf2k implements BytesField {
    /**
     * subfield
     */
    private final Gf2e subfield;
    /**
     * subfield L
     */
    private final int subfieldL;
    /**
     * subfield byte L
     */
    private final int subfieldByteL;
    /**
     * field
     */
    private final Gf2k field;
    /**
     * field L
     */
    private final int fieldL;
    /**
     * field byte L
     */
    private final int fieldByteL;
    /**
     * r
     */
    private final int r;

    /**
     * Creates an abstract combined GF2K.
     *
     * @param envType   environment.
     * @param subfieldL subfield L.
     */
    Dgf2k(EnvType envType, int subfieldL) {
        // l ∈ {1, 2, 4, 8, 16, 32, 64, 128}
        Preconditions.checkArgument(
            IntMath.isPowerOfTwo(subfieldL) && subfieldL <= CommonConstants.BLOCK_BIT_LENGTH
        );
        // init subfield
        subfield = Gf2eFactory.createInstance(envType, subfieldL);
        this.subfieldL = subfieldL;
        subfieldByteL = subfield.getByteL();
        // init field
        field = Gf2kFactory.createInstance(envType);
        fieldL = CommonConstants.BLOCK_BIT_LENGTH;
        fieldByteL = CommonConstants.BLOCK_BYTE_LENGTH;
        r = fieldL / subfieldL;
    }

    /**
     * Gets the subfield.
     *
     * @return subfield.
     */
    public Gf2e getSubfield() {
        return subfield;
    }

    /**
     * Gets the maximal l (in bit length) so that all elements in {0, 1}^l is a valid subfield element.
     *
     * @return the maximal l (in bit length) for the subfield.
     */
    public int getSubfieldL() {
        return subfieldL;
    }

    /**
     * Gets the maximal l (in byte length) so that all elements in {0, 1}^l is a valid subfield element.
     *
     * @return the maximal l (in byte length) for the subfield.
     */
    public int getSubfieldByteL() {
        return subfieldByteL;
    }

    @Override
    public int getL() {
        return fieldL;
    }

    @Override
    public int getByteL() {
        return fieldByteL;
    }

    @Override
    public int getElementBitLength() {
        return fieldL;
    }

    @Override
    public int getElementByteLength() {
        return fieldByteL;
    }

    /**
     * gets r, i.e., field L / subfield L.
     *
     * @return r.
     */
    public int getR() {
        return r;
    }

    @Override
    public byte[] add(byte[] p, byte[] q) {
        return field.add(p, q);
    }

    @Override
    public void addi(byte[] p, byte[] q) {
        field.addi(p, q);
    }

    @Override
    public byte[] neg(byte[] p) {
        return field.neg(p);
    }

    @Override
    public void negi(byte[] p) {
        field.negi(p);
    }

    @Override
    public byte[] sub(byte[] p, byte[] q) {
        return field.sub(p, q);
    }

    @Override
    public void subi(byte[] p, byte[] q) {
        field.subi(p, q);
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        return field.mul(p, q);
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        field.muli(p, q);
    }

    @Override
    public byte[] inv(byte[] p) {
        return field.inv(p);
    }

    @Override
    public void invi(byte[] p) {
        field.invi(p);
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        return field.div(p, q);
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        field.divi(p, q);
    }

    @Override
    public byte[] createZero() {
        return field.createZero();
    }

    @Override
    public byte[] createOne() {
        return field.createOne();
    }

    @Override
    public byte[] createRandom(SecureRandom secureRandom) {
        return field.createRandom(secureRandom);
    }

    @Override
    public byte[] createRandom(byte[] seed) {
        return field.createRandom(seed);
    }

    @Override
    public byte[] createNonZeroRandom(SecureRandom secureRandom) {
        return field.createNonZeroRandom(secureRandom);
    }

    @Override
    public byte[] createNonZeroRandom(byte[] seed) {
        return field.createNonZeroRandom(seed);
    }

    @Override
    public byte[] createRangeRandom(SecureRandom secureRandom) {
        return field.createRangeRandom(secureRandom);
    }

    @Override
    public byte[] createRangeRandom(byte[] seed) {
        return field.createRangeRandom(seed);
    }

    @Override
    public boolean isZero(byte[] p) {
        return field.isZero(p);
    }

    @Override
    public boolean isOne(byte[] p) {
        return field.isOne(p);
    }

    @Override
    public boolean validateElement(byte[] p) {
        return field.validateElement(p);
    }

    @Override
    public boolean validateNonZeroElement(byte[] p) {
        return field.validateNonZeroElement(p);
    }

    @Override
    public boolean validateRangeElement(byte[] p) {
        return field.validateRangeElement(p);
    }

    /**
     * Extends the subfield element to a field element.
     *
     * @param subfieldElement the subfield element.
     * @return the field element.
     */
    public byte[] extend(byte[] subfieldElement) {
        subfield.validateElement(subfieldElement);
        byte[] fieldElement = new byte[fieldByteL];
        System.arraycopy(subfieldElement, 0, fieldElement, fieldByteL - subfieldByteL, subfieldByteL);
        return fieldElement;
    }

    /**
     * Computes p · X^h, where p is in subfield, and the result is in field.
     *
     * @param p the subfield element p.
     * @param h X^h.
     * @return p · X^h.
     */
    public byte[] mixPow(byte[] p, int h) {
        assert subfield.validateElement(p);
        assert h >= 0 && h < r : "h must be in range [0, " + r + "): " + h;
        byte[] result = createZero();
        switch (subfieldL) {
            case 2:
                result[(r - 1 - h) / 4] |= (p[0] << (h * 2) % 8);
                break;
            case 4:
                result[(r - 1 - h) / 2] |= (p[0] << (h * 4) % 8);
                break;
            case 8:
                result[(r - 1 - h)] = (p[0]);
                break;
            case 16:
                result[(r - 1 - h) * 2] = (p[0]);
                result[(r - 1 - h) * 2 + 1] = (p[1]);
                break;
            case 32:
                result[(r - 1 - h) * 4] = (p[0]);
                result[(r - 1 - h) * 4 + 1] = (p[1]);
                result[(r - 1 - h) * 4 + 2] = (p[2]);
                result[(r - 1 - h) * 4 + 3] = (p[3]);
                break;
            case 64:
                result[(r - 1 - h) * 8] = (p[0]);
                result[(r - 1 - h) * 8 + 1] = (p[1]);
                result[(r - 1 - h) * 8 + 2] = (p[2]);
                result[(r - 1 - h) * 8 + 3] = (p[3]);
                result[(r - 1 - h) * 8 + 4] = (p[4]);
                result[(r - 1 - h) * 8 + 5] = (p[5]);
                result[(r - 1 - h) * 8 + 6] = (p[6]);
                result[(r - 1 - h) * 8 + 7] = (p[7]);
                break;
            case 128:
                BytesUtils.ori(result, p);
                break;
            default:
                throw new IllegalStateException("Invalid subfield L, must be ∈ {2, 4, 8, 16, 32, 64, 128}: " + subfieldL);
        }
        return result;
    }

    /**
     * Computes p · X^h, where p is in field, and the result is in field.
     *
     * @param p the field element p.
     * @param h X^h.
     * @return p · X^h.
     */
    public byte[] fieldPow(byte[] p, int h) {
        assert validateElement(p);
        assert h >= 0 && h < r : "h must be in range [0, " + r + "): " + h;
        byte[] result = createZero();
        BinaryUtils.setBoolean(result, (r - h) * subfieldL - 1, true);
        muli(result, p);
        return result;
    }

    /**
     * Computes p · q, where p is in subfield, q is in field, and the result is in field.
     *
     * @param p the subfield element p.
     * @param q the field element q.
     * @return p · q.
     */
    public byte[] mixMul(byte[] p, byte[] q) {
        assert subfield.validateElement(p);
        byte[] result = extend(p);
        muli(result, q);
        return result;
    }

    /**
     * For a vector (x_0, ..., x_127) where x_i ∈ F_{2^t}, computes x_0 * X^{127} + ... + x_127 * X^0.
     *
     * @param xs xs.
     * @return inner product.
     */
    public byte[] mixInnerProduct(byte[][] xs) {
        int fieldL = getL();
        assert xs.length == fieldL;
        byte[] result = createZero();
        for (int i = getL() - 1; i >= 0; i--) {
            byte[] shift = createZero();
            BinaryUtils.setBoolean(shift, i, true);
            addi(result, mul(extend(xs[i]), shift));
        }
        return result;
    }

    /**
     * For a vector (x_0, ..., x_{r - 1}) where x_i ∈ F_{{2^t}}, computes x_{r - 1} * X^{r - 1} + ... + x_0 * X^0.
     *
     * @param xs xs.
     * @return inner product.
     */
    public byte[] innerProduct(byte[][] xs) {
        int r = getR();
        assert xs.length == r;
        byte[] result = createZero();
        for (int h = r - 1; h >= 0; h--) {
            byte[] mul = fieldPow(xs[h], h);
            addi(result, mul);
        }
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (t = " + subfieldL + ", r = " + r + ")";
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(field)
            .append(subfield)
            .hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Dgf2k that = (Dgf2k) o;
        // KDF and PRG can be different, all GF2K instance are the same
        return this.subfieldL == that.subfieldL;
    }
}
