package edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.oos17;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * OOS17-PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Orrù, Michele, Emmanuela Orsini, and Peter Scholl. Actively secure 1-out-of-N OT extension with application to
 * private set intersection. CT-RSA 2017, pp. 381-396. Springer International Publishing, 2017.
 * </p>
 * The paper mentioned that the construction can be used to construct more efficient (semi-honest) PSI.
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
class Oos17PsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3132310804096938310L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "OOS17_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * client sends cuckoo hash keys
         */
        CLIENT_SEND_CUCKOO_HASH_KEYS,
        /**
         * server sends PRFs in bins
         */
        SERVER_SEND_BIN_PRFS,
        /**
         * server sends PRFs in stashes
         */
        SERVER_SEND_STASH_PRFS,
    }

    /**
     * singleton mode
     */
    private static final Oos17PsiPtoDesc INSTANCE = new Oos17PsiPtoDesc();

    /**
     * private constructor.
     */
    private Oos17PsiPtoDesc() {
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
