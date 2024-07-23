package edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

/**
 * Label PSI standard KSPIR protocol description. The protocol comes from the following paper:
 * <p>
 * Kelong Cong, Radames Cruz Moreno, Mariana Botelho da Gama, Wei Dai, Ilia Iliashenko, Kim Laine, and Michael
 * Rosenberg. Labeled psi from homomorphic encryption with reduced computation and communication. ACM CCS 2021, pp.
 * 1135-1150. 2021.
 * </p>
 *
 * @author Liqiang Peng
 * @date 2022/6/20
 */
public class LabelpsiStdKsPirPtoDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7261080771728862744L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LABEL_PSI";

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server send cuckoo hash keys
         */
        SERVER_SEND_CUCKOO_HASH_KEYS,
        /**
         * client send encryption params
         */
        CLIENT_SEND_PUBLIC_KEYS,
        /**
         * client send query
         */
        CLIENT_SEND_QUERY,
        /**
         * server send item response
         */
        SERVER_SEND_ITEM_RESPONSE,
        /**
         * server send label response
         */
        SERVER_SEND_LABEL_RESPONSE,
        /**
         * client send blind
         */
        CLIENT_SEND_BLIND,
        /**
         * server send blind prf
         */
        SERVER_SEND_BLIND_PRF,
    }

    /**
     * the singleton mode
     */
    private static final LabelpsiStdKsPirPtoDesc INSTANCE = new LabelpsiStdKsPirPtoDesc();

    /**
     * private constructor.
     */
    private LabelpsiStdKsPirPtoDesc() {
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
