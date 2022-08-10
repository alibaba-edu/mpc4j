package edu.alibaba.mpc4j.common.tool.okve.ovdm.gf2e;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.gf2e.Gf2eOvdmFactory.Gf2eOvdmType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GF(2^l)-OVDM测试。
 *
 * @author Weiran Liu
 * @date 2022/01/06
 */
@RunWith(Parameterized.class)
public class Gf2eOvdmTest {
    /**
     * 默认键值对数量
     */
    private static final int DEFAULT_N = 10;
    /**
     * 随机测试轮数
     */
    private static final int MAX_RANDOM_ROUND = 10;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // H2_TWO_CORE_GCT
        configurationParams.add(new Object[]{Gf2eOvdmType.H2_TWO_CORE_GCT.name(), Gf2eOvdmType.H2_TWO_CORE_GCT});
        // H2_SINGLETON_GCT
        configurationParams.add(new Object[]{Gf2eOvdmType.H2_SINGLETON_GCT.name(), Gf2eOvdmType.H2_SINGLETON_GCT});
        // H3_SINGLETON_GCT
        configurationParams.add(new Object[]{Gf2eOvdmType.H3_SINGLETON_GCT.name(), Gf2eOvdmType.H3_SINGLETON_GCT});

        return configurationParams;
    }

    /**
     * GF(2^l)-OVDM类型
     */
    private final Gf2eOvdmType type;

    public Gf2eOvdmTest(String name, Gf2eOvdmType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        // 尝试设置错误数量的密钥
        if (Gf2eOvdmFactory.getHashNum(type) > 0) {
            byte[][] moreKeys = CommonUtils.generateRandomKeys(
                Gf2eOvdmFactory.getHashNum(type) + 1, Gf2eOvdmTestUtils.SECURE_RANDOM
            );
            try {
                Gf2eOvdmFactory.createInstance(
                    EnvType.STANDARD, type, Gf2eOvdmTestUtils.DEFAULT_L, DEFAULT_N, moreKeys
                );
                throw new IllegalStateException("ERROR: successfully create OVDM with more keys");
            } catch (AssertionError ignored) {

            }
            byte[][] lessKeys = CommonUtils.generateRandomKeys(
                Gf2eOvdmFactory.getHashNum(type) - 1, Gf2eOvdmTestUtils.SECURE_RANDOM
            );
            try {
                Gf2eOvdmFactory.createInstance(
                    EnvType.STANDARD, type, Gf2eOvdmTestUtils.DEFAULT_L, DEFAULT_N, lessKeys
                );
                throw new IllegalStateException("ERROR: successfully create OVDM with less keys");
            } catch (AssertionError ignored) {

            }
        }
        byte[][] keys = CommonUtils.generateRandomKeys(
            Gf2eOvdmFactory.getHashNum(type), Gf2eOvdmTestUtils.SECURE_RANDOM
        );
        // 尝试让n = 0
        try {
            Gf2eOvdmFactory.createInstance(EnvType.STANDARD, type, Gf2eOvdmTestUtils.DEFAULT_L, 0, keys);
            throw new IllegalStateException("ERROR: successfully create OVDM with n = 0");
        } catch (AssertionError ignored) {

        }
        Gf2eOvdm<ByteBuffer> ovdm = Gf2eOvdmFactory.createInstance(
            EnvType.STANDARD, type, Gf2eOvdmTestUtils.DEFAULT_L, DEFAULT_N, keys
        );
        // 尝试编码更多的元素
        Map<ByteBuffer, byte[]> keyValueMap = Gf2eOvdmTestUtils.randomKeyValueMap(DEFAULT_N + 1);
        try {
            ovdm.encode(keyValueMap);
            throw new IllegalStateException("ERROR: successfully encode key-value map with more elements");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testType() {
        byte[][] keys = CommonUtils.generateRandomKeys(
            Gf2eOvdmFactory.getHashNum(type), Gf2eOvdmTestUtils.SECURE_RANDOM
        );
        Gf2eOvdm<ByteBuffer> ovdm = Gf2eOvdmFactory.createInstance(
            EnvType.STANDARD, type, Gf2eOvdmTestUtils.DEFAULT_L, DEFAULT_N, keys
        );
        Assert.assertEquals(type, ovdm.getGf2xOvdmType());
    }

    @Test
    public void test1n() {
        testOvdm(1);
    }

    @Test
    public void test2n() {
        testOvdm(2);
    }

    @Test
    public void test3n() {
        testOvdm(3);
    }

    @Test
    public void test40n() {
        testOvdm(40);
    }

    @Test
    public void test256n() {
        testOvdm(256);
    }

    @Test
    public void test4096n() {
        testOvdm(4096);
    }

    private void testOvdm(int n) {
        for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
            // 生成密钥
            byte[][] keys = CommonUtils.generateRandomKeys(
                Gf2eOvdmFactory.getHashNum(type), Gf2eOvdmTestUtils.SECURE_RANDOM
            );
            // 创建OKVS实例
            Gf2eOvdm<ByteBuffer> ovdm = Gf2eOvdmFactory.createInstance(
                EnvType.STANDARD, type, Gf2eOvdmTestUtils.DEFAULT_L, n, keys
            );
            // 生成随机键值对
            Map<ByteBuffer, byte[]> keyValueMap = Gf2eOvdmTestUtils.randomKeyValueMap(n);
            // 编码
            byte[][] storage = ovdm.encode(keyValueMap);
            // 并发解码，验证结果
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                byte[] value = keyValueMap.get(key);
                byte[] decodeValue = ovdm.decode(storage, key);
                Assert.assertArrayEquals(value, decodeValue);
            });
            // 验证随机输入的解码结果不在值集合中
            Set<ByteBuffer> valueSet = keyValueMap.values().stream().map(ByteBuffer::wrap).collect(Collectors.toSet());
            IntStream.range(0, MAX_RANDOM_ROUND).forEach(index -> {
                // 生成比特长度为安全常数的x，生成l比特长的y，插入到Map中
                byte[] randomKeyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                Gf2eOvdmTestUtils.SECURE_RANDOM.nextBytes(randomKeyBytes);
                ByteBuffer randomKey = ByteBuffer.wrap(randomKeyBytes);
                if (!keyValueMap.containsKey(randomKey)) {
                    byte[] randomDecodeValue = ovdm.decode(storage, randomKey);
                    Assert.assertFalse(valueSet.contains(ByteBuffer.wrap(randomDecodeValue)));
                }
            });
        }
    }
}
