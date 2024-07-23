package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;

/**
 * Subfield GF2K (t = 128, r = 1).
 *
 * @author Weiran Liu
 * @date 2024/6/2
 */
public class Sgf2k128 implements Sgf2k {
    /**
     * field
     */
    private final Gf2k field;

    public Sgf2k128(EnvType envType) {
        field = Gf2kFactory.createInstance(envType);
    }

    @Override
    public Gf2e getSubfield() {
        return field;
    }

    @Override
    public int getSubfieldL() {
        return field.getL();
    }

    @Override
    public int getSubfieldByteL() {
        return field.getByteL();
    }

    @Override
    public int getL() {
        return field.getL();
    }

    @Override
    public int getByteL() {
        return field.getByteL();
    }

    @Override
    public int getElementBitLength() {
        return field.getElementBitLength();
    }

    @Override
    public int getElementByteLength() {
        return field.getElementByteLength();
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

    @Override
    public int getR() {
        return 1;
    }

    @Override
    public byte[][] decomposite(byte[] fieldElement) {
        assert validateElement(fieldElement);
        return new byte[][] {BytesUtils.clone(fieldElement)};
    }

    @Override
    public byte[] composite(byte[][] subfieldElements) {
        assert subfieldElements.length == 1;
        return BytesUtils.clone(subfieldElements[0]);
    }

    @Override
    public byte[] extend(byte[] subfieldElement) {
        assert field.validateElement(subfieldElement);
        return BytesUtils.clone(subfieldElement);
    }

    @Override
    public byte[] mixPow(byte[] p, int h) {
        assert field.validateElement(p);
        assert h == 0;
        return BytesUtils.clone(p);
    }

    @Override
    public byte[] fieldPow(byte[] p, int h) {
        assert field.validateElement(p);
        assert h == 0;
        return BytesUtils.clone(p);
    }

    @Override
    public byte[] mixMul(byte[] p, byte[] q) {
        return field.mul(p, q);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (t = 128, r = 1)";
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(field)
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
        Sgf2k128 that = (Sgf2k128) o;
        // KDF and PRG can be different, all GF2K instance are the same
        return this.field.equals(that.field);
    }
}
