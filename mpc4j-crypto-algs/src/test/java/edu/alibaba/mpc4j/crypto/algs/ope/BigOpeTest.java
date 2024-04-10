package edu.alibaba.mpc4j.crypto.algs.ope;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.crypto.algs.range.BigValueRange;
import org.bouncycastle.crypto.CryptoException;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * OPE unit tests.
 *
 * @author Weiran Liu
 * @date 2024/1/13
 */
public class BigOpeTest {

    @Test
    public void testDefault() throws CryptoException {
        Bclo19BigOpeEngine opeEngine = new Bclo19BigOpeEngine();
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        BigValueRange rangeD = new BigValueRange(BigInteger.valueOf(1L << 32).negate(), BigInteger.valueOf(1L << 32));
        BigValueRange rangeR = new BigValueRange(BigInteger.valueOf(1L << 48).negate(), BigInteger.valueOf(1L << 48));
        opeEngine.init(key, rangeD, rangeR);

        int minPlaintext = -100;
        int maxPlaintext = 100;
        testOpe(opeEngine, minPlaintext, maxPlaintext);
    }

    @Test
    public void testRandom() throws CryptoException {
        SecureRandom secureRandom = new SecureRandom();
        Bclo19BigOpeEngine opeEngine = new Bclo19BigOpeEngine();
        byte[] key = opeEngine.keyGen(secureRandom);
        BigValueRange rangeD = new BigValueRange(BigInteger.valueOf(1L << 32).negate(), BigInteger.valueOf(1L << 32));
        BigValueRange rangeR = new BigValueRange(BigInteger.valueOf(1L << 48).negate(), BigInteger.valueOf(1L << 48));
        opeEngine.init(key, rangeD, rangeR);

        int minPlaintext = -100;
        int maxPlaintext = 100;
        testOpe(opeEngine, minPlaintext, maxPlaintext);
    }

    private void testOpe(Bclo19BigOpeEngine opeEngine, int minPlaintext, int maxPlaintext) throws CryptoException {
        List<BigInteger> ciphertexts = new ArrayList<>();
        int count = 0;
        for (int i = minPlaintext; i <= maxPlaintext; i++) {
            BigInteger plaintext = BigInteger.valueOf(i);
            BigInteger encryption = opeEngine.encrypt(plaintext);
            ciphertexts.add(encryption);
            // verify order-preserving
            if (count > 0) {
                Assert.assertTrue(BigIntegerUtils.greater(ciphertexts.get(count), ciphertexts.get(count - 1)));
            }
            BigInteger decryption = opeEngine.decrypt(encryption);
            Assert.assertEquals(plaintext, decryption);
            count++;
        }
    }
}
