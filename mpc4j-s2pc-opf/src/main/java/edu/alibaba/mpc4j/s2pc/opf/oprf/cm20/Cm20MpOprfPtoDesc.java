package edu.alibaba.mpc4j.s2pc.opf.oprf.cm20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * CM20-MP-OPRF protocol description. This OPRF is described in the following paper:
 * <p>
 * Chase M, Miao P. Private Set Intersection in the Internet Setting from Lightweight Oblivious PRF. CRYPTO 2020.
 * </p>
 * The following paper abstract CM20-OPRF as an instance of MP-OPRF:
 * <p>
 * Jia, Yanxue, Shi-Feng Sun, Hong-Sheng Zhou, Jiajun Du, and Dawu Gu. Shuffle-based Private Set Union: Faster and More
 * Secure. USENIX Security 2022.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/03/03
 */
class Cm20MpOprfPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 132060736192853349L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CM20_MP-OPRF";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * receiver sends encoding key
         */
        RECEIVER_SEND_KEY,
        /**
         * receiver sends matrix Δ
         */
        RECEIVER_SEND_DELTA,
    }

    /**
     * singleton mode
     */
    private static final Cm20MpOprfPtoDesc INSTANCE = new Cm20MpOprfPtoDesc();

    /**
     * private constructor.
     */
    private Cm20MpOprfPtoDesc() {
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
     * minimal log(n) for getting w
     */
    static final int MIN_LOG_N_FOR_W = 8;
    /**
     * maximal log(n) for getting w
     */
    static final int MAX_LOG_N_FOR_W = 24;
    /**
     * map: num -> w
     */
    static final TIntIntMap LOG_N_W_MAP = new TIntIntHashMap();

    static {
        LOG_N_W_MAP.put(8, 585);
        LOG_N_W_MAP.put(9, 588);
        LOG_N_W_MAP.put(10, 591);
        LOG_N_W_MAP.put(11, 594);
        LOG_N_W_MAP.put(12, 597);
        LOG_N_W_MAP.put(13, 600);
        LOG_N_W_MAP.put(14, 603);
        LOG_N_W_MAP.put(15, 606);
        LOG_N_W_MAP.put(16, 609);
        LOG_N_W_MAP.put(17, 612);
        LOG_N_W_MAP.put(18, 615);
        LOG_N_W_MAP.put(19, 618);
        LOG_N_W_MAP.put(20, 621);
        LOG_N_W_MAP.put(21, 624);
        LOG_N_W_MAP.put(22, 627);
        LOG_N_W_MAP.put(23, 630);
        LOG_N_W_MAP.put(24, 633);
    }

    /**
     * Gets w, see Table 1 of the paper.
     *
     * @param n n.
     * @return w.
     */
    static int getW(int n) {
        MathPreconditions.checkPositiveInRangeClosed("n", n, 1 << MAX_LOG_N_FOR_W);
        int nLogValue = LongUtils.ceilLog2(Math.max(n, 1 << MIN_LOG_N_FOR_W));
        return LOG_N_W_MAP.get(nLogValue);
    }
}
