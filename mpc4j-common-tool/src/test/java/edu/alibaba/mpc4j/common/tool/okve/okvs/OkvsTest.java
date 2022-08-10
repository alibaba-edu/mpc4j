package edu.alibaba.mpc4j.common.tool.okve.okvs;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * OKVS测试。
 *
 * @author Weiran Liu
 * @date 2022/01/02
 */
@RunWith(Parameterized.class)
public class OkvsTest {
    /**
     * 默认键值对数量
     */
    private static final int DEFAULT_N = 10;
    /**
     * 随机测试轮数
     */
    private static final int MAX_RANDOM_ROUND = 50;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // H3_SINGLETON_GCT
        configurationParams.add(new Object[] {OkvsType.H3_SINGLETON_GCT.name(), OkvsType.H3_SINGLETON_GCT });
        // H2_SINGLETON_GCT
        configurationParams.add(new Object[] {OkvsType.H2_SINGLETON_GCT.name(), OkvsType.H2_SINGLETON_GCT });
        // H2_TWO_CORE_GCT
        configurationParams.add(new Object[] {OkvsType.H2_TWO_CORE_GCT.name(), OkvsType.H2_TWO_CORE_GCT });
        // H2_DFS_GCT
        configurationParams.add(new Object[] {OkvsType.H2_DFS_GCT.name(), OkvsType.H2_DFS_GCT });
        // GBF
        configurationParams.add(new Object[] {OkvsType.GBF.name(), OkvsType.GBF });
        // MEGA_BIN
        configurationParams.add(new Object[] {OkvsType.MEGA_BIN.name(), OkvsType.MEGA_BIN });
        // POLYNOMIAL
        configurationParams.add(new Object[] {OkvsType.POLYNOMIAL.name(), OkvsType.POLYNOMIAL });

        return configurationParams;
    }

    /**
     * OKVS类型
     */
    private final OkvsType type;

    public OkvsTest(String name, OkvsType type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.type = type;
    }

    @Test
    public void testIllegalInputs() {
        // 尝试设置错误数量的密钥
        if (OkvsFactory.getHashNum(type) > 0) {
            try {
                byte[][] lessKeys = CommonUtils.generateRandomKeys(
                    OkvsFactory.getHashNum(type) + 1, OkvsTestUtils.SECURE_RANDOM
                );
                OkvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, OkvsTestUtils.DEFAULT_L, lessKeys);
                throw new IllegalStateException("ERROR: successfully create OKVS with more keys");
            } catch (AssertionError ignored) {

            }
            try {
                byte[][] moreKeys = CommonUtils.generateRandomKeys(
                    OkvsFactory.getHashNum(type) - 1, OkvsTestUtils.SECURE_RANDOM
                );
                OkvsFactory.createInstance(EnvType.STANDARD, type, DEFAULT_N, OkvsTestUtils.DEFAULT_L, moreKeys);
                throw new IllegalStateException("ERROR: successfully create OKVS with less keys");
            } catch (AssertionError ignored) {

            }
        }
        byte[][] keys = CommonUtils.generateRandomKeys(OkvsFactory.getHashNum(type), OkvsTestUtils.SECURE_RANDOM);
        // 尝试让l小于统计安全参数
        try {
            OkvsFactory.createInstance(
                EnvType.STANDARD, type, DEFAULT_N, CommonConstants.STATS_BIT_LENGTH - 1, keys
            );
            throw new IllegalStateException("ERROR: successfully create OKVS with l less than λ");
        } catch (AssertionError ignored) {

        }
        // 尝试让n = 0
        try {
            OkvsFactory.createInstance(EnvType.STANDARD, type, 0, OkvsTestUtils.DEFAULT_L, keys);
            throw new IllegalStateException("ERROR: successfully create OKVS with n = 0");
        } catch (AssertionError ignored) {

        }
        // 尝试编码更多的元素
        try {
            Map<ByteBuffer, byte[]> keyValueMap = OkvsTestUtils.randomKeyValueMap(DEFAULT_N + 1);
            Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(
                EnvType.STANDARD, type, DEFAULT_N, OkvsTestUtils.DEFAULT_L, keys
            );
            okvs.encode(keyValueMap);
            throw new IllegalStateException("ERROR: successfully encode key-value map with more elements");
        } catch (AssertionError ignored) {

        }
    }

    @Test
    public void testType() {
        byte[][] keys = CommonUtils.generateRandomKeys(OkvsFactory.getHashNum(type), OkvsTestUtils.SECURE_RANDOM);
        Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(
            EnvType.STANDARD, type, DEFAULT_N, OkvsTestUtils.DEFAULT_L, keys
        );
        Assert.assertEquals(type, okvs.getOkvsType());
    }

    @Test
    public void testEmptyOkvs() {
        byte[][] keys = CommonUtils.generateRandomKeys(OkvsFactory.getHashNum(type), OkvsTestUtils.SECURE_RANDOM);
        Okvs<ByteBuffer> emptyOkvs = OkvsFactory.createInstance(
            EnvType.STANDARD, type, DEFAULT_N, OkvsTestUtils.DEFAULT_L, keys
        );
        // 创建空的键值对
        Map<ByteBuffer, byte[]> emptyKeyValueMap = new HashMap<>(0);
        byte[][] storage = emptyOkvs.encode(emptyKeyValueMap);
        Assert.assertEquals(emptyOkvs.getM(), storage.length);
        Arrays.stream(storage).forEach(row -> Assert.assertEquals(emptyOkvs.getL(), row.length * Byte.SIZE));
    }

    @Test
    public void test1n() {
        testOkvs(1);
    }

    @Test
    public void test2n() {
        testOkvs(2);
    }

    @Test
    public void test3n() {testOkvs(3);}

    @Test
    public void test40n() {
        testOkvs(40);
    }

    @Test
    public void test256n() {
        testOkvs(256);
    }

    @Test
    public void test4096n() {
        testOkvs(4096);
    }

    private void testOkvs(int n) {
        switch (type) {
            case POLYNOMIAL:
            case MEGA_BIN:
                try {
                    byte[][] keys = CommonUtils.generateRandomKeys(
                        OkvsFactory.getHashNum(type), OkvsTestUtils.SECURE_RANDOM
                    );
                    OkvsFactory.createInstance(EnvType.STANDARD, type, 1, OkvsTestUtils.DEFAULT_L, keys);
                    throw new IllegalStateException("ERROR: successfully create OKVS with n = 1");
                } catch (AssertionError ignored) {

                }
                break;
            default:
                for (int round = 0; round < MAX_RANDOM_ROUND; round++) {
                    // 生成密钥
                    byte[][] keys = CommonUtils.generateRandomKeys(
                        OkvsFactory.getHashNum(type), OkvsTestUtils.SECURE_RANDOM
                    );
                    // 创建OKVS实例
                    Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(
                        EnvType.STANDARD, type, n, OkvsTestUtils.DEFAULT_L, keys
                    );
                    // 生成随机键值对
                    Map<ByteBuffer, byte[]> keyValueMap = OkvsTestUtils.randomKeyValueMap(n);
                    // 编码
                    byte[][] storage = okvs.encode(keyValueMap);
                    // 并发解码，验证结果
                    keyValueMap.keySet().stream().parallel().forEach(key -> {
                        byte[] valueBytes = keyValueMap.get(key);
                        byte[] decodeValueBytes = okvs.decode(storage, key);
                        Assert.assertArrayEquals(valueBytes, decodeValueBytes);
                    });
                }
        }
    }
}
