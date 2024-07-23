package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

/**
 * GMR21-mqRPMT protocol description. The protocol comes from the following paper.
 * <p>
 * Garimella G, Mohassel P, Rosulek M, et al. Private Set Operations from Oblivious Switching. PKC 2021, Springer,
 * Cham, pp. 591-617.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
class Gmr21MqRpmtPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8473841492126133075L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "GMR21_mqRPMT";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends keys
         */
        SERVER_SEND_KEYS,
        /**
         * server sends cuckoo hash keys
         */
        SERVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * client sends OKVS
         */
        CLIENT_SEND_OKVS,
        /**
         * server sends a'
         */
        SERVER_SEND_A_PRIME_OPRFS,
    }

    /**
     * singleton mode
     */
    private static final Gmr21MqRpmtPtoDesc INSTANCE = new Gmr21MqRpmtPtoDesc();

    /**
     * private constructor
     */
    private Gmr21MqRpmtPtoDesc() {
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
     * finite field byte length
     */
    static final int FINITE_FIELD_BYTE_LENGTH = Long.BYTES;

    /**
     * Gets PEQT byte length: σ + 2 * log_2(binNum).
     *
     * @param binNum 桶数量（β）。
     * @return PEQT协议对比长度。
     */
    static int getPeqtByteLength(int binNum) {
        return CommonConstants.STATS_BYTE_LENGTH + CommonUtils.getByteLength(2 * (LongUtils.ceilLog2(binNum)));
    }
}
