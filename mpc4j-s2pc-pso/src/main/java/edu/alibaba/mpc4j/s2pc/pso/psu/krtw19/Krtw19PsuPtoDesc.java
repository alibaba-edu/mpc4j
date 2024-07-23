package edu.alibaba.mpc4j.s2pc.pso.psu.krtw19;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.DoubleUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;

/**
 * KRTW19-PSU protocol description. The scheme is described in the following paper:
 * <p>
 * Kolesnikov V, Rosulek M, Trieu N, et al. Scalable private set union from symmetric-key techniques. ASIACRYPT 2019,
 * pp. 636-666.
 * </p>
 * The implementation follows the open-source code in the paper, i.e., root polynomial instead of interpolation.
 *
 * @author Weiran Liu
 * @date 2022/02/20
 */
class Krtw19PsuPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int)1647093902110062076L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "KRTW19_PSU";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends keys
         */
        SERVER_SEND_KEYS,
        /**
         * client sends polynomials
         */
        CLIENT_SEND_POLYS,
        /**
         * server sends S* OPRFs.
         */
        SERVER_SEND_S_STAR_OPRFS,
        /**
         * server sends encrypted elements
         */
        SERVER_SEND_ENC_ELEMENTS,
    }

    /**
     * singleton mode
     */
    private static final Krtw19PsuPtoDesc INSTANCE = new Krtw19PsuPtoDesc();

    /**
     * private constructor.
     */
    private Krtw19PsuPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }

    /**
     * lookup table for bin num (β)
     */
    private static final TIntIntMap BIN_NUM_MATRIX = new TIntIntHashMap();

    static {
        // n = 2^8, beta / n = 0.043
        BIN_NUM_MATRIX.put(8, 11);
        // n = 2^10, beta / n = 0.055
        BIN_NUM_MATRIX.put(10, 56);
        // n = 2^12, beta / n = 0.05
        BIN_NUM_MATRIX.put(12, 204);
        // n = 2^14, beta / n = 0.053
        BIN_NUM_MATRIX.put(14, 868);
        // n = 2^16, beta / n = 0.058
        BIN_NUM_MATRIX.put(16, 3801);
        // n = 2^18, beta / n = 0.052
        BIN_NUM_MATRIX.put(18, 13631);
        // n = 2^20, beta / n = 0.06
        BIN_NUM_MATRIX.put(20, 62914);
        // n = 2^22, beta / n = 0.051
        BIN_NUM_MATRIX.put(22, 213909);
        // n = 2^24, beta / n = 0.051
        BIN_NUM_MATRIX.put(24, 855638);
    }

    /**
     * Gets bin num (β), see Table 2 of the paper.
     *
     * @param n the max of server element size and client element size.
     * @return bin num (β).
     */
    static int getBinNum(int n) {
        assert n > 0;
        // minimal n = 2^8, interval is 2^2, we need to consider log(n) and log(n + 1).
        int nLogValue = LongUtils.ceilLog2(Math.max(n, 1 << 8));
        if (BIN_NUM_MATRIX.containsKey(nLogValue)) {
            return BIN_NUM_MATRIX.get(nLogValue);
        } else if (BIN_NUM_MATRIX.containsKey(nLogValue + 1)) {
            return BIN_NUM_MATRIX.get(nLogValue + 1);
        }
        throw new IllegalArgumentException("Max element size = " + n + " exceeds supported size = " + (1 << 22));
    }

    /**
     * max bin num (β)
     */
    static int MAX_BIN_NUM = Arrays.stream(BIN_NUM_MATRIX.values()).max().orElse(0);

    /**
     * lookup table for max bin size (m)
     */
    private static final TIntIntMap MAX_BIN_SIZE_MATRIX = new TIntIntHashMap();

    static {
        // n = 2^8, m = 63
        MAX_BIN_SIZE_MATRIX.put(8, 63);
        // n = 2^10, m = 58
        MAX_BIN_SIZE_MATRIX.put(10, 58);
        // n = 2^12, m = 63
        MAX_BIN_SIZE_MATRIX.put(12, 63);
        // n = 2^14, m = 62
        MAX_BIN_SIZE_MATRIX.put(14, 62);
        // n = 2^16, m = 60
        MAX_BIN_SIZE_MATRIX.put(16, 60);
        // n = 2^18, m = 65
        MAX_BIN_SIZE_MATRIX.put(18, 65);
        // n = 2^20, m = 61
        MAX_BIN_SIZE_MATRIX.put(20, 61);
        // n = 2^22, m = 68
        MAX_BIN_SIZE_MATRIX.put(22, 68);
        // n = 2^24, m = 69
        MAX_BIN_SIZE_MATRIX.put(24, 69);
    }

    /**
     * Gets max bin size (m), see Table 2 of the paper.
     *
     * @param n the max of server element size and client element size.
     * @return max bin size (m).
     */
    static int getMaxBinSize(int n) {
        assert n > 0;
        // minimal n = 2^8, interval is 2^2, we need to consider log(n) and log(n + 1).
        int nLogValue = LongUtils.ceilLog2(Math.max(n, 1 << 8));
        if (MAX_BIN_SIZE_MATRIX.containsKey(nLogValue)) {
            return MAX_BIN_SIZE_MATRIX.get(nLogValue);
        } else if (MAX_BIN_SIZE_MATRIX.containsKey(nLogValue + 1)) {
            return MAX_BIN_SIZE_MATRIX.get(nLogValue + 1);
        }
        throw new IllegalArgumentException("Max element size = " + n + " exceeds supported size = " + (1 << 22));
    }

    /**
     * Gets finite field bit length: σ = λ + log(β * (m + 1)^2), must divides Byte.SIZE.
     *
     * @param binNum     bin num (β).
     * @param maxBinSize max bin size(m).
     * @return finite field bit length σ.
     */
    static int getFiniteFieldByteLength(int binNum, int maxBinSize) {
        assert binNum > 0;
        assert maxBinSize > 0;
        return CommonUtils.getByteLength(
            CommonConstants.STATS_BIT_LENGTH
                + (int) Math.ceil(Math.log(binNum * (maxBinSize + 1) * (maxBinSize + 1)) / Math.log(2.0))
        );
    }

    /**
     * Gets PEQT byte length: σ + log_2(maxBinSize^2 * binNum).
     *
     * @param binNum     bin num (β).
     * @param maxBinSize max bin size (m).
     * @return PEQT协议对比长度
     */
    static int getPeqtByteLength(int binNum, int maxBinSize) {
        return CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(
            (int) (DoubleUtils.log2(Math.pow(maxBinSize, 2) * binNum))
        );
    }
}
