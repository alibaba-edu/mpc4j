package edu.alibaba.mpc4j.common.tool.crypto.crhf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.crhf.CrhfFactory.CrhfType;
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
 * 抗关联哈希函数测试。
 *
 * @author Weiran Liu
 * @date 2022/01/11
 */
@RunWith(Parameterized.class)
public class CrhfTest {
    /**
     * 最大随机轮数
     */
    private static final int MAX_RANDOM_ROUND = 400;
    /**
     * 并发数量
     */
    private static final int MAX_PARALLEL = 1 << 10;
    /**
     * 全0明文
     */
    private static final byte[] ZERO_MESSAGE = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name="{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // MMO_SIGMA
        configurations.add(new Object[] {CrhfType.MMO_SIGMA.name(), CrhfType.MMO_SIGMA, });
        // MMO
        configurations.add(new Object[] {CrhfType.MMO.name(), CrhfType.MMO, });

        return configurations;
    }

    /**
     * 测试类型
     */
    public final CrhfType type;

    public CrhfTest(String name, CrhfType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        Crhf crhf = CrhfFactory.createInstance(EnvType.STANDARD, type);
        try {
            // 尝试短明文输入
            crhf.hash(new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1]);
            throw new IllegalStateException("ERROR: successfully call CRHF with length less than 16 bytes");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试长明文输入
            crhf.hash(new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1]);
            throw new IllegalStateException("ERROR: successfully call CRHF with length larger than 16 bytes");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testType() {
        Crhf crhf = CrhfFactory.createInstance(EnvType.STANDARD, type);
        Assert.assertEquals(type, crhf.getCrhfType());
    }

    @Test
    public void testConstantCrhf() {
        Crhf crhf = CrhfFactory.createInstance(EnvType.STANDARD, type);
        // 调用一次CRHF
        byte[] hash = crhf.hash(ZERO_MESSAGE);
        Assert.assertNotEquals(ByteBuffer.wrap(ZERO_MESSAGE), ByteBuffer.wrap(hash));
        // 两次调用CRHF，结果相同
        byte[] anHash = crhf.hash(ZERO_MESSAGE);
        Assert.assertEquals(ByteBuffer.wrap(hash), ByteBuffer.wrap(anHash));
    }

    @Test
    public void testRandomMessageCrhf() {
        Set<ByteBuffer> randomHashSet = new HashSet<>();
        Crhf crhf = CrhfFactory.createInstance(EnvType.STANDARD, type);
        // 不同消息的CRHF结果应不相同
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomMessage = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(randomMessage);
            randomHashSet.add(ByteBuffer.wrap(crhf.hash(randomMessage)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, randomHashSet.size());
    }

    @Test
    public void testParallel() {
        Crhf crhf = CrhfFactory.createInstance(EnvType.STANDARD, type);
        Set<ByteBuffer> hashSet = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> crhf.hash(ZERO_MESSAGE))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, hashSet.size());
    }
}
