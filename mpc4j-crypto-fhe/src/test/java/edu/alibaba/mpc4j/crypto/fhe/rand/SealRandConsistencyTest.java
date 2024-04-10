package edu.alibaba.mpc4j.crypto.fhe.rand;

import org.bouncycastle.crypto.digests.SHAKEDigest;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * SEAL PRG consistency test. This ensures that the Java PRG output is the same as SEAL.
 * <p></p>
 * We note that:
 * <li><p>seed length is uint64_t[8], see https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/randomgen.h#L22</p></li>
 * <li><p>buffer_size is 4096, see https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/randomgen.h#L391</p></li>
 *
 * @author Weiran Liu
 * @date 2024/1/29
 */
public class SealRandConsistencyTest {
    /**
     * seed length in long
     */
    private static final int PRNG_SEED_UINT64_COUNT = 8;
    /**
     * seed length in byte
     */
    private static final int PRNG_SEED_BYTE_COUNT = PRNG_SEED_UINT64_COUNT * Long.BYTES;
    /**
     * buffer size
     */
    private static final int BUFFER_SIZE = 4096;

    @Test
    public void testShake256() {
        byte[] seed = new byte[PRNG_SEED_BYTE_COUNT];
        long counter = 0;
        byte[] output = new byte[BUFFER_SIZE];
        // we need to create SHAKE with 256 bit length
        SHAKEDigest shakeDigest = new SHAKEDigest(256);
        // in SEAL, the seed is uint64_t[seed, counter]
        byte[] seedExt = ByteBuffer.allocate(PRNG_SEED_BYTE_COUNT + Long.BYTES)
            .put(seed)
            .putLong(counter)
            .array();
        shakeDigest.update(seedExt, 0, seedExt.length);
        shakeDigest.doFinal(output, 0, output.length);
        // the expected output is generated using SHAKE256 imported in SEAL and get the first and last 32-byte output
        String expectBegin = "64FF78306D2EC7B31BEDDB9B444F1D3F712A9F4F3F69E102B8F4190093836964";
        String expectEnd = "2C0F83D2C26B437EA54C7F9A0BE972DA4E80AE55EACA8E5AA28D5ABFEE6269BF";
        String actual = Hex.toHexString(output).toUpperCase(Locale.ROOT);
        Assert.assertTrue(actual.startsWith(expectBegin));
        Assert.assertTrue(actual.endsWith(expectEnd));
    }
}
