package edu.alibaba.mpc4j.s2pc.pjc.pid.czz24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;


/**
 * CZZ24 PID protocol description. The protocol comes from the following paper:
 * <p>
 * Yu Chen, Min Zhang, Cong Zhang, Minglang Dong, and Weiran Liu. Private set operations from multi-query reverse
 * private membership test. PKC 2024, pp. 387-416. Cham: Springer Nature Switzerland, 2024.
 * </p>
 *
 * @author Yufei Wang
 * @date 2023/7/26
 */
class Czz24PidPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 2745659961904676027L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CZZ24_PID";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends keys
         */
        SERVER_SEND_KEYS,
        /**
         * client sends keys
         */
        CLIENT_SEND_KEYS,
        /**
         * server sends cuckoo hash keys
         */
        SERVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * client sends OKVS
         */
        CLIENT_SEND_OKVS,
        /**
         * client sends cuckoo hash keys
         */
        CLIENT_SEND_CUCKOO_HASH_KEYS,
        /**
         * server sends OKVS
         */
        SERVER_SEND_OKVS,
        /**
         * client sends union
         */
        CLIENT_SEND_UNION,
    }

    /**
     * singleton mode
     */
    private static final Czz24PidPtoDesc INSTANCE = new Czz24PidPtoDesc();

    /**
     * private constructor
     */
    private Czz24PidPtoDesc() {
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
}
