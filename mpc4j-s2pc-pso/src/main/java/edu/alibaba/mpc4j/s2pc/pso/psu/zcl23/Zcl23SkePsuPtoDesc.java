package edu.alibaba.mpc4j.s2pc.pso.psu.zcl23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * ZCL23-SKE-PSU protocol. The protocol comes from the following paper:
 * <p></p>
 * Zhang, Cong, Yu Chen, Weiran Liu, Min Zhang, and Dongdai Lin. Linear Private Set Union from Multi-Query Reverse
 * Private Membership Test. USENIX Security 2023, pp. 337-354. 2023.
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
class Zcl23SkePsuPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 9138205311944704383L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "ZCL23_SKE_PSU";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends DOKVS keys
         */
        SERVER_SEND_DOKVS_KEYS,
        /**
         * client sends DOKVS
         */
        CLIENT_SEND_DOKVS,
        /**
         * server sends PETQ shares
         */
        SERVER_SEND_PEQT_SHARES,
        /**
         * server sends encrypted elements
         */
        SERVER_SEND_ENC_ELEMENTS,
    }

    /**
     * singleton mode
     */
    private static final Zcl23SkePsuPtoDesc INSTANCE = new Zcl23SkePsuPtoDesc();

    /**
     * private constructor.
     */
    private Zcl23SkePsuPtoDesc() {
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
