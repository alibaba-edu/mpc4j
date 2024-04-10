package edu.alibaba.mpc4j.crypto.algs.ope;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.algs.range.LongValueRange;
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
    public void testDefault() throws CryptoException {
        Bclo19LongOpeEngine opeEngine = new Bclo19LongOpeEngine();
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        LongValueRange rangeD = new LongValueRange(-(1L << 32), 1L << 32);
        LongValueRange rangeR = new LongValueRange(-(1L << 48), 1L << 48);
        opeEngine.init(key, rangeD, rangeR);

        int minPlaintext = -100;
        int maxPlaintext = 100;
        testOpe(opeEngine, minPlaintext, maxPlaintext);
    }

    @Test
    public void testRandom() throws CryptoException {
        SecureRandom secureRandom = new SecureRandom();
        Bclo19LongOpeEngine opeEngine = new Bclo19LongOpeEngine();
        byte[] key = opeEngine.keyGen(secureRandom);
        LongValueRange rangeD = new LongValueRange(-(1L << 32), 1L << 32);
        LongValueRange rangeR = new LongValueRange(-(1L << 48), 1L << 48);
        opeEngine.init(key, rangeD, rangeR);

        int minPlaintext = -100;
        int maxPlaintext = 100;
        testOpe(opeEngine, minPlaintext, maxPlaintext);
    }

    private void testOpe(Bclo19LongOpeEngine opeEngine, int minPlaintext, int maxPlaintext) throws CryptoException {
        TLongList ciphertexts = new TLongArrayList();
        int count = 0;
        for (int plaintext = minPlaintext; plaintext <= maxPlaintext; plaintext++) {
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
