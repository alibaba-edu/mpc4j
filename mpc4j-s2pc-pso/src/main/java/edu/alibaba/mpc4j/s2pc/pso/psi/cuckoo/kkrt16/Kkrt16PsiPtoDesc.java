package edu.alibaba.mpc4j.s2pc.pso.psi.cuckoo.kkrt16;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * KKRT16-PSI protocol description. The protocol comes from the following paper:
 * <p>
 * Kolesnikov V, Kumaresan R, Rosulek M, et al. Efficient batched oblivious PRF with applications to private set
 * intersection. CCS 2016, ACM, 2016, pp. 818-829.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
class Kkrt16PsiPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7043357406784082959L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "KKRT16_PSI";

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
    private static final Kkrt16PsiPtoDesc INSTANCE = new Kkrt16PsiPtoDesc();

    /**
     * private constructor.
     */
    private Kkrt16PsiPtoDesc() {
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
