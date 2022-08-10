package edu.alibaba.mpc4j.common.tool.okve.okvs;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePoly;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePolyFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * 基于多项式插值的不经意键值对存储器（OKVS）。
 * 与其他OKVS相比，多项式插值OKVS的特点在于，键值（key）和映射值（value）都必须是相同长度的字节数组。
 *
 * @author Weiran Liu
 * @date 2021/09/13
 */
class PolynomialOkvs implements Okvs<ByteBuffer> {
    /**
     * 多项式插值服务
     */
    private final Gf2ePoly gf2ePoly;
    /**
     * 插值数量
     */
    private final int n;
    /**
     * 插值对的比特长度，要求是Byte.SIZE的整数倍
     */
    private final int l;

    PolynomialOkvs(EnvType envType, int n, int l) {
        // 多项式OKVS要求至少编码2个元素
        assert n > 1;
        this.n = n;
        // 要求l > 统计安全常数，且l可以被Byte.SIZE整除
        assert l >= CommonConstants.STATS_BIT_LENGTH && l % Byte.SIZE == 0;
        this.l = l;
        gf2ePoly = Gf2ePolyFactory.createInstance(envType, l);
    }

    @Override
    public void setParallelEncode(boolean parallelEncode) {
        // 多项式编码难以并行
    }

    @Override
    public byte[][] encode(Map<ByteBuffer, byte[]> keyValueMap) throws ArithmeticException {
        assert keyValueMap.size() <= n;
        byte[][] xArray = keyValueMap.keySet().stream().map(ByteBuffer::array).toArray(byte[][]::new);
        byte[][] yArray = keyValueMap.keySet().stream().map(keyValueMap::get).toArray(byte[][]::new);
        // 给定的键值对数量可能小于n，此时要用dummy interpolate将插入的点数量补充到n
        return gf2ePoly.interpolate(n, xArray, yArray);
    }

    @Override
    public byte[] decode(byte[][] storage, ByteBuffer key) {
        // 这里不能验证storage每一行的长度，否则整体算法复杂度会变为O(n^2)
        assert storage.length == getM();
        return gf2ePoly.evaluate(storage, key.array());
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
    public int getM() {
        // 等于多项式插值的系数数量
        return gf2ePoly.coefficientNum(n);
    }

    @Override
    public OkvsType getOkvsType() {
        return OkvsType.POLYNOMIAL;
    }

    @Override
    public int getNegLogFailureProbability() {
        return Integer.MAX_VALUE;
    }
}
