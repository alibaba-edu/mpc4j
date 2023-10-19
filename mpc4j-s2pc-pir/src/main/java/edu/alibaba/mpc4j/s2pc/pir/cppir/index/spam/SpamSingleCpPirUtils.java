package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * SPAM client-specific preprocessing PIR utilities.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public class SpamSingleCpPirUtils {
    /**
     * private constructor.
     */
    private SpamSingleCpPirUtils() {
        // empty
    }

    /**
     * Gets chunk num (the same as set size).
     *
     * @param n database size.
     * @return chunk num.
     */
    public static int getChunkNum(int n) {
        MathPreconditions.checkPositive("n", n);
        // ChunkNum must be greater than 1 and an even number, otherwise server cannot respond the query.
        int chunkNum = (int) Math.ceil(Math.sqrt(n));
        if (chunkNum % 2 == 1) {
            chunkNum++;
        }
        return Math.max(chunkNum, 2);
    }

    /**
     * Gets chunk size.
     *
     * @param n database size.
     * @return chunk size.
     */
    public static int getChunkSize(int n) {
        // ChunkSize is n / ChunkNum and also must be greater than 1.
        int chunkNum = getChunkNum(n);
        return (int) Math.max(Math.ceil((double) n / chunkNum), 2);
    }

    /**
     * Gets query num for each preprocessing round.
     *
     * @param n database size.
     * @return query num for each preprocessing round.
     */
    public static int getRoundQueryNum(int n) {
        MathPreconditions.checkPositive("n", n);
        // Q = 0.5 * λ * √N, but the client can make chose to, but fewer than, 0.5 * λ * √N (say 0.4 * λ * √N)
        // online queries before having to run the offline phase again.
        return (int) Math.floor(0.4 * Math.sqrt(n) * (CommonConstants.STATS_BIT_LENGTH + 1));
    }

    /**
     * Gets M1, the total number of primary hints.
     *
     * @param n database size.
     * @return M1.
     */
    public static int getM1(int n) {
        MathPreconditions.checkPositive("n", n);
        // the client retrieves λ * √N primary hints
        return (int) Math.ceil(Math.sqrt(n) * (CommonConstants.STATS_BIT_LENGTH + 1));
    }

    /**
     * Gets M2, the total number of backup hints.
     *
     * @param n database size.
     * @return M2.
     */
    public static int getM2(int n) {
        MathPreconditions.checkPositive("n", n);
        // the client retrieves 0.5 * λ * √N backup hints
        return (int) Math.floor(0.5 * Math.sqrt(n) * (CommonConstants.STATS_BIT_LENGTH + 1));
    }
}
