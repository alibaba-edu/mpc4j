package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.structure.lpn.LpnParams;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19.Bcg19RegMspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.ywl20.Ywl20UniMspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory.BspCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory.MspCotType;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 * YWL20-NC-COT protocol description. The protocol comes from the following paper:
 * <p>
 * Yang, Kang, Chenkai Weng, Xiao Lan, Jiang Zhang, and Xiao Wang. Ferret: Fast extension for correlated OT with small
 * communication. CCS 2020, pp. 1607-1626. 2020.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/01/31
 */
class Ywl20NcCotPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 5867648382625101131L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "YWL20_NC_COT";

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
    private static final Ywl20NcCotPtoDesc INSTANCE = new Ywl20NcCotPtoDesc();

    /**
     * private constructor.
     */
    private Ywl20NcCotPtoDesc() {
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
    static final int MIN_LOG_N = Ywl20NcCotLpnParamsFinder.ITERATION_MIN_LOG_N;
    /**
     * maximal supported log(n)
     */
    static final int MAX_LOG_N = Ywl20NcCotLpnParamsFinder.ITERATION_MAX_LOG_N;

    /**
     * BCG19-REG-MSP-COT + YWL20-SH-BSP-COT setup LPN parameters
     */
    private static final TIntObjectMap<LpnParams> YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(12, LpnParams.create(5030, 1024, 359));
        YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(13, LpnParams.create(6876, 1152, 445));
        YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(14, LpnParams.create(8792, 1152, 581));
        YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(15, LpnParams.create(9722, 1152, 647));
        YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(16, LpnParams.create(12832, 2304, 409));
        YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(17, LpnParams.create(18370, 2304, 604));
        YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(18, LpnParams.create(27451, 2432, 872));
        YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(19, LpnParams.create(42590, 4608, 701));
        YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(20, LpnParams.create(71040, 4864, 1131));
        YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(21, LpnParams.create(127379, 8448, 1161));
        YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(22, LpnParams.create(237343, 12160, 1508));
        YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(23, LpnParams.create(453934, 22784, 1528));
        YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(24, LpnParams.create(882063, 43776, 1533));
    }

    /**
     * BCG19-REG-MSP-COT + YWL20-SH-BSP-COT iteration LPN parameters
     */
    private static final TIntObjectMap<LpnParams> YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(12, LpnParams.create(9126, 512, 1506));
        YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(13, LpnParams.create(15060, 896, 1493));
        YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(14, LpnParams.create(25167, 1408, 1475));
        YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(15, LpnParams.create(42490, 2432, 1458));
        YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(16, LpnParams.create(78354, 4352, 1411));
        YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(17, LpnParams.create(149434, 7680, 1526));
        YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(18, LpnParams.create(289584, 15232, 1526));
        YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(19, LpnParams.create(566876, 28800, 1532));
        YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(20, LpnParams.create(1119616, 55680, 1536));
        YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(21, LpnParams.create(2224501, 110464, 1535));
        YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(22, LpnParams.create(4431616, 218880, 1536));
        YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(23, LpnParams.create(8842496, 433920, 1536));
        YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(24, LpnParams.create(17658880, 860160, 1536));
    }

    /**
     * BCG19-REG-MSP-COT + YWL20-MA-BSP-COT setup LPN parameters
     */
    private static final TIntObjectMap<LpnParams> YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(12, LpnParams.create(5160, 1152, 323));
        YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(13, LpnParams.create(7003, 1152, 454));
        YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(14, LpnParams.create(8919, 1152, 590));
        YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(15, LpnParams.create(9863, 1152, 657));
        YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(16, LpnParams.create(13683, 2304, 439));
        YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(17, LpnParams.create(18512, 2304, 609));
        YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(18, LpnParams.create(27571, 2432, 876));
        YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(19, LpnParams.create(42731, 4480, 725));
        YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(20, LpnParams.create(71222, 4864, 1134));
        YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(21, LpnParams.create(127485, 8448, 1162));
        YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(22, LpnParams.create(237496, 12160, 1509));
        YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(23, LpnParams.create(454223, 22784, 1529));
        YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.put(24, LpnParams.create(882063, 43776, 1533));
    }

    /**
     * BCG19-REG-MSP-COT + YWL20-MA-BSP-COT iteration LPN parameters
     */
    private static final TIntObjectMap<LpnParams> YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(12, LpnParams.create(9254, 512, 1506));
        YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(13, LpnParams.create(15188, 896, 1493));
        YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(14, LpnParams.create(25295, 1408, 1475));
        YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(15, LpnParams.create(42618, 2432, 1458));
        YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(16, LpnParams.create(79208, 4352, 1532));
        YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(17, LpnParams.create(149562, 7680, 1526));
        YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(18, LpnParams.create(289712, 15232, 1526));
        YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(19, LpnParams.create(567004, 28800, 1532));
        YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(20, LpnParams.create(1119744, 55680, 1536));
        YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(21, LpnParams.create(2224629, 110464, 1535));
        YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(22, LpnParams.create(4431744, 218880, 1536));
        YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(23, LpnParams.create(8842624, 433920, 1536));
        YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.put(24, LpnParams.create(17659008, 860160, 1536));
    }

    /**
     * YWL20-UNI-MSP-COT + YWL20-SH-BSP-COT setup LPN parameters
     */
    private static final TIntObjectMap<LpnParams> YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(12, LpnParams.create(10271, 2816, 252));
        YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(13, LpnParams.create(10582, 2816, 261));
        YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(14, LpnParams.create(11898, 2816, 299));
        YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(15, LpnParams.create(15058, 2944, 371));
        YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(16, LpnParams.create(25011, 5120, 350));
        YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(17, LpnParams.create(31299, 5888, 384));
        YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(18, LpnParams.create(40836, 6400, 469));
        YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(19, LpnParams.create(57725, 6528, 666));
        YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(20, LpnParams.create(84031, 10112, 620));
        YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(21, LpnParams.create(140424, 13056, 812));
        YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(22, LpnParams.create(251283, 13952, 1386));
        YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(23, LpnParams.create(468517, 25088, 1428));
        YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(24, LpnParams.create(897162, 44544, 1532));
    }

    /**
     * YWL20-UNI-MSP-COT + YWL20-SH-BSP-COT iteration LPN parameters
     */
    private static final TIntObjectMap<LpnParams> YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(12, LpnParams.create(14336, 1536, 725));
        YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(13, LpnParams.create(18752, 2048, 709));
        YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(14, LpnParams.create(28272, 2944, 745));
        YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(15, LpnParams.create(47802, 4864, 753));
        YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(16, LpnParams.create(90540, 4736, 1501));
        YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(17, LpnParams.create(162340, 8448, 1521));
        YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(18, LpnParams.create(302932, 15488, 1533));
        YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(19, LpnParams.create(581968, 30080, 1533));
        YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(20, LpnParams.create(1132544, 56320, 1536));
        YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(21, LpnParams.create(2237555, 110464, 1535));
        YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(22, LpnParams.create(4445440, 218880, 1536));
        YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(23, LpnParams.create(8857088, 433920, 1536));
        YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(24, LpnParams.create(17674240, 860160, 1536));
    }

    /**
     * YWL20-UNI-MSP-COT + YWL20-MA-BSP-COT setup LPN parameters
     */
    private static final TIntObjectMap<LpnParams> YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(12, LpnParams.create(10617, 2816, 262));
        YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(13, LpnParams.create(10859, 2816, 269));
        YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(14, LpnParams.create(12037, 2816, 303));
        YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(15, LpnParams.create(15167, 2944, 374));
        YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(16, LpnParams.create(25138, 5120, 352));
        YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(17, LpnParams.create(31399, 5760, 395));
        YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(18, LpnParams.create(40916, 6400, 470));
        YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(19, LpnParams.create(57888, 6528, 668));
        YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(20, LpnParams.create(84108, 9856, 638));
        YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(21, LpnParams.create(140543, 12800, 830));
        YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(22, LpnParams.create(251283, 13952, 1386));
        YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(23, LpnParams.create(468637, 24960, 1436));
        YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.put(24, LpnParams.create(897162, 44544, 1532));
    }

    /**
     * YWL20-UNI-MSP-COT + YWL20-MA-BSP-COT iteration LPN parameters
     */
    private static final TIntObjectMap<LpnParams> YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP = new TIntObjectHashMap<>();

    static {
        YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(12, LpnParams.create(14680, 1536, 743));
        YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(13, LpnParams.create(19048, 2048, 723));
        YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(14, LpnParams.create(28400, 2944, 745));
        YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(15, LpnParams.create(47930, 4864, 753));
        YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(16, LpnParams.create(90668, 4736, 1501));
        YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(17, LpnParams.create(162468, 8448, 1521));
        YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(18, LpnParams.create(303060, 15488, 1533));
        YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(19, LpnParams.create(582096, 30080, 1533));
        YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(20, LpnParams.create(1132672, 56320, 1536));
        YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(21, LpnParams.create(2237683, 110464, 1535));
        YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(22, LpnParams.create(4445568, 218880, 1536));
        YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(23, LpnParams.create(8857216, 433920, 1536));
        YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.put(24, LpnParams.create(17674368, 860160, 1536));
    }

    /**
     * Gets setup LPN parameter.
     *
     * @param config config.
     * @param num    num.
     * @return setup LPN parameter.
     */
    static LpnParams getSetupLpnParams(MspCotConfig config, int num) {
        int ceilLogN = LongUtils.ceilLog2(num);
        MathPreconditions.checkNonNegativeInRangeClosed("ceil(log(num))", ceilLogN, MAX_LOG_N);
        if (ceilLogN < MIN_LOG_N) {
            ceilLogN = MIN_LOG_N;
        }
        MspCotType mspCotType = config.getPtoType();
        switch (mspCotType) {
            case BCG19_REG:
                Bcg19RegMspCotConfig bcg19RegMspCotConfig = (Bcg19RegMspCotConfig) config;
                BspCotConfig bcg19RegBspCotConfig = bcg19RegMspCotConfig.getBspCotConfig();
                BspCotType bcg19RegBspCotType = bcg19RegBspCotConfig.getPtoType();
                switch (bcg19RegBspCotType) {
                    case YWL20_SEMI_HONEST:
                        return YWL20_SH_BCG19_REG_SETUP_LPN_PARAMS_MAP.get(ceilLogN);
                    case YWL20_MALICIOUS:
                        return YWL20_MA_BCG19_REG_SETUP_LPN_PARAMS_MAP.get(ceilLogN);
                    default:
                        throw new IllegalArgumentException(String.format(
                            "Invalid %s: %s", BspCotType.class.getSimpleName(), bcg19RegBspCotType
                        ));
                }
            case YWL20_UNI:
                Ywl20UniMspCotConfig ywl20UniMspCotConfig = (Ywl20UniMspCotConfig) config;
                BspCotConfig ywl20UniBspCotConfig = ywl20UniMspCotConfig.getBspCotConfig();
                BspCotType ywl20UniBspCotType = ywl20UniBspCotConfig.getPtoType();
                switch (ywl20UniBspCotType) {
                    case YWL20_SEMI_HONEST:
                        return YWL20_SH_YWL20_UNI_SETUP_LPN_PARAMS_MAP.get(ceilLogN);
                    case YWL20_MALICIOUS:
                        return YWL20_MA_YWL20_UNI_SETUP_LPN_PARAMS_MAP.get(ceilLogN);
                    default:
                        throw new IllegalArgumentException(String.format(
                            "Invalid %s: %s", BspCotType.class.getSimpleName(), ywl20UniBspCotType
                        ));
                }
            default:
                throw new IllegalArgumentException(String.format(
                    "Invalid %s: %s", MspCotType.class.getSimpleName(), mspCotType
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
    static LpnParams getIterationLpnParams(MspCotConfig config, int num) {
        int ceilLogN = LongUtils.ceilLog2(num);
        MathPreconditions.checkNonNegativeInRangeClosed("ceil(log(num))", ceilLogN, MAX_LOG_N);
        if (ceilLogN < MIN_LOG_N) {
            ceilLogN = MIN_LOG_N;
        }
        MspCotType mspCotType = config.getPtoType();
        switch (mspCotType) {
            case BCG19_REG:
                Bcg19RegMspCotConfig bcg19RegMspCotConfig = (Bcg19RegMspCotConfig) config;
                BspCotConfig bcg19RegBspCotConfig = bcg19RegMspCotConfig.getBspCotConfig();
                BspCotType bcg19RegBspCotType = bcg19RegBspCotConfig.getPtoType();
                switch (bcg19RegBspCotType) {
                    case YWL20_SEMI_HONEST:
                        return YWL20_SH_BCG19_REG_ITERATION_LPN_PARAMS_MAP.get(ceilLogN);
                    case YWL20_MALICIOUS:
                        return YWL20_MA_BCG19_REG_ITERATION_LPN_PARAMS_MAP.get(ceilLogN);
                    default:
                        throw new IllegalArgumentException(String.format(
                            "Invalid %s: %s", BspCotType.class.getSimpleName(), bcg19RegBspCotType
                        ));
                }
            case YWL20_UNI:
                Ywl20UniMspCotConfig ywl20UniMspcotConfig = (Ywl20UniMspCotConfig) config;
                BspCotConfig ywl20UniBspSotConfig = ywl20UniMspcotConfig.getBspCotConfig();
                BspCotType ywl20UniBspCotType = ywl20UniBspSotConfig.getPtoType();
                switch (ywl20UniBspCotType) {
                    case YWL20_SEMI_HONEST:
                        return YWL20_SH_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.get(ceilLogN);
                    case YWL20_MALICIOUS:
                        return YWL20_MA_YWL20_UNI_ITERATION_LPN_PARAMS_MAP.get(ceilLogN);
                    default:
                        throw new IllegalArgumentException(String.format(
                            "Invalid %s: %s", BspCotType.class.getSimpleName(), ywl20UniBspCotType
                        ));
                }
            default:
                throw new IllegalArgumentException(String.format(
                    "Invalid %s: %s", MspCotType.class.getSimpleName(), mspCotType
                ));
        }
    }
}
