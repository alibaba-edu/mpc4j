package edu.alibaba.mpc4j.common.tool.crypto.tcrhf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.tcrhf.TcrhfFactory.TcrhfType;
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
 * 可调抗关联哈希函数测试。
 *
 * @author Weiran Liu
 * @date 2022/01/11
 */
@RunWith(Parameterized.class)
public class TcrhfTest {

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
        Collection<Object[]> configurationParams = new ArrayList<>();
        // TMMO
        configurationParams.add(new Object[] {TcrhfType.TMMO.name(), TcrhfType.TMMO, });

        return configurationParams;
    }

    /**
     * 待测试的CRHF类型
     */
    public final TcrhfType type;

    public TcrhfTest(String name, TcrhfType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        Tcrhf tcrhf = TcrhfFactory.createInstance(EnvType.STANDARD, type);
        try {
            // 尝试短明文输入
            tcrhf.hash(0, new byte[CommonConstants.BLOCK_BYTE_LENGTH - 1]);
            throw new IllegalStateException("ERROR: successfully call TCRHF with length less than 16 bytes");
        } catch (AssertionError ignored) {

        }
        try {
            // 尝试长明文输入
            tcrhf.hash(0, new byte[CommonConstants.BLOCK_BYTE_LENGTH + 1]);
            throw new IllegalStateException("ERROR: successfully call TCRHF with length larger than 16 bytes");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testType() {
        Tcrhf tcrhf = TcrhfFactory.createInstance(EnvType.STANDARD, type);
        Assert.assertEquals(type, tcrhf.getTcrhfType());
    }

    @Test
    public void testConstantTcrhf() {
        Tcrhf tcrhf = TcrhfFactory.createInstance(EnvType.STANDARD, type);
        // 调用一次TCRHF
        byte[] hash0 = tcrhf.hash(0, ZERO_MESSAGE);
        Assert.assertNotEquals(ByteBuffer.wrap(ZERO_MESSAGE), ByteBuffer.wrap(hash0));
        // 两次调用TCRHF，结果相同
        byte[] anHash0 = tcrhf.hash(0, ZERO_MESSAGE);
        Assert.assertEquals(ByteBuffer.wrap(hash0), ByteBuffer.wrap(anHash0));
        // 用另一个索引值调用TCRHF，结果应该也不相同
        byte[] hash1 = tcrhf.hash(1, ZERO_MESSAGE);
        Assert.assertNotEquals(ByteBuffer.wrap(ZERO_MESSAGE), ByteBuffer.wrap(hash1));
        Assert.assertNotEquals(ByteBuffer.wrap(hash0), ByteBuffer.wrap(hash1));
    }

    @Test
    public void testRandomMessageTcrhf() {
        Set<ByteBuffer> randomHashSet = new HashSet<>();
        Tcrhf tcrhf = TcrhfFactory.createInstance(EnvType.STANDARD, type);
        // 不同消息的CRHF结果应不相同
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            byte[] randomMessage = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
            SECURE_RANDOM.nextBytes(randomMessage);
            randomHashSet.add(ByteBuffer.wrap(tcrhf.hash(SECURE_RANDOM.nextInt(), randomMessage)));
        }
        Assert.assertEquals(MAX_RANDOM_ROUND, randomHashSet.size());
    }

    @Test
    public void testParallel() {
        Tcrhf tcrhf = TcrhfFactory.createInstance(EnvType.STANDARD, type);
        Set<ByteBuffer> hashSet = IntStream.range(0, MAX_PARALLEL)
            .parallel()
            .mapToObj(index -> tcrhf.hash(0, ZERO_MESSAGE))
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        Assert.assertEquals(1, hashSet.size());
    }
}
