package edu.alibaba.mpc4j.s2pc.pcg;

/**
 * batched PCG output.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public interface BatchPcgOutput extends PcgPartyOutput {
    /**
     * Gets batch num.
     *
     * @return batch num.
     */
    int getBatchNum();

    /**
     * Gets num, which is equal to batch num.
     *
     * @return num.
     */
    @Override
    default int getNum() {
        return getBatchNum();
    }
}
