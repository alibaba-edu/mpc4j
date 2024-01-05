package edu.alibaba.mpc4j.crypto.fhe.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Hash function unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/hash.cpp
 * </p>
 *
 * @author Liqiang Peng
 * @date 2023/12/26
 */
public class HashFunctionTest {

    private void hash(long value, long[] destination) {
        HashFunction.hash(new long[] {value}, 1, destination);
    }

    @Test
    public void testHash() {
        long[] input = new long[] { 0, 0, 0 };
        long[] hash1 = new long[HashFunction.HASH_BLOCK_UINT64_COUNT];
        long[] hash2 = new long[HashFunction.HASH_BLOCK_UINT64_COUNT];
        hash(0, hash1);

        HashFunction.hash(input, 0, hash2);
        Assert.assertFalse(Arrays.equals(hash1, hash2));

        HashFunction.hash(input, 1, hash2);
        Assert.assertArrayEquals(hash1, hash2);

        HashFunction.hash(input, 2, hash2);
        Assert.assertFalse(Arrays.equals(hash1, hash2));

        hash(0x123456, hash1);
        hash(0x023456, hash2);
        Assert.assertFalse(Arrays.equals(hash1, hash2));

        input[0] = 0x123456;
        input[1] = 1;
        hash(0x123456, hash1);
        HashFunction.hash(input, 2, hash2);
        Assert.assertFalse(Arrays.equals(hash1, hash2));
    }
}