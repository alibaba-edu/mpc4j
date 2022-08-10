package edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc.EccOvdmFactory.EccOvdmType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.math.ec.ECPoint;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.IntStream;

/**
 * ECC-OVDM测试。
 *
 * @author Weiran Liu
 * @date 2022/01/04
 */
@RunWith(Parameterized.class)
public class EccOvdmTest {
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
        configurationParams.add(new Object[]{EccOvdmType.H2_TWO_CORE_GCT.name(), EccOvdmType.H2_TWO_CORE_GCT});
        // H2_SINGLETON_GCT
        configurationParams.add(new Object[]{EccOvdmType.H2_SINGLETON_GCT.name(), EccOvdmType.H2_SINGLETON_GCT});
        // H3_SINGLETON_GCT
        configurationParams.add(new Object[]{EccOvdmType.H3_SINGLETON_GCT.name(), EccOvdmType.H3_SINGLETON_GCT});

        return configurationParams;
    }

    /**
     * ECC-OVDM类型
     */
    private final EccOvdmType type;

    public EccOvdmTest(String name, EccOvdmType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        // 尝试设置错误数量的密钥
        if (EccOvdmFactory.getHashNum(type) > 0) {
            byte[][] moreKeys = CommonUtils.generateRandomKeys(
                EccOvdmFactory.getHashNum(type) + 1, EccOvdmTestUtils.SECURE_RANDOM
            );
            try {
                EccOvdmFactory.createInstance(EnvType.STANDARD, type, EccOvdmTestUtils.ECC, DEFAULT_N, moreKeys);
                throw new IllegalStateException("ERROR: successfully create OVDM with more keys");
            } catch (AssertionError ignored) {

            }
            byte[][] lessKeys = CommonUtils.generateRandomKeys(
                EccOvdmFactory.getHashNum(type) - 1, EccOvdmTestUtils.SECURE_RANDOM
            );
            try {
                EccOvdmFactory.createInstance(EnvType.STANDARD, type, EccOvdmTestUtils.ECC, DEFAULT_N, lessKeys);
                throw new IllegalStateException("ERROR: successfully create OVDM with less keys");
            } catch (AssertionError ignored) {

            }
        }
        byte[][] keys = CommonUtils.generateRandomKeys(EccOvdmFactory.getHashNum(type), EccOvdmTestUtils.SECURE_RANDOM);
        // 尝试让n = 0
        try {
            EccOvdmFactory.createInstance(EnvType.STANDARD, type, EccOvdmTestUtils.ECC, 0, keys);
            throw new IllegalStateException("ERROR: successfully create OVDM with n = 0");
        } catch (AssertionError ignored) {

        }
        // 尝试编码更多的元素
        Map<ByteBuffer, ECPoint> keyValueMap = EccOvdmTestUtils.randomKeyValueMap(DEFAULT_N + 1);
        try {
            EccOvdm<ByteBuffer> ovdm = EccOvdmFactory.createInstance(
                EnvType.STANDARD, type, EccOvdmTestUtils.ECC, DEFAULT_N, keys
            );
            ovdm.encode(keyValueMap);
            throw new IllegalStateException("ERROR: successfully encode key-value map with more elements");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testType() {
        byte[][] keys = CommonUtils.generateRandomKeys(EccOvdmFactory.getHashNum(type), EccOvdmTestUtils.SECURE_RANDOM);
        EccOvdm<ByteBuffer> ovdm = EccOvdmFactory.createInstance(
            EnvType.STANDARD, type, EccOvdmTestUtils.ECC, DEFAULT_N, keys
        );
        Assert.assertEquals(type, ovdm.getEccOvdmType());
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
                EccOvdmFactory.getHashNum(type), EccOvdmTestUtils.SECURE_RANDOM
            );
            // 创建OKVS实例
            EccOvdm<ByteBuffer> ovdm = EccOvdmFactory.createInstance(
                EnvType.STANDARD, type, EccOvdmTestUtils.ECC, n, keys
            );
            // 生成随机键值对
            Map<ByteBuffer, ECPoint> keyValueMap = EccOvdmTestUtils.randomKeyValueMap(n);
            // 编码
            ECPoint[] storage = ovdm.encode(keyValueMap);
            // 并发解码，验证结果
            keyValueMap.keySet().stream().parallel().forEach(key -> {
                ECPoint value = keyValueMap.get(key);
                ECPoint decodeValue = ovdm.decode(storage, key);
                Assert.assertEquals(value, decodeValue);
            });
            // 验证随机输入的解码结果不在值集合中
            Set<ECPoint> valueSet = new HashSet<>(keyValueMap.values());
            IntStream.range(0, MAX_RANDOM_ROUND).forEach(index -> {
                // 生成比特长度为安全常数的x，生成l比特长的y，插入到Map中
                byte[] randomKeyBytes = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                EccOvdmTestUtils.SECURE_RANDOM.nextBytes(randomKeyBytes);
                ByteBuffer randomKey = ByteBuffer.wrap(randomKeyBytes);
                if (!keyValueMap.containsKey(randomKey)) {
                    ECPoint randomDecodeValue = ovdm.decode(storage, randomKey);
                    Assert.assertFalse(valueSet.contains(randomDecodeValue));
                }
            });
        }
    }
}
