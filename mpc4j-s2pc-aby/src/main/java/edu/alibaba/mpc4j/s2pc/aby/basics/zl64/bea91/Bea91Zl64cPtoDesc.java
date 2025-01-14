package edu.alibaba.mpc4j.s2pc.aby.basics.zl64.bea91;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Bea91 Zl circuit protocol description. The protocol implements multiplication using Beaver's triple presented
 * in the following paper:
 * <p>
 * Beaver, Donald. Efficient multiparty protocols using circuit randomization. CRYPTO 1991, pp. 420-432. Springer,
 * Berlin, Heidelberg, 1991.
 * </p>
 *
 * @author Li Peng
 * @date 2024/7/23
 */
class Bea91Zl64cPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 123498850831013577L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "BEA91_ZL64";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * the sender sends the share
         */
        SENDER_SEND_INPUT_SHARE,
        /**
         * the receiver sends the share
         */
        RECEIVER_SEND_INPUT_SHARE,
        /**
         * the sender sends e0 and f0
         */
        SENDER_SEND_E0_F0,
        /**
         * the receiver sends e1 and f1
         */
        RECEIVER_SEND_E1_F1,
        /**
         * the sender sends the output share
         */
        SENDER_SEND_OUTPUT_SHARE,
        /**
         * the receiver sends the output share
         */
        RECEIVER_SEND_OUTPUT_SHARE
    }

    /**
     * singleton mode
     */
    private static final Bea91Zl64cPtoDesc INSTANCE = new Bea91Zl64cPtoDesc();

    /**
     * private constructor
     */
    private Bea91Zl64cPtoDesc() {
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
