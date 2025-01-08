package edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.hint;

import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.MirCpIdxPirUtils;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * abstract hint for MIR with randomly generated cutoff.
 *
 * @author Weiran Liu
 * @date 2023/8/31
 */
public abstract class AbstractRandomCutoffMirHint extends AbstractMirHint {
    /**
     * 1/16 Upper
     */
    private static final int CUTOFF_LOWER_BOUND = -(1 << 28);
    /**
     * 1/16 Lower
     */
    private static final int CUTOFF_UPPER_BOUND = 1 << 28;

    /**
     * we use optimization when n >= 2^18
     */
    private static final int CUTOFF_CHUNK_NUM = MirCpIdxPirUtils.getChunkNum(1 << 18);
    /**
     * the cutoff ^v
     */
    protected final int cutoff;
    /**
     * vs, which will be cleaned by subclasses.
     */
    protected int[] vs;

    protected AbstractRandomCutoffMirHint(FixedKeyPrp fixedKeyPrp,
                                          int chunkSize, int chunkNum, int l, SecureRandom secureRandom) {
        super(fixedKeyPrp, chunkSize, chunkNum, l);
        // sample ^v
        boolean success = false;
        int tryCutoff = Integer.MAX_VALUE;
        while (!success) {
            secureRandom.nextBytes(hintId);
            // compute V = [v_0, v_1, ..., v_{ChunkNum}]
            vs = getIntegersForMedian();

            // all v in vs are distinct, find the median
            int[] copy = Arrays.copyOf(vs, vs.length);
            int checkLength = chunkNum;
            if (vs.length <= CUTOFF_CHUNK_NUM) {
                tryCutoff = IntQuickSelect.quickSelect(copy, chunkNum / 2 + 1);
                copy[chunkNum / 2] = Integer.MAX_VALUE;
            }else{
                int lowerCnt = 0, upperCnt = 0, middleCnt = 0;
                for (int i = 0; i < chunkNum; i++) {
                    if (copy[i] < CUTOFF_LOWER_BOUND) {
                        lowerCnt++;
                    } else if (copy[i] > CUTOFF_UPPER_BOUND) {
                        upperCnt++;
                    } else {
                        // move to beginning
                        copy[middleCnt] = copy[i];
                        middleCnt++;
                    }
                }
                if (lowerCnt >= chunkNum / 2 - 1 || upperCnt >= chunkNum / 2 - 1) {
                    continue;
                }
                tryCutoff = IntQuickSelect.quickSelect(copy, 0, middleCnt - 1, chunkNum / 2 - lowerCnt + 1);
                copy[chunkNum / 2 - lowerCnt] = Integer.MAX_VALUE;
                checkLength = middleCnt;
            }
            success = true;
            for (int i = 0; i < checkLength; i++) {
                if (copy[i] == tryCutoff) {
                    success = false;
                    break;
                }
            }
        }
        cutoff = tryCutoff;
    }

    @Override
    protected int getCutoff(){
        return cutoff;
    }
}
