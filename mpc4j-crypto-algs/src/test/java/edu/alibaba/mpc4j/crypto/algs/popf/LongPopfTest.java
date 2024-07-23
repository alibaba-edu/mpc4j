package edu.alibaba.mpc4j.crypto.algs.popf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.algs.restriction.LongEmptyRestriction;
import edu.alibaba.mpc4j.crypto.algs.restriction.LongLinearBoundRestriction;
import edu.alibaba.mpc4j.crypto.algs.utils.range.LongRange;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import org.bouncycastle.crypto.CryptoException;
import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * POPF unit tests.
 *
 * @author Liqiang Peng
 * @date 2024/5/10
 */
public class LongPopfTest {
    /**
     * random state
     */
    private final SecureRandom secureRandom;

    public LongPopfTest() {
        secureRandom = new SecureRandom();
    }

    @Test
    public void testEqualSmallRange() throws CryptoException {
        SecureRandom secureRandom = new SecureRandom();
        Zlp24LongPopfEngine popfEngine = new Zlp24LongPopfEngine();
        byte[] key = popfEngine.keyGen(secureRandom);
        LongRange rangeD = new LongRange(0, 1L << 5);
        LongRange rangeR = new LongRange(0, 1L << 5);
        popfEngine.init(key, new LongEmptyRestriction(rangeD, rangeR));

        testPopf(popfEngine, rangeD.getStart(), rangeD.getEnd());
    }

    @Test
    public void testEqualDefault() throws CryptoException {
        Zlp24LongPopfEngine popfEngine = new Zlp24LongPopfEngine();
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(key);
        LongRange rangeD = new LongRange(0, 1L << 16);
        LongRange rangeR = new LongRange(0, 1L << 16);
        popfEngine.init(key, new LongEmptyRestriction(rangeD, rangeR));

        // verify starting and ending points
        testPopf(popfEngine, rangeD.getStart(), rangeD.getStart() + (1 << 10));
        testPopf(popfEngine, rangeD.getEnd() - (1 << 10), rangeD.getEnd());
    }

    @Test
    public void testEqualBoundRestriction() throws CryptoException {
        Zlp24LongPopfEngine popfEngine = new Zlp24LongPopfEngine();
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(key);
        LongRange rangeD = new LongRange(0, 1L << 16);
        LongRange rangeR = new LongRange(0, 1L << 16);
        popfEngine.init(key, new LongLinearBoundRestriction(rangeD, rangeR, 0.9, 1.1));

        // verify starting and ending points
        testPopf(popfEngine, rangeD.getStart(), rangeD.getStart() + (1 << 10));
        testPopf(popfEngine, rangeD.getEnd() - (1 << 10), rangeD.getEnd());
    }

    @Test
    public void testShrinkSmallRange() throws CryptoException {
        Zlp24LongPopfEngine popfEngine = new Zlp24LongPopfEngine();
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(key);
        LongRange rangeD = new LongRange(0, 1L << 5);
        LongRange rangeR = new LongRange(0, 1L << 4);
        popfEngine.init(key, new LongEmptyRestriction(rangeD, rangeR));

        testPopf(popfEngine, rangeD.getStart(), rangeD.getEnd());
    }

    @Test
    public void testShrinkDefault() throws CryptoException {
        Zlp24LongPopfEngine popfEngine = new Zlp24LongPopfEngine();
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(key);
        LongRange rangeD = new LongRange(0, 1L << 16);
        LongRange rangeR = new LongRange(0, 1L << 15);
        popfEngine.init(key, new LongEmptyRestriction(rangeD, rangeR));

        // verify starting and ending points
        testPopf(popfEngine, rangeD.getStart(), rangeD.getStart() + (1 << 10));
        testPopf(popfEngine, rangeD.getEnd() - (1 << 10), rangeD.getEnd());
    }

    @Test
    public void testShrinkBoundRestriction() throws CryptoException {
        Zlp24LongPopfEngine popfEngine = new Zlp24LongPopfEngine();
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(key);
        LongRange rangeD = new LongRange(0, 1L << 16);
        LongRange rangeR = new LongRange(0, 1L << 15);
        popfEngine.init(key, new LongLinearBoundRestriction(rangeD, rangeR, 0.4, 0.6));

        // verify starting and ending points
        testPopf(popfEngine, rangeD.getStart(), rangeD.getStart() + (1 << 10));
        testPopf(popfEngine, rangeD.getEnd() - (1 << 10), rangeD.getEnd());
    }

    @Test
    public void testMagnifySmallRange() throws CryptoException {
        Zlp24LongPopfEngine popfEngine = new Zlp24LongPopfEngine();
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(key);
        LongRange rangeD = new LongRange(0, 1L << 5);
        LongRange rangeR = new LongRange(0, 1L << 6);
        popfEngine.init(key, new LongEmptyRestriction(rangeD, rangeR));

        testPopf(popfEngine, rangeD.getStart(), rangeD.getEnd());
    }

    @Test
    public void testMagnifyDefault() throws CryptoException {
        Zlp24LongPopfEngine popfEngine = new Zlp24LongPopfEngine();
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(key);
        LongRange rangeD = new LongRange(0, 1L << 16);
        LongRange rangeR = new LongRange(0, 1L << 17);
        popfEngine.init(key, new LongEmptyRestriction(rangeD, rangeR));

        // verify starting and ending points
        testPopf(popfEngine, rangeD.getStart(), rangeD.getStart() + (1 << 10));
        testPopf(popfEngine, rangeD.getEnd() - (1 << 10), rangeD.getEnd());
    }

    private void testPopf(Zlp24LongPopfEngine popfEngine, long minInput, long maxInput) throws CryptoException {
        TLongList outputs = new TLongArrayList();
        int count = 0;
        for (long input = minInput; input <= maxInput; input++) {
            long output = popfEngine.popf(input);
            outputs.add(output);
            // verify partial order-preserving
            if (count > 0) {
                Assert.assertTrue((outputs.get(count) >= outputs.get(count - 1)));
            }
            count++;
        }
    }
}
