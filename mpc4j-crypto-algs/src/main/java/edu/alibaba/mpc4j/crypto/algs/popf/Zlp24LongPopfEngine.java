package edu.alibaba.mpc4j.crypto.algs.popf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.algs.restriction.LongRestriction;
import edu.alibaba.mpc4j.crypto.algs.utils.distribution.Coins;
import edu.alibaba.mpc4j.crypto.algs.utils.distribution.Hgd;
import edu.alibaba.mpc4j.crypto.algs.utils.distribution.HgdFactory;
import edu.alibaba.mpc4j.crypto.algs.utils.distribution.HgdFactory.HgdType;
import edu.alibaba.mpc4j.crypto.algs.utils.range.LongRange;
import org.bouncycastle.crypto.CryptoException;

import java.security.SecureRandom;

/**
 * Partial Order Preserving Function (POPF) implemented using long.
 *
 * @author Liqiang Peng
 * @date 2024/5/10
 */
public class Zlp24LongPopfEngine {
    /**
     * HGD
     */
    private final Hgd hgd;
    /**
     * key
     */
    private byte[] key;
    /**
     * inner input range
     */
    private LongRange innerInputRange;
    /**
     * inner output range: R ∪ {max(R) + 1, ..., max(R) + |D| - 1}.
     */
    private LongRange innerOutputRange;
    /**
     * initialized
     */
    private boolean initialized;
    /**
     * restricted function
     */
    private LongRestriction restriction;

    /**
     * Creates a new POPF engine.
     */
    public Zlp24LongPopfEngine() {
        this(HgdType.FAST);
    }

    /**
     * Creates a new POPF engine.
     *
     * @param hgdType HGD type.
     */
    public Zlp24LongPopfEngine(HgdType hgdType) {
        hgd = HgdFactory.getInstance(hgdType);
        initialized = false;
    }

    /**
     * Generates a key.
     *
     * @param secureRandom the random state.
     * @return key.
     */
    public byte[] keyGen(SecureRandom secureRandom) {
        return BlockUtils.randomBlock(secureRandom);
    }

    /**
     * Initializes the POPF engine.
     *
     * @param key         key.
     * @param restriction restricted function.
     */
    public void init(byte[] key, LongRestriction restriction) {
        innerInputRange = restriction.getInputRange();
        long inputSize = innerInputRange.size();
        LongRange outputRange = restriction.getOutputRange();
        // R ∪ {max(R) + 1, ..., max(R) + |D| - 1}
        innerOutputRange = new LongRange(outputRange.getStart(), outputRange.getEnd() + inputSize - 1);
        Preconditions.checkArgument(BlockUtils.valid(key));
        this.key = BlockUtils.clone(key);
        this.restriction = restriction;
        initialized = true;
    }

    /**
     * Evaluates Partial Order-Preserving Function.
     *
     * @param input input.
     * @return output.
     */
    public long popf(long input) throws CryptoException {
        Preconditions.checkArgument(initialized, getClass().getSimpleName() + " is not initialized");
        Preconditions.checkArgument(
            innerInputRange.contains(input),
            "Input is not within the input range " + innerInputRange + ": " + input
        );
        return lazySample(input, innerInputRange, innerOutputRange) - input + innerInputRange.getStart();
    }

    private long lazySample(long m, LongRange rangeD, LongRange rangeR) throws CryptoException {
        // LazySample(D, R, m)
        // M ← |D| ; N ← |R|
        long dSize = rangeD.size();
        long rSize = rangeR.size();
        // M <= N, otherwise there must exist a plaintext that does not have its corresponding ciphertext
        if (dSize > rSize) {
            throw new CryptoException("rangeD (" + rangeD + ") is larger than rangeR (" + rangeR + ")");
        }

        // d ← min(D) − 1 ; r ← min(R) − 1
        long d = rangeD.getStart() - 1;
        long r = rangeR.getStart() - 1;
        // If |D| = 1
        if (dSize == 1) {
            long uniformX = rangeD.getStart();
            boolean uniformSuccess = false;
            long uniformY = 0L;
            long uniformCounter = 0L;
            while (!uniformSuccess) {
                uniformCounter++;
                // cc ← GetCoins(D, R, 1 || m), where D = 1
                Coins cc = new Coins(key, LongUtils.longToByteArray((m << 1) + 1), uniformCounter);
                // F[D, R, m] ← R
                uniformY = sampleUniform(rangeR, cc);
                // g(m, F[D, R, m] + m - 1) = 1
                uniformSuccess = restriction.restriction(uniformX, uniformY - uniformX + innerOutputRange.getStart());
            }
            // return F[D, R, m]
            return uniformY;
        }
        // If |D| > 1, sample HGD
        // y ← r + ⌈N/2⌉
        long halfN = (long) Math.ceil((double) rSize / 2.0);
        long y = r + halfN;
        boolean hgdSuccess = false;
        long x = 0L;
        long hgdCounter = 0L;
        while (!hgdSuccess) {
            hgdCounter++;
            // cc ← GetCoins(D, R, 0 || y)
            Coins cc = new Coins(key, LongUtils.longToByteArray(y << 1), hgdCounter);
            // I[D, R, y] ← HG(M, N, y − r; cc); x ← d + I[D, R, y]
            x = sampleHgd(rangeD, rangeR, y, cc);
            // g(m, F[D, R, m] + m - 1) = 1
            hgdSuccess = restriction.restriction(x, y - x + innerOutputRange.getStart());
        }
        // If m ≤ x
        if (m <= x) {
            // D ← {d + 1, ..., x}
            rangeD = new LongRange(d + 1, x);
            // R ← {r + 1, ..., y}
            rangeR = new LongRange(r + 1, y);
        } else {
            // D ← {x + 1, ..., d + M}
            rangeD = new LongRange(x + 1, d + dSize);
            // R ← {y + 1, ..., r + N}
            rangeR = new LongRange(y + 1, r + rSize);
        }
        // Return LazySample(D, R, m)
        return lazySample(m, rangeD, rangeR);
    }

    private long sampleUniform(LongRange inRange, Coins coins) {
        LongRange curRange = new LongRange(inRange);
        assert curRange.size() != 0;
        // sample using binary search
        while (curRange.size() > 1) {
            // we use shift right instead of "/ 2" to avoid bugs for negative numbers.
            long mid = ((curRange.getStart() + curRange.getEnd()) >> 1);
            boolean bit = coins.next();
            if (!bit) {
                curRange.setEnd(mid);
            } else {
                curRange.setStart(mid + 1);
            }
        }

        assert curRange.size() != 0;

        return curRange.getStart();
    }

    /**
     * Samples HG(M, N, y - r; cc), where M is the plaintext space, N is the ciphertext space, y - r is the mid.
     *
     * @param rangeM plaintext space.
     * @param rangeN ciphertext space.
     * @param sample mid.
     * @param coins  random coin.
     * @return sample result.
     */
    private long sampleHgd(LongRange rangeM, LongRange rangeN, long sample, Coins coins) {
        long mSize = rangeM.size();
        long nSize = rangeN.size();

        assert mSize > 0 && nSize > 0;
        assert mSize <= nSize;
        assert rangeN.contains(sample);

        // k = n - r
        long k = sample - rangeN.getStart() + 1;

        // input size == output size, one-to-one map.
        if (mSize == nSize) {
            return rangeM.getStart() + k - 1;
        }

        long r = hgd.sample(k, mSize, nSize - mSize, coins);

        if (r == 0) {
            return rangeM.getStart();
        } else if (r == mSize) {
            return rangeM.getEnd();
        } else {
            // x ← min(D) − 1 + I[D, R, y]
            return rangeM.getStart() + r - 1;
        }
    }
}
