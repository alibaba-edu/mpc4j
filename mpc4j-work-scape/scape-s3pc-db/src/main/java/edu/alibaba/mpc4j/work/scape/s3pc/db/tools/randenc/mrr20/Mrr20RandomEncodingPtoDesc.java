package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.mrr20;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * The random encoding protocol
 * <p>
 * P. Mohassel, P. Rindal, and M. Rosulek
 * Fast databases and psi for secret shared data
 * Proceedings of the 2020 ACM SIGSAC Conference on Computer and Communications Security 2020 (CCS20)
 * </p>
 *
 * @author Feng Han
 * @date 2025/2/25
 */
public class Mrr20RandomEncodingPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) -852390696983171125L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "RANDOM_ENCODING_MRR20";

    /**
     * singleton mode
     */
    private static final Mrr20RandomEncodingPtoDesc INSTANCE = new Mrr20RandomEncodingPtoDesc();

    /**
     * private constructor
     */
    private Mrr20RandomEncodingPtoDesc() {
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
