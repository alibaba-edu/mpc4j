package edu.alibaba.mpc4j.common.tool.hash.bobhash;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hash.IntHash;
import edu.alibaba.mpc4j.common.tool.hash.IntHashFactory;

import java.nio.ByteBuffer;

/**
 * Bob hash that outputs 32-bit integer. Modified from:
 * <p>
 * https://github.com/Gavindeed/HeavyGuardian/blob/master/heavyhitter/BOBHash32.h
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/15
 */
public class BobIntHash implements IntHash {
    /**
     * byte length per block
     */
    private static final int BLOCK_BYTE_LENGTH = 12;
    /**
     * the golden ratio: an arbitrary value
     */
    private static final int GOLDEN_RATIO = 0x9e3779b9;
    /**
     * the prime index
     */
    private final int primeTableIndex;

    public BobIntHash() {
        this(0);
    }

    public BobIntHash(int primeTableIndex) {
        MathPreconditions.checkNonNegativeInRange("prime index", primeTableIndex, BobHashUtils.PRIME_BIT_TABLE_SIZE);
        this.primeTableIndex = primeTableIndex;
    }

    @Override
    public IntHashFactory.IntHashType getType() {
        return IntHashFactory.IntHashType.BOB_HASH_32;
    }

    @Override
    @SuppressWarnings({"AlibabaMethodTooLong", "AlibabaSwitchStatement"})
    public int hash(byte[] data) {
        MathPreconditions.checkPositive("data.length", data.length);
        // Set up the internal state;
        int a = GOLDEN_RATIO;
        int b = GOLDEN_RATIO;
        // the previous hash value
        int c = BobHashUtils.PRIME_12_BIT_TABLE[primeTableIndex];
        // Set up the internal state
        int offset = 0;
        int length = data.length;
        // handle most of the key
        while (length >= BLOCK_BYTE_LENGTH) {
            a += (data[offset] + (data[1 + offset] << 8) + (data[2 + offset] << 16) + (data[3 + offset] << 24));
            b += (data[4 + offset] + (data[5 + offset] << 8) + (data[6 + offset] << 16) + (data[7 + offset] << 24));
            c += (data[8 + offset] + (data[9 + offset] << 8) + (data[10 + offset] << 16) + (data[11 + offset] << 24));
            // mix(a, b, c)
            a -= b;
            a -= c;
            a ^= (c >> 13);
            b -= c;
            b -= a;
            b ^= (a << 8);
            c -= a;
            c -= b;
            c ^= (b >> 13);
            a -= b;
            a -= c;
            a ^= (c >> 12);
            b -= c;
            b -= a;
            b ^= (a << 16);
            c -= a;
            c -= b;
            c ^= (b >> 5);
            a -= b;
            a -= c;
            a ^= (c >> 3);
            b -= c;
            b -= a;
            b ^= (a << 10);
            c -= a;
            c -= b;
            c ^= (b >> 15);
            offset += BLOCK_BYTE_LENGTH;
            length -= BLOCK_BYTE_LENGTH;
        }
        // handle the last 11 bytes
        c += length;
        // all the case statements fall through
        switch (length) {
            case 11:
                c += (data[10 + offset] << 24);
            case 10:
                c += (data[9 + offset] << 16);
            case 9:
                c += (data[8 + offset] << 8);
            case 8:
                b += (data[7 + offset] << 24);
            case 7:
                b += (data[6 + offset] << 16);
            case 6:
                b += (data[5 + offset] << 8);
            case 5:
                b += data[4 + offset];
            case 4:
                a += (data[3 + offset] << 24);
            case 3:
                a += (data[2 + offset] << 16);
            case 2:
                a += (data[1 + offset] << 8);
            case 1:
                a += data[offset];
            default:
                // case 0: nothing left to add
        }
        // mix(a, b, c)
        a -= b;
        a -= c;
        a ^= (c >> 13);
        b -= c;
        b -= a;
        b ^= (a << 8);
        c -= a;
        c -= b;
        c ^= (b >> 13);
        a -= b;
        a -= c;
        a ^= (c >> 12);
        b -= c;
        b -= a;
        b ^= (a << 16);
        c -= a;
        c -= b;
        c ^= (b >> 5);
        a -= b;
        a -= c;
        a ^= (c >> 3);
        b -= c;
        b -= a;
        b ^= (a << 10);
        c -= a;
        c -= b;
        c ^= (b >> 15);

        return c;
    }

    @Override
    public int hash(byte[] data, int seed) {
        MathPreconditions.checkPositive("data.length", data.length);
        return hash(ByteBuffer.allocate(Integer.BYTES + data.length).putInt(seed).put(data).array());
    }
}
