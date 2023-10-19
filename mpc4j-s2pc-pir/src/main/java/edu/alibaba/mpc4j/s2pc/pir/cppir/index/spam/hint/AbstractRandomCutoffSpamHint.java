package edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.hint;

import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleCpPirUtils;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * abstract hint for SPAM with randomly generated cutoff.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public abstract class AbstractRandomCutoffSpamHint extends AbstractSpamHint {
    /**
     * 1/2 - 1/16
     */
    private static final double CUTOFF_LOWER_BOUND = 1.0 / 2 - 1.0 / 16;
    /**
     * 1/2 + 1/16
     */
    private static final double CUTOFF_UPPER_BOUND = 1.0 / 2 + 1.0 / 16;
    /**
     * we use optimization when n >= 2^18
     */
    private static final int CUTOFF_CHUNK_NUM = SpamSingleCpPirUtils.getChunkNum(1 << 18);
    /**
     * the cutoff ^v
     */
    protected final double cutoff;
    /**
     * vs
     */
    protected double[] vs;

    protected AbstractRandomCutoffSpamHint(int chunkSize, int chunkNum, int l, SecureRandom secureRandom) {
        super(chunkSize, chunkNum, l);
        // sample ^v
        boolean success = false;
        double tryCutoff = 0L;
        while (!success) {
            secureRandom.nextBytes(hintId);
            // compute V = [v_0, v_1, ..., v_{ChunkNum}]
            vs = IntStream.range(0, chunkNum)
                .mapToDouble(this::getDouble)
                .toArray();
            // we need all v in vs are distinct
            long count = Arrays.stream(vs).distinct().count();
            if (count == vs.length) {
                // all v in vs are distinct, find the median
                double[] copy = Arrays.copyOf(vs, vs.length);
                if (vs.length <= CUTOFF_CHUNK_NUM) {
                    // copy and sort
                    Arrays.sort(copy);
                    double left = copy[chunkNum / 2 - 1];
                    double right = copy[chunkNum / 2];
                    // divide then add, otherwise we may meet overflow
                    tryCutoff = left / 2 + right / 2;
                } else {
                    // We think of the random values as numbers between 0 and 1, choose two filtering bounds as 1/2 ± 1/16
                    int smallFilterCount = (int) Arrays.stream(copy).filter(v -> v < CUTOFF_LOWER_BOUND).count();
                    int largeFilterCount = (int) Arrays.stream(copy).filter(v -> v > CUTOFF_UPPER_BOUND).count();
                    if (smallFilterCount >= chunkNum / 2 - 1 || largeFilterCount >= chunkNum / 2 - 1) {
                        continue;
                    }
                    double[] filterVs = Arrays.stream(copy)
                        .filter(v -> v >= CUTOFF_LOWER_BOUND && v <= CUTOFF_UPPER_BOUND)
                        .toArray();
                    Arrays.sort(filterVs);
                    double left = filterVs[chunkNum / 2 - 1 - smallFilterCount];
                    double right = filterVs[chunkNum / 2 - smallFilterCount];
                    // divide then add, otherwise we may meet overflow
                    tryCutoff = left / 2 + right / 2;
                }
                success = true;
            }
        }
        cutoff = tryCutoff;
    }

    @Override
    protected double getCutoff() {
        return cutoff;
    }
}
