package edu.alibaba.mpc4j.s2pc.pcg.dpprf.sp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.PcgPartyOutput;

import java.util.Arrays;

/**
 * single-point DPPRF sender output.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public class SpDpprfSenderOutput implements PcgPartyOutput {
    /**
     * α upper bound
     */
    private final int alphaBound;
    /**
     * α bit length
     */
    private final int h;
    /**
     * PRF outputs containing α-bound PRF keys
     */
    private final byte[][] prfKeys;

    public SpDpprfSenderOutput(int alphaBound, byte[][] prfKeys) {
        assert alphaBound > 0 : "α upper bound must be greater than 0: " + alphaBound;
        this.alphaBound = alphaBound;
        h = LongUtils.ceilLog2(alphaBound);
        assert prfKeys.length == alphaBound : "# of PRF keys must be " + alphaBound + ": " + prfKeys.length;
        this.prfKeys = Arrays.stream(prfKeys)
            .peek(prfKey -> {
                assert prfKey.length == CommonConstants.BLOCK_BYTE_LENGTH
                    : "key byte length must be equal to " + CommonConstants.BLOCK_BYTE_LENGTH + ": " + prfKey.length;
            })
            .toArray(byte[][]::new);
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
     * Get the PRF keys.
     *
     * @return PRF keys.
     */
    public byte[][] getPrfKeys() {
        return prfKeys;
    }

    @Override
    public int getNum() {
        return 1;
    }
}
