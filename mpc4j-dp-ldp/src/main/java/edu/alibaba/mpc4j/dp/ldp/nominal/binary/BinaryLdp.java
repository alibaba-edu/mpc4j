package edu.alibaba.mpc4j.dp.ldp.nominal.binary;

import edu.alibaba.mpc4j.dp.ldp.nominal.NominalLdp;

/**
 * Binary LDP mechanism.
 *
 * @author Weiran Liu
 * @date 2024/4/26
 */
public interface BinaryLdp extends NominalLdp {
    /**
     * Randomizes the input.
     *
     * @param value value.
     * @return randomized value.
     */
    boolean randomize(boolean value);
}
