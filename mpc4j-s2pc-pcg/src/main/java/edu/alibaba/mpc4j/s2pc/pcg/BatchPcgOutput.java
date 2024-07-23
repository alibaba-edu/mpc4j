package edu.alibaba.mpc4j.s2pc.pcg;

/**
 * batched PCG output.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public interface BatchPcgOutput {
    /**
     * Gets batch num.
     *
     * @return batch num.
     */
    int getBatchNum();

    /**
     * Gets each num.
     *
     * @return each num.
     */
    int getEachNum();

    /**
     * Gets the output.
     *
     * @param index index.
     * @return the output.
     */
    PcgPartyOutput get(int index);
}
