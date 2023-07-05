package edu.alibaba.mpc4j.crypto.matrix.okve.okvs;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.okve.tool.BinaryLinearSolver;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.security.SecureRandom;

/**
 * 二进制不经意键值存储器（OKVS）抽象类。
 *
 * @author Weiran Liu
 * @date 2021/09/05
 */
abstract class AbstractBinaryOkvs<T> implements BinaryOkvs<T> {
    /**
     * OKVS允许编码的键值对数量
     */
    protected final int n;
    /**
     * OKVS长度，满足{@code m % Byte.SIZE == 0}。
     */
    protected final int m;
    /**
     * m in byte
     */
    protected final int byteM;
    /**
     * offset m
     */
    protected final int offsetM;
    /**
     * OKVS映射值比特长度
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
     * 线性求解器
     */
    protected final BinaryLinearSolver linearSolver;
    /**
     * parallel encode
     */
    protected boolean parallelEncode;

    protected AbstractBinaryOkvs(int n, int m, int l) {
        // 二进制OKVS可以编码1个元素
        assert n > 0;
        this.n = n;
        // l >= σ
        int minL = LongUtils.ceilLog2(n) + CommonConstants.STATS_BIT_LENGTH;
        assert l >= minL : "l must be greater than or equal to " + minL + ": " + l;
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        // 要求m >= n，且m可以被Byte.SIZE整除
        assert m >= n && m % Byte.SIZE == 0;
        this.m = m;
        byteM = CommonUtils.getByteLength(m);
        offsetM = byteM * Byte.SIZE - m;
        secureRandom = new SecureRandom();
        linearSolver = new BinaryLinearSolver(l);
        parallelEncode = false;
    }

    @Override
    public void setParallelEncode(boolean parallelEncode) {
        this.parallelEncode = parallelEncode;
    }

    @Override
    public boolean getParallelEncode() {
        return parallelEncode;
    }

    @Override
    public int getN() {
        return n;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int getByteL() {
        return byteL;
    }

    @Override
    public int getM() {return m;}
}
