package edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.psz14;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * PSZ14-PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Benny Pinkas and Thomas Schneider and Michael Zohner Faster Private Set Intersection Based on OT Extension.
 * USENIX Security 2014, pp. 797--812.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/9/18
 */
class Psz14PsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 6557483477744526789L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PSZ14_PSI";

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
    private static final Psz14PsiPtoDesc INSTANCE = new Psz14PsiPtoDesc();

    /**
     * private constructor.
     */
    private Psz14PsiPtoDesc() {
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
