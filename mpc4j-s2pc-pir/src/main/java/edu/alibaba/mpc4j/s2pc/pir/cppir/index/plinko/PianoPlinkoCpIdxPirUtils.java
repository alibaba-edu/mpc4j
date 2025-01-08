package edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

/**
 * Piano-based Plinko client-specific preprocessing index PIR utilities.
 *
 * @author Weiran Liu
 * @date 2024/10/9
 */
public class PianoPlinkoCpIdxPirUtils {
    /**
     * private constructor.
     */
    private PianoPlinkoCpIdxPirUtils() {
        // empty
    }

    /**
     * Gets block size, which is chunk size in Piano. Recall that given database index i, Plinko needs to parse it as
     * (α, β) = (i / w, i mod w), where w can be any parameters. We require α ∈ [0, BlockNum) and β ∈ [0, BlockSize).
     * In Section 5.2, Plinko papers states that
     * <p>
     * Plinko is parameterized with respect to a security parameter λ, a block size w, and a number of supported queries
     * before refresh q. We view w and q as parameters which influence the amount of storage required by the client.
     * In prior work, these parameters were fixed around √n for simplicity and asymptotic optimums, but we present Plinko
     * without fixing these, since it highlights where we differ from prior work and how we achieve an optimal trade-off
     * between client storage and query time.
     * </p>
     * Here we follow the original choice by Piano, setting it as √n while making sure that it is greater than 1.
     *
     * @param n database size.
     * @return block num.
     */
    public static int getBlockSize(int n) {
        MathPreconditions.checkPositive("n", n);
        // We require BlockSize must be greater than 1
        return Math.max((int) Math.ceil(Math.sqrt(n)), 2);
    }

    /**
     * Gets block num, which is chunk num in Piano. Recall that given database index i, Plinko needs to parse it as
     * (α, β) = (i / w, i mod w), where w can be any parameters. We require α ∈ [0, BlockNum) and β ∈ [0, BlockSize).
     * In Section 5.2, Plinko papers states that
     * <p>
     * Plinko is parameterized with respect to a security parameter λ, a block size w, and a number of supported queries
     * before refresh q. We view w and q as parameters which influence the amount of storage required by the client.
     * In prior work, these parameters were fixed around √n for simplicity and asymptotic optimums, but we present Plinko
     * without fixing these, since it highlights where we differ from prior work and how we achieve an optimal trade-off
     * between client storage and query time.
     * </p>
     * Here we follow the original choice by Piano, setting it as n / BlockSize while making sure that it is greater than 1.
     *
     * @param n database size.
     * @return block size.
     */
    public static int getBlockNum(int n) {
        int blockNum = getBlockSize(n);
        // BlockNum must be greater than 1, otherwise server cannot respond the query.
        return (int) Math.max(Math.ceil((double) n / blockNum), 2);
    }

    /**
     * Gets query num for each preprocessing round. Here we follow the original choice by Piano.
     *
     * @param n database size.
     * @return query num for each preprocessing round.
     */
    public static int getRoundQueryNum(int n) {
        MathPreconditions.checkPositive("n", n);
        // when n = 1, log(1) = 0, so we would have an invalid parameter. Here we manually let log(n) = 1 for n = 1.
        if (n == 1) {
            return (int) Math.ceil(Math.sqrt(n));
        } else {
            // for other cases, we just support Q=(√n) * ln(n) queries.
            return (int) Math.ceil(Math.sqrt(n) * Math.log(n));
        }
    }

    /**
     * Gets number of primary hints.
     *
     * @param n database size.
     * @return number of primary hints.
     */
    static int getM1(int n) {
        return CommonConstants.STATS_BIT_LENGTH * getBlockSize(n);
    }

    /**
     * Gets default number of backup hints.
     *
     * @param n database size.
     * @return number of backup hints.
     */
    static int getDefaultM2(int n) {
        return 3 * getRoundQueryNum(n);
    }

    /**
     * Gets specific number of backup hints.
     *
     * @param n database size.
     * @param q specific number of queries in each round.
     * @return specific number of backup hints.
     */
    static int getSpecificM2(int n, int q) {
        MathPreconditions.checkPositive("q", q);
        int defaultQ = getRoundQueryNum(n);
        if (defaultQ > q) {
            return getDefaultM2(n);
        } else {
            return CommonConstants.STATS_BIT_LENGTH * q;
        }
    }
}
