package edu.alibaba.mpc4j.crypto.algs.ope;

import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.crypto.algs.restriction.LongLinearMaxBoundRestriction;
import edu.alibaba.mpc4j.crypto.algs.restriction.LongLinearMinBoundRestriction;
import edu.alibaba.mpc4j.crypto.algs.utils.range.LongRange;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import org.bouncycastle.crypto.CryptoException;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * ROPE unit tests.
 *
 * @author Liqiang Peng
 * @date 2024/5/10
 */
public class LongRopeTest {

    @Test
    public void testLongLinearMinBoundSmallRange() throws CryptoException {
        SecureRandom secureRandom = new SecureRandom();
        Zlp24LongRopeEngine ropeEngine = new Zlp24LongRopeEngine();
        byte[] key = ropeEngine.keyGen(secureRandom);
        LongRange rangeD = new LongRange(0, 1L << 4);
        LongRange rangeR = new LongRange(0, 1L << 5);
        ropeEngine.init(key, new LongLinearMinBoundRestriction(rangeD, rangeR, 2));

        testRope(ropeEngine, rangeD.getStart(), rangeD.getEnd());
    }

    @Test
    public void testLongLinearMinBoundDefault() throws CryptoException {
        Zlp24LongRopeEngine ropeEngine = new Zlp24LongRopeEngine();
        byte[] key = BlockUtils.zeroBlock();
        LongRange rangeD = new LongRange(0, 1L << 16);
        LongRange rangeR = new LongRange(0, 1L << 17);
        ropeEngine.init(key, new LongLinearMinBoundRestriction(rangeD, rangeR, 2));

        // verify starting and ending points
        testRope(ropeEngine, rangeD.getStart(), rangeD.getStart() + (1 << 10));
        testRope(ropeEngine, rangeD.getEnd() - (1 << 10), rangeD.getEnd());
    }

    @Test
    public void testLongLinearMaxBoundSmallRange() throws CryptoException {
        SecureRandom secureRandom = new SecureRandom();
        Zlp24LongRopeEngine ropeEngine = new Zlp24LongRopeEngine();
        byte[] key = ropeEngine.keyGen(secureRandom);
        LongRange rangeD = new LongRange(0, 1L << 4);
        LongRange rangeR = new LongRange(0, 1L << 5);
        ropeEngine.init(key, new LongLinearMaxBoundRestriction(rangeD, rangeR, 2));

        testRope(ropeEngine, rangeD.getStart(), rangeD.getEnd());
    }

    @Test
    public void testLongLinearMaxBoundDefault() throws CryptoException {
        Zlp24LongRopeEngine ropeEngine = new Zlp24LongRopeEngine();
        byte[] key = BlockUtils.zeroBlock();
        LongRange rangeD = new LongRange(0, 1L << 16);
        LongRange rangeR = new LongRange(0, 1L << 17);
        ropeEngine.init(key, new LongLinearMaxBoundRestriction(rangeD, rangeR, 2));

        // verify starting, and ending points
        testRope(ropeEngine, rangeD.getStart(), rangeD.getStart() + (1 << 10));
        testRope(ropeEngine, rangeD.getEnd() - (1 << 10), rangeD.getEnd());
    }

    private void testRope(Zlp24LongRopeEngine ropeEngine, long minPlaintext, long maxPlaintext) throws CryptoException {
        TLongList ciphertexts = new TLongArrayList();
        int count = 0;
        for (long plaintext = minPlaintext; plaintext <= maxPlaintext; plaintext++) {
            long encryption = ropeEngine.encrypt(plaintext);
            ciphertexts.add(encryption);
            // verify order-preserving
            if (count > 0) {
                Assert.assertTrue((ciphertexts.get(count) > ciphertexts.get(count - 1)));
            }
            long decryption = ropeEngine.decrypt(encryption);
            Assert.assertEquals(plaintext, decryption);
            count++;
        }
    }
}
