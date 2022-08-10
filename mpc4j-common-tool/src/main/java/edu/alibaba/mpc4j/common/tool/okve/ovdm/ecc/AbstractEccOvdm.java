package edu.alibaba.mpc4j.common.tool.okve.ovdm.ecc;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.EccLinearSolver;

import java.security.SecureRandom;

/**
 * 椭圆曲线不经意映射值解密匹配（OVDM）抽象类。
 *
 * @author Weiran Liu
 * @date 2021/09/07
 */
abstract class AbstractEccOvdm<T> implements EccOvdm<T> {
    /**
     * 椭圆曲线操作接口
     */
    protected final Ecc ecc;
    /**
     * 椭圆曲线线性求解器
     */
    protected final EccLinearSolver eccLinearSolver;
    /**
     * OVDM允许编码的键值对数量
     */
    protected final int n;
    /**
     * OVDM长度，满足{@code m % Byte.SIZE == 0}。
     */
    protected final int m;
    /**
     * m的字节长度
     */
    protected final int mByteLength;
    /**
     * 编码随机状态
     */
    protected SecureRandom secureRandom;

    protected AbstractEccOvdm(Ecc ecc, int n, int m) {
        assert n > 0;
        // m > 0，且m为Byte.SIZE的整数倍
        assert m >= n && m % Byte.SIZE == 0;
        this.ecc = ecc;
        eccLinearSolver = new EccLinearSolver(ecc);
        this.n = n;
        this.m = m;
        this.mByteLength = m / Byte.SIZE;
        secureRandom = new SecureRandom();
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public int getM() {
        return m;
    }
}
