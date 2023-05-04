package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.util.stream.IntStream;

/**
 * single-point DPPRF receiver output.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class SpDpprfReceiverOutput implements PcgPartyOutput {
    /**
     * α upper bound
     */
    private final int alphaBound;
    /**
     * α bit length
     */
    private final int h;
    /**
     * PPRF output keys, containing α-bound PRF keys, the α-th PRF key is null
     */
    private final byte[][] pprfKeys;
    /**
     * α
     */
    private final int alpha;

    public SpDpprfReceiverOutput(int alphaBound, int alpha, byte[][] pprfKeys) {
        assert alphaBound > 0 : "α upper bound must be greater than 0: " + alphaBound;
        this.alphaBound = alphaBound;
        h = LongUtils.ceilLog2(alphaBound);
        assert alpha >= 0 && alpha < alphaBound : "α must be in range [0," + alphaBound + "): " + alpha;
        this.alpha = alpha;
        assert pprfKeys.length == alphaBound : "# of PPRF keys must be " + alphaBound + ": " + pprfKeys.length;
        this.pprfKeys = IntStream.range(0, alphaBound)
            .mapToObj(index -> {
                byte[] prfKey = pprfKeys[index];
                if (index == alpha) {
                    assert prfKey == null : "the " + index + "-th key must be null";
                } else {
                    assert prfKey.length == CommonConstants.BLOCK_BYTE_LENGTH
                        : "key byte length must be equal to " + CommonConstants.BLOCK_BYTE_LENGTH + ": " + prfKey.length;
                }
                return prfKey;
            })
            .toArray(byte[][]::new);
    }

    /**
     * Get α.
     *
     * @return α.
     */
    public int getAlpha() {
        return alpha;
    }

    /**
     * Get α bit length.
     *
     * @return α bit length.
     */
    public int getH() {
        return h;
    }

    /**
     * Get α upper bound.
     *
     * @return α upper bound.
     */
    public int getAlphaBound() {
        return alphaBound;
    }

    /**
     * Get the PPRF keys that contains α-bound PRF keys and the α-th PRF key is null.
     *
     * @return PPRF keys.
     */
    public byte[][] getPprfKeys() {
        return pprfKeys;
    }

    @Override
    public int getNum() {
        return 1;
    }
}
