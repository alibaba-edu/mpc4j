package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.mrr20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * The pkpk equal-join protocol
 * The semi-honest scheme comes from the following paper:
 *
 * <p>
 * P. Mohassel, P. Rindal, and M. Rosulek
 * Fast databases and psi for secret shared data
 * Proceedings of the 2020 ACM SIGSAC Conference on Computer and Communications Security 2020 (CCS20)
 * </p>
 * <p>
 * the malicious version is the extended work of HZF22(Scape)
 *
 * @author Feng Han
 * @date 2024/03/08
 */
public class Mrr20PkPkJoinPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -552120047571441884L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "PKPK_JOIN_MRR20";

    /**
     * singleton mode
     */
    private static final Mrr20PkPkJoinPtoDesc INSTANCE = new Mrr20PkPkJoinPtoDesc();

    /**
     * private constructor
     */
    private Mrr20PkPkJoinPtoDesc() {
        // empty
    }

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * send and receive the hash success sign
         */
        HASH_SUCCESS_SIGN,
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
