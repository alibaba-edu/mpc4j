package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * PIANO client-specific preprocessing PIR utilities.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoSingleCpPirUtils {
    /**
     * private constructor.
     */
    private PianoSingleCpPirUtils() {
        // empty
    }

    /**
     * Gets chunk size.
     *
     * @param n database size.
     * @return chunk size.
     */
    public static int getChunkSize(int n) {
        MathPreconditions.checkPositive("n", n);
        // ChunkSize is 2√n and round up to the next power of 2.
        // Here we use a more generalized ChunkSize, and we require ChunkSize must be greater than 1
        return Math.max((int) Math.ceil(Math.sqrt(n)), 2);
    }

    /**
     * Gets chunk num (the same as set size).
     *
     * @param n database size.
     * @return chunk num.
     */
    public static int getChunkNum(int n) {
        int chunkSize = getChunkSize(n);
        // ChunkNum is n / ChunkSize and round up to the next multiple of 4. Here we use a more generated ChunkNum.
        // Note that ChunkNum must be greater than 1, otherwise server cannot respond the query.
        return (int) Math.max(Math.ceil((double) n / chunkSize), 2);
    }

    /**
     * Gets query num for each preprocessing round.
     *
     * @param n database size.
     * @return query num for each preprocessing round.
     */
    public static int getRoundQueryNum(int n) {
        MathPreconditions.checkPositive("n", n);
        // when n = 1, log(1) = 0, so we would have an invalid parameter. Here we manually let log(n) = 1 for n = 1.
        if (n == 1) {
            return (int) Math.floor(Math.sqrt(n));
        } else {
            // for other cases, we just support Q=(√n) * ln(n) queries.
            return (int) Math.floor(Math.sqrt(n) * Math.log(n));
        }
    }

    /**
     * Gets M1, the total number of primary hints.
     *
     * @param n database size.
     * @return M1.
     */
    public static int getM1(int n) {
        // For any query and any hint, the hint contains the query with prob 1 / ChunkSize.
        // If we have k*ChunkSize hints, the failure probability is less than (1/ChunkSize)^(k*ChunkSize) <= (1/e)^k.
        // We have Q queries, so we need Q * e^(-k) <= 2^(-σ). Therefore, k = ln(2)*σ + ln(Q)
        // original code sets σ = 40 + 1
        int q = getRoundQueryNum(n);
        int k = (int) Math.ceil(Math.log(2) * (CommonConstants.STATS_BIT_LENGTH + 1) + Math.log(q));
        int chunkSize = getChunkSize(n);
        return k * chunkSize;
    }

    /**
     * Gets M2 (per group), the number of backup hints for each Chunk ID.
     *
     * @param n database size.
     * @return M2.
     */
    public static int getM2PerGroup(int n) {
        // M2 = 3 * Q / ChunkNum
        int q = getRoundQueryNum(n);
        int chunkNum = getChunkNum(n);
        return (int) Math.ceil(3.0 * q / chunkNum);
    }
}
