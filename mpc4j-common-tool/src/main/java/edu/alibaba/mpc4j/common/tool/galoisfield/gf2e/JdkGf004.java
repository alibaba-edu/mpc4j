package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;

/**
 * GF(2^4) using pure Java.
 *
 * @author Weiran Liu
 * @date 2024/6/4
 */
class JdkGf004 extends AbstractGf2e {
    /**
     * mul lookup table
     */
    private static final byte[] MUL_LOOKUP_TABLE = new byte[]{
        (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
        (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
        (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
        (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
        (byte) 0b00000000, (byte) 0b00000001, (byte) 0b00000010, (byte) 0b00000011,
        (byte) 0b00000100, (byte) 0b00000101, (byte) 0b00000110, (byte) 0b00000111,
        (byte) 0b00001000, (byte) 0b00001001, (byte) 0b00001010, (byte) 0b00001011,
        (byte) 0b00001100, (byte) 0b00001101, (byte) 0b00001110, (byte) 0b00001111,
        (byte) 0b00000000, (byte) 0b00000010, (byte) 0b00000100, (byte) 0b00000110,
        (byte) 0b00001000, (byte) 0b00001010, (byte) 0b00001100, (byte) 0b00001110,
        (byte) 0b00000011, (byte) 0b00000001, (byte) 0b00000111, (byte) 0b00000101,
        (byte) 0b00001011, (byte) 0b00001001, (byte) 0b00001111, (byte) 0b00001101,
        (byte) 0b00000000, (byte) 0b00000011, (byte) 0b00000110, (byte) 0b00000101,
        (byte) 0b00001100, (byte) 0b00001111, (byte) 0b00001010, (byte) 0b00001001,
        (byte) 0b00001011, (byte) 0b00001000, (byte) 0b00001101, (byte) 0b00001110,
        (byte) 0b00000111, (byte) 0b00000100, (byte) 0b00000001, (byte) 0b00000010,
        (byte) 0b00000000, (byte) 0b00000100, (byte) 0b00001000, (byte) 0b00001100,
        (byte) 0b00000011, (byte) 0b00000111, (byte) 0b00001011, (byte) 0b00001111,
        (byte) 0b00000110, (byte) 0b00000010, (byte) 0b00001110, (byte) 0b00001010,
        (byte) 0b00000101, (byte) 0b00000001, (byte) 0b00001101, (byte) 0b00001001,
        (byte) 0b00000000, (byte) 0b00000101, (byte) 0b00001010, (byte) 0b00001111,
        (byte) 0b00000111, (byte) 0b00000010, (byte) 0b00001101, (byte) 0b00001000,
        (byte) 0b00001110, (byte) 0b00001011, (byte) 0b00000100, (byte) 0b00000001,
        (byte) 0b00001001, (byte) 0b00001100, (byte) 0b00000011, (byte) 0b00000110,
        (byte) 0b00000000, (byte) 0b00000110, (byte) 0b00001100, (byte) 0b00001010,
        (byte) 0b00001011, (byte) 0b00001101, (byte) 0b00000111, (byte) 0b00000001,
        (byte) 0b00000101, (byte) 0b00000011, (byte) 0b00001001, (byte) 0b00001111,
        (byte) 0b00001110, (byte) 0b00001000, (byte) 0b00000010, (byte) 0b00000100,
        (byte) 0b00000000, (byte) 0b00000111, (byte) 0b00001110, (byte) 0b00001001,
        (byte) 0b00001111, (byte) 0b00001000, (byte) 0b00000001, (byte) 0b00000110,
        (byte) 0b00001101, (byte) 0b00001010, (byte) 0b00000011, (byte) 0b00000100,
        (byte) 0b00000010, (byte) 0b00000101, (byte) 0b00001100, (byte) 0b00001011,
        (byte) 0b00000000, (byte) 0b00001000, (byte) 0b00000011, (byte) 0b00001011,
        (byte) 0b00000110, (byte) 0b00001110, (byte) 0b00000101, (byte) 0b00001101,
        (byte) 0b00001100, (byte) 0b00000100, (byte) 0b00001111, (byte) 0b00000111,
        (byte) 0b00001010, (byte) 0b00000010, (byte) 0b00001001, (byte) 0b00000001,
        (byte) 0b00000000, (byte) 0b00001001, (byte) 0b00000001, (byte) 0b00001000,
        (byte) 0b00000010, (byte) 0b00001011, (byte) 0b00000011, (byte) 0b00001010,
        (byte) 0b00000100, (byte) 0b00001101, (byte) 0b00000101, (byte) 0b00001100,
        (byte) 0b00000110, (byte) 0b00001111, (byte) 0b00000111, (byte) 0b00001110,
        (byte) 0b00000000, (byte) 0b00001010, (byte) 0b00000111, (byte) 0b00001101,
        (byte) 0b00001110, (byte) 0b00000100, (byte) 0b00001001, (byte) 0b00000011,
        (byte) 0b00001111, (byte) 0b00000101, (byte) 0b00001000, (byte) 0b00000010,
        (byte) 0b00000001, (byte) 0b00001011, (byte) 0b00000110, (byte) 0b00001100,
        (byte) 0b00000000, (byte) 0b00001011, (byte) 0b00000101, (byte) 0b00001110,
        (byte) 0b00001010, (byte) 0b00000001, (byte) 0b00001111, (byte) 0b00000100,
        (byte) 0b00000111, (byte) 0b00001100, (byte) 0b00000010, (byte) 0b00001001,
        (byte) 0b00001101, (byte) 0b00000110, (byte) 0b00001000, (byte) 0b00000011,
        (byte) 0b00000000, (byte) 0b00001100, (byte) 0b00001011, (byte) 0b00000111,
        (byte) 0b00000101, (byte) 0b00001001, (byte) 0b00001110, (byte) 0b00000010,
        (byte) 0b00001010, (byte) 0b00000110, (byte) 0b00000001, (byte) 0b00001101,
        (byte) 0b00001111, (byte) 0b00000011, (byte) 0b00000100, (byte) 0b00001000,
        (byte) 0b00000000, (byte) 0b00001101, (byte) 0b00001001, (byte) 0b00000100,
        (byte) 0b00000001, (byte) 0b00001100, (byte) 0b00001000, (byte) 0b00000101,
        (byte) 0b00000010, (byte) 0b00001111, (byte) 0b00001011, (byte) 0b00000110,
        (byte) 0b00000011, (byte) 0b00001110, (byte) 0b00001010, (byte) 0b00000111,
        (byte) 0b00000000, (byte) 0b00001110, (byte) 0b00001111, (byte) 0b00000001,
        (byte) 0b00001101, (byte) 0b00000011, (byte) 0b00000010, (byte) 0b00001100,
        (byte) 0b00001001, (byte) 0b00000111, (byte) 0b00000110, (byte) 0b00001000,
        (byte) 0b00000100, (byte) 0b00001010, (byte) 0b00001011, (byte) 0b00000101,
        (byte) 0b00000000, (byte) 0b00001111, (byte) 0b00001101, (byte) 0b00000010,
        (byte) 0b00001001, (byte) 0b00000110, (byte) 0b00000100, (byte) 0b00001011,
        (byte) 0b00000001, (byte) 0b00001110, (byte) 0b00001100, (byte) 0b00000011,
        (byte) 0b00001000, (byte) 0b00000111, (byte) 0b00000101, (byte) 0b00001010,
    };

    /**
     * inv lookup table
     */
    private static final byte[] INV_LOOKUP_TABLE = new byte[]{
        (byte) 0b00000000, (byte) 0b00000001, (byte) 0b00001001, (byte) 0b00001110,
        (byte) 0b00001101, (byte) 0b00001011, (byte) 0b00000111, (byte) 0b00000110,
        (byte) 0b00001111, (byte) 0b00000010, (byte) 0b00001100, (byte) 0b00000101,
        (byte) 0b00001010, (byte) 0b00000100, (byte) 0b00000011, (byte) 0b00001000,
    };

    /**
     * div lookup table
     */
    private static final byte[] DIV_LOOKUP_TABLE = new byte[] {
        (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
        (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
        (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
        (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000, (byte) 0b00000000,
        (byte) 0b00000000, (byte) 0b00000001, (byte) 0b00001001, (byte) 0b00001110,
        (byte) 0b00001101, (byte) 0b00001011, (byte) 0b00000111, (byte) 0b00000110,
        (byte) 0b00001111, (byte) 0b00000010, (byte) 0b00001100, (byte) 0b00000101,
        (byte) 0b00001010, (byte) 0b00000100, (byte) 0b00000011, (byte) 0b00001000,
        (byte) 0b00000000, (byte) 0b00000010, (byte) 0b00000001, (byte) 0b00001111,
        (byte) 0b00001001, (byte) 0b00000101, (byte) 0b00001110, (byte) 0b00001100,
        (byte) 0b00001101, (byte) 0b00000100, (byte) 0b00001011, (byte) 0b00001010,
        (byte) 0b00000111, (byte) 0b00001000, (byte) 0b00000110, (byte) 0b00000011,
        (byte) 0b00000000, (byte) 0b00000011, (byte) 0b00001000, (byte) 0b00000001,
        (byte) 0b00000100, (byte) 0b00001110, (byte) 0b00001001, (byte) 0b00001010,
        (byte) 0b00000010, (byte) 0b00000110, (byte) 0b00000111, (byte) 0b00001111,
        (byte) 0b00001101, (byte) 0b00001100, (byte) 0b00000101, (byte) 0b00001011,
        (byte) 0b00000000, (byte) 0b00000100, (byte) 0b00000010, (byte) 0b00001101,
        (byte) 0b00000001, (byte) 0b00001010, (byte) 0b00001111, (byte) 0b00001011,
        (byte) 0b00001001, (byte) 0b00001000, (byte) 0b00000101, (byte) 0b00000111,
        (byte) 0b00001110, (byte) 0b00000011, (byte) 0b00001100, (byte) 0b00000110,
        (byte) 0b00000000, (byte) 0b00000101, (byte) 0b00001011, (byte) 0b00000011,
        (byte) 0b00001100, (byte) 0b00000001, (byte) 0b00001000, (byte) 0b00001101,
        (byte) 0b00000110, (byte) 0b00001010, (byte) 0b00001001, (byte) 0b00000010,
        (byte) 0b00000100, (byte) 0b00000111, (byte) 0b00001111, (byte) 0b00001110,
        (byte) 0b00000000, (byte) 0b00000110, (byte) 0b00000011, (byte) 0b00000010,
        (byte) 0b00001000, (byte) 0b00001111, (byte) 0b00000001, (byte) 0b00000111,
        (byte) 0b00000100, (byte) 0b00001100, (byte) 0b00001110, (byte) 0b00001101,
        (byte) 0b00001001, (byte) 0b00001011, (byte) 0b00001010, (byte) 0b00000101,
        (byte) 0b00000000, (byte) 0b00000111, (byte) 0b00001010, (byte) 0b00001100,
        (byte) 0b00000101, (byte) 0b00000100, (byte) 0b00000110, (byte) 0b00000001,
        (byte) 0b00001011, (byte) 0b00001110, (byte) 0b00000010, (byte) 0b00001000,
        (byte) 0b00000011, (byte) 0b00001111, (byte) 0b00001001, (byte) 0b00001101,
        (byte) 0b00000000, (byte) 0b00001000, (byte) 0b00000100, (byte) 0b00001001,
        (byte) 0b00000010, (byte) 0b00000111, (byte) 0b00001101, (byte) 0b00000101,
        (byte) 0b00000001, (byte) 0b00000011, (byte) 0b00001010, (byte) 0b00001110,
        (byte) 0b00001111, (byte) 0b00000110, (byte) 0b00001011, (byte) 0b00001100,
        (byte) 0b00000000, (byte) 0b00001001, (byte) 0b00001101, (byte) 0b00000111,
        (byte) 0b00001111, (byte) 0b00001100, (byte) 0b00001010, (byte) 0b00000011,
        (byte) 0b00001110, (byte) 0b00000001, (byte) 0b00000110, (byte) 0b00001011,
        (byte) 0b00000101, (byte) 0b00000010, (byte) 0b00001000, (byte) 0b00000100,
        (byte) 0b00000000, (byte) 0b00001010, (byte) 0b00000101, (byte) 0b00000110,
        (byte) 0b00001011, (byte) 0b00000010, (byte) 0b00000011, (byte) 0b00001001,
        (byte) 0b00001100, (byte) 0b00000111, (byte) 0b00000001, (byte) 0b00000100,
        (byte) 0b00001000, (byte) 0b00001110, (byte) 0b00001101, (byte) 0b00001111,
        (byte) 0b00000000, (byte) 0b00001011, (byte) 0b00001100, (byte) 0b00001000,
        (byte) 0b00000110, (byte) 0b00001001, (byte) 0b00000100, (byte) 0b00001111,
        (byte) 0b00000011, (byte) 0b00000101, (byte) 0b00001101, (byte) 0b00000001,
        (byte) 0b00000010, (byte) 0b00001010, (byte) 0b00001110, (byte) 0b00000111,
        (byte) 0b00000000, (byte) 0b00001100, (byte) 0b00000110, (byte) 0b00000100,
        (byte) 0b00000011, (byte) 0b00001101, (byte) 0b00000010, (byte) 0b00001110,
        (byte) 0b00001000, (byte) 0b00001011, (byte) 0b00001111, (byte) 0b00001001,
        (byte) 0b00000001, (byte) 0b00000101, (byte) 0b00000111, (byte) 0b00001010,
        (byte) 0b00000000, (byte) 0b00001101, (byte) 0b00001111, (byte) 0b00001010,
        (byte) 0b00001110, (byte) 0b00000110, (byte) 0b00000101, (byte) 0b00001000,
        (byte) 0b00000111, (byte) 0b00001001, (byte) 0b00000011, (byte) 0b00001100,
        (byte) 0b00001011, (byte) 0b00000001, (byte) 0b00000100, (byte) 0b00000010,
        (byte) 0b00000000, (byte) 0b00001110, (byte) 0b00000111, (byte) 0b00001011,
        (byte) 0b00001010, (byte) 0b00001000, (byte) 0b00001100, (byte) 0b00000010,
        (byte) 0b00000101, (byte) 0b00001111, (byte) 0b00000100, (byte) 0b00000011,
        (byte) 0b00000110, (byte) 0b00001101, (byte) 0b00000001, (byte) 0b00001001,
        (byte) 0b00000000, (byte) 0b00001111, (byte) 0b00001110, (byte) 0b00000101,
        (byte) 0b00000111, (byte) 0b00000011, (byte) 0b00001011, (byte) 0b00000100,
        (byte) 0b00001010, (byte) 0b00001101, (byte) 0b00001000, (byte) 0b00000110,
        (byte) 0b00001100, (byte) 0b00001001, (byte) 0b00000010, (byte) 0b00000001,
    };

    public JdkGf004(EnvType envType) {
        super(envType, 4);
    }

    @Override
    public Gf2eType getGf2eType() {
        return Gf2eType.JDK;
    }

    @Override
    public boolean validateElement(byte[] p) {
        return p.length == 1 && ((p[0] & 0b00001111) == p[0]);
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        byte[] r = new byte[byteL];
        r[0] = MUL_LOOKUP_TABLE[(p[0] << 4) | q[0]];
        return r;
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        p[0] = MUL_LOOKUP_TABLE[(p[0] << 4) | q[0]];
    }

    @Override
    public byte[] inv(byte[] p) {
        assert validateNonZeroElement(p);
        return new byte[]{INV_LOOKUP_TABLE[p[0]]};
    }

    @Override
    public void invi(byte[] p) {
        assert validateNonZeroElement(p);
        p[0] = INV_LOOKUP_TABLE[p[0]];
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        byte[] r = new byte[byteL];
        r[0] = DIV_LOOKUP_TABLE[(p[0] << 4) | q[0]];
        return r;
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        p[0] = DIV_LOOKUP_TABLE[(p[0] << 4) | q[0]];
    }
}
