package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.nc.wykw21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.bsp.Gf2kBspVoleFactory.Gf2kBspVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleConfig;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.Gf2kMspVoleFactory.Gf2kMspVoleType;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.msp.bcg19.Bcg19RegGf2kMspVoleConfig;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * WYKW21-GF2K-NC-VOLE protocol description. The protocol comes from the following paper:
 * <p>
 * Weng, Chenkai, Kang Yang, Jonathan Katz, and Xiao Wang. Wolverine: fast, scalable, and communication-efficient
 * zero-knowledge proofs for boolean and arithmetic circuits. S&P 2021, pp. 1074-1091. IEEE, 2021.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/23
 */
class Wykw21Gf2kNcVolePtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6884511416447286741L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "YWKW21_GF2K_NC_VOLE";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * receiver sends setup matrix key
         */
        RECEIVER_SEND_SETUP_KEY,
        /**
         * receiver sends iteration matrix key
         */
        RECEIVER_SEND_ITERATION_LEY,
    }

    /**
     * singleton mode
     */
    private static final Wykw21Gf2kNcVolePtoDesc INSTANCE = new Wykw21Gf2kNcVolePtoDesc();

    /**
     * private constructor.
     */
    private Wykw21Gf2kNcVolePtoDesc() {
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
    static final int MIN_LOG_N = Wykw21Gf2kNcVoleLpnParamsFinder.ITERATION_MIN_LOG_N;
    /**
     * maximal supported log(n)
     */
    static final int MAX_LOG_N = Wykw21Gf2kNcVoleLpnParamsFinder.ITERATION_MAX_LOG_N;

    /**
     * BCG19-REG-GF2K-MSP-VOLE + WYKW21-SH-GF2K-BSP-VOLE setup LPN parameters
     */
    private static final TIntObjectMap<LpnParams> WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(12, LpnParams.create(2018, 384, 391));
        WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(13, LpnParams.create(2217, 384, 434));
        WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(14, LpnParams.create(2703, 512, 392));
        WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(15, LpnParams.create(3665, 512, 547));
        WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(16, LpnParams.create(5602, 640, 677));
        WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(17, LpnParams.create(9215, 896, 800));
        WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(18, LpnParams.create(16126, 1152, 1101));
        WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(19, LpnParams.create(30349, 1664, 1441));
        WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(20, LpnParams.create(57222, 2944, 1528));
        WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(21, LpnParams.create(112061, 5760, 1517));
        WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(22, LpnParams.create(220488, 11136, 1532));
        WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(23, LpnParams.create(435497, 21760, 1536));
        WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(24, LpnParams.create(861724, 42752, 1534));
    }

    /**
     * BCG19-REG-GF2K-MSP-VOLE + WYKW21-SH-GF2K-BSP-VOLE iteration LPN parameters
     */
    private static final TIntObjectMap<LpnParams> WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(12, LpnParams.create(6114, 512, 1506));
        WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(13, LpnParams.create(10405, 768, 1445));
        WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(14, LpnParams.create(19084, 1280, 1420));
        WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(15, LpnParams.create(36428, 2176, 1484));
        WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(16, LpnParams.create(71135, 4096, 1503));
        WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(17, LpnParams.create(140278, 7680, 1526));
        WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(18, LpnParams.create(278260, 14592, 1524));
        WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(19, LpnParams.create(554620, 28800, 1532));
        WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(20, LpnParams.create(1105792, 55680, 1536));
        WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(21, LpnParams.create(2209151, 110464, 1535));
        WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(22, LpnParams.create(4414720, 218880, 1536));
        WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(23, LpnParams.create(8824064, 433920, 1536));
        WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(24, LpnParams.create(17638912, 860160, 1536));
    }

    /**
     * BCG19-REG-GF2K-MSP-VOLE + WYKW21-MA-GF2K-BSP-VOLE setup LPN parameters
     */
    private static final TIntObjectMap<LpnParams> WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(12, LpnParams.create(2022, 384, 392));
        WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(13, LpnParams.create(2217, 384, 434));
        WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(14, LpnParams.create(2703, 512, 392));
        WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(15, LpnParams.create(3665, 512, 547));
        WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(16, LpnParams.create(5602, 640, 677));
        WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(17, LpnParams.create(9215, 896, 800));
        WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(18, LpnParams.create(16126, 1152, 1101));
        WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(19, LpnParams.create(30349, 1664, 1441));
        WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(20, LpnParams.create(57222, 2944, 1528));
        WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(21, LpnParams.create(112061, 5760, 1517));
        WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(22, LpnParams.create(220488, 11136, 1532));
        WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(23, LpnParams.create(435497, 21760, 1536));
        WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(24, LpnParams.create(861724, 42752, 1534));
    }

    /**
     * BCG19-REG-GF2K-MSP-VOLE + WYKW21-MA-GF2K-BSP-VOLE iteration LPN parameters
     */
    private static final TIntObjectMap<LpnParams> WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(12, LpnParams.create(6115, 512, 1506));
        WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(13, LpnParams.create(10406, 768, 1445));
        WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(14, LpnParams.create(19085, 1280, 1420));
        WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(15, LpnParams.create(36429, 2176, 1484));
        WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(16, LpnParams.create(71136, 4096, 1503));
        WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(17, LpnParams.create(140279, 7680, 1526));
        WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(18, LpnParams.create(278261, 14592, 1524));
        WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(19, LpnParams.create(554621, 28800, 1532));
        WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(20, LpnParams.create(1105793, 55680, 1536));
        WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(21, LpnParams.create(2209152, 110464, 1535));
        WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(22, LpnParams.create(4414721, 218880, 1536));
        WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(23, LpnParams.create(8824065, 433920, 1536));
        WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(24, LpnParams.create(17638913, 860160, 1536));
    }

    /**
     * Gets setup LPN parameter.
     *
     * @param config config.
     * @param num    num.
     * @return setup LPN parameter.
     */
    static LpnParams getSetupLpnParams(Gf2kMspVoleConfig config, int num) {
        int ceilLogN = LongUtils.ceilLog2(num);
        MathPreconditions.checkNonNegativeInRangeClosed("ceil(log(num))", ceilLogN, MAX_LOG_N);
        if (ceilLogN < MIN_LOG_N) {
            ceilLogN = MIN_LOG_N;
        }
        Gf2kMspVoleType gf2kMspVoleType = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (gf2kMspVoleType) {
            case BCG19_REG:
                Bcg19RegGf2kMspVoleConfig bcg19RegGf2kMspVoleConfig = (Bcg19RegGf2kMspVoleConfig) config;
                Gf2kBspVoleConfig bcg19RegGf2kBspVoleConfig = bcg19RegGf2kMspVoleConfig.getGf2kBspVoleConfig();
                Gf2kBspVoleType bcg19RegGf2kBspVoleType = bcg19RegGf2kBspVoleConfig.getPtoType();
                switch (bcg19RegGf2kBspVoleType) {
                    case WYKW21_SEMI_HONEST:
                        return WYKW21_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.get(ceilLogN);
                    case WYKW21_MALICIOUS:
                        return WYKW21_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.get(ceilLogN);
                    default:
                        throw new IllegalArgumentException(String.format(
                            "Invalid %s: %s", Gf2kBspVoleType.class.getSimpleName(), bcg19RegGf2kBspVoleType
                        ));
                }
            default:
                throw new IllegalArgumentException(String.format(
                    "Invalid %s: %s", Gf2kMspVoleType.class.getSimpleName(), gf2kMspVoleType
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
    static LpnParams getIterationLpnParams(Gf2kMspVoleConfig config, int num) {
        int ceilLogN = LongUtils.ceilLog2(num);
        MathPreconditions.checkNonNegativeInRangeClosed("ceil(log(num))", ceilLogN, MAX_LOG_N);
        if (ceilLogN < MIN_LOG_N) {
            ceilLogN = MIN_LOG_N;
        }
        Gf2kMspVoleType gf2kMspVoleType = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (gf2kMspVoleType) {
            case BCG19_REG:
                Bcg19RegGf2kMspVoleConfig bcg19RegGf2kMspVoleConfig = (Bcg19RegGf2kMspVoleConfig) config;
                Gf2kBspVoleConfig bcg19RegGf2kBspVoleConfig = bcg19RegGf2kMspVoleConfig.getGf2kBspVoleConfig();
                Gf2kBspVoleType bcg19RegGf2kBspVoleType = bcg19RegGf2kBspVoleConfig.getPtoType();
                switch (bcg19RegGf2kBspVoleType) {
                    case WYKW21_SEMI_HONEST:
                        return WYKW21_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.get(ceilLogN);
                    case WYKW21_MALICIOUS:
                        return WYKW21_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.get(ceilLogN);
                    default:
                        throw new IllegalArgumentException(String.format(
                            "Invalid %s: %s", Gf2kBspVoleType.class.getSimpleName(), bcg19RegGf2kBspVoleType
                        ));
                }
            default:
                throw new IllegalArgumentException(String.format(
                    "Invalid %s: %s", Gf2kMspVoleType.class.getSimpleName(), gf2kMspVoleType
                ));
        }
    }
}
