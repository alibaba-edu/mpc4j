package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import cc.redberry.rings.poly.FiniteField;
import cc.redberry.rings.poly.univar.UnivariatePolynomial;
import cc.redberry.rings.poly.univar.UnivariatePolynomialZp64;
import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eManager;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.Gf2xUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * abstract Subfield GF2K for l ∈ {2, 4, 8, 16, 32, 64}.
 *
 * @author Weiran Liu
 * @date 2024/6/2
 */
abstract class AbstractSubSgf2k implements Sgf2k {
    /**
     * subfield
     */
    protected final Gf2e subfield;
    /**
     * subfield finite field
     */
    protected final FiniteField<UnivariatePolynomialZp64> subFieldFiniteField;
    /**
     * subfield minimal polynomial
     */
    protected final byte[] subfieldMinimalPolynomial;
    /**
     * subfield L
     */
    protected final int subfieldL;
    /**
     * subfield byte L
     */
    protected final int subfieldByteL;
    /**
     * field finite field
     */
    protected final FiniteField<UnivariatePolynomial<UnivariatePolynomialZp64>> fieldFiniteField;
    /**
     * field minimal polynomial
     */
    protected final byte[][] fieldMinimalPolynomial;
    /**
     * field L
     */
    protected final int fieldL;
    /**
     * field byte L
     */
    protected final int fieldByteL;
    /**
     * r
     */
    protected final int r;
    /**
     * the zero field element
     */
    protected final byte[] fieldElementZero;
    /**
     * the identity field element
     */
    protected final byte[] fieldElementOne;
    /**
     * the key derivation function for field
     */
    protected final Kdf fieldKdf;
    /**
     * the pseudo-random generator for field
     */
    protected final Prg fieldPrg;

    /**
     * Creates an abstract combined GF2K.
     *
     * @param envType   environment.
     * @param subfieldL subfield L.
     */
    protected AbstractSubSgf2k(EnvType envType, int subfieldL) {
        // l ∈ {2, 4, 8, 16, 32, 64}
        Preconditions.checkArgument(
            IntMath.isPowerOfTwo(subfieldL) && subfieldL > 1 && subfieldL < CommonConstants.BLOCK_BIT_LENGTH
        );
        // init subfield
        subfield = Gf2eFactory.createInstance(envType, subfieldL);
        subFieldFiniteField = Gf2eManager.getFiniteField(subfieldL);
        subfieldMinimalPolynomial = Gf2eManager.getMinimalPolynomial(subfieldL);
        this.subfieldL = subfieldL;
        subfieldByteL = subfield.getByteL();
        // init field
        fieldFiniteField = Sgf2kManager.getFieldFiniteField(subfieldL);
        fieldMinimalPolynomial = Sgf2kManager.getFieldMinimalPolynomial(subfieldL);
        fieldL = CommonConstants.BLOCK_BIT_LENGTH;
        fieldByteL = CommonConstants.BLOCK_BYTE_LENGTH;
        r = fieldL / subfieldL;
        fieldElementZero = createZero();
        fieldElementOne = createOne();
        fieldKdf = KdfFactory.createInstance(envType);
        fieldPrg = PrgFactory.createInstance(envType, fieldByteL);
    }

    @Override
    public int getSubfieldL() {
        return subfieldL;
    }

    @Override
    public int getSubfieldByteL() {
        return subfieldByteL;
    }

    @Override
    public Gf2e getSubfield() {
        return subfield;
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

    @Override
    public byte[][] decomposite(byte[] fieldElement) {
        assert validateElement(fieldElement);
        byte[][] subfieldElements = new byte[r][];
        switch (subfieldL) {
            case 2:
                for (int i = 0; i < r; i++) {
                    subfieldElements[i] = new byte[]{(byte) ((fieldElement[(r - 1 - i) / 4] >>> ((i * 2) % 8)) & 0b00000011)};
                }
                break;
            case 4:
                for (int i = 0; i < r; i++) {
                    subfieldElements[i] = new byte[]{(byte) ((fieldElement[(r - 1 - i) / 2] >>> ((i * 4) % 8)) & 0b00001111)};
                }
                break;
            case 8:
                for (int i = 0; i < r; i++) {
                    subfieldElements[i] = new byte[]{fieldElement[r - 1 - i]};
                }
                break;
            case 16:
                for (int i = 0; i < r; i++) {
                    subfieldElements[i] = new byte[]{fieldElement[(r - 1 - i) * 2], fieldElement[(r - 1 - i) * 2 + 1]};
                }
                break;
            case 32:
                for (int i = 0; i < r; i++) {
                    subfieldElements[i] = new byte[]{
                        fieldElement[(r - 1 - i) * 4], fieldElement[(r - 1 - i) * 4 + 1],
                        fieldElement[(r - 1 - i) * 4 + 2], fieldElement[(r - 1 - i) * 4 + 3]
                    };
                }
                break;
            case 64:
                for (int i = 0; i < r; i++) {
                    subfieldElements[i] = new byte[]{
                        fieldElement[(r - 1 - i) * 8], fieldElement[(r - 1 - i) * 8 + 1],
                        fieldElement[(r - 1 - i) * 8 + 2], fieldElement[(r - 1 - i) * 8 + 3],
                        fieldElement[(r - 1 - i) * 8 + 4], fieldElement[(r - 1 - i) * 8 + 5],
                        fieldElement[(r - 1 - i) * 8 + 6], fieldElement[(r - 1 - i) * 8 + 7]
                    };
                }
                break;
            case 128:
                subfieldElements[0] = BytesUtils.clone(fieldElement);
                break;
            default:
                throw new IllegalStateException("Invalid subfield L, must be ∈ {2, 4, 8, 16, 32, 64, 128}: " + subfieldL);

        }
        return subfieldElements;
    }

    @Override
    public byte[] composite(byte[][] subfieldElements) {
        assert subfieldElements.length == r;
        Gf2e subfield = getSubfield();
        byte[] p = new byte[fieldByteL];
        switch (subfieldL) {
            case 2:
                for (int i = 0; i < r; i++) {
                    assert subfield.validateElement(subfieldElements[i]);
                    p[(r - 1 - i) / 4] |= (subfieldElements[i][0] << (i * 2) % 8);
                }
                break;
            case 4:
                for (int i = 0; i < r; i++) {
                    assert subfield.validateElement(subfieldElements[i]);
                    p[(r - 1 - i) / 2] |= (subfieldElements[i][0] << (i * 4) % 8);
                }
                break;
            case 8:
                for (int i = 0; i < r; i++) {
                    assert subfield.validateElement(subfieldElements[i]);
                    p[(r - 1 - i)] = (subfieldElements[i][0]);
                }
                break;
            case 16:
                for (int i = 0; i < r; i++) {
                    assert subfield.validateElement(subfieldElements[i]);
                    p[(r - 1 - i) * 2] = (subfieldElements[i][0]);
                    p[(r - 1 - i) * 2 + 1] = (subfieldElements[i][1]);
                }
                break;
            case 32:
                for (int i = 0; i < r; i++) {
                    assert subfield.validateElement(subfieldElements[i]);
                    p[(r - 1 - i) * 4] = (subfieldElements[i][0]);
                    p[(r - 1 - i) * 4 + 1] = (subfieldElements[i][1]);
                    p[(r - 1 - i) * 4 + 2] = (subfieldElements[i][2]);
                    p[(r - 1 - i) * 4 + 3] = (subfieldElements[i][3]);
                }
                break;
            case 64:
                for (int i = 0; i < r; i++) {
                    assert subfield.validateElement(subfieldElements[i]);
                    p[(r - 1 - i) * 8] = (subfieldElements[i][0]);
                    p[(r - 1 - i) * 8 + 1] = (subfieldElements[i][1]);
                    p[(r - 1 - i) * 8 + 2] = (subfieldElements[i][2]);
                    p[(r - 1 - i) * 8 + 3] = (subfieldElements[i][3]);
                    p[(r - 1 - i) * 8 + 4] = (subfieldElements[i][4]);
                    p[(r - 1 - i) * 8 + 5] = (subfieldElements[i][5]);
                    p[(r - 1 - i) * 8 + 6] = (subfieldElements[i][6]);
                    p[(r - 1 - i) * 8 + 7] = (subfieldElements[i][7]);
                }
                break;
            case 128:
                BytesUtils.ori(p, subfieldElements[0]);
                break;
            default:
                throw new IllegalStateException("Invalid subfield L, must be ∈ {2, 4, 8, 16, 32, 64, 128}: " + subfieldL);
        }
        return p;
    }

    @Override
    public byte[] add(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        // p + q is bit-wise p ⊕ q
        return BytesUtils.xor(p, q);
    }

    @Override
    public void addi(byte[] p, byte[] q) {
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
    public byte[] sub(byte[] p, byte[] q) {
        // p - q = p + (-q) = p + q
        return add(p, q);
    }

    @Override
    public void subi(byte[] p, byte[] q) {
        // p - q = p + (-q) = p + q
        addi(p, q);
    }

    @Override
    public byte[] createZero() {
        return new byte[fieldByteL];
    }

    @Override
    public byte[] createOne() {
        byte[] one = new byte[fieldByteL];
        one[one.length - 1] = 0x01;
        return one;
    }

    @Override
    public byte[] createRandom(SecureRandom secureRandom) {
        return BytesUtils.randomByteArray(fieldByteL, secureRandom);
    }

    @Override
    public byte[] createRandom(byte[] seed) {
        byte[] key = fieldKdf.deriveKey(seed);
        return fieldPrg.extendToBytes(key);
    }

    @Override
    public byte[] createNonZeroRandom(SecureRandom secureRandom) {
        byte[] randomFieldElement = new byte[fieldByteL];
        while (isZero(randomFieldElement)) {
            secureRandom.nextBytes(randomFieldElement);
        }
        return randomFieldElement;
    }

    @Override
    public byte[] createNonZeroRandom(byte[] seed) {
        byte[] randomFieldElement;
        byte[] key = BytesUtils.clone(seed);
        do {
            key = fieldKdf.deriveKey(key);
            randomFieldElement = createRandom(key);
        } while (isZero(randomFieldElement));
        return randomFieldElement;
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
        return BytesUtils.equals(p, fieldElementZero);
    }

    @Override
    public boolean isOne(byte[] p) {
        assert validateElement(p);
        return BytesUtils.equals(p, fieldElementOne);
    }

    @Override
    public boolean validateElement(byte[] p) {
        return p.length == fieldByteL;
    }

    @Override
    public boolean validateNonZeroElement(byte[] p) {
        return !isZero(p);
    }

    @Override
    public boolean validateRangeElement(byte[] p) {
        return p.length == fieldByteL;
    }

    @Override
    public byte[] extend(byte[] subfieldElement) {
        assert subfield.validateElement(subfieldElement);
        byte[] fieldElement = new byte[fieldByteL];
        System.arraycopy(subfieldElement, 0, fieldElement, fieldByteL - subfieldByteL, subfieldByteL);
        return fieldElement;
    }

    @Override
    public int getR() {
        return r;
    }

    @Override
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

    @Override
    public byte[] fieldPow(byte[] p, int h) {
        assert validateElement(p);
        assert h >= 0 && h < r : "h must be in range [0, " + r + "): " + h;
        byte[] result = createZero();
        BinaryUtils.setBoolean(result, (r - h) * subfieldL - 1, true);
        muli(result, p);
        return result;
    }

    protected UnivariatePolynomial<UnivariatePolynomialZp64> createRingsFieldElement(byte[] p) {
        byte[][] subfieldElements = decomposite(p);
        UnivariatePolynomial<UnivariatePolynomialZp64> ringsFieldElement = UnivariatePolynomial.zero(subFieldFiniteField);
        for (int i = 0; i < subfieldElements.length; i++) {
            ringsFieldElement.set(i, Gf2xUtils.byteArrayToRings(subfieldElements[i]));
        }
        return ringsFieldElement;
    }

    protected byte[] createFieldElement(UnivariatePolynomial<UnivariatePolynomialZp64> ringsFieldElement) {
        byte[][] subfieldElements = new byte[r][];
        for (int i = 0; i < r; i++) {
            subfieldElements[i] = Gf2xUtils.ringsToByteArray(ringsFieldElement.get(i), subfieldByteL);
        }
        return composite(subfieldElements);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (t = " + subfieldL + ", r = " + r + ")";
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(fieldFiniteField)
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
        AbstractSubSgf2k that = (AbstractSubSgf2k) o;
        // KDF and PRG can be different, all GF2K instance are the same
        return this.fieldFiniteField.equals(that.fieldFiniteField);
    }
}
