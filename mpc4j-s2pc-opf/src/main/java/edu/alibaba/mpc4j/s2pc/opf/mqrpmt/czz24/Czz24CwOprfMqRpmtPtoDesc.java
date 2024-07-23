package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * CZZ24 cwOPRF-based mqRPMT. The protocol comes from the following paper:
 * <p>
 * Yu Chen, Min Zhang, Cong Zhang, Minglang Dong, and Weiran Liu. Private set operations from multi-query reverse
 * private membership test. PKC 2024, pp. 387-416. Cham: Springer Nature Switzerland, 2024.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/9/10
 */
class Czz24CwOprfMqRpmtPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7216266257988855911L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CZZ24_cwOPRF_mqRPMT";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * client sends H(Y)^β
         */
        CLIENT_SEND_HY_BETA,
        /**
         * server sends H(X)^α
         */
        SERVER_SEND_HX_ALPHA,
        /**
         * server sends H(Y)^βα
         */
        CLIENT_SEND_HY_BETA_ALPHA,
    }

    /**
     * singleton mode
     */
    private static final Czz24CwOprfMqRpmtPtoDesc INSTANCE = new Czz24CwOprfMqRpmtPtoDesc();

    /**
     * private constructor
     */
    private Czz24CwOprfMqRpmtPtoDesc() {
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
     * Gets PEQT byte length: σ + log_2(serverSize) + long(clientSize).
     *
     * @param serverElementSize server element size.
     * @param clientElementSize client element size.
     * @return PEQT byte length.
     */
    static int getPeqtByteLength(int serverElementSize, int clientElementSize) {
        return CommonConstants.STATS_BYTE_LENGTH
            + CommonUtils.getByteLength(LongUtils.ceilLog2(serverElementSize))
            + CommonUtils.getByteLength(LongUtils.ceilLog2(clientElementSize));
    }
}
