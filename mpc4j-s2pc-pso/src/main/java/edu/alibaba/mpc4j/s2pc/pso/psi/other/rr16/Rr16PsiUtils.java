package edu.alibaba.mpc4j.s2pc.pso.psi.other.rr16;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * RR16-PSI utilities.
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/9/7
 */
public class Rr16PsiUtils {
    /**
     * private constructor.
     */
    private Rr16PsiUtils() {
        // empty
    }

    /**
     * Not table
     */
    private static final TIntIntMap N_OT_INIT_MATRIX = new TIntIntHashMap();

    static {
        N_OT_INIT_MATRIX.put(2, 8295);
        N_OT_INIT_MATRIX.put(3, 10663);
        N_OT_INIT_MATRIX.put(4, 14715);
        N_OT_INIT_MATRIX.put(5, 21762);
        N_OT_INIT_MATRIX.put(6, 34286);
        N_OT_INIT_MATRIX.put(7, 57068);
        N_OT_INIT_MATRIX.put(8, 99372);
        N_OT_INIT_MATRIX.put(9, 179281);
        N_OT_INIT_MATRIX.put(10, 331450);
        N_OT_INIT_MATRIX.put(11, 623180);
        N_OT_INIT_MATRIX.put(12, 1187141);
        N_OT_INIT_MATRIX.put(13, 2285265);
        N_OT_INIT_MATRIX.put(14, 4434188);
        N_OT_INIT_MATRIX.put(15, 8658560);
        N_OT_INIT_MATRIX.put(16, 16992857);
        N_OT_INIT_MATRIX.put(17, 33479820);
        N_OT_INIT_MATRIX.put(18, 66165163);
        N_OT_INIT_MATRIX.put(19, 131108816);
        N_OT_INIT_MATRIX.put(20, 260252093);
        N_OT_INIT_MATRIX.put(21, 517435654);
        N_OT_INIT_MATRIX.put(22, 1030082690);
        N_OT_INIT_MATRIX.put(23, 2052497778);
    }

    /**
     * number of ot, refer: https://github.com/osu-crypto/libPSI/
     *
     * @param maxBatchSize maximum batch size
     * @return number of OT instance
     */
    public static int getOtBatchSize(int maxBatchSize) {
        assert maxBatchSize > 0;
        // 支持的最小值为2^2
        int nLogValue = LongUtils.ceilLog2(Math.max(maxBatchSize, 1 << 2));
        if (N_OT_INIT_MATRIX.containsKey(nLogValue)) {
            return N_OT_INIT_MATRIX.get(nLogValue);
        }
        throw new IllegalArgumentException(
            "MaxBatch Size = " + maxBatchSize + " exceeds supported size = " + (1 << 23));
    }

    /**
     * Notone table
     */
    private static final TIntIntMap N_ONE_INIT_MATRIX = new TIntIntHashMap();

    static {
        N_ONE_INIT_MATRIX.put(2, 517);
        N_ONE_INIT_MATRIX.put(3, 959);
        N_ONE_INIT_MATRIX.put(4, 1829);
        N_ONE_INIT_MATRIX.put(5, 3549);
        N_ONE_INIT_MATRIX.put(6, 6961);
        N_ONE_INIT_MATRIX.put(7, 13743);
        N_ONE_INIT_MATRIX.put(8, 27246);
        N_ONE_INIT_MATRIX.put(9, 54101);
        N_ONE_INIT_MATRIX.put(10, 105905);
        N_ONE_INIT_MATRIX.put(11, 207952);
        N_ONE_INIT_MATRIX.put(12, 407982);
        N_ONE_INIT_MATRIX.put(13, 790117);
        N_ONE_INIT_MATRIX.put(14, 1582849);
        N_ONE_INIT_MATRIX.put(15, 3073716);
        N_ONE_INIT_MATRIX.put(16, 6113957);
        N_ONE_INIT_MATRIX.put(17, 12162968);
        N_ONE_INIT_MATRIX.put(18, 23957707);
        N_ONE_INIT_MATRIX.put(19, 47283348);
        N_ONE_INIT_MATRIX.put(20, 95333932);
        N_ONE_INIT_MATRIX.put(21, 188162824);
        N_ONE_INIT_MATRIX.put(22, 371340163);
        N_ONE_INIT_MATRIX.put(23, 758786092);
    }

    /**
     * the number of "not-one" choices in OT, refer: https://github.com/osu-crypto/libPSI/
     *
     * @param maxBatchSize maximum batch size
     * @return the number of "not-one" choices
     */
    public static int getOtOneCount(int maxBatchSize) {
        assert maxBatchSize > 0;
        // 支持的最小值为2^2
        int nLogValue = LongUtils.ceilLog2(Math.max(maxBatchSize, 1 << 2));
        if (N_ONE_INIT_MATRIX.containsKey(nLogValue)) {
            return N_ONE_INIT_MATRIX.get(nLogValue);
        }
        throw new IllegalArgumentException(
            "MaxBatch Size = " + maxBatchSize + " exceeds supported size = " + (1 << 23));
    }

    /**
     * CncThreshold table
     */
    private static final TIntIntMap T_CNC_INIT_MATRIX = new TIntIntHashMap();

    static {
        T_CNC_INIT_MATRIX.put(2, 138);
        T_CNC_INIT_MATRIX.put(3, 204);
        T_CNC_INIT_MATRIX.put(4, 323);
        T_CNC_INIT_MATRIX.put(5, 540);
        T_CNC_INIT_MATRIX.put(6, 944);
        T_CNC_INIT_MATRIX.put(7, 1711);
        T_CNC_INIT_MATRIX.put(8, 3182);
        T_CNC_INIT_MATRIX.put(9, 5973);
        T_CNC_INIT_MATRIX.put(10, 9648);
        T_CNC_INIT_MATRIX.put(11, 15440);
        T_CNC_INIT_MATRIX.put(12, 22958);
        T_CNC_INIT_MATRIX.put(13, 36452);
        T_CNC_INIT_MATRIX.put(14, 59137);
        T_CNC_INIT_MATRIX.put(15, 91828);
        T_CNC_INIT_MATRIX.put(16, 150181);
        T_CNC_INIT_MATRIX.put(17, 235416);
        T_CNC_INIT_MATRIX.put(18, 364747);
        T_CNC_INIT_MATRIX.put(19, 621716);
        T_CNC_INIT_MATRIX.put(20, 962092);
        T_CNC_INIT_MATRIX.put(21, 1516296);
        T_CNC_INIT_MATRIX.put(22, 2241411);
        T_CNC_INIT_MATRIX.put(23, 3811372);
    }

    /**
     * get the quantity threshold of choice 1 in OT, refer: https://github.com/osu-crypto/libPSI/
     *
     * @param maxBatchSize maximum batch size
     * @return the quantity threshold
     */
    public static int getCncThreshold(int maxBatchSize) {
        assert maxBatchSize > 0;
        // 支持的最小值为2^2
        int nLogValue = LongUtils.ceilLog2(Math.max(maxBatchSize, 1 << 2));
        if (T_CNC_INIT_MATRIX.containsKey(nLogValue)) {
            return T_CNC_INIT_MATRIX.get(nLogValue);
        }
        throw new IllegalArgumentException(
            "MaxBatch Size = " + maxBatchSize + " exceeds supported size = " + (1 << 23));
    }

    /**
     * CncProb table
     */
    private static final Map<Integer, Double> P_CNC_INIT_MATRIX = new HashMap<>();

    static {
        P_CNC_INIT_MATRIX.put(2, 0.099);
        P_CNC_INIT_MATRIX.put(3, 0.099);
        P_CNC_INIT_MATRIX.put(4, 0.099);
        P_CNC_INIT_MATRIX.put(5, 0.099);
        P_CNC_INIT_MATRIX.put(6, 0.099);
        P_CNC_INIT_MATRIX.put(7, 0.099);
        P_CNC_INIT_MATRIX.put(8, 0.099);
        P_CNC_INIT_MATRIX.put(9, 0.098);
        P_CNC_INIT_MATRIX.put(10, 0.083);
        P_CNC_INIT_MATRIX.put(11, 0.069);
        P_CNC_INIT_MATRIX.put(12, 0.053);
        P_CNC_INIT_MATRIX.put(13, 0.044);
        P_CNC_INIT_MATRIX.put(14, 0.036);
        P_CNC_INIT_MATRIX.put(15, 0.029);
        P_CNC_INIT_MATRIX.put(16, 0.024);
        P_CNC_INIT_MATRIX.put(17, 0.019);
        P_CNC_INIT_MATRIX.put(18, 0.015);
        P_CNC_INIT_MATRIX.put(19, 0.013);
        P_CNC_INIT_MATRIX.put(20, 0.01);
        P_CNC_INIT_MATRIX.put(21, 0.008);
        P_CNC_INIT_MATRIX.put(22, 0.006);
        P_CNC_INIT_MATRIX.put(23, 0.005);
    }

    /**
     * get the value of Prob for different input size, refer: https://github.com/osu-crypto/libPSI/
     *
     * @param maxBatchSize maximum batch size
     * @return Prob
     */
    public static double getCncProb(int maxBatchSize) {
        assert maxBatchSize > 0;
        // 支持的最小值为2^2
        int nLogValue = LongUtils.ceilLog2(Math.max(maxBatchSize, 1 << 2));
        if (P_CNC_INIT_MATRIX.containsKey(nLogValue)) {
            return P_CNC_INIT_MATRIX.get(nLogValue);
        }
        throw new IllegalArgumentException(
            "MaxBatch Size = " + maxBatchSize + " exceeds supported size = " + (1 << 23));
    }

    /**
     * get the value from GBF
     *
     * @param storage GBF storage
     * @param key     GBF input
     * @param gbfHash hash function to generate BF index
     * @return w的值。
     */
    public static byte[] decode(byte[][] storage, byte[] key, Prf gbfHash) {
        int[] sparsePositions = Arrays.stream(IntUtils.byteArrayToIntArray(gbfHash.getBytes(ObjectUtils.objectToByteArray(key))))
            .map(hi -> Math.abs(hi % storage.length)).distinct().toArray();
        byte[] value = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        for (int position : sparsePositions) {
            BytesUtils.xori(value, storage[position]);
        }
        assert BytesUtils.isFixedReduceByteArray(value, CommonConstants.BLOCK_BYTE_LENGTH, CommonConstants.BLOCK_BIT_LENGTH);
        return value;
    }
}
