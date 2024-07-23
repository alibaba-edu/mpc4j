package edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.nc.aprr24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.bsp.Gf2kBspVodeFactory.Gf2kBspVodeType;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.Gf2kMspVodeFactory.Gf2kMspVodeType;
import edu.alibaba.mpc4j.s2pc.pcg.vode.gf2k.sp.msp.bcg19.Bcg19RegGf2kMspVodeConfig;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * APRR24 GF2K-NC-VODE protocol description. The protocol comes from the following paper:
 * <p>
 * Weng, Chenkai, Kang Yang, Jonathan Katz, and Xiao Wang. Wolverine: fast, scalable, and communication-efficient
 * zero-knowledge proofs for boolean and arithmetic circuits. S&P 2021, pp. 1074-1091. IEEE, 2021.
 * </p>
 *
 * @author Weiran Liu
 * @date 2024/6/13
 */
class Aprr24Gf2kNcVodePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8833130397146606584L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "APRR24_GF2K_NC_VODE";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * receiver sends setup matrix key
         */
        RECEIVER_SEND_KEYS,
    }

    /**
     * singleton mode
     */
    private static final Aprr24Gf2kNcVodePtoDesc INSTANCE = new Aprr24Gf2kNcVodePtoDesc();

    /**
     * private constructor.
     */
    private Aprr24Gf2kNcVodePtoDesc() {
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
     * minimal supported log(n)
     */
    static final int MIN_LOG_N = Aprr24Gf2kNcVodeLpnParamsFinder.ITERATION_MIN_LOG_N;
    /**
     * maximal supported log(n)
     */
    static final int MAX_LOG_N = Aprr24Gf2kNcVodeLpnParamsFinder.ITERATION_MAX_LOG_N;

    /**
     * semi-honest REG setup LPN parameters
     */
    private static final TIntObjectMap<LpnParams> SH_REG_SETUP_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        SH_REG_SETUP_LPN_PARAMS_MAP.put(12, LpnParams.create(2018, 384, 391));
        SH_REG_SETUP_LPN_PARAMS_MAP.put(13, LpnParams.create(2217, 384, 434));
        SH_REG_SETUP_LPN_PARAMS_MAP.put(14, LpnParams.create(2703, 512, 392));
        SH_REG_SETUP_LPN_PARAMS_MAP.put(15, LpnParams.create(3665, 512, 547));
        SH_REG_SETUP_LPN_PARAMS_MAP.put(16, LpnParams.create(5602, 640, 677));
        SH_REG_SETUP_LPN_PARAMS_MAP.put(17, LpnParams.create(9215, 896, 800));
        SH_REG_SETUP_LPN_PARAMS_MAP.put(18, LpnParams.create(16126, 1152, 1101));
        SH_REG_SETUP_LPN_PARAMS_MAP.put(19, LpnParams.create(30349, 1664, 1441));
        SH_REG_SETUP_LPN_PARAMS_MAP.put(20, LpnParams.create(57222, 2944, 1528));
        SH_REG_SETUP_LPN_PARAMS_MAP.put(21, LpnParams.create(112061, 5760, 1517));
        SH_REG_SETUP_LPN_PARAMS_MAP.put(22, LpnParams.create(220488, 11136, 1532));
        SH_REG_SETUP_LPN_PARAMS_MAP.put(23, LpnParams.create(435497, 21760, 1536));
        SH_REG_SETUP_LPN_PARAMS_MAP.put(24, LpnParams.create(861724, 42752, 1534));
    }

    /**
     * semi-honest REG iteration LPN parameters
     */
    private static final TIntObjectMap<LpnParams> SH_REG_ITERATION_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        SH_REG_ITERATION_LPN_PARAMS_MAP.put(12, LpnParams.create(6114, 512, 1506));
        SH_REG_ITERATION_LPN_PARAMS_MAP.put(13, LpnParams.create(10405, 768, 1445));
        SH_REG_ITERATION_LPN_PARAMS_MAP.put(14, LpnParams.create(19084, 1280, 1420));
        SH_REG_ITERATION_LPN_PARAMS_MAP.put(15, LpnParams.create(36428, 2176, 1484));
        SH_REG_ITERATION_LPN_PARAMS_MAP.put(16, LpnParams.create(71135, 4096, 1503));
        SH_REG_ITERATION_LPN_PARAMS_MAP.put(17, LpnParams.create(140278, 7680, 1526));
        SH_REG_ITERATION_LPN_PARAMS_MAP.put(18, LpnParams.create(278260, 14592, 1524));
        SH_REG_ITERATION_LPN_PARAMS_MAP.put(19, LpnParams.create(554620, 28800, 1532));
        SH_REG_ITERATION_LPN_PARAMS_MAP.put(20, LpnParams.create(1105792, 55680, 1536));
        SH_REG_ITERATION_LPN_PARAMS_MAP.put(21, LpnParams.create(2209151, 110464, 1535));
        SH_REG_ITERATION_LPN_PARAMS_MAP.put(22, LpnParams.create(4414720, 218880, 1536));
        SH_REG_ITERATION_LPN_PARAMS_MAP.put(23, LpnParams.create(8824064, 433920, 1536));
        SH_REG_ITERATION_LPN_PARAMS_MAP.put(24, LpnParams.create(17638912, 860160, 1536));
    }

    /**
     * Gets setup LPN parameter.
     *
     * @param config config.
     * @param num    num.
     * @return setup LPN parameter.
     */
    static LpnParams getSetupLpnParams(Gf2kMspVodeConfig config, int num) {
        int ceilLogN = LongUtils.ceilLog2(num);
        MathPreconditions.checkNonNegativeInRangeClosed("ceil(log(num))", ceilLogN, MAX_LOG_N);
        if (ceilLogN < MIN_LOG_N) {
            ceilLogN = MIN_LOG_N;
        }
        Gf2kMspVodeType gf2kMspVodeType = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (gf2kMspVodeType) {
            case BCG19_REG:
                Bcg19RegGf2kMspVodeConfig bcg19RegGf2kMspVodeConfig = (Bcg19RegGf2kMspVodeConfig) config;
                Gf2kBspVodeConfig bcg19RegGf2kBspVodeConfig = bcg19RegGf2kMspVodeConfig.getGf2kBspVodeConfig();
                Gf2kBspVodeType bcg19RegGf2kBspVodeType = bcg19RegGf2kBspVodeConfig.getPtoType();
                switch (bcg19RegGf2kBspVodeType) {
                    case GYW23:
                    case APRR24:
                        return SH_REG_SETUP_LPN_PARAMS_MAP.get(ceilLogN);
                    default:
                        throw new IllegalArgumentException(String.format(
                            "Invalid %s: %s", Gf2kBspVodeType.class.getSimpleName(), bcg19RegGf2kBspVodeType
                        ));
                }
            default:
                throw new IllegalArgumentException(String.format(
                    "Invalid %s: %s", Gf2kMspVodeType.class.getSimpleName(), gf2kMspVodeType
                ));
        }
    }

    /**
     * Gets iteration LPN parameter.
     *
     * @param config config.
     * @param num    num.
     * @return iteration LPN parameter.
     */
    static LpnParams getIterationLpnParams(Gf2kMspVodeConfig config, int num) {
        int ceilLogN = LongUtils.ceilLog2(num);
        MathPreconditions.checkNonNegativeInRangeClosed("ceil(log(num))", ceilLogN, MAX_LOG_N);
        if (ceilLogN < MIN_LOG_N) {
            ceilLogN = MIN_LOG_N;
        }
        Gf2kMspVodeType gf2kMspVodeType = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (gf2kMspVodeType) {
            case BCG19_REG:
                Bcg19RegGf2kMspVodeConfig bcg19RegGf2kMspVodeConfig = (Bcg19RegGf2kMspVodeConfig) config;
                Gf2kBspVodeConfig bcg19RegGf2kBspVodeConfig = bcg19RegGf2kMspVodeConfig.getGf2kBspVodeConfig();
                Gf2kBspVodeType bcg19RegGf2kBspVodeType = bcg19RegGf2kBspVodeConfig.getPtoType();
                switch (bcg19RegGf2kBspVodeType) {
                    case GYW23:
                    case APRR24:
                        return SH_REG_ITERATION_LPN_PARAMS_MAP.get(ceilLogN);
                    default:
                        throw new IllegalArgumentException(String.format(
                            "Invalid %s: %s", Gf2kBspVodeType.class.getSimpleName(), bcg19RegGf2kBspVodeType
                        ));
                }
            default:
                throw new IllegalArgumentException(String.format(
                    "Invalid %s: %s", Gf2kMspVodeType.class.getSimpleName(), gf2kMspVodeType
                ));
        }
    }
}
