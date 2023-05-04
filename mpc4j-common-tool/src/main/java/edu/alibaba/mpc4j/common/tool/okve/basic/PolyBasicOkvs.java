package edu.alibaba.mpc4j.common.tool.okve.basic;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePoly;
import edu.alibaba.mpc4j.common.tool.polynomial.gf2e.Gf2ePolyFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Polynomial basic OKVS.
 *
 * @author Weiran Liu
 * @date 2023/3/27
 */
public class PolyBasicOkvs implements BasicOkvs {
    /**
     * the polynomial interpolation interface.
     */
    private final Gf2ePoly gf2ePoly;
    /**
     * the number of interpolated points.
     */
    private final int n;
    /**
     * the key / value bit length, which must satisfies l % Byte.SIZE == 0
     */
    private final int l;
    /**
     * the key / value byte length.
     */
    private final int byteL;
    /**
     * parallel encode
     */
    private boolean parallelEncode;

    public PolyBasicOkvs(EnvType envType, int n, int l) {
        assert n > 0 : "n must be greater than 0: " + n;
        this.n = n;
        int minL = LongUtils.ceilLog2(n) + CommonConstants.STATS_BIT_LENGTH;
        assert l >= minL : "l must be greater than or equal to " + minL + ": " + l;
        this.l = l;
        byteL = CommonUtils.getByteLength(l);
        gf2ePoly = Gf2ePolyFactory.createInstance(envType, l);
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
    public byte[][] encode(Map<ByteBuffer, byte[]> keyValueMap) {
        assert keyValueMap.size() <= n
            : "# of key-value pairs must be less than or equal to " + n + ": " + keyValueMap.size();
        // here we even allow 0 key-value pairs.
        byte[][] xArray = keyValueMap.keySet().stream()
            .map(ByteBuffer::array)
            .peek(x -> {
                assert BytesUtils.isFixedReduceByteArray(x, byteL, l);
            })
            .toArray(byte[][]::new);
        byte[][] yArray = keyValueMap.keySet().stream()
            .map(keyValueMap::get)
            .peek(y -> {
                assert BytesUtils.isFixedReduceByteArray(y, byteL, l);
            })
            .toArray(byte[][]::new);
        // we use dummy interpolate technique to obtain n points interpolation.
        if (n == 1) {
            // if we only allow one key-value pair, we need to return a polynomial with one dummy point.
            return gf2ePoly.interpolate(2, xArray, yArray);
        } else {
            return gf2ePoly.interpolate(n, xArray, yArray);
        }
    }

    @Override
    public byte[] decode(byte[][] storage, ByteBuffer key) {
        // We do not need to verify byte length for each storage, which runs in O(n). We only verify storage.length
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
    public int getByteL() {
        return byteL;
    }

    @Override
    public int getM() {
        // m equals the number of coefficients for the interpolated polynomial.
        return gf2ePoly.coefficientNum(Math.max(2, n));
    }

    @Override
    public int getNegLogFailureProbability() {
        return Integer.MAX_VALUE;
    }

    @Override
    public BasicOkvsFactory.BasicOkvsType getType() {
        return BasicOkvsFactory.BasicOkvsType.POLYNOMIAL;
    }
}
