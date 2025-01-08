package edu.alibaba.mpc4j.common.tool.galoisfield;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Z3 implemented using JDK.
 * <p></p>
 * Here we note that <code>Z3ByteField</code> does not provide <code>createRandom(byte[] seed)</code> for efficiency
 * reason. If one wants to generate deterministic Z3 elements, we recommend creating a <code>SecureRandom</code> with
 * seed. In this way, we can reuse seed and do not waste so much randomness for just generating one Z3 element.
 *
 *
 * @author Weiran Liu
 * @date 2024/5/22
 */
public class Z3ByteField implements ByteField {
    /**
     * modulus prime
     */
    private static final byte PRIME = 3;
    /**
     * non-negative lookup table
     */
    private static final byte[] NON_NEGATIVE_LOOKUP_TABLE = new byte[]{0, 1, 2, 0, 1};

    @Override
    public byte getPrime() {
        return PRIME;
    }

    @Override
    public byte add(byte p, byte q) {
        assert validateElement(p);
        assert validateElement(q);
        return NON_NEGATIVE_LOOKUP_TABLE[p + q];
    }

    @Override
    public byte neg(byte p) {
        assert validateElement(p);
        return NON_NEGATIVE_LOOKUP_TABLE[PRIME - p];
    }

    @Override
    public byte sub(byte p, byte q) {
        assert validateElement(p);
        assert validateElement(q);
        if (p >= q) {
            return NON_NEGATIVE_LOOKUP_TABLE[p - q];
        } else {
            return NON_NEGATIVE_LOOKUP_TABLE[p - q + PRIME];
        }
    }

    @Override
    public byte mul(byte p, byte q) {
        assert validateElement(p);
        assert validateElement(q);
        return NON_NEGATIVE_LOOKUP_TABLE[p * q];
    }

    @Override
    public byte inv(byte p) {
        assert validateNonZeroElement(p);
        // 1^-1 = 1 mod 3, 2^-1 = 2 mod 3
        return p;
    }

    @Override
    public byte div(byte p, byte q) {
        assert validateElement(p);
        assert validateNonZeroElement(q);
        // q^-1 = q mod 3, so p / q = p * q
        return NON_NEGATIVE_LOOKUP_TABLE[p * q];
    }

    @Override
    public byte createZero() {
        return 0;
    }

    /**
     * Creates elements (0,0,...,0).
     *
     * @param num num.
     * @return (0, 0, ..., 0).
     */
    public byte[] createZeros(int num) {
        return new byte[num];
    }

    @Override
    public byte createOne() {
        return 1;
    }

    /**
     * Creates elements (1,1,...,1).
     *
     * @param num num.
     * @return (1, 1, ..., 1).
     */
    public byte[] createOnes(int num) {
        byte[] elements = new byte[num];
        Arrays.fill(elements, (byte) 0b00000001);
        return elements;
    }

    /**
     * Creates element 2.
     *
     * @return element 2.
     */
    public byte createTwo() {
        return 0b00000010;
    }

    /**
     * Creates elements (2,2,...,2).
     *
     * @param num num.
     * @return (2, 2, ..., 2).
     */
    public byte[] createTwos(int num) {
        byte[] elements = new byte[num];
        Arrays.fill(elements, (byte) 0b00000010);
        return elements;
    }

    @Override
    public byte createRandom(SecureRandom secureRandom) {
        return (byte) secureRandom.nextInt(3);
    }

    /**
     * Creates random elements.
     *
     * @param num          num.
     * @param secureRandom secure random.
     * @return random elements.
     */
    public byte[] createRandoms(int num, SecureRandom secureRandom) {
        byte[] elements = new byte[num];
        for (int i = 0; i < num; i++) {
            elements[i] = createRandom(secureRandom);
        }
        return elements;
    }

    @Override
    public byte createNonZeroRandom(SecureRandom secureRandom) {
        return secureRandom.nextBoolean() ? (byte) 2 : (byte) 1;
    }

    /**
     * Creates non-zero random elements.
     *
     * @param num          num.
     * @param secureRandom secure random.
     * @return non-zero random elements.
     */
    public byte[] createNonZeroRandoms(int num, SecureRandom secureRandom) {
        byte[] elements = new byte[num];
        for (int i = 0; i < num; i++) {
            elements[i] = createNonZeroRandom(secureRandom);
        }
        return elements;
    }

    @Override
    public boolean isZero(byte p) {
        validateElement(p);
        return p == 0;
    }

    @Override
    public boolean isOne(byte p) {
        validateElement(p);
        return p == 1;
    }

    @Override
    public boolean validateElement(byte p) {
        return p == 0 || p == 1 || p == 2;
    }

    @Override
    public boolean validateNonZeroElement(byte p) {
        return p == 1 || p == 2;
    }

    /**
     * mod.
     *
     * @param p p.
     * @return p mod 3.
     */
    public byte mod(int p) {
        byte output = (byte) (p % 3);
        if (output < 0) {
            output += PRIME;
        }
        return output;
    }
}
