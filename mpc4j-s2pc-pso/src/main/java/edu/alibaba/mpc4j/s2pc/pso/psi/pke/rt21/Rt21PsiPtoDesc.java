package edu.alibaba.mpc4j.s2pc.pso.psi.pke.rt21;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * RT21-PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Mike Rosulek and Ni Trieu. 2021. Compact and Malicious Private Set Intersection for Small Sets. CCS 2021.
 * </p>
 *
 * @author Ziyuan Liang, Feng Han
 * @date 2023/8/10
 */
class Rt21PsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 600495959111237630L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RT21_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends msg(a)
         */
        SERVER_SEND_INIT,
        /**
         * client sends OKVS key
         */
        CLIENT_SEND_OKVS_KEY,
        /**
         * client sends OKVS
         */
        CLIENT_SEND_OKVS,
        /**
         * server sends K
         */
        SERVER_SEND_KS,
    }

    /**
     * singleton mode
     */
    private static final Rt21PsiPtoDesc INSTANCE = new Rt21PsiPtoDesc();

    /**
     * private constructor.
     */
    private Rt21PsiPtoDesc() {
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
     * field bit length
     */
    static final int FIELD_BIT_LENGTH = 256;
    /**
     * field byte length
     */
    static final int FIELD_BYTE_LENGTH = FIELD_BIT_LENGTH / Byte.SIZE;
}
