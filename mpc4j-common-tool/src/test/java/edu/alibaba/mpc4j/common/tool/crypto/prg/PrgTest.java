package edu.alibaba.mpc4j.common.tool.crypto.prg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory.PrgType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * PRG test.
 *
 * @author Weiran Liu
 * @date 2021/12/05
 */
@RunWith(Parameterized.class)
public class PrgTest {
    /**
     * max random round
     */
    private static final int MAX_RANDOM_ROUND = 400;
    /**
     * max parallel num
     */
    private static final int MAX_PARALLEL = 1 << 10;
    /**
     * all-zero seed
     */
    private static final byte[] ZERO_SEED = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * the random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {

        Collection<Object[]> configurations = new ArrayList<>();
        // JDK_SECURE_RANDOM
        configurations.add(new Object[]{PrgType.JDK_SECURE_RANDOM.name(), PrgType.JDK_SECURE_RANDOM,});
        // JDK_AES_CTR
        configurations.add(new Object[]{PrgType.JDK_AES_CTR.name(), PrgType.JDK_AES_CTR,});
        // JDK_AES_ECB
        configurations.add(new Object[]{PrgType.JDK_AES_ECB.name(), PrgType.JDK_AES_ECB,});
        // JDK_SM4_CTR
        configurations.add(new Object[]{PrgType.BC_SM4_CTR.name(), PrgType.BC_SM4_CTR,});
        // JDK_SM4_ECB
        configurations.add(new Object[]{PrgType.BC_SM4_ECB.name(), PrgType.BC_SM4_ECB,});

        return configurations;
    }

    /**
     * type
     */
    private final PrgType type;

    public PrgTest(String name, PrgType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        // create PRG with 0 output byte length
        Assert.assertThrows(AssertionError.class, () -> PrgFactory.createInstance(type, 0));

        Prg prg = PrgFactory.createInstance(type, CommonConstants.BLOCK_BYTE_LENGTH);
        // invoke PRG with seed.length < λ
        Assert.assertThrows(AssertionError.class, () -> prg.extendToBytes(new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1]));
        // invoke PRG with seed.length > λ
        Assert.assertThrows(AssertionError.class, () -> prg.extendToBytes(new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1]));
    }

    @Test
    public void testType() {
        Prg prg = PrgFactory.createInstance(type, CommonConstants.BLOCK_BYTE_LENGTH);
        Assert.assertEquals(type, prg.getPrgType());
    }

    @Test
    public void testConstantSeed() {
        testConstantSeed(1);
        testConstantSeed(CommonConstants.STATS_BYTE_LENGTH - 1);
        testConstantSeed(CommonConstants.STATS_BYTE_LENGTH);
        testConstantSeed(CommonConstants.STATS_BYTE_LENGTH + 1);
        testConstantSeed(CommonConstants.BLOCK_BYTE_LENGTH - 1);
        testConstantSeed(CommonConstants.BLOCK_BYTE_LENGTH);
        testConstantSeed(CommonConstants.BLOCK_BYTE_LENGTH + 1);
        testConstantSeed(2 * CommonConstants.BLOCK_BYTE_LENGTH - 1);
        testConstantSeed(2 * CommonConstants.BLOCK_BYTE_LENGTH);
        testConstantSeed(2 * CommonConstants.BLOCK_BYTE_LENGTH + 1);
    }

    private void testConstantSeed(int outputByteLength) {
        Prg prg = PrgFactory.createInstance(type, outputByteLength);
        // 第1次调用，输出长度应为指定的输出长度
        byte[] output = prg.extendToBytes(ZERO_SEED);
        Assert.assertEquals(outputByteLength, output.length);
        // 第2次调用，输出结果与第一次结果相同
        byte[] anOutput = prg.extendToBytes(ZERO_SEED);
        Assert.assertArrayEquals(output, anOutput);
    }

    @Test
    public void testRandomSeed() {
        testRandomSeed(CommonConstants.STATS_BYTE_LENGTH);
        testRandomSeed(CommonConstants.BLOCK_BYTE_LENGTH - 1);
        testRandomSeed(CommonConstants.BLOCK_BYTE_LENGTH);
        testRandomSeed(CommonConstants.BLOCK_BYTE_LENGTH + 1);
        testRandomSeed(2 * CommonConstants.BLOCK_BYTE_LENGTH - 1);
        testRandomSeed(2 * CommonConstants.BLOCK_BYTE_LENGTH);
        testRandomSeed(2 * CommonConstants.BLOCK_BYTE_LENGTH + 1);
    }

    private void testRandomSeed(int outputByteLength) {
        Set<ByteBuffer> outputSet = new HashSet<>();
        Prg prg = PrgFactory.createInstance(type, outputByteLength);
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomSeed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(randomSeed);
            outputSet.add(ByteBuffer.wrap(prg.extendToBytes(randomSeed)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, outputSet.size());
    }

    @Test
    public void testParallel() {
        Prg prg = PrgFactory.createInstance(type, CommonConstants.BLOCK_BYTE_LENGTH);
        Set<ByteBuffer> extendSet = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> prg.extendToBytes(ZERO_SEED))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, extendSet.size());
    }
}
