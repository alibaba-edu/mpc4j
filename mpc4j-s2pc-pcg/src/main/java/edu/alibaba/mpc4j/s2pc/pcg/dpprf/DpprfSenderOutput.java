package edu.alibaba.mpc4j.s2pc.pcg.dpprf;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;

/**
 * Distributed Puncturable PRF (DPPRF) sender output interface.
 *
 * @author Weiran Liu
 * @date 2022/12/21
 */
public class DpprfSenderOutput {
    /**
     * α upper bound
     */
    private final int alphaBound;
    /**
     * α bit length
     */
    private final int h;
    /**
     * batched PRF output groups, each output group contains α-bound PRF key
     */
    private final byte[][][] prfArrays;
    /**
     * batch num
     */
    private final int batchNum;

    public DpprfSenderOutput(int alphaBound, byte[][][] prfArrays) {
        batchNum = prfArrays.length;
        assert batchNum > 0 : "batch num must be greater than 0: " + batchNum;
        assert alphaBound > 0 : "α upper bound must be greater than 0: " + alphaBound;
        this.alphaBound = alphaBound;
        h = LongUtils.ceilLog2(alphaBound);
        this.prfArrays = Arrays.stream(prfArrays)
            .peek(prfKeys -> {
                assert prfKeys.length == alphaBound : "# of PRF keys must be " + alphaBound + ": " + prfKeys.length;
                Arrays.stream(prfKeys).forEach(prfKey -> {
                    assert prfKey.length == CommonConstants.BLOCK_BYTE_LENGTH
                        : "key byte length must be equal to " + CommonConstants.BLOCK_BYTE_LENGTH + ": " + prfKey.length;
                });
            })
            .toArray(byte[][][]::new);
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
     * Get PRF keys at the given batch index.
     *
     * @param batchIndex the batch index.
     * @return PRF keys.
     */
    public byte[][] getPrfs(int batchIndex) {
        return prfArrays[batchIndex];
    }
}
