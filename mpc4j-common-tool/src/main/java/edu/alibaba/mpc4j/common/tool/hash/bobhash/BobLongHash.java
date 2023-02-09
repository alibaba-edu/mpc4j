package edu.alibaba.mpc4j.common.tool.hash.bobhash;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hash.LongHash;
import edu.alibaba.mpc4j.common.tool.hash.LongHashFactory;

import java.nio.ByteBuffer;

/**
 * Bob hash that outputs 64-bit long. Modified from:
 * <p>
 * https://github.com/Gavindeed/HeavyGuardian/blob/master/heavyhitter/BOBHash64.h
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/15
 */
public class BobLongHash implements LongHash {
    /**
     * byte length per block
     */
    private static final int BLOCK_BYTE_LENGTH = 24;
    /**
     * the golden ratio: an arbitrary value
     */
    private static final long GOLDEN_RATIO = 0x9e3779b97f4a7c13L;

    /**
     * 质数表索引
     */
    private final int primeTableIndex;

    public BobLongHash() {
        this(0);
    }

    public BobLongHash(int primeTableIndex) {
        MathPreconditions.checkNonNegativeInRange("prime index", primeTableIndex, BobHashUtils.PRIME_BIT_TABLE_SIZE);
        this.primeTableIndex = primeTableIndex;
    }

    @Override
    public LongHashFactory.LongHashType getType() {
        return LongHashFactory.LongHashType.BOB_HASH_64;
    }

    @Override
    @SuppressWarnings({"AlibabaMethodTooLong", "AlibabaSwitchStatement"})
    public long hash(byte[] data) {
        MathPreconditions.checkPositive("data.length", data.length);
        // Set up the internal state;
        long a = GOLDEN_RATIO;
        long b = GOLDEN_RATIO;
        // the previous hash value
        long c = BobHashUtils.PRIME_12_BIT_TABLE[primeTableIndex];
        // Set up the internal state
        int offset = 0;
        int length = data.length;
        // handle most of the key
        while (length >= BLOCK_BYTE_LENGTH) {
            a += ((long) data[offset] + ((long) data[1 + offset] << 8)
                + ((long) data[2 + offset] << 16) + ((long) data[3 + offset] << 24)
                + ((long) data[4 + offset] << 32) + ((long) data[5 + offset] << 40)
                + ((long) data[6 + offset] << 48) + ((long) data[7 + offset] << 56));
            b += ((long) data[8 + offset] + ((long) data[9 + offset] << 8)
                + ((long) data[10 + offset] << 16) + ((long) data[11 + offset] << 24)
                + ((long) data[12 + offset] << 32) + ((long) data[13 + offset] << 40)
                + ((long) data[14 + offset] << 48) + ((long) data[15 + offset] << 56));
            c += ((long) data[16 + offset] + ((long) data[17 + offset] << 8)
                + ((long) data[18 + offset] << 16) + ((long) data[19 + offset] << 24)
                + ((long) data[20 + offset] << 32) + ((long) data[21 + offset] << 40)
                + ((long) data[22 + offset] << 48) + ((long) data[23 + offset] << 56));
            // mix64(a, b, c);
            a -= b;
            a -= c;
            a ^= (c >> 43);
            b -= c;
            b -= a;
            b ^= (a << 9);
            c -= a;
            c -= b;
            c ^= (b >> 8);
            a -= b;
            a -= c;
            a ^= (c >> 38);
            b -= c;
            b -= a;
            b ^= (a << 23);
            c -= a;
            c -= b;
            c ^= (b >> 5);
            a -= b;
            a -= c;
            a ^= (c >> 35);
            b -= c;
            b -= a;
            b ^= (a << 49);
            c -= a;
            c -= b;
            c ^= (b >> 11);
            a -= b;
            a -= c;
            a ^= (c >> 12);
            b -= c;
            b -= a;
            b ^= (a << 18);
            c -= a;
            c -= b;
            c ^= (b >> 22);
            offset += BLOCK_BYTE_LENGTH;
            length -= BLOCK_BYTE_LENGTH;
        }
        // handle the last 23 bytes
        c += length;
        // all the case statements fall through
        switch (length) {
            case 23:
                c += ((long) data[22 + offset] << 56);
            case 22:
                c += ((long) data[21 + offset] << 48);
            case 21:
                c += ((long) data[20 + offset] << 40);
            case 20:
                c += ((long) data[19 + offset] << 32);
            case 19:
                c += ((long) data[18 + offset] << 24);
            case 18:
                c += ((long) data[17 + offset] << 16);
            case 17:
                c += ((long) data[16 + offset] << 8);
            case 16:
                b += ((long) data[15 + offset] << 56);
            case 15:
                b += ((long) data[14 + offset] << 48);
            case 14:
                b += ((long) data[13 + offset] << 40);
            case 13:
                b += ((long) data[12 + offset] << 32);
            case 12:
                b += ((long) data[11 + offset] << 24);
            case 11:
                b += ((long) data[10 + offset] << 16);
            case 10:
                b += ((long) data[9 + offset] << 8);
            case 9:
                b += data[8 + offset];
            case 8:
                a += ((long) data[7 + offset] << 56);
            case 7:
                a += ((long) data[6 + offset] << 48);
            case 6:
                a += ((long) data[5 + offset] << 40);
            case 5:
                a += ((long) data[4 + offset] << 32);
            case 4:
                a += ((long) data[3 + offset] << 24);
            case 3:
                a += ((long) data[2 + offset] << 16);
            case 2:
                a += ((long) data[1 + offset] << 8);
            case 1:
                a += data[offset];
            default:
                // case 0: nothing left to add
        }
        // mix64(a, b, c)
        a -= b;
        a -= c;
        a ^= (c >> 43);
        b -= c;
        b -= a;
        b ^= (a << 9);
        c -= a;
        c -= b;
        c ^= (b >> 8);
        a -= b;
        a -= c;
        a ^= (c >> 38);
        b -= c;
        b -= a;
        b ^= (a << 23);
        c -= a;
        c -= b;
        c ^= (b >> 5);
        a -= b;
        a -= c;
        a ^= (c >> 35);
        b -= c;
        b -= a;
        b ^= (a << 49);
        c -= a;
        c -= b;
        c ^= (b >> 11);
        a -= b;
        a -= c;
        a ^= (c >> 12);
        b -= c;
        b -= a;
        b ^= (a << 18);
        c -= a;
        c -= b;
        c ^= (b >> 22);

        return c;
    }

    @Override
    public long hash(byte[] data, long seed) {
        MathPreconditions.checkPositive("data.length", data.length);
        return hash(ByteBuffer.allocate(Long.BYTES + data.length).putLong(seed).put(data).array());
    }
}
