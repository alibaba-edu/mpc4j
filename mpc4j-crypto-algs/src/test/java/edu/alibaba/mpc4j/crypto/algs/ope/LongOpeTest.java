package edu.alibaba.mpc4j.crypto.algs.ope;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.algs.utils.range.LongRange;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import org.bouncycastle.crypto.CryptoException;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * OPE unit tests.
 *
 * @author Weiran Liu
 * @date 2024/1/13
 */
public class LongOpeTest {

    @Test
    public void testSmallRange() throws CryptoException {
        SecureRandom secureRandom = new SecureRandom();
        Bclo09LongOpeEngine opeEngine = new Bclo09LongOpeEngine();
        byte[] key = opeEngine.keyGen(secureRandom);
        LongRange rangeD = new LongRange(-(1L << 3), 1L << 3);
        LongRange rangeR = new LongRange(-(1L << 4), 1L << 4);
        opeEngine.init(key, rangeD, rangeR);

        int minPlaintext = -8;
        int maxPlaintext = 8;
        testOpe(opeEngine, minPlaintext, maxPlaintext);
    }

    @Test
    public void testDefault() throws CryptoException {
        Bclo09LongOpeEngine opeEngine = new Bclo09LongOpeEngine();
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        LongRange rangeD = new LongRange(-(1L << 15), 1L << 16);
        LongRange rangeR = new LongRange(-(1L << 16), 1L << 17);
        opeEngine.init(key, rangeD, rangeR);

        // verify starting, middle, and ending points
        testOpe(opeEngine, rangeD.getStart(), rangeD.getStart() + (1 << 10));
        testOpe(opeEngine, -(1 << 10), (1 << 10));
        testOpe(opeEngine, rangeD.getEnd() - (1 << 10), rangeD.getEnd());
    }


    private void testOpe(Bclo09LongOpeEngine opeEngine, long minPlaintext, long maxPlaintext) throws CryptoException {
        TLongList ciphertexts = new TLongArrayList();
        int count = 0;
        for (long plaintext = minPlaintext; plaintext <= maxPlaintext; plaintext++) {
            long encryption = opeEngine.encrypt(plaintext);
            ciphertexts.add(encryption);
            // verify order-preserving
            if (count > 0) {
                Assert.assertTrue((ciphertexts.get(count) > ciphertexts.get(count - 1)));
            }
            long decryption = opeEngine.decrypt(encryption);
            Assert.assertEquals(plaintext, decryption);
            count++;
        }
    }
}
