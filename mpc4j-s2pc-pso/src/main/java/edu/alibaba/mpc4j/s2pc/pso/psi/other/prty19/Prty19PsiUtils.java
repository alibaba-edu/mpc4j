package edu.alibaba.mpc4j.s2pc.pso.psi.other.prty19;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * PRTY19-PSI utilities.
 *
 * @author Weiran Liu
 * @date 2023/9/7
 */
class Prty19PsiUtils {
    /**
     * private constructor.
     */
    private Prty19PsiUtils() {
        // empty
    }

    /**
     * log(n) → l, see Figure 6 of the paper.
     */
    private static final TIntIntMap L_INIT_MATRIX = new TIntIntHashMap();

    static {
        L_INIT_MATRIX.put(8, 412);
        L_INIT_MATRIX.put(9, 414);
        L_INIT_MATRIX.put(10, 416);
        L_INIT_MATRIX.put(11, 418);
        L_INIT_MATRIX.put(12, 420);
        L_INIT_MATRIX.put(13, 422);
        L_INIT_MATRIX.put(14, 424);
        L_INIT_MATRIX.put(15, 426);
        L_INIT_MATRIX.put(16, 428);
        L_INIT_MATRIX.put(17, 430);
        L_INIT_MATRIX.put(18, 432);
        L_INIT_MATRIX.put(19, 434);
        L_INIT_MATRIX.put(20, 436);
        L_INIT_MATRIX.put(21, 438);
        L_INIT_MATRIX.put(22, 440);
        L_INIT_MATRIX.put(23, 442);
        L_INIT_MATRIX.put(24, 444);
    }

    /**
     * Gets l (for low communication version). See Figure 6 of the paper.
     *
     * @param maxBatchSize max batch size.
     * @return l.
     */
    public static int getLowL(int maxBatchSize) {
        MathPreconditions.checkPositive("max_batch_size", maxBatchSize);
        // min support n = 2^8
        int nLogValue = LongUtils.ceilLog2(Math.max(maxBatchSize, 1 << 8));
        if (L_INIT_MATRIX.containsKey(nLogValue)) {
            return L_INIT_MATRIX.get(nLogValue);
        }
        throw new IllegalArgumentException("max_batch_size (" + maxBatchSize + ") exceeds " + (1 << 24));
    }

    /**
     * Gets l (for fast version). In section 4.3.1, the paper states that:
     * <p>
     * With this new optimization, she accesses twice as many rows (rows x || 1 and x || 2 for every x ∈ X). This leads
     * to a slight increase in l. For the concrete parameters we consider (see Figure 6), l must increase by only 2 bits.
     * </p>
     *
     * @param maxBatchSize max batch size.
     * @return l.
     */
    public static int getFastL(int maxBatchSize) {
        MathPreconditions.checkPositive("max_batch_size", maxBatchSize);
        // min support n = 2^8
        int nLogValue = LongUtils.ceilLog2(Math.max(maxBatchSize, 1 << 8));
        if (L_INIT_MATRIX.containsKey(nLogValue)) {
            return L_INIT_MATRIX.get(nLogValue) + 2;
        }
        throw new IllegalArgumentException("max_batch_size (" + maxBatchSize + ") exceeds " + (1 << 24));
    }
}
