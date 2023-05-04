package edu.alibaba.mpc4j.common.tool.okve.ovdm.gf2e;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2e;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eLinearSolver;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.security.SecureRandom;

/**
 * GF(2^l)-OVDM抽象类。
 *
 * @author Weiran Liu
 * @date 2021/09/27
 */
abstract class AbstractGf2eOvdm<T> implements Gf2eOvdm<T> {
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
    final int mByteLength;
    /**
     * OKVS映射值最大比特长度，满足{l % Byte.SIZE == 0}。
     */
    protected final int l;
    /**
     * l的字节长度
     */
    protected final int byteL;
    /**
     * 编码过程所用到的随机状态
     */
    protected final SecureRandom secureRandom;
    /**
     * GF2E
     */
    protected final Gf2e gf2e;
    /**
     * 线性求解器
     */
    protected final Gf2eLinearSolver gf2eLinearSolver;

    protected AbstractGf2eOvdm(EnvType envType, int l, int n, int m) {
        assert n > 0;
        this.n = n;
        int minL = LongUtils.ceilLog2(n) + CommonConstants.STATS_BIT_LENGTH;
        assert l >= minL : "l must be greater than or equal to " + minL + ": " + l;
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // 要求m >= n，且m可以被Byte.SIZE整除
        assert m >= n && m % Byte.SIZE == 0;
        this.m = m;
        mByteLength = m / Byte.SIZE;
        secureRandom = new SecureRandom();
        gf2e = Gf2eFactory.createInstance(envType, l);
        gf2eLinearSolver = new Gf2eLinearSolver(gf2e);
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public int getM() {return m;}

    @Override
    public int getL() {
        return l;
    }
}
