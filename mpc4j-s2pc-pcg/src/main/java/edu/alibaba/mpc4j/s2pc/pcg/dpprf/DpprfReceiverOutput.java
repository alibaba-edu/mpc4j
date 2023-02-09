package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * DPPRF receiver output.
 *
 * @author Weiran Liu
 * @date 2022/8/16
 */
public class DpprfReceiverOutput {
    /**
     * α upper bound
     */
    private final int alphaBound;
    /**
     * α bit length
     */
    private final int h;
    /**
     * batched PRF output groups, each output group contains α-bound PRF key, the α-th PRF key is null
     */
    private final byte[][][] pprfArrays;
    /**
     * α array
     */
    private final int[] alphaArray;
    /**
     * batch num
     */
    private final int batchNum;

    public DpprfReceiverOutput(int alphaBound, int[] alphaArray, byte[][][] pprfArrays) {
        batchNum = alphaArray.length;
        assert batchNum > 0 : "batch num must be greater than 0: " + batchNum;
        assert pprfArrays.length == batchNum : "# of PPRF keys must be equal to " + batchNum + ": " + pprfArrays.length;
        assert alphaBound > 0 : "α upper bound must be greater than 0: " + alphaBound;
        this.alphaBound = alphaBound;
        h = LongUtils.ceilLog2(alphaBound);
        this.alphaArray = Arrays.stream(alphaArray)
            .peek(alpha -> {
                assert alpha >= 0 && alpha < alphaBound : "α must be in range [0," + alphaBound + "): " + alpha;
            })
            .toArray();
        this.pprfArrays = IntStream.range(0, batchNum)
            .mapToObj(batchIndex -> {
                byte[][] pprfKeys = pprfArrays[batchIndex];
                assert pprfKeys.length == alphaBound : "# of PPRF keys must be " + alphaBound + ": " + pprfKeys.length;
                for (int index = 0; index < alphaBound; index++) {
                    if (index == alphaArray[batchIndex]) {
                        assert pprfKeys[index] == null : "the " + index + "-th key must be null";
                    } else {
                        assert pprfKeys[index].length == CommonConstants.BLOCK_BYTE_LENGTH
                            : "key byte length must be equal to " + CommonConstants.BLOCK_BYTE_LENGTH + ": " + pprfKeys[index].length;
                    }
                }
                return pprfKeys;
            })
            .toArray(byte[][][]::new);
    }

    /**
     * Get α at the given batch index.
     *
     * @param batchIndex the batch index.
     * @return α.
     */
    public int getAlpha(int batchIndex) {
        return alphaArray[batchIndex];
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
     * Get batch num.
     *
     * @return batch num.
     */
    public int getBatchNum() {
        return batchNum;
    }

    /**
     * Get PPRF keys at the given batch index. The PPRF keys contains α-bound PRF keys, the α-th PRF key is null.
     *
     * @param batchIndex the batch index.
     * @return PPRF keys.
     */
    public byte[][] getPprfs(int batchIndex) {
        return pprfArrays[batchIndex];
    }
}
