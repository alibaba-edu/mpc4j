package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory.ByteEccType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 乘法字节椭圆曲线一致性测试。
 *
 * @author Weiran Liu
 * @date 2022/9/6
 */
@RunWith(Parameterized.class)
@Ignore
public class ByteEccConsistencyTest {
    /**
     * 最大随机轮数
     */
    private static final int MAX_RANDOM_ROUND = 100;
    /**
     * 随机状态
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // X25519
        configurationParams.add(new Object[] {"X25519 (BC v.s. Sodium)", ByteEccType.X25519_BC, ByteEccType.X25519_SODIUM});
        // ED25519
        configurationParams.add(new Object[] {"ED25519 (BC v.s. Sodium)", ByteEccType.ED25519_BC, ByteEccType.ED25519_SODIUM});

        return configurationParams;
    }

    /**
     * JDK类型
     */
    private final ByteEccType jdkType;
    /**
     * 本地类型
     */
    private final ByteEccType nativeType;

    public ByteEccConsistencyTest(String name, ByteEccType jdkType, ByteEccType nativeType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.jdkType = jdkType;
        this.nativeType = nativeType;
    }

    @Test
    public void testMul() {
        ByteMulEcc jdkByteMulEcc = ByteEccFactory.createMulInstance(jdkType);
        ByteMulEcc nativeByteMulEcc = ByteEccFactory.createMulInstance(nativeType);
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            // 用JDK的ByteEcc生成随机数
            byte[] k = jdkByteMulEcc.randomScalar(SECURE_RANDOM);
            byte[] p = jdkByteMulEcc.randomPoint(SECURE_RANDOM);
            byte[] jdkMul = jdkByteMulEcc.mul(p, k);
            byte[] nativeMul = nativeByteMulEcc.mul(p, k);
            Assert.assertArrayEquals(jdkMul, nativeMul);
            // 用Native的ByteEcc生成随机数
            k = nativeByteMulEcc.randomScalar(SECURE_RANDOM);
            p = nativeByteMulEcc.randomPoint(SECURE_RANDOM);
            jdkMul = jdkByteMulEcc.mul(p, k);
            nativeMul = nativeByteMulEcc.mul(p, k);
            Assert.assertArrayEquals(jdkMul, nativeMul);
        }
    }
}
