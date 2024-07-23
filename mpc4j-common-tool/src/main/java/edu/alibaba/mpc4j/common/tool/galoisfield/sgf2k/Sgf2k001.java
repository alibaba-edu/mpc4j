package edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Subfield GF2K (t = 1, r = 128).
 *
 * @author Weiran Liu
 * @date 2024/6/18
 */
public class Sgf2k001 implements Sgf2k {
    /**
     * subfield
     */
    private final Gf2e subfield;
    /**
     * field
     */
    private final Gf2k field;

    public Sgf2k001(EnvType envType) {
        subfield = Gf2eFactory.createInstance(envType, 1);
        field = Gf2kFactory.createInstance(envType);
    }

    @Override
    public Gf2e getSubfield() {
        return subfield;
    }

    @Override
    public int getSubfieldL() {
        return subfield.getL();
    }

    @Override
    public int getSubfieldByteL() {
        return subfield.getByteL();
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
        return 128;
    }

    @Override
    public byte[][] decomposite(byte[] fieldElement) {
        assert validateElement(fieldElement);
        return IntStream.range(0, fieldElement.length)
            .mapToObj(i -> {
                byte b = fieldElement[fieldElement.length - 1 - i];
                return new byte[][]{
                    new byte[]{(byte) (b & 0b00000001)}, new byte[]{(byte) ((b & 0b00000010) >> 1)},
                    new byte[]{(byte) ((b & 0b00000100) >> 2)}, new byte[]{(byte) ((b & 0b00001000) >> 3)},
                    new byte[]{(byte) ((b & 0b00010000) >> 4)}, new byte[]{(byte) ((b & 0b00100000) >> 5)},
                    new byte[]{(byte) ((b & 0b01000000) >> 6)}, new byte[]{(byte) ((b & 0b10000000) >> 7)},
                };
            })
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
    }

    @Override
    public byte[] composite(byte[][] subfieldElements) {
        int l = getL();
        byte[] fieldElement = createZero();
        for (int i = 0; i < l; i++) {
            BinaryUtils.setBoolean(fieldElement, i, subfieldElements[l - 1 - i][0] != 0);
        }
        return fieldElement;
    }

    @Override
    public byte[] extend(byte[] subfieldElement) {
        assert subfield.validateElement(subfieldElement);
        byte[] fieldElement = createZero();
        fieldElement[fieldElement.length - 1] = subfieldElement[0];
        return fieldElement;
    }

    @Override
    public byte[] mixPow(byte[] p, int h) {
        assert subfield.validateElement(p);
        int l = getL();
        assert h >= 0 && h < l;
        byte[] fieldElement = createZero();
        BinaryUtils.setBoolean(fieldElement, l - 1 - h, p[0] != 0);
        return fieldElement;
    }

    @Override
    public byte[] fieldPow(byte[] p, int h) {
        assert field.validateElement(p);
        int l = getL();
        assert h >= 0 && h < l;
        byte[] fieldElement = createZero();
        BinaryUtils.setBoolean(fieldElement, l - 1 - h, true);
        field.muli(fieldElement, p);
        return fieldElement;
    }

    @Override
    public byte[] mixMul(byte[] p, byte[] q) {
        assert subfield.validateElement(p);
        assert validateElement(q);
        if (p[0] == 0) {
            return createZero();
        } else {
            return BytesUtils.clone(q);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (t = 1, r = 128)";
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
        Sgf2k001 that = (Sgf2k001) o;
        // KDF and PRG can be different, all GF2K instance are the same
        return this.field.equals(that.field) && this.subfield.equals(that.subfield);
    }
}
