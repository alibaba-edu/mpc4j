package edu.alibaba.mpc4j.s2pc.pso.psi.other.dcw13;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * DCW13-PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Dong, Changyu, Liqun Chen, and Zikai Wen. When private set intersection meets big data: an efficient and scalable
 * protocol. CCS 2013, pp. 789-800.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
class Dcw13PsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 8635439476687893783L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "DCW13_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * receiver sends GBF key
         */
        CLIENT_SEND_GBF_KEY,
        /**
         * server sends PRF filter
         */
        SERVER_SEND_PRF_FILTER,
    }

    /**
     * singleton mode
     */
    private static final Dcw13PsiPtoDesc INSTANCE = new Dcw13PsiPtoDesc();

    /**
     * private constructor.
     */
    private Dcw13PsiPtoDesc() {
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
