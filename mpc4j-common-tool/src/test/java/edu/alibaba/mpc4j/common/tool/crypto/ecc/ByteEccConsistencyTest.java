package edu.alibaba.mpc4j.common.tool.crypto.ecc;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory.ByteEccType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
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
        Collection<Object[]> configurations = new ArrayList<>();

        // STANDARD
        configurations.add(new Object[]{
            EnvType.STANDARD.name() + " v.s. " + EnvType.STANDARD_JDK.name() + " (Full Type)",
            ByteEccFactory.getFullType(EnvType.STANDARD), ByteEccFactory.getFullType(EnvType.STANDARD_JDK)
        });
        configurations.add(new Object[]{
            EnvType.STANDARD.name() + " v.s. " + EnvType.STANDARD_JDK.name() + " (Mul Type)",
            ByteEccFactory.getMulType(EnvType.STANDARD), ByteEccFactory.getMulType(EnvType.STANDARD_JDK)
        });
        // INLAND
        configurations.add(new Object[]{
            EnvType.INLAND.name() + " v.s. " + EnvType.INLAND_JDK.name() + " (Full Type)",
            ByteEccFactory.getFullType(EnvType.INLAND), ByteEccFactory.getFullType(EnvType.INLAND_JDK)
        });
        configurations.add(new Object[]{
            EnvType.INLAND.name() + " v.s. " + EnvType.INLAND_JDK.name() + " (Mul Type)",
            ByteEccFactory.getMulType(EnvType.INLAND), ByteEccFactory.getMulType(EnvType.INLAND_JDK)
        });
        // X25519
        configurations.add(new Object[] {"X25519 (BC v.s. Sodium)", ByteEccType.X25519_BC, ByteEccType.X25519_SODIUM});
        // ED25519
        configurations.add(new Object[] {"ED25519 (BC v.s. Sodium)", ByteEccType.ED25519_BC, ByteEccType.ED25519_SODIUM});
        configurations.add(new Object[] {"ED25519 (BC v.s. Cafe)", ByteEccType.ED25519_BC, ByteEccType.ED25519_CAFE});

        return configurations;
    }

    /**
     * 被比较类型
     */
    private final ByteEccType thisType;
    /**
     * 比较类型
     */
    private final ByteEccType thatType;

    public ByteEccConsistencyTest(String name, ByteEccType thisType, ByteEccType thatType) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.thisType = thisType;
        this.thatType = thatType;
    }

    @Test
    public void testMul() {
        ByteMulEcc thisByteMulEcc = ByteEccFactory.createMulInstance(thisType);
        ByteMulEcc thatByteMulEcc = ByteEccFactory.createMulInstance(thatType);
        for (int i = 0; i < MAX_RANDOM_ROUND; i++) {
            // 用被比较的ByteEcc生成随机数
            byte[] k = thisByteMulEcc.randomScalar(SECURE_RANDOM);
            byte[] p = thisByteMulEcc.randomPoint(SECURE_RANDOM);
            byte[] jdkMul = thisByteMulEcc.mul(p, k);
            byte[] nativeMul = thatByteMulEcc.mul(p, k);
            Assert.assertArrayEquals(jdkMul, nativeMul);
            // 用比较的ByteEcc生成随机数
            k = thatByteMulEcc.randomScalar(SECURE_RANDOM);
            p = thatByteMulEcc.randomPoint(SECURE_RANDOM);
            jdkMul = thisByteMulEcc.mul(p, k);
            nativeMul = thatByteMulEcc.mul(p, k);
            Assert.assertArrayEquals(jdkMul, nativeMul);
        }
    }
}
