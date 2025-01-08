package edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * MIR-based Plinko client-preprocessing index PIR utilities.
 *
 * @author Weiran Liu
 * @date 2024/10/11
 */
public class MirPlinkoCpIdxPirUtils {
    /**
     * private constructor.
     */
    private MirPlinkoCpIdxPirUtils() {
        // empty
    }

    /**
     * Gets block num, which is chunk num in MIR. Recall that given database index i, Plinko needs to parse it as
     * (α, β) = (i / w, i mod w), where w can be any parameters. We require α ∈ [0, BlockNum) and β ∈ [0, BlockSize).
     * In Section 5.2, Plinko papers states that
     * <p>
     * Plinko is parameterized with respect to a security parameter λ, a block size w, and a number of supported queries
     * before refresh q. We view w and q as parameters which influence the amount of storage required by the client.
     * In prior work, these parameters were fixed around √n for simplicity and asymptotic optimums, but we present Plinko
     * without fixing these, since it highlights where we differ from prior work and how we achieve an optimal trade-off
     * between client storage and query time.
     * </p>
     * MIR additionally requires that chunk num must be even, since hint requires (√n / 2) + 1 chunks. Therefore, here
     * we also need an even block num .
     *
     * @param n database size.
     * @return block num.
     */
    public static int getBlockNum(int n) {
        MathPreconditions.checkPositive("n", n);
        // BlockNum must be greater than 1 and an even number, otherwise server cannot respond the query.
        int blockNum = (int) Math.ceil(Math.sqrt(n));
        if (blockNum % 2 == 1) {
            blockNum++;
        }
        return Math.max(blockNum, 2);
    }

    /**
     * Gets block size, which is chunk size in MIR. Recall that given database index i, Plinko needs to parse it as
     * (α, β) = (i / w, i mod w), where w can be any parameters. We require α ∈ [0, BlockNum) and β ∈ [0, BlockSize).
     * In Section 5.2, Plinko papers states that
     * <p>
     * Plinko is parameterized with respect to a security parameter λ, a block size w, and a number of supported queries
     * before refresh q. We view w and q as parameters which influence the amount of storage required by the client.
     * In prior work, these parameters were fixed around √n for simplicity and asymptotic optimums, but we present Plinko
     * without fixing these, since it highlights where we differ from prior work and how we achieve an optimal trade-off
     * between client storage and query time.
     * </p>
     * Here we follow the original choice by NUR, setting it as n / BlockNum while making sure that it is greater than 1.
     *
     * @param n database size.
     * @return block size.
     */
    public static int getBlockSize(int n) {
        // BlockSize is n / BlockNum and also must be greater than 1.
        int blockNum = getBlockNum(n);
        return (int) Math.max(Math.ceil((double) n / blockNum), 2);
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
     * Gets default M2, the default total number of backup hints.
     *
     * @param n database size.
     * @return default M2.
     */
    public static int getDefaultM2(int n) {
        MathPreconditions.checkPositive("n", n);
        // the client retrieves 0.5 * λ * √N backup hints
        return (int) Math.floor(0.5 * Math.sqrt(n) * (CommonConstants.STATS_BIT_LENGTH + 1));
    }

    /**
     * Gets specific M2, the specific total number of backup hints.
     *
     * @param q specific number of queries in each round.
     * @return specific M2.
     */
    public static int getSpecificM2(int n, int q) {
        MathPreconditions.checkPositive("q", q);
        int defaultQ = getRoundQueryNum(n);
        if (defaultQ > q) {
            return getDefaultM2(n);
        } else {
            return (int) Math.ceil(1.2 * CommonConstants.STATS_BIT_LENGTH * q);
        }
    }
}
