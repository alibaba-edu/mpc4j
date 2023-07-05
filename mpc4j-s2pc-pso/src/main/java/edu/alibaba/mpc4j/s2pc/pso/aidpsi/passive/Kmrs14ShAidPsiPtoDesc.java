package edu.alibaba.mpc4j.s2pc.pso.aidpsi.passive;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KMRS14 semi-honest aid PSI protocol description. The protocol comes from Figure 2 of the following paper:
 * <p>
 * Kamara, Seny, Payman Mohassel, Mariana Raykova, and Saeed Sadeghian. Scaling private set intersection to
 * billion-element sets. FC 2014, pp. 195-215. Springer Berlin Heidelberg, 2014.
 * </p>
 * Theorem 1 shows the security result:
 * <p>
 * The protocol described in Fig. 1 is secure in the presence (1) a semi-honest server and honest parties or (2) a
 * honest server and any collusion of malicious parties.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
class Kmrs14ShAidPsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6768596528401856642L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "KMRS14_SEMI_HONEST_AID_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * sever sends T_S to aider.
         */
        SERVER_TO_AIDER_TS,
        /**
         * client sends T_C to aider.
         */
        CLIENT_TO_AIDER_TC,
        /**
         * aider sends T_I to server.
         */
        AIDER_TO_SERVER_T_I,
        /**
         * aider sends T_I to client.
         */
        AIDER_TO_CLIENT_T_I,
    }

    /**
     * singleton mode
     */
    private static final Kmrs14ShAidPsiPtoDesc INSTANCE = new Kmrs14ShAidPsiPtoDesc();

    /**
     * private constructor.
     */
    private Kmrs14ShAidPsiPtoDesc() {
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
